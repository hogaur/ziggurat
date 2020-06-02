(ns ziggurat.messaging.impl.rabbitmq.create-rabbitmq-queues
  (:require [clojure.tools.logging :as log]
            [langohr.basic :as lb]
            [langohr.channel :as lch]
            [langohr.exchange :as le]
            [langohr.queue :as lq]
            [sentry-clj.async :as sentry]
            [taoensso.nippy :as nippy]
            [ziggurat.config :refer [ziggurat-config rabbitmq-config channel-retry-config]]
            [ziggurat.messaging.connection :refer [connection is-connection-required?]]
            [ziggurat.messaging.util :refer :all]
            [ziggurat.retry :refer [with-retry]]
            [ziggurat.sentry :refer [sentry-reporter]]))

(def MAX_EXPONENTIAL_RETRIES 25)

(defn delay-queue-name [topic-entity queue-name]
  (prefixed-queue-name topic-entity queue-name))

(defn- create-queue [queue props ch]
  (lq/declare ch queue {:durable true :arguments props :auto-delete false})
  (log/info "Created queue - " queue))

(defn- declare-exchange [ch exchange]
  (le/declare ch exchange "fanout" {:durable true :auto-delete false})
  (log/info "Declared exchange - " exchange))

(defn- bind-queue-to-exchange [ch queue exchange]
  (lq/bind ch queue exchange)
  (log/infof "Bound queue %s to exchange %s" queue exchange))

(defn- create-and-bind-queue
  ([queue-name exchange]
   (create-and-bind-queue queue-name exchange nil))
  ([queue-name exchange-name dead-letter-exchange]
   (try
     (let [props (if dead-letter-exchange
                   {"x-dead-letter-exchange" dead-letter-exchange}
                   {})]
       (let [ch (lch/open connection)]
         (create-queue queue-name props ch)
         (declare-exchange ch exchange-name)
         (bind-queue-to-exchange ch queue-name exchange-name)))
     (catch Exception e
       (sentry/report-error sentry-reporter e "Error while declaring RabbitMQ queues")
       (throw e)))))

(defn- retry-type []
  (-> (ziggurat-config) :retry :type))

(defn- channel-retries-enabled [topic-entity channel]
  (:enabled (channel-retry-config topic-entity channel)))

(defn- channel-retry-type [topic-entity channel]
  (:type (channel-retry-config topic-entity channel)))

(defn- get-channel-retry-count [topic-entity channel]
  (:count (channel-retry-config topic-entity channel)))

(defn- make-delay-queue [topic-entity]
  (let [{:keys [queue-name exchange-name dead-letter-exchange]} (:delay (rabbitmq-config))
        queue-name                (delay-queue-name topic-entity queue-name)
        exchange-name             (prefixed-queue-name topic-entity exchange-name)
        dead-letter-exchange-name (prefixed-queue-name topic-entity dead-letter-exchange)]
    (create-and-bind-queue queue-name exchange-name dead-letter-exchange-name)))

(defn- make-delay-queue-with-retry-count [topic-entity retry-count]
  (let [{:keys [queue-name exchange-name dead-letter-exchange]} (:delay (rabbitmq-config))
        queue-name                (delay-queue-name topic-entity queue-name)
        exchange-name             (prefixed-queue-name topic-entity exchange-name)
        dead-letter-exchange-name (prefixed-queue-name topic-entity dead-letter-exchange)
        sequence                  (min MAX_EXPONENTIAL_RETRIES (inc retry-count))]
    (doseq [s (range 1 sequence)]
      (create-and-bind-queue (prefixed-queue-name queue-name s) (prefixed-queue-name exchange-name s) dead-letter-exchange-name))))

(defn- make-channel-delay-queue-with-retry-count [topic-entity channel retry-count]
  (make-delay-queue-with-retry-count (with-channel-name topic-entity channel) retry-count))

(defn- make-channel-delay-queue [topic-entity channel]
  (make-delay-queue (with-channel-name topic-entity channel)))

(defn- make-queue [topic-identifier queue-type]
  (let [{:keys [queue-name exchange-name]} (queue-type (rabbitmq-config))
        queue-name    (prefixed-queue-name topic-identifier queue-name)
        exchange-name (prefixed-queue-name topic-identifier exchange-name)]
    (create-and-bind-queue queue-name exchange-name)))

(defn- make-channel-queue [topic-entity channel-name queue-type]
  (make-queue (with-channel-name topic-entity channel-name) queue-type))

(defn- make-channel-queues [channels topic-entity]
  (doseq [channel channels]
    (make-channel-queue topic-entity channel :instant)
    (when (channel-retries-enabled topic-entity channel)
      (make-channel-queue topic-entity channel :dead-letter)
      (let [channel-retry-type (channel-retry-type topic-entity channel)]
        (cond
          (= :exponential channel-retry-type) (do
                                                (log/warn "[Alpha Feature]: Exponential backoff based retries is an alpha feature."
                                                          "Please use it only after understanding its risks and implications."
                                                          "Its contract can change in the future releases of Ziggurat.")
                                                (make-channel-delay-queue-with-retry-count topic-entity channel (get-channel-retry-count topic-entity channel)))
          (= :linear channel-retry-type) (make-channel-delay-queue topic-entity channel)
          (nil? channel-retry-type) (do
                                      (log/warn "[Deprecation Notice]: Please note that the configuration for channel retries has changed."
                                                "Please look at the upgrade guide for details: https://github.com/gojek/ziggurat/wiki/Upgrade-guide"
                                                "Use :type to specify the type of retry mechanism in the channel config.")
                                      (make-channel-delay-queue topic-entity channel))
          :else (do
                  (log/warn "Incorrect keyword for type passed, falling back to linear backoff for channel: " channel)
                  (make-channel-delay-queue topic-entity channel)))))))

(defn make-queues [stream-routes]
  (when (is-connection-required?)
    (println "Connection required is TRUE")
    (println stream-routes)
    (doseq [topic-entity (keys stream-routes)]
      (let [channels   (get-channel-names stream-routes topic-entity)
            retry-type (retry-type)]
        (println "Channels => " channels)
        (println "topic-entity => " topic-entity)
        (make-channel-queues channels topic-entity)
        (when (-> (ziggurat-config) :retry :enabled)
          (println "retry is enabled")
          (make-queue topic-entity :instant)
          (make-queue topic-entity :dead-letter)
          (cond
            (= :exponential retry-type) (do
                                          (log/warn "[Alpha Feature]: Exponential backoff based retries is an alpha feature."
                                                    "Please use it only after understanding its risks and implications."
                                                    "Its contract can change in the future releases of Ziggurat.")
                                          (make-delay-queue-with-retry-count topic-entity (-> (ziggurat-config) :retry :count)))
            (= :linear retry-type) (make-delay-queue topic-entity)
            (nil? retry-type) (do
                                (log/warn "[Deprecation Notice]: Please note that the configuration for retries has changed."
                                          "Please look at the upgrade guide for details: https://github.com/gojek/ziggurat/wiki/Upgrade-guide"
                                          "Use :type to specify the type of retry mechanism in the config.")
                                (make-delay-queue topic-entity))
            :else (do
                    (log/warn "Incorrect keyword for type passed, falling back to linear backoff for topic Entity: " topic-entity)
                    (make-delay-queue topic-entity))))))))


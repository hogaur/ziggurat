(ns ziggurat.messaging.impl.rabbitmq.start-subscribers
  (:require [clojure.tools.logging :as log]
            [langohr.basic :as lb]
            [langohr.channel :as lch]
            [langohr.consumers :as lcons]
            [ziggurat.config :refer [get-in-config]]
            [ziggurat.mapper :as mpr]
            [ziggurat.messaging.impl.rabbitmq.connection :refer [connection]]
            [ziggurat.sentry :refer [sentry-reporter]]
            [ziggurat.messaging.util :refer :all]
            [ziggurat.metrics :as metrics]
            [sentry-clj.async :as sentry]
            [taoensso.nippy :as nippy]
            [schema.core :as s]))

(defn- convert-to-message-payload
  "This function is used for migration from Ziggurat Version 2.x to 3.x. It checks if the message is a message payload or a message(pushed by Ziggurat version < 3.0.0) and converts messages to
   message-payload to pass onto the mapper-fn.

   If the `:retry-count` key is absent in the `message`, then it puts `0` as the value for `:retry-count` in `MessagePayload`.
   It also converts the topic-entity into a keyword while constructing MessagePayload."
  [message topic-entity]
  (try
    (s/validate mpr/message-payload-schema message)
    (catch Exception e
      (log/info "old message format read, converting to message-payload: " message)
      (let [retry-count (or (:retry-count message) 0)
            message-payload (mpr/->MessagePayload (dissoc message :retry-count) (keyword topic-entity))]
        (assoc message-payload :retry-count retry-count)))))

(defn convert-and-ack-message
  "De-serializes the message payload (`payload`) using `nippy/thaw` and converts it to `MessagePayload`. Acks the message
  if `ack?` is true."
  [ch {:keys [delivery-tag] :as meta} ^bytes payload ack? topic-entity]
  (try
    (let [message (nippy/thaw payload)]
      (when ack?
        (lb/ack ch delivery-tag))
      (convert-to-message-payload message topic-entity))
    (catch Exception e
      (lb/reject ch delivery-tag false)
      (sentry/report-error sentry-reporter e "Error while decoding message")
      (metrics/increment-count ["rabbitmq-message" "conversion"] "failure" {:topic_name (name topic-entity)})
      nil)))

(defn- ack-message
  [ch delivery-tag]
  (lb/ack ch delivery-tag))

(defn process-message-from-queue [ch meta payload topic-entity processing-fn]
  (let [delivery-tag (:delivery-tag meta)
        message-payload      (convert-and-ack-message ch meta payload false topic-entity)]
    (when message-payload
      (log/infof "Processing message [%s] from RabbitMQ " message-payload)
      (try
        (log/debug "Calling processor-fn with the message-payload - " message-payload " with retry count - " (:retry-count message-payload))
        (processing-fn message-payload)
        (ack-message ch delivery-tag)
        (catch Exception e
          (lb/reject ch delivery-tag true)
          (sentry/report-error sentry-reporter e "Error while processing message-payload from RabbitMQ")
          (metrics/increment-count ["rabbitmq-message" "process"] "failure" {:topic_name (name topic-entity)}))))))

(defn- message-handler [wrapped-mapper-fn topic-entity]
  (fn [ch meta ^bytes payload]
    (process-message-from-queue ch meta payload topic-entity wrapped-mapper-fn)))

(defn- start-subscriber* [ch prefetch-count queue-name wrapped-mapper-fn topic-entity]
  (lb/qos ch prefetch-count)
  (let [consumer-tag (lcons/subscribe ch
                                      queue-name
                                      (message-handler #((println "Consuming message")) topic-entity)
                                      {:handle-shutdown-signal-fn (fn [consumer_tag reason]
                                                                    (log/infof "channel closed with consumer tag: %s, reason: %s " consumer_tag, reason))
                                       :handle-consume-ok-fn      (fn [consumer_tag]
                                                                    (log/infof "consumer started for %s with consumer tag %s " queue-name consumer_tag))})]))

(defn start-retry-subscriber* [mapper-fn topic-entity channels]
  (when (get-in-config [:retry :enabled])
    (dotimes [_ (get-in-config [:jobs :instant :worker-count])]
      (start-subscriber* (lch/open connection)
                         (get-in-config [:jobs :instant :prefetch-count])
                         (prefixed-queue-name topic-entity (get-in-config [:rabbit-mq :instant :queue-name]))
                         (mpr/mapper-func mapper-fn channels)
                         topic-entity))))

(defn start-channels-subscriber [channels topic-entity]
  (doseq [channel channels]
    (let [channel-key        (first channel)
          channel-handler-fn (second channel)]
      (dotimes [_ (get-in-config [:stream-router topic-entity :channels channel-key :worker-count])]
        (start-subscriber* (lch/open connection)
                           1
                           (prefixed-channel-name topic-entity channel-key (get-in-config [:rabbit-mq :instant :queue-name]))
                           (mpr/channel-mapper-func channel-handler-fn channel-key)
                           topic-entity)))))

(defn start-subscribers
  "Starts the subscriber to the instant queue of the rabbitmq"
  [stream-routes]
  (doseq [stream-route stream-routes]
    (let [topic-entity  (first stream-route)
          topic-handler (-> stream-route second :handler-fn)
          channels      (-> stream-route second (dissoc :handler-fn))]
      (start-channels-subscriber channels topic-entity)
      (start-retry-subscriber* topic-handler topic-entity (keys channels)))))

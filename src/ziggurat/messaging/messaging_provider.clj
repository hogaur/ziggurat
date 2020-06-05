(ns ziggurat.messaging.messaging-provider
  (:require [ziggurat.config :refer [statsd-config ziggurat-config]]
            [clojure.tools.logging :as log]
            [ziggurat.messaging.interface.producer]
            [ziggurat.messaging.interface.consumer]
            [mount.core :refer [defstate]]
            [ziggurat.messaging.impl.rabbitmq.connection :as rmq-conn]
            [ziggurat.tracer]
            [mount.core :as mount])
  (:import (ziggurat.messaging.interface.producer Producer)
           (ziggurat.messaging.interface.consumer Consumer)))

(defn get-implementation-class [class-config-keys]
  (if-let [constructor-clazz (get-in (ziggurat-config) class-config-keys)]
    (let [constructor-class-symbol (symbol constructor-clazz)
          _                        (require [(symbol (namespace constructor-class-symbol))])
          constructor              (resolve constructor-class-symbol)]

      (if (nil? constructor)
        (throw (ex-info (format "No implementation exists for the configured class: [%s] Please fix it." constructor-clazz) {:constructor-configured constructor-clazz}))
        constructor))
    nil))

(defn instantiate [class-config-keys]
  (if-let [impl-class (get-implementation-class class-config-keys)]
    (impl-class)
    (throw (ex-message (format "Class specified by the keys [%s] does not exist" class-config-keys)))))

(defstate producer
  :start (do (println "Initializing the Producer")
             (let [^Producer producer-impl (instantiate [:messaging-provider :producer-class])]
               (.initialize producer-impl (mount/args))
               producer-impl))
  :stop (do (log/info "Stopping the Producer")
            (.terminate producer)))

(defstate consumer
  :start (do (println "Initializing the Consumer")
             (let [^Consumer consumer-impl (instantiate [:messaging-provider :consumer-class])]
               (.initialize consumer-impl (mount/args))
               consumer-impl))
  :stop (do (log/info "Stopping the Consumer")
            (.terminate consumer)))
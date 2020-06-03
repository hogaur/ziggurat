(ns ziggurat.messaging.messaging-provider
  (:require [ziggurat.config :refer [statsd-config ziggurat-config]]
            [clojure.tools.logging :as log]
            [ziggurat.messaging.interface.producer :as producer-interface]
            [mount.core :refer [defstate]]
            [ziggurat.messaging.connection]

            [ziggurat.tracer]
            [mount.core :as mount])
  (:import (ziggurat.messaging.interface.producer Producer)))

(defn- get-implementation-class [class-config-keys]
  (if-let [constructor-clazz (get-in (ziggurat-config) class-config-keys)]
    (let [constructor-class-symbol (symbol constructor-clazz)
          _                        (require [(symbol (namespace constructor-class-symbol))])
          constructor              (resolve constructor-class-symbol)]

      (if (nil? constructor)
        (throw (ex-info (format "No implementation exists for the configured class: [%s] Please fix it." constructor-clazz) {:constructor-configured constructor-clazz}))
        constructor))
    nil))

(defn initialise-message-producer []
  (if-let [producer-impl-constructor (get-implementation-class [:messaging-provider :producer-class])]
    (producer-impl-constructor)
    (throw (ex-message "Messaging provider not configured."))))

;; config to be started first
;; tracer to be started
;; last connection to be started

;(defn start-required-states []
;  (do (-> [#'ziggurat.config/config
;           #'ziggurat.tracer/tracer]
;          (mount/with-args {:stream-routes {:booking {:handler-fn #()}}})
;          (mount/start)))
;  (mount/start #'ziggurat.messaging.connection/connection))

(defstate producer
  :start (do (println "Initializing the Producer")
             (let [^Producer producer-impl (initialise-message-producer)]
               (producer-interface/initialize producer-impl (mount/args))
               producer-impl))
  :stop (do (log/info "Stopping the Producer")
            (producer-interface/terminate producer)))
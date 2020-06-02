(ns ziggurat.messaging.messaging-provider
  (:require [ziggurat.config :refer [statsd-config ziggurat-config]]
            [clojure.tools.logging :as log]
            [mount.core :refer [defstate]])
  (:import (ziggurat.messaging.interface.producer Producer)))

(def ^{:private true} ^Producer producer-impl (atom nil))

(defn- get-implementation-class [class-config-keys]
  (if-let [constructor-clazz (get-in (ziggurat-config) class-config-keys)]
    (let  [constructor-class-symbol (symbol constructor-clazz)
          _                             (require [(symbol (namespace constructor-class-symbol))])
          constructor            (resolve constructor-class-symbol)]

      (if (nil? constructor)
        (throw (ex-info (format "No implementation exists for the configured class: [%s] Please fix it." constructor-clazz) {:constructor-configured constructor-clazz}))
        constructor))
    nil))

(defn initialise-message-producer []
  (if-let [producer-impl-constructor (get-implementation-class [:messaging-provider :producer-class])]
    (reset! producer-impl (producer-impl-constructor))
    (throw (ex-message "Messaging provider not configured."))))

(defstate producer
          :start (do (log/info "Initializing the Producer")
                     (initialise-message-producer)
                     (.initialize producer-impl)
                     producer-impl)
          :stop (do (log/info "Stopping the Producer")
                    (.terminate producer-impl)))
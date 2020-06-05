(ns ziggurat.messaging.impl.rabbitmq.rabbitmq-connection
  (:require [ziggurat.messaging.impl.rabbitmq.connection :as connection])
  (:import (com.rabbitmq.client AMQP$Connection)
           (io.opentracing.contrib.rabbitmq TracingConnection)))

(def ^{:private true} connection (atom nil))

(defn start-connection []
  (if (or (instance? AMQP$Connection @connection)
          (instance? TracingConnection @connection))
    @connection
    (do (reset! connection (connection/start-connection))
        @connection)))

(defn stop-connection []
  (do
    (connection/stop-connection connection)
    (reset! connection nil)))

(defn get-conn [] @connection)
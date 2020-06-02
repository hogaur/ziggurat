(ns ziggurat.rabbitmq-retry-wrapper
  (:require [ziggurat.messaging.connection :as conn])
  (:import (ziggurat.retry_interface RetryProtocol)))

(def rabbitmq-connection (atom nil))

(defn start-rabbitmq-connection []
  (reset! rabbitmq-connection (conn/start-connection)))

(defn stop-rabbitmq-connection []
  (conn/stop-connection rabbitmq-connection))

(deftype RabbitMQRetry []
  RetryProtocol
  (initialize [this] (start-rabbitmq-connection))
  (cleanup [this]) (start-rabbitmq-connection))
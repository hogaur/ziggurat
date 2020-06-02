(ns ziggurat.rabbitmq-retry-wrapper
  (:import (ziggurat.retry_interface RetryProtocol)))


(deftype RabbitMQRetry []
  RetryProtocol)
(ns ziggurat.retry-interface
  (:import (ziggurat.mapper MessagePayload)))

(defprotocol RetryProtocol
  "A Protocol that defines the interface for retries in Ziggurat using message queue implementation."
  (initialize [impl rabbitmq-config])
  (cleanup [impl])
  (retry [impl queue-name queue-type message-payload])
  (retry [impl channel-name queue-name queue-type message-payload])
  (start-consumers [impl stream-routes]))
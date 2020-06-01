(ns ziggurat.retry-interface
  (:import (ziggurat.mapper MessagePayload)))

(defprotocol RetryProtocol
  (initialize [impl rabbitmq-config])
  (cleanup [impl])
  (publish-to-queue [impl queue-name queue-type message-payload])
  (publish-to-channel-queue [impl channel-name queue-name queue-type message-payload]))
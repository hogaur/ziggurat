(ns ziggurat.retry-interface)

(defprotocol RetryProtocol
  "A Protocol that defines the interface for retries in Ziggurat using message queue implementation."
  (initialize [impl])
  (cleanup [impl])
  (retry
    [impl message-payload queue-name queue-type]
    [impl message-payload queue-name queue-type channel-name])
  (start-consumers [impl stream-routes]))
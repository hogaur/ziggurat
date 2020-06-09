(ns ziggurat.messaging.interface.producer)

(defprotocol Producer
  "A protocol which defines Producer interface for publishing messages to message queues"
  (initialize [impl args])
  (terminate  [impl])
  (retry [impl message-payload])
  (publish [impl message topic-entity]))


(ns ziggurat.messaging.interface.producer)

(defprotocol Producer
  "A protocol which defines Producer interface for publishing messages to message queues"
  (initialize [impl args])
  (terminate  [impl])
  (publish [impl message destination] [impl message destination delay]))



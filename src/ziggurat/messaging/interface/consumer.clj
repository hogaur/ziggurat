(ns ziggurat.messaging.interface.consumer)

(defprotocol Consumer
  "A protocol which defines Consumer interface for consuming messages to message queues"
  (initialize [impl args])
  (terminate  [impl])
  (read [impl message source] [impl message source ack?])
  (process [impl message source fn] [impl message source fn ack?]))



(ns ziggurat.messaging.impl.rabbitmq.rabbitmq-channel
  (:require [ziggurat.messaging.impl.rabbitmq.publish :as pub]
            [ziggurat.messaging.impl.rabbitmq.connection :as rmq-conn])
  (:import (ziggurat.messaging.interface.channel Channel)))

(deftype RabbitMQChannel []
  Channel
  (initialize [impl args] (rmq-conn/initialize-connection))
  (publish [impl message-payload channel-name]
    (pub/publish-to-channel-instant-queue channel-name message-payload))
  (retry [impl message-payload channel-name]
    (pub/retry-for-channel message-payload channel-name))
  (terminate [impl]
    (let [conn (rmq-conn/get-connection)]
      (when-not (nil? conn)
        (rmq-conn/stop-connection conn)))))

(ns ziggurat.messaging.impl.rabbitmq.rabbitmq-producer
  (:require [ziggurat.messaging.impl.rabbitmq.create-queues :as queues]
            [ziggurat.messaging.impl.rabbitmq.publish :as pub]
            [ziggurat.messaging.impl.rabbitmq.connection :as rmq-conn]
            [ziggurat.messaging.connection]
            [ziggurat.tracer]
            [mount.core :as mount])
  (:import (ziggurat.messaging.interface.producer Producer)))

(deftype RabbitMQProducer []
  Producer
  (initialize [this args] (do (rmq-conn/initialize-connection)
                              (queues/make-queues (:stream-routes args))))
  (terminate [this]
    (let [conn (rmq-conn/get-connection)]
      (when-not (nil? conn)
        (rmq-conn/stop-connection conn))))
  (publish [this message destination delay]
    (pub/publish-message message destination delay))
  (publish [this message destination]
    (pub/publish-message message destination)))
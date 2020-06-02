(ns ziggurat.messaging.impl.rabbitmq.rabbitmq-producer
  (:require [ziggurat.messaging.impl.rabbitmq.connection]
            [mount.core :as mount])
  (:import (ziggurat.messaging.interface.producer Producer)))

(deftype RabbitMQProducer []
  Producer
  (initialize [this] #(println "I'm initializing"))
    ;(mount/start connection)
    ;(make-queues))
  (terminate [this]
    #(println "I'm terminating"))
  (publish [this message-payload]
    #(println "I'm publishing")))
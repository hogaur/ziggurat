(ns ziggurat.messaging.impl.rabbitmq.rabbitmq-producer
  (:require [ziggurat.messaging.impl.rabbitmq.connection]
            [ziggurat.messaging.impl.rabbitmq.create-rabbitmq-queues :as queues]
            [ziggurat.messaging.impl.rabbitmq.publish :as pub]
            [ziggurat.messaging.impl.rabbitmq.connection :as rmq-conn]
            [ziggurat.config :as config]
            [ziggurat.messaging.connection]
            [ziggurat.tracer]
            [mount.core :as mount])
  (:import (ziggurat.messaging.interface.producer Producer)))




(deftype RabbitMQProducer []
  Producer
  (initialize [this args] (do (println "I'm initializing")
                              (println "args => " args)
                              (queues/make-queues (:stream-routes args))))

  (retry [this message-payload]
    (println "retrying message")
    (pub/retry message-payload))
  (terminate [this]
    (println "I'm terminating"))
  (publish [this message-payload return-code]
    (println "I'm publishing")))
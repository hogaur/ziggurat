(ns ziggurat.messaging.impl.rabbitmq.rabbitmq-producer
  (:require [ziggurat.messaging.impl.rabbitmq.connection]
            [ziggurat.messaging.impl.rabbitmq.create-rabbitmq-queues :as queues]
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
  ;(mount/start connection)
  ;(make-queues))
  (terminate [this]
    (println "I'm terminating"))
  (publish [this message-payload]
    (println "I'm publishing")))
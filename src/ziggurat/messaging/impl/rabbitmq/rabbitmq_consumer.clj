(ns ziggurat.messaging.impl.rabbitmq.rabbitmq-consumer
  (:require [mount.core :as mount]
            [ziggurat.messaging.impl.rabbitmq.connection :as rmq-conn]
            [ziggurat.messaging.impl.rabbitmq.start-subscribers :as rmq-subs])
  (:import (ziggurat.messaging.interface.consumer Consumer)))

(deftype RabbitMQConsumer []
  Consumer
  (initialize [this args] (do (println "I'm initializing")
                              (println "args => " args)
                              (-> #'rmq-conn/connection
                                  (mount/with-args args)
                                  (mount/start))
                              (rmq-subs/start-subscribers (:stream-routes args))))
  (terminate [this]
    (do (println "I'm terminating")
        (mount/stop #'rmq-conn/connection)))
  (read [impl message source]
    #())
  (read [impl message source ack?]
    #())
  (process [impl message source fn]
    #())
  (process [impl message source fn ack?]
    #()))
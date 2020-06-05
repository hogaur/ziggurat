(ns ziggurat.messaging.impl.rabbitmq.rabbitmq-consumer
  (:require [mount.core :as mount]
            [ziggurat.messaging.impl.rabbitmq.connection :as rmq-conn]
            [ziggurat.messaging.impl.rabbitmq.start-subscribers :as rmq-subs])
  (:import (ziggurat.messaging.interface.consumer Consumer)))

(deftype RabbitMQConsumer []
  Consumer
  (initialize [this args] (do
                            (rmq-conn/initialize-connection)
                            (rmq-subs/start-subscribers (:stream-routes args))))
  (terminate [this]
    (let [conn (rmq-conn/get-connection)]
      (when-not (nil? conn)
        (rmq-conn/stop-connection conn))))
  (read [impl message source]
    #())
  (read [impl message source ack?]
    #())
  (process [impl message source fn]
    #())
  (process [impl message source fn ack?]
    #()))
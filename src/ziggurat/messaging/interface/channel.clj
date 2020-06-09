(ns ziggurat.messaging.interface.channel)

(defprotocol Channel
  (initialize [impl args])
  (publish [impl message-payload channel-name])
  (retry [impl message-payload channel-name])
  (terminate [impl]))


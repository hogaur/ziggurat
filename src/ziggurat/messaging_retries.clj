(ns ziggurat.messaging-retries
  (:require [mount.core :refer [defstate]]
            [clojure.tools.logging :as log]
            [ziggurat.retry-interface :as retry-interface]))

(def retry-impl (atom nil))
(defn initialize-retry-library [])

(defstate retry-backend
  :start (do (log/info "Initializing retries")
             (initialize-retry-library)
             (retry-interface/initialize @retry-impl))
  :stop (do (log/info "Stopping retries")
            (retry-interface/cleanup @retry-impl)))


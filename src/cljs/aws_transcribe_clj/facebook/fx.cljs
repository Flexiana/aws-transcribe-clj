(ns aws-transcribe-clj.facebook.fx
  (:require [oops.core :refer [ocall!]]
            [re-frame.core :as rf]
            [aws-transcribe-clj.config :as config]
            [aws-transcribe-clj.facebook.sdk :as fb]))

(rf/reg-fx
  ::load-sdk!
  (fn [{:keys [callback]}]
    (fb/load-sdk
      (clj->js {:appId   config/FB-APP-ID
                :cookie  true
                :xfbml   true
                :version config/FB-SDK-VERSION})
      callback)))

(rf/reg-fx
  ::xfbml-parse!
  (fn [id]
    (when (fb/sdk-ready?)
      (fb/xfbml-parse
        (ocall! js/document :getElementById id)))))
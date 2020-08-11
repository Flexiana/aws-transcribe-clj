(ns aws-transcribe-clj.facebook.sdk
  (:require [oops.core :refer [oget oset! ocall!]]
            [aws-transcribe-clj.config :as config]
            [taoensso.timbre :as log]))

(defn sdk-ready? []
  (some? (oget js/window :?FB)))

(defn init
  [params callback]
  (fn []
    (ocall! js/window :FB.init params)
    (callback)))

(defn load-sdk [params callback]
  (let [doc js/document
        uid "fb-sdk-cljs"]
    (when-not (ocall! doc :getElementById uid)
      (aset js/window "fbAsyncInit" (init params callback))
      (let [script (ocall! doc :createElement "script")]
        (oset! script :id uid)
        (oset! script :async true)
        (oset! script :src
               (str "https://connect.facebook.net/en_US/sdk.js"
                    "#version=" (log/spy :debug config/FB-SDK-VERSION)
                    "&appId="   (log/spy :debug config/FB-APP-ID)
                    "&autoLogAppEvents=1"))
        (let [fst-js (-> doc
                         (ocall! :getElementsByTagName "script")
                         (aget 0))
              parent (oget fst-js :parentNode)]
          (ocall! parent :insertBefore script fst-js))))))

(defn subscribe-event
  [event-name callback-fn]
  (ocall! js/window :FB.Event.subscribe event-name callback-fn))

(defn xfbml-parse
  ([]     (ocall! js/window :FB.XFBML.parse))
  ([elem] (ocall! js/window :FB.XFBML.parse elem)))

(defn login [callback]
  (ocall! js/window :FB.login callback))

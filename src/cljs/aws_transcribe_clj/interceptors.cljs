(ns aws-transcribe-clj.interceptors
  (:require [re-frame.core :as rf]))

(def clojurize-event-data
  (rf/->interceptor
    :id     ::clojurize-event-data
    :before (fn [context]
              (update-in context [:coeffects :event]
                         (fn [event]
                           (mapv #(js->clj % :keywordize-keys true) event))))))
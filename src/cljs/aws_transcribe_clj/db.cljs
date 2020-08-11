(ns aws-transcribe-clj.db
  (:require [re-frame.core :as rf]))

(defn default-db [] {:route {:data {:name :loading}}})

(rf/reg-event-db
  ::initialize-db
  (fn [_ _]
    (default-db)))

(ns aws-transcribe-clj.transcribe.controllers
  (:require [re-frame.core :as rf]
            [aws-transcribe-clj.transcribe.events :as e]))


(defn start
  [params]
  (fn [match]
    (when @(rf/subscribe (-> params :subs :authenticated?))
      (rf/dispatch [::e/set-up]))))

(defn stop
  ([] (stop {}))
  ([params]
   (fn [match]
     (rf/dispatch [::e/tear-down]))))
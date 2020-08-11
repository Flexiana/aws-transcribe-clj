(ns aws-transcribe-clj.routes
  (:require [re-frame.core :as rf]
            [reitit.frontend :as rfd]
            [reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfe]
            [aws-transcribe-clj.facebook.subs :as fsubs]
            [aws-transcribe-clj.transcribe.controllers :as tc]
            [aws-transcribe-clj.views :as v]))

(rf/reg-sub ::route #(:route %))

(rf/reg-fx
  ::navigate!
  (fn [[route params query]]
    (rfe/push-state route params query)))

(rf/reg-event-fx
  ::navigate
  [rf/trim-v]
  (fn [_ [route params query]]
    {::navigate! [route params query]}))

(rf/reg-event-db
  ::set-route
  [rf/trim-v]
  (fn [db [match]]
    (assoc db :route match)))

(defn on-navigate!
  [new-match history]
  (let [old-match   @(rf/subscribe [::route])
        controllers (rfc/apply-controllers (:controllers old-match) new-match)]
    (rf/dispatch [::set-route (when (some? new-match)
                                (assoc new-match :controllers controllers))])))

(defn wrap-auth [match]
  (let [authenticated? @(rf/subscribe [::fsubs/connected?])]
    (when-not authenticated?
      (rf/dispatch [::navigate :welcome]))))

(def router
  (rfd/router
    ["" {:controllers [{:identity identity
                        :start    wrap-auth}]}
     ["/"
      {:name :welcome
       :view #'v/welcome}]
     ["/transcribe"
      {:name        :transcribe
       :view        #'v/transcribe
       :controllers [{:identity identity
                      :start    (tc/start {:subs {:authenticated? [::fsubs/connected?]}})
                      :stop     (tc/stop)}]}]]
    {:default-view #'v/not-found}))

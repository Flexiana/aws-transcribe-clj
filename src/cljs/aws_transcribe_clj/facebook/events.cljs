(ns aws-transcribe-clj.facebook.events
  (:require [re-frame.core :as rf]
            [aws-transcribe-clj.facebook.fx :as fx]
            [aws-transcribe-clj.interceptors :as i]))

(rf/reg-event-fx
  ::load-sdk
  [rf/trim-v]
  (fn [_ [callback]]
    {::fx/load-sdk! {:callback callback}}))

(rf/reg-event-fx
  ::auth-response-changed
  [rf/trim-v i/clojurize-event-data]
  (fn [{:keys [db]} [{:keys [status] :as data} {:keys [on-connected on-disconnected]}]]
    (cond-> {:db (assoc-in db [:facebook :auth]
                           (merge (select-keys (:authResponse data) [:accessToken :userID])
                                  (select-keys data [:status])))}
      (and (= "connected" status) (seq on-connected)) (assoc :dispatch on-connected)
      (and (= "unknown" status) (seq on-disconnected)) (assoc :dispatch on-disconnected))))

(rf/reg-event-db
  ::activate-login
  (fn [db _]
    (assoc-in db [:facebook :login :ready?] true)))

(rf/reg-event-db
  ::deactivate-login
  (fn [db _]
    (assoc-in db [:facebook :login :ready?] false)))

(rf/reg-event-fx
  ::refresh-login-button
  [rf/trim-v]
  (fn [_ [container-id]]
    {::fx/xfbml-parse! container-id}))

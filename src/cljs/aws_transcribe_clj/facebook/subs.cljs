(ns aws-transcribe-clj.facebook.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  ::connected?
  (fn [db]
    (= "connected" (get-in db [:facebook :auth :status]))))

(rf/reg-sub
  ::login-ready?
  (fn [db]
    (get-in db [:facebook :login :ready?])))

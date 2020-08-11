(ns aws-transcribe-clj.core
  (:require [oops.core :refer [oget ocall!]]
            [reagent.core :as reagent]
            [re-frame.core :as rf]
            [reitit.core :as rt]
            [reitit.frontend.easy :as rfe]
            [taoensso.timbre :as log]
            [aws-transcribe-clj.aws.sdk :as aws]
            [aws-transcribe-clj.config :as c]
            [aws-transcribe-clj.db :as db]
            [aws-transcribe-clj.facebook.events :as fe]
            [aws-transcribe-clj.facebook.sdk :as fb]
            [aws-transcribe-clj.routes :as r]))

(defn init-facebook! []
  (rf/dispatch
    [::fe/load-sdk
     (fn []
       (fb/subscribe-event "auth.authResponseChange"
                           #(rf/dispatch [::fe/auth-response-changed %
                                          {:on-connected    [::r/navigate :transcribe]
                                           :on-disconnected [::r/navigate :welcome]}]))
       (fb/subscribe-event "xfbml.render"
                           #(rf/dispatch [::fe/activate-login %])))]))

(defn root-component
  [{:keys [router]}]
  (let [route @(rf/subscribe [::r/route])]
    [:div.container
     (if (some? route)
       (when-let [view (->> route :data :view)]
         [view])
       [(-> router rt/options :default-view)])]))

(defn dev-setup []
  (if-not c/debug?
    (log/set-level! :info)
    (do
      (enable-console-print!)
      (log/set-level! :debug)
      (println "dev mode"))))

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (reagent/render [root-component {:router r/router}] (ocall! js/document :getElementById "app")))

(defn ^:export init! []
  (dev-setup)
  (rf/dispatch-sync [::db/initialize-db])
  (rfe/start! r/router r/on-navigate! {:use-fragment false})
  (aws/set-region! c/AWS-REGION)
  (init-facebook!)
  (mount-root))

(ns aws-transcribe-clj.views
  (:require [re-frame.core :as rf]
            [aws-transcribe-clj.facebook.subs :as fsubs]
            [aws-transcribe-clj.facebook.ui :as fui]
            [aws-transcribe-clj.transcribe.subs :as ts]
            [aws-transcribe-clj.transcribe.ui :as tui]))

(defn welcome []
  [:div {:hidden (not @(rf/subscribe [::fsubs/login-ready?]))}
   [:h1.m-5.text-center
    "Scriber"]
   [:div.row.justify-content-center
    [:div.col-md-6
     [:div.card.text-center
      [:div.card-header
       [:h6 "Try it now!"]]
      [:div.card-body
       [:p [:strong "Bored with making notes, right?"]]
       [:p "Record your speech and get a transcription even in a minute!"]
       [:p "All you need is Facebook account!"]
       [:div.m-4
        [fui/login-button]]]]]]])

(defn transcribe []
  [:div
   (when @(rf/subscribe [::ts/show?])
     [:div.m-5
      [tui/recording-panel]
      [tui/upload-panel]
      [tui/transcription-panel]])])

(defn not-found []
  [:div
   [:h1 "Not found"]])


(ns aws-transcribe-clj.transcribe.ui
  (:require [cljs.core.match :refer [match]]
            [oops.core :refer [ocall!]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [aws-transcribe-clj.transcribe.events :as e]
            [aws-transcribe-clj.transcribe.subs :as s]))

;;;;;;;;;;;;
;; common ;;
;;;;;;;;;;;;

(defn try-again-button []
  [:div
   [:button.btn.btn-outline-primary
    {:type     "button"
     :on-click #(rf/dispatch [::e/try-again])}
    "Try again!"]])

;;;;;;;;;;;;;;;
;; recording ;;
;;;;;;;;;;;;;;;

(defn record-button []
  [:div
   [:p "Your speech will be converted to text."]
   [:p "Click button below to start recording."]
   [:button.btn.btn-outline-danger
    {:type     "button"
     :on-click #(rf/dispatch [::e/start-recording])}
    "Record"]])

(defn recording-loading-spinner []
  [:div.spinner-border.text-muted])

(defn recording-pending-box []
  (let [clock @(rf/subscribe [::s/recording-clock])]
   [:div
    [:div.blinking-circle.mx-auto]
    [:div clock]]))

(defn recording-play []
  (let [{:keys [url mime-type]} @(rf/subscribe [::s/recording-audio])]
    [:div
     [:p "Review your audio:"]
     [:audio {:controls true}
      [:source {:src url :type mime-type}]]]))

(defn recording-status []
  (when-let [status @(rf/subscribe [::s/recording-status])]
    [:div.alert.alert-primary [:p "Recording status: " status]]))

(defn recording-error-box [error]
  ;; FIXME: Show proper message that corresponds to given error
  [:<>
   [:div.alert.alert-danger
    "Could not start recording. Is your microphone turned on?"]
   [try-again-button]])

;;;;;;;;;;;;
;; upload ;;
;;;;;;;;;;;;

(defn upload-pending-box []
  [:div
   [:div.spinner-border.text-muted]
   [:p "Uploading ..."]])

(defn upload-success-box []
  [:div.alert.alert-success
   "Uploaded successfully!"])

(defn upload-error-box [error]
  ;; FIXME: Show proper message that corresponds to given error
  [:<>
   [:div.alert.alert-danger
    [:p "Upload failure!"]]
   [try-again-button]])

;;;;;;;;;;;;;;;;;;;
;; transcription ;;
;;;;;;;;;;;;;;;;;;;

(defn transcript [text]
  [:p text])

(defn transcription-error []
  (when-let [error @(rf/subscribe [::s/transcription-error])]
    [:div.alert.alert-danger
     (:message error)]))

(defn transcription-starting-box []
  [:div
   [:p "Starting transcription..."]])

(defn transcription-job-pending-box []
  [:div
   [:div.spinner-border.text-muted]
   [:p "Transcribing..."]])

(defn transcription-job-success-box []
  [:div.alert.alert-success
   [:p "Transcribed successfully!"]])

(defn transcription-error-box [error]
  [:<>
   [:div.alert.alert-danger
    [:p "Transcription failed."]]
   [try-again-button]])

(defn transcription-results-box []
  (let [job-duration @(rf/subscribe [::s/transcription-job-duration])
        results      @(rf/subscribe [::s/transcription-results])]
    [:<>
     [:div
      (when results
        [:<>
         (for [t (:transcripts results)]
           (let [id (random-uuid)]
             ^{:key id} [transcript (:transcript t)]))])]
     [:div.text-center.small
      [:p "Transcription took " job-duration " seconds."]]
     [try-again-button]]))

;;;;;;;;;;;;
;; panels ;;
;;;;;;;;;;;;

(defn recording-panel []
  (let [recording-status @(rf/subscribe [::s/recording-status])
        recording-error  @(rf/subscribe [::s/recording-error])
        transcription-status @(rf/subscribe [::s/transcription-status])
        transcription-job-status  @(rf/subscribe [::s/transcription-job-status])
        component (match
                    [recording-status transcription-status transcription-job-status]
                    [nil nil nil]          [record-button]
                    [:initialized nil nil] [recording-loading-spinner]
                    [:failure nil nil]     [recording-error-box recording-error]
                    [:pending nil nil]     [recording-pending-box]
                    [:success _ _]         [recording-play])]
    [:div.mt-3
     [:div.row.justify-content-center
      [:div.col-md-6
       [:div.card.text-center
        [:div.card-header
         [:h6 "Recording"]]
        [:div.card-body
         component]]]]]))

(defn upload-panel []
  (let [recording-status @(rf/subscribe [::s/recording-status])
        s3-object-status @(rf/subscribe [::s/recording-s3-object-status])
        s3-object-error  @(rf/subscribe [::s/recording-s3-object-error])
        component        (match [recording-status s3-object-status]
                           [_ nil] nil
                           [:success :pending] [upload-pending-box]
                           [:success :success] [upload-success-box]
                           [:success :failure] [upload-error-box s3-object-error])]
    (when (some? component)
      [:div.mt-3
       [:div.row.justify-content-center
        [:div.col-md-6
         [:div.card.text-center
          [:div.card-header
           [:h6 "Upload"]]
          [:div.card-body
           component]]]]])))

(defn transcription-panel []
  (let [recording-status         @(rf/subscribe [::s/recording-status])
        s3-object-status         @(rf/subscribe [::s/recording-s3-object-status])
        transcription-status     @(rf/subscribe [::s/transcription-status])
        transcription-job-status @(rf/subscribe [::s/transcription-job-status])
        transcription-error      @(rf/subscribe [::s/transcription-error])
        component (when (= #{:success} (set [recording-status s3-object-status]))
                    (match
                      [transcription-status transcription-job-status]
                      [nil                  nil                     ] nil
                      [:pending             nil                     ] [transcription-starting-box]
                      [:pending             :pending                ] [transcription-job-pending-box]
                      [:pending             :success                ] [transcription-job-success-box]
                      [:failure             :failure                ] [transcription-error-box transcription-error]
                      [:success             :success                ] [transcription-results-box]))]
    (when (some? component)
      [:div.mt-3
       [:div.row.justify-content-center
        [:div.col-md-6
         [:div.card.text-center
          [:div.card-header
           [:h6 "Transcription"]]
          [:div.card-body
           component]]]]])))
(ns aws-transcribe-clj.transcribe.events
  (:require [cljs.core.async :as a]
            [oops.core :refer [oget ocall!]]
            [re-frame.core :as rf]
            [aws-transcribe-clj.common :refer [err-map]]
            [aws-transcribe-clj.config :as config]
            [aws-transcribe-clj.transcribe.fx :as fx]))

(rf/reg-event-db
  ::set-up
  [rf/trim-v]
  (fn [db]
    (assoc-in db [:transcribe :ui :show?] true)))

(rf/reg-event-db
  ::tear-down
  [rf/trim-v]
  (fn [db]
    (dissoc db :transcribe)))

(rf/reg-event-db
  ::try-again
  [rf/trim-v]
  (fn [db]
    (update db :transcribe dissoc :recording :transcription)))

(rf/reg-event-fx
  ::start-recording
  [rf/trim-v]
  (fn [{:keys [db]} _]
    (let [limit config/APP-AUDIO-LIMIT]
      {:db                (-> db
                              (update :transcribe #(dissoc % :transcription))
                              (assoc-in [:transcribe :recording] {:status   :initialized
                                                                  :progress {:max     limit
                                                                             :current 0}}))
       ::fx/record-audio! [{:delay     500
                            :limit     limit
                            :id        (random-uuid)
                            :mime-type "audio/wav"
                            :tick      {:interval 1000}}
                           {:on-started [::recording-started]
                            :on-tick    [::update-recording-progress]
                            :on-stop    [::stop-recording]
                            :on-error   [::recording-error]}]})))

(rf/reg-event-db
  ::recording-started
  [rf/trim-v]
  (fn [db _]
    (assoc-in db [:transcribe :recording :status] :pending)))

(rf/reg-event-db
  ::update-recording-progress
  [rf/trim-v]
  (fn [db [tick-delay]]
    (update-in db [:transcribe :recording :progress :current] (partial + tick-delay))))

(rf/reg-event-fx
  ::stop-recording
  [rf/trim-v]
  (fn [{:keys [db]} [recording]]
    {:db       (-> db
                   (assoc-in [:transcribe :recording :status] :success)
                   (update-in [:transcribe :recording :progress] (fn [{:keys [max] :as progress}]
                                                                   (assoc progress :current max))))
     :dispatch [::store-recording-locally recording]}))

(rf/reg-event-db
  ::recording-error
  [rf/trim-v]
  (fn [db [error]]
    (assoc-in db [:transcribe :recording] {:status :failure
                                           :error  (err-map error)})))

(rf/reg-event-fx
  ::store-recording-locally
  [rf/trim-v]
  (fn [{:keys [db]} [recording]]
    {:db       (update-in db [:transcribe :recording] #(merge % recording))
     :dispatch [::store-recording-on-s3 recording]}))

;;;;;;;;;;;;;;;;;;;;;
;; uploading audio ;;
;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::store-recording-on-s3
  [rf/trim-v]
  (fn [{:keys [db]} [recording]]
    (let [user-id      (get-in db [:facebook :auth :userID])
          access-token (get-in db [:facebook :auth :accessToken])]
      {:db          (assoc-in db [:transcribe :recording :s3-object :status] :pending)
       ::fx/upload! [{:recording recording
                      :user-id   user-id}
                     {:access-token access-token
                      :role-arn     config/AWS-ROLE-ARN
                      :bucket-name  config/AWS-RECORDINGS-BUCKET
                      :provider-id  config/AWS-WEB-IDENTITY-PROVIDER-ID}
                     {:on-success [::store-recording-on-s3-success]
                      :on-error   [::store-recording-on-s3-error]}]})))

(rf/reg-event-fx
  ::store-recording-on-s3-success
  [rf/trim-v]
  (fn [{:keys [db]} [{:keys [recording s3-object]}]]
    (let [s3-object' (assoc s3-object :status :success)
          recording' (assoc recording :s3-object s3-object')]
      {:db       (update-in db [:transcribe :recording] merge recording')
       :dispatch [::start-transcription recording']})))

(rf/reg-event-db
  ::store-recording-on-s3-error
  [rf/trim-v]
  (fn [db [error]]
    (assoc-in db [:transcribe :recording :s3-object] {:status :failure
                                                      :error  (err-map error)})))
;;;;;;;;;;;;;;;;;;;
;; transcription ;;
;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::start-transcription
  [rf/trim-v]
  (fn [{:keys [db]} [recording]]
    (let [user-id      (get-in db [:facebook :auth :userID])
          access-token (get-in db [:facebook :auth :accessToken])]
      {:db              (assoc-in db [:transcribe :transcription :status] :pending)
       ::fx/transcribe! [{:recording recording
                          :user-id   user-id}
                         {:access-token       access-token
                          :role-arn           config/AWS-ROLE-ARN
                          :provider-id        config/AWS-WEB-IDENTITY-PROVIDER-ID
                          :transcripts-bucket config/AWS-TRANSCRIPTS-BUCKET}
                         {:on-started [::transcription-started]
                          :on-done    [::transcription-job-success]
                          :on-error   [::transcription-job-error]}]})))

;;;;;;;;;;;;;;;;;;;;;;;
;; transcription job ;;
;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::transcription-started
  [rf/trim-v]
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:transcribe :transcription :job :status] :pending)}))

(rf/reg-event-fx
  ::transcription-job-success
  [rf/trim-v]
  (fn [{:keys [db]} [job]]
    (let [duration (- (:CompletionTime job) (:StartTime job))
          job'     (assoc job :status :success :duration duration)]
      {:db       (assoc-in db [:transcribe :transcription :job] job')
       :dispatch [::get-transcript-content job']})))

(rf/reg-event-fx
  ::transcription-job-error
  [rf/trim-v]
  (fn [{:keys [db]} [error]]
    {:db (-> db
             (assoc-in [:transcribe :transcription] {:status :failure
                                                     :error  (err-map error)})
             (assoc-in [:transcribe :transcription :job] {:status :failure
                                                          :error  (err-map error)}))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; transcription results ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::get-transcript-content
  [rf/trim-v]
  (fn [{:keys [db]} [transcription-job]]
    (let [access-token (get-in db [:facebook :auth :accessToken])]
      {::fx/fetch-transcription! [{:transcription-job transcription-job}
                                  {:access-token access-token
                                   :role-arn     config/AWS-ROLE-ARN
                                   :provider-id  config/AWS-WEB-IDENTITY-PROVIDER-ID
                                   :bucket-name  config/AWS-TRANSCRIPTS-BUCKET}
                                  {:on-success [::store-transcript-content]
                                   :on-error   [::get-transcript-content-error]}]})))

(rf/reg-event-db
  ::store-transcript-content
  [rf/trim-v]
  (fn [db [content]]
    (let [body (as-> content $
                     (js->clj $ :keywordize-keys true)
                     (:Body $)
                     (ocall! $ :toString "utf8")
                     (ocall! js/JSON :parse $)
                     (js->clj $ :keywordize-keys true))]
      (update-in db [:transcribe :transcription] #(merge % {:results (-> body :results)
                                                            :status  :success})))))

(rf/reg-event-db
  ::get-transcript-content-error
  [rf/trim-v]
  (fn [db [error]]
    (assoc-in db [:transcribe :transcription] {:status :failure
                                               :error  (err-map error)})))

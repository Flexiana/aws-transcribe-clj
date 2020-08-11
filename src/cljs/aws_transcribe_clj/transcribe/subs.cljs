(ns aws-transcribe-clj.transcribe.subs
  (:require [oops.core :refer [ocall!]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]))

;;;;;;;;;;;;;
;; helpers ;;
;;;;;;;;;;;;;

(defn- ms->clock [value]
  (let [time-s      (/ value 1000)
        seconds     (mod time-s 60)
        seconds-str (if (< seconds 10) (str "0" seconds) seconds)
        minutes     (ocall! js/Math :floor (/ time-s 60))
        minutes-str (if (< minutes 10) (str "0" minutes) minutes)]
    (str minutes-str ":" seconds-str)))

;;;;;;;;
;; ui ;;
;;;;;;;;

(rf/reg-sub
  ::show?
  (fn [db]
    (get-in db [:transcribe :ui :show?])))

;;;;;;;;;;;;;;;
;; recording ;;
;;;;;;;;;;;;;;;

(rf/reg-sub
  ::recording
  (fn [db]
    (get-in db [:transcribe :recording])))

(rf/reg-sub
  ::recording-status
  :<- [::recording]
  (fn [recording]
    (:status recording)))

(rf/reg-sub
  ::recording-progress
  :<- [::recording]
  (fn [recording]
    (:progress recording)))

(rf/reg-sub
  ::recording-clock
  :<- [::recording-progress]
  (fn [{:keys [current max]}]
    (str (ms->clock current) " / " (ms->clock max))))

(rf/reg-sub
  ::recording-audio
  :<- [::recording]
  (fn [recording]
    (select-keys recording [:mime-type :url])))

(rf/reg-sub
  ::recording-error
  :<- [::recording]
  (fn [recording]
    (:error recording)))

(rf/reg-sub
  ::recording-s3-object
  (fn [db]
    (get-in db [:transcribe :recording :s3-object])))

(rf/reg-sub
  ::recording-s3-object-status
  :<- [::recording-s3-object]
  (fn [s3-object]
    (:status s3-object)))

(rf/reg-sub
  ::recording-s3-object-error
  :<- [::recording-s3-object]
  (fn [s3-object]
    (:error s3-object)))

;;;;;;;;;;;;;;;;;;;
;; transcription ;;
;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
  ::transcription
  (fn [db]
    (get-in db [:transcribe :transcription])))

(rf/reg-sub
  ::transcription-status
  :<- [::transcription]
  (fn [transcription]
    (:status transcription)))

(rf/reg-sub
  ::transcription-results
  :<- [::transcription]
  (fn [transcription]
    (:results transcription)))

(rf/reg-sub
  ::transcription-error
  :<- [::transcription]
  (fn [transcription]
    (:error transcription)))

;;;;;;;;;;;;;;;;;;;;;;;
;; transcription job ;;
;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
  ::transcription-job
  (fn [db]
    (get-in db [:transcribe :transcription :job])))

(rf/reg-sub
  ::transcription-job-duration
  :<- [::transcription-job]
  (fn [{:keys [duration]}]
    (ocall! js/Math :floor (/ duration 1000))))

(rf/reg-sub
  ::transcription-job-status
  :<- [::transcription-job]
  (fn [job]
    (:status job)))

(rf/reg-sub
  ::transcription-job-error
  :<- [::transcription-job]
  (fn [job]
    (:error job)))
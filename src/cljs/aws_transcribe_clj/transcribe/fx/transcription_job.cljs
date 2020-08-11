(ns aws-transcribe-clj.transcribe.fx.transcription-job
  (:require [cljs.core.async :as a]
            [re-frame.core :as rf]
            [aws-transcribe-clj.aws.sdk :as aws]
            [aws-transcribe-clj.common :as c]))

(defn start-transcription-job-callback
  [{:keys [recording]} {:keys [watcher]} {:keys [on-started on-error]}]
  (fn [err resp]
    (if (some? err)
      (rf/dispatch (conj on-error err))
      (do
        (rf/dispatch (conj on-started recording resp))
        (a/go (a/>! watcher resp))))))

(defn get-transcription-job-callback
  [_ {:keys [watcher]} {:keys [on-error]}]
  (fn [err resp]
    (if (some? err)
      (rf/dispatch (conj on-error err))
      (a/go (a/>! watcher resp)))))

(defn track-transcription-job
  [{:keys [transcription-job] :as context}
   {:keys [aws-transcribe watcher] :as opts}
   {:keys [on-done on-error] :as events}]
  (a/go-loop [job (-> transcription-job (js->clj :keywordize-keys true) :TranscriptionJob)]
    (cond
      (some? (:CompletionTime job))
      (rf/dispatch (conj on-done job))

      (some? (:FailureReason job))
      (rf/dispatch (conj on-error (c/js-error "TranscriptionFailed" (:FailureReason job))))

      :else
      (do (a/<! (a/timeout 5000))
          (aws/get-transcription-job
            aws-transcribe
            (clj->js {:TranscriptionJobName (:TranscriptionJobName job)})
            (get-transcription-job-callback context opts events))
          (let [result (a/<! watcher)]
            (if (c/js-error? result)
              (rf/dispatch (conj on-error result))
              (recur (-> result (js->clj :keywordize-keys true) :TranscriptionJob))))))))
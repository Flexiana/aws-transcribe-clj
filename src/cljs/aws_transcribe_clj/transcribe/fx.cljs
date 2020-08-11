(ns aws-transcribe-clj.transcribe.fx
  (:require [cljs.core.async :as a]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [aws-transcribe-clj.aws.sdk :as aws]
            [aws-transcribe-clj.common :as c]
            [aws-transcribe-clj.transcribe.fx.audio :as audio]
            [aws-transcribe-clj.transcribe.fx.transcription-job :as tjob]))

(rf/reg-fx
  ::record-audio!
  (fn [[params {:keys [on-stop on-error] :as events}]]
    (let [output (a/chan)]
      (a/go
        (audio/record! (assoc params :output output) events)
        (let [result (a/<! output)
              event  (if (instance? js/Error result) on-error on-stop)]
          (rf/dispatch (conj event result)))))))

(rf/reg-fx
  ::upload!
  (fn [[{:keys [recording user-id]} {:keys [bucket-name] :as opts} {:keys [on-success on-error]}]]
    (let [file      (:file recording)
          obj-key   (str "facebook-" user-id "/" (oget file :name))
          bucket    (aws/bucket-service opts)]
      (aws/put-object
        bucket
        #js{:Key         obj-key
            :ContentType (oget file :type)
            :Body        file}
        (fn [err _]
          (if (some? err)
            (rf/dispatch (conj on-error err))
            (rf/dispatch (conj on-success {:recording recording
                                           :s3-object {:object-key  obj-key
                                                       :bucket-name bucket-name}}))))))))

(rf/reg-fx
  ::transcribe!
  (fn [[{:keys [recording user-id] :as context}
        {:keys [transcripts-bucket] :as opts}
        {:keys [on-error] :as events}]]
    (let [{:keys [object-key bucket-name]} (:s3-object recording)
          transcribe (aws/transcribe-service opts)
          watcher    (a/chan)
          opts'      (assoc opts :aws-transcribe transcribe :watcher watcher)]
      (a/go

        (aws/start-transcription-job
          transcribe
          #js{:TranscriptionJobName (str user-id "-" (:id recording))
              :LanguageCode         "en-US"
              :Media                #js{:MediaFileUri (str "s3://" bucket-name "/" object-key)}
              :OutputBucketName     transcripts-bucket}
          (tjob/start-transcription-job-callback context opts' events))
        (let [result (a/<! watcher)]
          (if (c/js-error? result)
            (rf/dispatch (conj on-error result))
            (tjob/track-transcription-job
              (assoc context :transcription-job result)
              opts'
              events)))))))

(rf/reg-fx
  ::fetch-transcription!
  (fn [[{:keys [transcription-job]} {:keys [bucket-name] :as opts} {:keys [on-success on-error]}]]
    (let [bucket      (aws/bucket-service opts)
          op-params   #js{:Bucket bucket-name
                          :Key (str (:TranscriptionJobName transcription-job) ".json")}]
      (aws/get-object bucket
                      op-params
                      (fn [err resp]
                        (if (some? err)
                          (rf/dispatch (conj on-error err))
                          (rf/dispatch (conj on-success resp))))))))

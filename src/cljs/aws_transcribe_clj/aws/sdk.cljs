(ns aws-transcribe-clj.aws.sdk
  (:require [oops.core :refer [oget oset! ocall!]]))

;; aws

(defn set-region!
  [region-name]
  (oset! js/AWS :config.region region-name))

(defn set-credentials!
  [service credentials]
  (oset! service :config.credentials credentials))

(defn credentials [provider-id role-arn access-token]
  (new js/AWS.WebIdentityCredentials
       #js{:ProviderId       provider-id
           :RoleArn          role-arn
           :WebIdentityToken access-token}))

;; s3

(defn bucket-service
  [{:keys [provider-id role-arn access-token bucket-name]}]
  (let [creds   (credentials provider-id role-arn access-token)
        service (new js/AWS.S3 (clj->js {:params {:Bucket bucket-name}}))]
    (set-credentials! service creds)))

(defn get-object
  [service params callback]
  (ocall! service :getObject params callback))

(defn put-object
  [service params callback]
  (ocall! service :putObject params callback))

;; transcribe

(defn transcribe-service
  [{:keys [provider-id role-arn access-token]}]
  (let [creds   (credentials provider-id role-arn access-token)
        service (new js/AWS.TranscribeService)]
    (set-credentials! service creds)))

(defn start-transcription-job
  [service params callback]
  (ocall! service :startTranscriptionJob params callback))

(defn get-transcription-job
  [service params callback]
  (ocall! service :getTranscriptionJob params callback))
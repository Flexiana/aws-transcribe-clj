(ns aws-transcribe-clj.transcribe.fx.audio
  (:require ["recorder-js" :default Recorder]
            [cljs.core.async :as a]
            [clojure.string :as s]
            [oops.core :refer [oget oset! ocall!]]
            [re-frame.core :as rf]))

(defn on-start-fn
  [{:keys [tick ticker]} {:keys [on-started on-tick]}]
  (fn []
    (rf/dispatch on-started)
    (a/go
      (a/>! ticker
            (js/setInterval
              #(rf/dispatch (conj on-tick (:interval tick)))
              (:interval tick))))))

(defn on-stop-fn
  [{:keys [mime-type output id stream ticker]} _]
  (fn [audio]
    (let [blob      (oget audio :blob)
          filetype  (-> mime-type (s/split #"/") second)
          file      (new js/File #js[blob] (str id "." filetype) #js{:type mime-type})]
      (-> stream (ocall! :getTracks) (ocall! :forEach #(.stop %)))
      (a/go
        (a/>! output
              {:id        id
               :file      file
               :type      filetype
               :mime-type mime-type
               :url       (ocall! js/URL :createObjectURL blob)})
        (js/clearInterval (a/<! ticker))))))

(defn set-start!
  [recorder {:keys [delay] :as params} events]
  (js/setTimeout
    #(-> recorder (ocall! :start) (ocall! :then (on-start-fn params events)))
    delay))

(defn set-stop!
  [recorder {:keys [delay limit] :as params} events]
  (js/setTimeout
    #(-> recorder (ocall! :stop) (ocall! :then (on-stop-fn params events)))
    (+ delay limit)))

(defn record-stream-fn
  [recorder params events]
  (fn [stream]
    (let [params' (assoc params :stream stream :ticker (a/chan))]
      (ocall! recorder :init stream)
      (set-start! recorder params' events)
      (set-stop! recorder params' events))))

(defn record!
  [{:keys [output] :as params} events]
  (let [audio-context (new js/AudioContext)
        recorder      (new Recorder audio-context)
        pstream       (ocall! js/navigator :mediaDevices.getUserMedia #js{:audio true})
        on-error      #(a/go (a/>! output %))]
    (-> pstream
        (ocall! :then (record-stream-fn recorder params events))
        (ocall! :catch on-error))))

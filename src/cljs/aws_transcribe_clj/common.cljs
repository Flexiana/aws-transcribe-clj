(ns aws-transcribe-clj.common
  (:require [oops.core :refer [oget oset!]]))

(defn err-map
  "Helper function. Takes js/Error object and returns a map with its name and massage."
  [error]
  {:name    (oget error :name)
   :message (oget error :message)})

(defn js-error [name message]
  (let [error (new js/Error message)]
    (oset! error :name name)))

(defn js-error? [obj]
  (instance? js/Error obj))
(ns ranked-queries.core
  (:require [clojure.tools.logging :as log :refer [info error]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [digest :as digest]))

(defn load-items
  [filename]
  (-> filename
      io/resource
      slurp
      (json/parse-string (fn [k] (keyword (str/replace k #"_" "-"))))))

(def items (load-items "elasticsearch-documents.json"))

(defn get-filename
  [headers]
  (->> (get headers "Content-Disposition")
       (re-matches #"attachment; filename=\"(.+)\";")
       last))

(defn download-item
  [{:keys [item-url]}]
  (let [res (http/get item-url {:insecure? true
                                :as :byte-array})
        filename (get-filename (:headers res))
        sha1 (digest/sha-1 (:body res))
        path (str "output/" sha1 " - " filename)]
    (with-open [w (io/output-stream path)]
      (.write w (:body res)))))

(download-item (first items))




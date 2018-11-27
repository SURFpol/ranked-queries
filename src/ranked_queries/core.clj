(ns ranked-queries.core
  (:require [clojure.tools.logging :as log :refer [info error]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [digest :as digest]
            [clojure.data.xml :as xml]))

(defn load-items
  [filename]
  (-> filename
      io/resource
      slurp
      (json/parse-string (fn [k] (keyword (str/replace k #"_" "-"))))))

(def items (load-items "elasticsearch-documents.json"))

(defn write-webloc
  [url path]
  (let [tags (xml/sexp-as-element
               ["plist" {"version" "1.0"}
                ["dict"
                 ["key" "URL"]
                 ["string" url]]])]
    (with-open [w (java.io.FileWriter. path)]
      (xml/emit tags w))))

(defn get-filename
  [headers]
  (->> (get headers "Content-Disposition")
       (re-matches #"attachment; filename=\"(.+)\";")
       last))

(defn clean-filename
  [filename]
  (str/replace filename #"[*&%/]+" ""))

(defn download-item
  [{:keys [item-url title] :as item}]
  (let [{:keys [headers body]} (http/get item-url {:insecure? true
                                                   :as :byte-array})
        folder "output/"
        sha1 (digest/sha-1 item-url)]
    {:item (dissoc item :text)
     :headers headers
     :result
     (if (contains? headers "Content-Disposition")
       (let [filename (get-filename headers)
             path (str folder sha1 " - " filename)]
         (with-open [w (io/output-stream path)]
           (.write w body)))
       (let [filename (clean-filename (str title ".webloc"))
             path (str folder sha1 " - " filename)]
         (write-webloc item-url path)))}))

(comment
  (map download-item items))

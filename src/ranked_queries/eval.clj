(ns ranked-queries.eval
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [ranked-queries.dev.clipboard :as clip]))

(def elastic-url "https://surfpol.sda.surf-hosted.nl")
(def user "")
(def pass "")

(defn get-indices
  []
  (let [url (str elastic-url "/_all")
        res (http/get url {:basic-auth [user pass]})]
    (json/decode (:body res) true)))

(get-indices)

(defn item->rating
  [index {:keys [hash rating]}]
  {"_index" index "_id" hash "rating" rating})

(defn cl-requests
  [index {:keys [items queries]}]
  (for [query queries]
    {:id (str (str/replace query #"\s+" "_") "_query")
     :request {:query {:match {:text query}}}
     :ratings (map (partial item->rating index) items)}))

(defn request-map
  [index]
  (let [input (-> "queries.json"
                  io/file
                  slurp
                  (json/decode true))
        requests (mapcat (partial cl-requests index) input)]
    {:requests requests
     :metric {"precision"
              {"k" 20
               "relevant_rating_threshold" 1
               "ignore_unlabeled" false}}}))

(defn metric
  [index]
  (let [req-body (json/encode (request-map index))
        url (str elastic-url "/" index "/_rank_eval")
        res (http/get url {:basic-auth [user pass]
                           :body req-body
                           :content-type :json
                           :accept :json})]
    (json/decode (:body res) true)))

(metric "freeze-1")

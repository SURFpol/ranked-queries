(ns user
  (:require [ranked-queries.dev.clipboard :as clipboard]))

(defn pbcopy
  [data]
  (clipboard/spit data))

(def spy #(do (println "DEBUG:" %) %))

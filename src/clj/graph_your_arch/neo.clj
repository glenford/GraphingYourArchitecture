(ns graph-your-arch.neo
  (:require [graph-your-arch.config :as config]
            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.cypher :as cy]))

(def connection (atom nil))

(defn conn
  "Use (or get if needed) a connection to neo4j"
  []
  (when (nil? @connection)
    (compare-and-set! connection nil (nr/connect config/neo4j-server-uri)))
  @connection)

(defn run-cypher
  "Execute cypher against neo4j using the common connection"
  [cypher]
  (cy/tquery (conn) cypher))

(defn escape-param [param]
  (clojure.string/replace param #"\'" "\\\\'"))

(defn build-foreach-string [values]
  (case values
    nil nil
    (()) "[]"
    (let [v (for [value values]
              (if (sequential? value)
                (build-foreach-string value)
                (str "'" (escape-param value) "'")))]
      (clojure.string/join (concat "[" (interpose "," v) "]")))))

(defn escape-params [params]
  (for [param params]
    (if (sequential? param)
      (build-foreach-string param)
      (escape-param param))))

(defn def-cypher
  "Helper function to build a cypher query from a template"
  [template params]
  (try
    (apply format (clojure.string/join template) (escape-params params))
    (catch Exception e (do
      (println ">> " e "\n\n" template "\n\n" params)
      (throw e)))))

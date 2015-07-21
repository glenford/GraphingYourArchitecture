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

(defn exec-cypher
  "Helper function to run a cypher query from a template"
  [template params]
  (let [cypher (clojure.string/join " " template)]
    (cy/tquery (conn) cypher params)))



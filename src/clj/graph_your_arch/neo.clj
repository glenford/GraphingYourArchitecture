(ns graph-you-arch.neo
  (:require [graph-your-arch.config :as config]
            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.cypher :as cy]))

(def connection (atom nil))

(defn conn
  "Use (or get if needed) a connection to neo4j"
  []
  (when (nil? @connection)
    (compare-and-set! connection nil (nr/connect config/neo-server-uri)))
  @connection)

(defn run-cypher
  "Execute cypher against neo4j using the common connection"
  [cypher]
  (cy/tquery (conn) cypher))

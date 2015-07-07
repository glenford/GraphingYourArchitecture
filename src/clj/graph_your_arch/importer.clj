(ns graph-your-arch.importer
  (:require [graph-your-arch.aws :as aws]
            [graph-your-arch.neo :as neo]))

(defn get-regions [] (aws/get-regions) )

(defn regions
  "imports AWS regions into the graph"
  []
  (doseq [region (get-regions)]
    (let [cypher-query (neo/def-cypher
                         ["MERGE (region:Region {name:'%s'})"]
                         [(:region-name region)])]
      (neo/run-cypher cypher-query))))

(defn availability-zones
  "imports AWS availability zones into the graph"
  []
  (doseq [region (get-regions)]
    (let [az-names (for [az (aws/get-azs (:endpoint region))] (:zone-name az))
          cypher-query (neo/def-cypher
                         ["UNWIND %s AS az"
                          "  MERGE (zone:AZ {name:az})"
                          "  WITH zone"
                          "    MATCH (region:Region {name:'%s'})"
                          "    MERGE (zone)-[:IN]->(region)"]
                         [az-names (:region-name region)])]
      (neo/run-cypher cypher-query))))


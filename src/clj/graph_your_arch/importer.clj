(ns graph-your-arch.importer
  (:require [graph-your-arch.aws :as aws]
            [graph-your-arch.neo :as neo]))

(defn regions
  "imports AWS regions into the graph"
  []
  (doseq [region (aws/get-regions)]
    (let [cypher-query (neo/def-cypher
                         ["MERGE (region:Region {name:'%s'})"]
                         [(:region-name region)])]
      (neo/run-cypher cypher-query))))
        


(ns graph-your-arch.importer
  (:require [graph-your-arch.aws :as aws]
            [graph-your-arch.neo :as neo]))

(def get-regions (memoize aws/get-regions))

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

(defn instances
  "import instances and reservations into the graph"
  []
  (doseq [region (get-regions)]
    (doseq [reservation (aws/get-reservations (:endpoint region))]
      (let [reservation-id (:reservation-id reservation)]
        (doseq [instance (:instances reservation)]
          (let [instance-id (:instance-id instance)
                instance-type (:instance-type instance)
                az (-> instance :placement :availability-zone)
                cypher-query (neo/def-cypher
                               ["MATCH (az:AZ {name:'%s'})"
                                "WITH az"
                                "  MERGE (r:Reservation {id:'%s'})"
                                "  MERGE (i:Instance {id:'%s'})"
                                "  MERGE (t:InstanceType {name:'%s'})"
                                "  MERGE (i)-[:CREATED_BY]->(r)"
                                "  MERGE (i)-[:OF_TYPE]->(t)"
                                "  MERGE (i)-[:IN]->(az)"]
                               [az reservation-id instance-id instance-type])]
            (neo/run-cypher cypher-query)))))))
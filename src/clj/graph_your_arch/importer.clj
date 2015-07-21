(ns graph-your-arch.importer
  (:require [graph-your-arch.aws :as aws]
            [graph-your-arch.neo :as neo]))

; we use regions quite often, so memoize them
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
    (let [az-names (for [az (aws/get-azs region)] (:zone-name az))
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
  (doseq [region (get-regions)
          reservation (aws/get-reservations region)]
      (let [reservation-id (:reservation-id reservation)]
        (doseq [instance (:instances reservation)]
          (let [instance-id (:instance-id instance)
                instance-type (:instance-type instance)
                az (-> instance :placement :availability-zone)
                ipaddrs (for [interface (:network-interfaces instance)] (:private-ip-address interface))
                cypher-query (neo/def-cypher
                               ["MATCH (az:AZ {name:'%s'})"
                                "WITH az"
                                "  MERGE (r:Reservation {id:'%s'})"
                                "  MERGE (i:Instance {id:'%s'})"
                                "  MERGE (t:InstanceType {name:'%s'})"
                                "  MERGE (i)-[:CREATED_BY]->(r)"
                                "  MERGE (i)-[:OF_TYPE]->(t)"
                                "  MERGE (i)-[:IN]->(az)"
                                "  WITH i"
                                "    UNWIND %s AS _ip"
                                "    MERGE (ipaddr:IPADDR {ip:_ip})"
                                "    MERGE (i)-[:HAS]->(ipaddr)"]
                               [az reservation-id instance-id instance-type ipaddrs])]
            (neo/run-cypher cypher-query))))))

;
; Note: there is no unique global id for ELBs, names can be duplicated across regions
; so for this example we prepend names with region names
;
(defn elbs
  "import elastic loadbalancers into the graph"
  []
  (doseq [region (get-regions)
          elb (aws/get-elbs region)]
    (let [name (str (:region-name region) "/" (:load-balancer-name elb))
          instances (:instances elb)
          instance-ids (for [i instances] (:instance-id i))
          azs (:availability-zones elb)
          dns (:dnsname elb)
          cypher-query (neo/def-cypher
                         ["MATCH (az:AZ) WHERE az.name in %s"
                          "MATCH (i:Instance) where i.id in %s"
                          "WITH az,i"
                          "  MERGE (elb:ELB {name:'%s'})"
                          "  MERGE (dns:DNS {dns:'%s'})"
                          "  MERGE (elb)-[:REACHED_BY]->(dns)"
                          "  MERGE (elb)-[:IN]->(az)"
                          "  MERGE (elb)-[:BALANCES]->(i)"]
                         [azs instance-ids name dns])]
      (neo/run-cypher cypher-query))))


(defn- strip-trailing-period
  "remove a trailing . if present"
  [string]
  (.replaceAll string "\\.$" ""))


(defn add-a-record
  "import an a-record into the graph"
  [record]
  (let [name (strip-trailing-period (:name record))
        ipaddrs (for [entry (:resource-records record)] (:value entry))
        cypher-query (neo/def-cypher
                       ["MERGE (dns:DNS {dns:'%s'})"
                        "  SET dns:A_RECORD"
                        "  WITH dns"
                        "    UNWIND %s AS _ip"
                        "    MERGE (ipaddr:IPADDR {ip:_ip})"
                        "    MERGE (dns)-[:POINTS_TO]->(ipaddr)"]
                       [name ipaddrs])]
    (neo/run-cypher cypher-query)))


(defn add-a-record-alias
  "import an a-record alias into the graph"
  [record]
  (let [name (strip-trailing-period (:name record))
        alias (strip-trailing-period (:dnsname (:alias-target record)))
        cypher-query (neo/def-cypher
                       ["MERGE (dns:DNS {dns:'%s'})"
                        "  SET dns:A_RECORD"
                        "  WITH dns"
                        "    MERGE (alias:DNS {dns:'%s'})"
                        "    MERGE (dns)-[:ALIAS_FOR]->(alias)"]
                       [name alias])]
    (neo/run-cypher cypher-query)))


(defn add-cname
  "import a cname into the graph"
  [record]
  (let [name (strip-trailing-period (:name record))
        named (for [entry (:resource-records record)] (strip-trailing-period (:value entry)))
        cypher-query (neo/def-cypher
                       ["MERGE (dns:DNS {dns:'%s'})"
                        "  SET dns:CNAME"
                        "  WITH dns"
                        "    UNWIND %s AS _named"
                        "    MERGE (named:DNS {dns:_named})"
                        "    MERGE (dns)-[:POINTS_TO]->(named)"]
                       [name named])]
    (neo/run-cypher cypher-query)))

(defn dns
  "import dns entries (A-Records and CNAMES) into the graph"
  []
  (doseq [record (aws/get-dns)]
    (cond
      (and (= (:type record) "A") (contains? record :alias-target)) (add-a-record-alias record)
      (= (:type record) "A") (add-a-record record)
      (= (:type record) "CNAME") (add-cname record)
      :else nil))) ; ignore others for time being

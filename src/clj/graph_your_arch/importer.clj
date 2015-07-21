(ns graph-your-arch.importer
  (:require [graph-your-arch.aws :as aws]
            [graph-your-arch.neo :as neo]))

; we use regions quite often, so memoize them
(def get-regions (memoize aws/get-regions))

(defn regions
  "imports AWS regions into the graph"
  []
  (doseq [region (get-regions)]
    (neo/exec-cypher [ "MERGE (region:Region {name:{name}})" ]
                     { :name (:region-name region) })))

(defn availability-zones
  "imports AWS availability zones into the graph"
  []
  (doseq [region (get-regions)]
    (let [az-names (for [az (aws/get-azs region)] (:zone-name az))]
      (neo/exec-cypher
         ["UNWIND {azs} AS az"
          "  MERGE (zone:AZ {name:az})"
          "  WITH zone"
          "    MATCH (region:Region {name:{region}})"
          "    MERGE (zone)-[:IN]->(region)"]
         { :azs az-names
           :region (:region-name region)}))))


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
                ipaddrs (for [interface (:network-interfaces instance)] (:private-ip-address interface))]
            (neo/exec-cypher
               ["MATCH (az:AZ {name:{az}})"
                "WITH az"
                "  MERGE (r:Reservation {id:{reservation}})"
                "  MERGE (i:Instance {id:{instance}})"
                "  MERGE (t:InstanceType {name:{type}})"
                "  MERGE (i)-[:CREATED_BY]->(r)"
                "  MERGE (i)-[:OF_TYPE]->(t)"
                "  MERGE (i)-[:IN]->(az)"
                "  WITH i"
                "    UNWIND {ips} AS _ip"
                "    MERGE (ipaddr:IPADDR {ip:_ip})"
                "    MERGE (i)-[:HAS]->(ipaddr)"]
               { :az az
                 :reservation reservation-id
                 :instance instance-id
                 :type instance-type
                 :ips ipaddrs}))))))

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
          dns (:dnsname elb)]
      (neo/exec-cypher
         ["MATCH (az:AZ) WHERE az.name in {azs}"
          "MATCH (i:Instance) where i.id in {instances}"
          "WITH az,i"
          "  MERGE (elb:ELB {name:{name}})"
          "  MERGE (dns:DNS {dns:{dns}})"
          "  MERGE (elb)-[:REACHED_BY]->(dns)"
          "  MERGE (elb)-[:IN]->(az)"
          "  MERGE (elb)-[:BALANCES]->(i)"]
         { :azs azs
           :instances instance-ids
           :name name
           :dns dns }))))

(defn- strip-trailing-period
  "remove a trailing . if present"
  [string]
  (.replaceAll string "\\.$" ""))


(defn add-a-record
  "import an a-record into the graph"
  [record]
  (let [name (strip-trailing-period (:name record))
        ipaddrs (for [entry (:resource-records record)] (:value entry))]
    (neo/exec-cypher
       ["MERGE (dns:DNS {dns:{dns}})"
        "  SET dns:A_RECORD"
        "  WITH dns"
        "    UNWIND {ipaddresses} AS _ip"
        "    MERGE (ipaddr:IPADDR {ip:_ip})"
        "    MERGE (dns)-[:POINTS_TO]->(ipaddr)"]
       { :dns name
         :ipaddresses ipaddrs })))

(defn add-a-record-alias
  "import an a-record alias into the graph"
  [record]
  (let [name (strip-trailing-period (:name record))
        alias (strip-trailing-period (:dnsname (:alias-target record)))]
    (neo/exec-cypher
       ["MERGE (dns:DNS {dns:{dns}})"
        "  SET dns:A_RECORD"
        "  WITH dns"
        "    MERGE (alias:DNS {dns:{alias}})"
        "    MERGE (dns)-[:ALIAS_FOR]->(alias)"]
       { :dns name
         :alias alias })))

(defn add-cname
  "import a cname into the graph"
  [record]
  (let [name (strip-trailing-period (:name record))
        named (for [entry (:resource-records record)] (strip-trailing-period (:value entry)))]
    (neo/exec-cypher
       ["MERGE (dns:DNS {dns:{dns}})"
        "  SET dns:CNAME"
        "  WITH dns"
        "    UNWIND {named} AS _named"
        "    MERGE (named:DNS {dns:_named})"
        "    MERGE (dns)-[:POINTS_TO]->(named)"]
       { :dns name
         :named named })))

(defn dns
  "import dns entries (A-Records and CNAMES) into the graph"
  []
  (doseq [record (aws/get-dns)]
    (cond
      (and (= (:type record) "A") (contains? record :alias-target)) (add-a-record-alias record)
      (= (:type record) "A") (add-a-record record)
      (= (:type record) "CNAME") (add-cname record)
      :else nil))) ; ignore others for time being

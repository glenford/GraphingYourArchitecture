(ns graph-your-arch.aws
 (:require [amazonica.aws.ec2 :as ec2]
           [amazonica.aws.elasticloadbalancing :as elb]
           [amazonica.aws.route53 :as r53]))


(defn get-regions
  "Get a map of regions and their associated endpoints"
  []
  (:regions (ec2/describe-regions)))

(defn get-azs
  "Gets a map of availability zones for the associated region endpoint"
  [region]
  (:availability-zones (ec2/describe-availability-zones {:endpoint (:endpoint region)})))

(defn get-reservations
  "Gets the AWS Instance reservations with associated instances for the associated region endpoint"
  [region]
  (:reservations (ec2/describe-instances {:endpoint (:endpoint region)})))

;
; note the *cough* consistency here, where endpoint is actually a region name
;
(defn get-elbs
  "Gets the AWS ELBS for the associated region endpoint"
  [region]
  (:load-balancer-descriptions (elb/describe-load-balancers {:endpoint (:region-name region)})))


(defn- add-arg-if-not-nil [args keyw col]
  (if-let [value (get col keyw)]
    (conj args keyw value)
    args))

(defn- set-args [zone-id starting]
  (let [args [:hosted-zone-id zone-id]]
    (-> args
        (add-arg-if-not-nil :start-record-name starting)
        (add-arg-if-not-nil :start-record-type starting)
        (add-arg-if-not-nil :start-record-identifier starting))))

(defn get-r53-records [zone-id]
  (loop [current nil starting nil]
    (let [records (apply r53/list-resource-record-sets (set-args zone-id starting))]
      (let [updated (concat current (:resource-record-sets records))]
        (if (= (:is-truncated records) true)
          (recur updated { :start-record-name (:next-record-name records)
                           :start-record-type (:next-record-type records)
                           :start-record-identifier (:next-record-identifier records)})
          updated)))))

(defn get-dns
  "Gets the DNS records (global)"
  []
  (flatten
    (for [hosted-zone (:hosted-zones (r53/list-hosted-zones))]
      (get-r53-records (:id hosted-zone)))))




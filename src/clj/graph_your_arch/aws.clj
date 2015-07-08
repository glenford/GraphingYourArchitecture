(ns graph-your-arch.aws
 (:require [amazonica.aws.ec2 :as ec2]
           [amazonica.aws.elasticloadbalancing :as elb]))


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




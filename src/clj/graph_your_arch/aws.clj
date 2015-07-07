(ns graph-your-arch.aws
 (:require [amazonica.aws.ec2 :as ec2]))


(defn get-regions
  "Get a map of regions and their associated endpoints"
  []
  (:regions (ec2/describe-regions)))

(defn get-azs
  "Gets a map of availability zones for the associated region endpoint"
  [endpoint]
  (:availability-zones (ec2/describe-availability-zones {:endpoint endpoint})))


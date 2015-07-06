(ns graph-your-arch.aws
 (:require [amazonica.aws.ec2 :as ec2]))


(defn get-regions
  "Get a map of regions and their associated endpoints"
  []
  (:regions (ec2/describe-regions)))


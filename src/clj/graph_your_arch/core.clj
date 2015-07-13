(ns graph-your-arch.core
  (:require [graph-your-arch.config :as config]
            [graph-your-arch.importer :as importer]
            [clojure.java.browse :as browse]))


(defn go
  "Entry point to query AWS resources and then build a graph in neo4j"
  []
  (println "Importing your AWS infrastructure into a graph.")
  (println "Importing regions...")
  (importer/regions)
  (println "Importing availability zones...")
  (importer/availability-zones)
  (println "Importing instances...")
  (importer/instances)
  (println "Importing elastic load-balancers...")
  (importer/elbs)
  (println "Importing dns entries...")
  (importer/dns)
  (browse/browse-url config/neo4j-browser-uri))

(defn -main [& args]
  (println "Intended to be used from the repl.  Please use 'lein repl'"))

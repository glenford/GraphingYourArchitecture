(defproject graph-your-arch "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clojurewerkz/neocons "3.0.0"]
                 [amazonica "0.3.28"]
                 [org.slf4j/jcl-over-slf4j "1.7.7"]
                 [ch.qos.logback/logback-classic "1.0.13"]]
  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
  :resource-paths ["resources"]
  :main graph-your-arch.core)

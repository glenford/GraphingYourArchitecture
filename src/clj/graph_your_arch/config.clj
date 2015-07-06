(ns graph-your-arch.config)

;
; neo4j configuration
;
(defonce neo4j-server-host "localhost")
(defonce neo4j-server-port 7474)
(defonce neo4j-server-uri (format "http://%s:%d/db/data" neo4j-server-host neo4j-server-port)

;
; AWS configuration
;

; this code uses either the AWS environment variables for Key and Secret, or
; if available the IAM credentials associated with the running instance

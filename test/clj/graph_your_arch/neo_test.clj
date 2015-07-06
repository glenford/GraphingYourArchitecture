(ns graph-your-arch.neo-test
  (:use [clojure.test])
  (:require [graph-your-arch.neo :as neo]))


(deftest validate-escape-param
  (is (= "foo\\'bar" (neo/escape-param "foo'bar")))
  (is (= "foo\\'" (neo/escape-param "foo'")))
  (is (= "\\'bar" (neo/escape-param "'bar"))))


(deftest validate-build-foreach-string
  (is (= "['one']" (neo/build-foreach-string '("one"))))
  (is (= "['one','two']" (neo/build-foreach-string '("one" "two"))))
  (is (= "[]" (neo/build-foreach-string '())))
  (is (= "[['a','b'],['c','d']]" (neo/build-foreach-string '(("a","b")("c","d")))))
  (is (= "[['a','b'],['c','d']]" (neo/build-foreach-string [["a","b"]["c","d"]])))
  (is (= nil (neo/build-foreach-string nil))))

(deftest validate-escape-params
  (is (= ["foo\\'bar"] (neo/escape-params ["foo'bar"])))
  (is (= '("['foo\\'','\\'bar']" "baz") (neo/escape-params [["foo'" "'bar"] "baz"]))))
  

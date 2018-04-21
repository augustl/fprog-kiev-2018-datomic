(defproject fprog-kiev-datomic "0.1.0-SNAPSHOT"
  :description "Live coding playground for Datomic demo, fprog kiev meetup April 2018"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.datomic/datomic-free "0.9.5697"]]
  :profiles
  {:dev {:source-paths ["dev"]}})

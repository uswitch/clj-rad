(defproject clj-rad "0.2.0"
  :description "Clojure wrapper for Netflix Robust Anomaly Detection (RAD) library"
  :url "https://github.com/uswitch/clj-rad"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :resource-paths ["resources/surus-0.1.4.jar"]
  :scm {:name "git"
        :url "https://github.com/uswitch/clj-rad"}  
  :deploy-repositories [["clojars" {:creds :gpg}]]
  :dependencies [[org.clojure/clojure            "1.6.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]])

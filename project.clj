(defproject clj-rad "0.3.6"
  :description "Clojure wrapper for Netflix Robust Anomaly Detection (RAD) library"
  :url "https://github.com/uswitch/clj-rad"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:name "git"
        :url  "https://github.com/uswitch/clj-rad"}
  :deploy-repositories [["clojars" {:creds :gpg}]]
  :repositories [["uswitch" "https://raw.githubusercontent.com/uswitch/clj-rad/master/repository"]]
  :dependencies [[org.clojure/clojure            "1.6.0"]
                 [netflix/surus                  "0.1.4"]
                 [org.clojure/math.numeric-tower "0.0.4"]])

(defproject peridot "0.0.3"
  :description "a basic api for testing ring apps"
  :url "https://github.com/xeqi/peridot"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [ring-mock "0.1.1"]
                 [org.clojure/data.codec "0.1.0"]]
  :profiles {:test {:dependencies [[net.cgrand/moustache "1.1.0"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0-beta1"]]}}
  :aliases {"all" ["with-profile" "test:test,1.4"]})
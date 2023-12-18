(require '[clojure.edn :as edn])

(def +deps+ (-> "deps.edn" slurp edn/read-string))

(defn deps->vec [deps]
  (vec (keep (fn [[dep {:keys [:mvn/version exclusions]}]]
               (when version ;; will skip deps.edn-based deps
                 (cond-> [dep version]
                   exclusions (conj :exclusions exclusions))))
            deps)))

(def dependencies (deps->vec (:deps +deps+)))
(def dev-dependencies (deps->vec (-> +deps+ :aliases :dev :extra-deps)))

(defproject peridot "0.5.4"
  :description "Interact with ring apps"
  :url "https://github.com/xeqi/peridot"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies ~dependencies

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "v"]
                  ["deploy" "clojars"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  :deploy-repositories [["releases"  {:sign-releases false :url "https://repo.clojars.org"}]
                        ["snapshots" {:sign-releases false :url "https://repo.clojars.org"}]]

  :profiles {:dev {:dependencies   ~dev-dependencies
                   :resource-paths ["test-resources"]}
             ;; use the relevant clojure version for testing
             :1.5 {:dependencies [^:replace [org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [^:replace [org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [^:replace [org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [^:replace [org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [^:replace [org.clojure/clojure "1.9.0"]]}}
  :aliases {"all" ["with-profile" "+1.5:+1.6:+1.7:+1.8:+1.9"]})

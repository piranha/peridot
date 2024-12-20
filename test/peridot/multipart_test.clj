(ns peridot.multipart-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [peridot.core :refer [session request]]
            [peridot.multipart :as multipart]
            [ring.middleware.multipart-params :as multiparams]
            [ring.util.response :as response]))

(def expected-content-type
  (let [[a b] (map #(Integer. %)
                   (str/split (System/getProperty "java.specification.version") #"\."))]
    (if (or (<= 9 a)                                        ;; Java 9 and above
            (and (<= 1 a)
                 (<= 8 b)))
      "text/plain; charset=UTF-8"                           ; Java 1.8 and above
      "application/octet-stream")))                         ; Java 1.7 and below

(deftest file-as-param-is-multipart
  (is (multipart/multipart? {"file" (io/file (io/resource "file.txt"))}))
  (is (not (multipart/multipart? {"file" "value"}))))

(def ok-with-multipart-params
  (-> (fn [req]
        (-> (response/response "ok")
            (assoc :multipart-params
                   (:multipart-params req))))
      (multiparams/wrap-multipart-params)))

(deftest uploading-a-file
  (let [file (io/file (io/resource "file.txt"))
        res (-> (session ok-with-multipart-params)
                (request "/"
                         :request-method :post
                         :params {"file" file})
                :response)]
    (let [{:keys [size filename content-type tempfile]}
          (get-in res [:multipart-params "file"])]
      (is (= size 13))
      (is (= filename "file.txt"))
      (is (= content-type expected-content-type))
      (is (= (slurp tempfile) (slurp file))))))

(deftest uploading-a-file-with-keyword-keys
  (let [file (io/file (io/resource "file.txt"))
        res (-> (session ok-with-multipart-params)
                (request "/"
                         :request-method :post
                         :params {:file file})
                :response)]
    (let [{:keys [size filename content-type tempfile]}
          (get-in res [:multipart-params "file"])]
      (is (= size 13))
      (is (= filename "file.txt"))
      (is (= content-type expected-content-type))
      (is (= (slurp tempfile) (slurp file))))))

(deftest uploading-a-file-with-params
  (let [file (io/file (io/resource "file.txt"))
        res (-> (session ok-with-multipart-params)
                (request "/"
                         :request-method :post
                         :params {"file"      file
                                  "something" "☃"})
                :response)]
    (let [{:keys [size filename content-type tempfile]}
          (get-in res [:multipart-params "file"])]
      (is (= size 13))
      (is (= filename "file.txt"))
      (is (= content-type expected-content-type))
      (is (= (slurp tempfile) (slurp file))))
    (is (= (get-in res [:multipart-params "something"])
           "☃"))))

(deftest uploading-a-file-input-stream
  (let [file (io/file (io/resource "file.txt"))
        res  (-> (session ok-with-multipart-params)
                 (request "/"
                   :request-method :post
                   :params {:file (io/input-stream file)})
                 :response)]
    (let [{:keys [size content-type tempfile]}
          (get-in res [:multipart-params "file"])]
      ;; filename is not known when it's an input stream
      (is (= size 13))
      ;; input-stream is always binary
      (is (= content-type "application/octet-stream"))
      (is (= (slurp tempfile) (slurp file))))))

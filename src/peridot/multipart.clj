(ns peridot.multipart
  (:require [clojure.java.io :as io]
            [ring.util.codec :as codec]
            [ring.util.mime-type :as mime-type])
  (:import (java.io ByteArrayOutputStream File InputStream)
           (java.nio.charset Charset)
           (org.apache.http HttpEntity)
           (org.apache.http.entity ContentType)
           (org.apache.http.entity.mime MultipartEntityBuilder)))

(def text-plain (ContentType/create "text/plain" (Charset/forName "UTF-8")))

(defn ensure-string [k]
  "Ensures that the resulting key is a form-encoded string. If k is not a
  keyword or a string, then (str k) turns it into a string and passes it on to
  form-encode."
  (codec/form-encode (if (keyword? k) (name k) (str k))))

(defmulti add-part
  (fn [multipartentity key value] (type value)))

(defmethod add-part File [^MultipartEntityBuilder m k ^File f]
  (let [ctype (-> (mime-type/ext-mime-type (.getName f))
                  (ContentType/create "UTF-8"))]
    (.addBinaryBody m (ensure-string k) f ctype (.getName f))))

(defmethod add-part InputStream [^MultipartEntityBuilder m k ^InputStream v]
  (.addBinaryBody m (ensure-string k) v ContentType/APPLICATION_OCTET_STREAM "input-stream.tmp"))

(defmethod add-part (Class/forName "[B") [^MultipartEntityBuilder m k ^"[B" v]
  (.addBinaryBody m (ensure-string k) v ContentType/APPLICATION_OCTET_STREAM "byte-array.tmp"))

(defmethod add-part :default [^MultipartEntityBuilder m k v]
  (.addTextBody m (ensure-string k) v text-plain))

(defn entity [params]
  (let [b (doto (MultipartEntityBuilder/create)
            (.setCharset (Charset/forName "UTF-8")))]
    (doseq [p params]
      (apply add-part b p))
    (.build b)))

(defn build [params]
  (let [^HttpEntity mpe (entity params)]
    {:body           (let [out (ByteArrayOutputStream.)]
                       (.writeTo mpe out)
                       (.close out)
                       (io/input-stream (.toByteArray out)))

     :content-length (.getContentLength mpe)
     :content-type   (.getValue (.getContentType mpe))
     :headers        {"content-type"   (.getValue (.getContentType mpe))
                      "content-length" (str (.getContentLength mpe))}}))

(defn multipart? [params]
  (let [known  (-> (methods add-part) (dissoc :default) keys)
        known? (fn [inst] (some #(instance? % inst) known))]
    (some known? (vals params))))

(ns monalisachallenge.models.lineage
  (:use [cheshire.core])
  (:require [cemerick.rummage :as sdb]
            [cemerick.rummage.encoding :as enc]
            [aws.sdk.s3 :as s3]))

(def client (sdb/create-client (System/getenv "AWS_ACCESS") (System/getenv "AWS_SECRET")))
(def config (assoc enc/keyword-strings :client client))
(def cred {:access-key (System/getenv "AWS_ACCESS"), :secret-key (System/getenv "AWS_SECRET")})

(defn get-parent
  "returns parent of representation"
  [id]
  (->> id
       (sdb/get-attrs
        config
        "monalisachallenge")
       (:parent)))

(declare lineage)

(def s3-base-bucket "monalisachallenge")
(def s3-base-path "lineages/%s")

(defn -lineage-from-cache
  "retrieves lineage from cache"
  [id]
  (let [path (format s3-base-path id)]
    (if (s3/object-exists? cred s3-base-bucket path)
      (parse-string (slurp (:content (s3/get-object cred s3-base-bucket path)))))))

(defn -lineage-to-cache
  "stores lineage in cache"
  [id result]
  (let [path (format s3-base-path id)]
    (s3/put-object cred s3-base-bucket path (generate-string result))))

(defn --lineage
  "determines lineage (ancestry starting with representation and ending at oldest ancestor
   returns lazy sequence of representation ids"
  [id]
  (lazy-seq
   (if-let [parent (get-parent id)]
     (cons id (--lineage parent)))))

(def lineage-from-cache (memoize -lineage-from-cache))

(defn -lineage
  [id]
  (if-let [result (lineage-from-cache id)]
    result
    (let [result (--lineage id)]
      (-lineage-to-cache id result)
      result)))

(def lineage (memoize -lineage))

(defn every-nth
  "returns every nth element of coll"
  [n coll]
  (map first (partition n coll)))

(defn limited-lineage
  "returns a limited lineage of id as a subsequence of linear of length k"
  [id k]
  (let [lin (lineage id)
        c   (count lin)
        c   (int (/ c k))]
    (rest (every-nth c lin))))
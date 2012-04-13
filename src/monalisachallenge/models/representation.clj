(ns monalisachallenge.models.representation
  (:use [monalisachallenge.rendering]
        [cheshire.core])
  (:require [cemerick.rummage :as sdb]
            [cemerick.rummage.encoding :as enc]
            [aws.sdk.s3 :as s3]
            [monalisachallenge.models.lineage :as lineage]
            [monalisachallenge.representation :as representation])
  (:import [java.awt Graphics Color]
           [java.awt.image BufferedImage]
           [javax.imageio ImageIO]
           [java.net URL]
           [java.security MessageDigest]))

(def client (sdb/create-client (System/getenv "AWS_ACCESS") (System/getenv "AWS_SECRET")))
(def config (assoc enc/keyword-strings :client client))
(def cred {:access-key (System/getenv "AWS_ACCESS"), :secret-key (System/getenv "AWS_SECRET")})

(defn colors-from-image
  [#^BufferedImage img]
  (for [x (range (.getWidth img))
        y (range (.getHeight img))]
       (Color. (.getRGB img x y))))

(defn l2-distance
  [#^Color x #^Color y]
  (let [r-x (.getRed x)
        g-x (.getGreen x)
        b-x (.getBlue x)
        r-y (.getRed y)
        g-y (.getGreen y)
        b-y (.getBlue y)]
    (+ (Math/pow (- r-x r-y) 2)
       (Math/pow (- g-x g-y) 2)
       (Math/pow (- b-x b-y) 2))))

(defn score-representation
  "scores a representation rendering against a reference image"
  [#^BufferedImage rendering #^BufferedImage reference]
  (let [pixels-rendering (colors-from-image rendering)
        pixels-reference (colors-from-image reference)]
    (reduce + (map l2-distance pixels-rendering pixels-reference))))

(def url-pattern "http://s3.amazonaws.com/monalisachallenge/mona-lisas/mona-lisa-%d-%d.jpg")

(defn url-for-reference
  [width height]
  (format url-pattern width height))

(defn -reference-image
  [width height]
  (try
    (ImageIO/read (URL. (url-for-reference width height)))
    (catch java.io.IOException e nil)))

(def reference-image (memoize -reference-image))

(defn reference-for-repr
  [repr]
  {:pre [(representation/valid-representation? repr)]}
  (let [width  (:width repr)
        height (:height repr)]
    (reference-image width height)))

;; from https://gist.github.com/1302024
(defn md5
  "Generate a md5 checksum for the given string"
  [token]
  (let [hash-bytes
         (doto (java.security.MessageDigest/getInstance "MD5")
               (.reset)
               (.update (.getBytes token)))]
       (.toString
         (new java.math.BigInteger 1 (.digest hash-bytes)) ; Positive and the size of the number
         16))) ; Use base16 i.e. hex

(defn hash-of-repr
  "returns a hash of representation"
  [repr]
  {:pre [(representation/valid-representation? repr)]}
  (let [repr  (select-keys repr [:height :width :polygons :background])
        repr  (pr-str repr)]
        (.substring (md5 repr) 0 8)))

(def best-repr (atom nil))

(defn query-best
  "queries for best representation"
  []
  (-> (sdb/query
      config
      "select * from monalisachallenge where score > '0' order by score limit 1")
      (first)))

(def base-url "http://s3.amazonaws.com/monalisachallenge/mona-lisas/representations/%s.png")

(defn url-of-repr
  "returns URL for image of representation"
  [id]
  (format base-url id))

(defn get-repr
  "returns representation from id"
  [id]
  (sdb/get-attrs config "monalisachallenge" id))

(defn url-of-best
  []
  (-> (query-best)
   (::sdb/id)
   (url-of-repr)))

(defn id-of-best
  []
  (-> (query-best)
      (::sdb/id)))

(defn read-representation-for-query-result
  "reads representation from s3"
  [result]
  (let  [path    (:representation-path result)
         content (s3/get-object cred "monalisachallenge" path)]
      (parse-string (slurp (:content content)) true)))

(defn best-representation
  "returns the representation with the best score"
  []
  (if-let [repr @best-repr]
    repr
    (if-let [result (query-best)]
          (read-representation-for-query-result result))))

(defn best-score
  "determines score of best representation"
  []
  (score-representation (render-representation (best-representation))
                        (reference-for-repr (best-representation))))

(defn score-of-id
  "scores representation with id"
  [id]
  (if-let [result (sdb/get-attrs config "monalisachallenge" id)]
    (let [repr       (read-representation-for-query-result result)
          repr       (:representation repr)
          rendering  (render-representation repr)
          reference  (reference-for-repr repr)]
      (score-representation rendering reference))))

(defn save!
  "saves a representation to SimpleDB and a rendered image to s3"
  [repr]
  {:pre  [(representation/valid-representation? (:representation repr))
          (if (:parent repr)
            (representation/valid-representation? (:parent repr))
            true)]
   :post [(representation/valid-representation? (:representation %))
          (if (:parent %)
            (representation/valid-representation? (:parent %))
            true)]}
  (let [base      (:representation repr)
        id        (hash-of-repr base)
        parent    (if-let [p (:parent repr)]
                    (hash-of-repr p))
        rendering (render-representation base)
        reference (reference-for-repr base)
        score     (score-representation rendering reference)
        scores    (format "%032.0f" score)
        path      (format "mona-lisa-representations/%s.json" id)
        base      (assoc repr :score score)]
    (if (or (not (score-of-id parent)) (< score (score-of-id parent)))
      (do
        (s3/put-object cred "monalisachallenge" path (generate-string base))
        (s3/put-object cred "monalisachallenge" (format "mona-lisas/representations/%s.png" id)
                       (make-input-stream rendering)
                       :content-type "image/png"
                       :canned-acl :public-read)
        (sdb/put-attrs config "monalisachallenge"
                       {::sdb/id id
                        :representation-path path
                        :score scores
                        :numerical-score score
                        :parent parent})
        ;;(lineage/lineage id) ;; save lineage to cache
        (swap! best-repr
               (fn [best] (if (or (not (:score best)) (< (:score base) (:score best))) base best)))))
    (best-representation)))
(ns monalisachallenge.models.representation
  (:use [monalisachallenge.rendering]
        [cheshire.core])
  (:require [cemerick.rummage :as sdb]
            [cemerick.rummage.encoding :as enc]
            [aws.sdk.s3 :as s3])
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
  (let [repr  (pr-str repr)]
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

(defn url-of-best
  []
  (-> (query-best)
      (::sdb/id)
      (url-of-repr)))

(defn best-representation
  "returns the representation with the best score"
  []
  (if-let [repr @best-repr]
    repr
    (let [result (query-best)
          path   (:representation-path result)
          content (s3/get-object cred "monalisachallenge" path)]
      (parse-string (slurp (:content content)) true))))

(defn save!
  "saves a representation to SimpleDB and a rendered image to s3"
  [repr]
  (let [base      (:representation repr)
        id        (hash-of-repr base)
        parent    (hash-of-repr (:parent repr))
        rendering (render-representation base)
        reference (reference-for-repr base)
        score     (score-representation rendering reference)
        scores    (format "%032.0f" score)
        path      (format "mona-lisa-representations/%s.json" id)
        repr      (assoc base :score score)]
    (s3/put-object cred "monalisachallenge" path (generate-string repr))
    (s3/put-object cred "monalisachallenge" (format "mona-lisas/representations/%s.png" id)
                   (make-input-stream rendering)
                   :content-type "image/png"
                   :canned-acl :public-read)
    (sdb/put-attrs config "monalisachallenge"
                   {::sdb/id id
                    :representation-path path
                    :score scores
                    :parent parent})
    (swap! best-repr
           (fn [best]
             (if (and best (< (:score best) (:score repr)))
               repr
               repr)))
    (best-representation)))
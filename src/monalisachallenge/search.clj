(ns monalisachallenge.search
  (:require [monalisachallenge.representation :as r]
            [monalisachallenge.models.representation :as m]
            [monalisachallenge.rendering :as n]
            [monalisachallenge.hyper :as h]))

(defn start
  "returns representation to use as search start point"
  [options]
  (if (:random-start options)
    (r/random-representation r/default-params)
    (m/best-representation)))

(defn score
  "determines score of representation"
  [repr]
  (let [ref      (m/reference-for-repr repr)
        rendered (n/render-representation repr)]
    (m/score-representation rendered ref)))

(defn best-descendant
  "finds best descendant of repr"
  [options params repr]
  (let [pop-size     (:pop-size options)
        population   (repeatedly pop-size #(r/mutate-representation params repr))
        population   (conj population repr)
        [score best] (->> population
                          (pmap (fn [r] [r (score r)]))
                          (sort-by last)
                          (first))]
    [score best]))

(defn search
  "performs an evolutionary search without exiting"
  [options]
  (loop [i      1
         best   (start options)
         params r/default-params]
    (let [mparams      (if (:mutate-params options) (h/mutate params) params)
          [next score] (best-descendant options params best)]
      (println (format "%d: [%d] %d" i (count (:polygons next)) (int score)))
      (if (= 0 (mod i 50)) (println (pr-str (dissoc params :limits))))
      (if (not= next best)
        (m/save! {:representation next :parent best :hyper mparams}))
      (recur (inc i)
             next
             (if (not= next best) mparams params)))))

(defn -main [& args]
  (.setLevel (java.util.logging.Logger/getLogger "com.amazonaws") java.util.logging.Level/WARNING)
  (search {:pop-size 500
           :random-start false
           :mutate-params false}))
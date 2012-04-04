(ns monalisachallenge.hyper
  (:require [monalisachallenge.stats :as stats]))

(defn mutate-point-within-limits
  "mutates a point within specified limits using distribution centered
   at current + (current - last)"
  [current last limits]
  (let [[a b]    limits
        center   (+ current (- current last))
        var      (* 2 (Math/abs (- current last)))
        samples  (repeatedly #(+ (stats/random-normal var) center))]
    (first (drop-while (fn [x] (or (<= x a) (>= x b))) samples))))

(defn default-last
  "determines rasonable last value"
  [limits]
  (->> (seq limits)
       (map (fn [[k v]] [k (nth v (rand-int 2))]))
       (into {})))

(defn mutate
  "mutates hyper parameters of the form:

   {:alpha 0.1
    :beta 0.2
    :limits {:alpha [0 1] :beta [0 0.5]}}
    :last {:alpha 0.05 :beta 0.25}

  both the :limits and :last keys are used for the hyperparameter optimization"
  [params]
  (let [limits (:limits params)
        lasts  (or (:last params) (default-last limits))
        params (dissoc params :limits :last)]
    (assoc (->> (seq params)
                (map (fn [[k v]] [k (if (limits k)
                                     (mutate-point-within-limits v (lasts k) (limits k))
                                     v)]))
                (into {}))
      :last params
      :limits limits)))
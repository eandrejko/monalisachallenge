(ns monalisachallenge.test.representation
  (:use [monalisachallenge.representation]
        [midje.sweet]))

(defn valid-polygon?
  [polygon]
  (and
   (polygon :alpha)
   (< 2 (count (polygon :points)))
   (polygon :color)))

(def params {
             :width 192
             :height 239
             :refine-polygon-thresh 0.1
             :polygon-color-noise 10
             :polygon-alpha-noise 0.1
             :add-polygon-thresh 0.1
             :background-color-noise 10
             :initial-polygon-count 5
             :point-noise 20
             })

(defn valid-representation?
  [repr]
  (and
   (repr :background)
   (every? valid-polygon? (repr :polygons))))

(fact "random-polygons are valid"
  (random-polygon params) => valid-polygon?)

(fact "representations generate valid polygons"
  (random-representation params) => valid-representation?)

(fact (mutate-point params [10 10]) => [10 10]
  (provided
    (add-int-noise 20 10) => 10))

(fact (mutate-point params [10 8]) => [12 10]
  (provided
    (add-int-noise 20 10) => 12
    (add-int-noise 20 8) => 10))

(fact "mutated-polygons are valid"
  (mutate-polygon params (random-polygon params)) => valid-polygon?)

(fact "mutate-representations are valid"
  (mutate-representation params (random-representation params)) => valid-representation?)
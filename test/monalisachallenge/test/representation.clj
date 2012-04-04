(ns monalisachallenge.test.representation
  (:use [monalisachallenge.representation]
        [monalisachallenge.stats]
        [midje.sweet]))

(defn valid-polygon?
  [polygon]
  (and
   (polygon :alpha)
   (< 2 (count (polygon :points)))
   (polygon :color)))

(defn valid-representation?
  [repr]
  (and
   (repr :background)
   (every? valid-polygon? (repr :polygons))))

(fact "random-polygons are valid"
  (random-polygon default-params) => valid-polygon?)

(fact "representations generate valid polygons"
  (random-representation default-params) => valid-representation?)

(fact (mutate-point default-params [10 10]) => [10 10]
  (provided
    (add-int-noise 20 10) => 10))

(fact (mutate-point default-params [10 8]) => [12 10]
  (provided
    (add-int-noise 20 10) => 12
    (add-int-noise 20 8) => 10))

(fact "mutated-polygons are valid"
  (mutate-polygon default-params (random-polygon default-params)) => valid-polygon?)

(fact "mutate-representations are valid"
  (mutate-representation default-params (random-representation default-params)) => valid-representation?)
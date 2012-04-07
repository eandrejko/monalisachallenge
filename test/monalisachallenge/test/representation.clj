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

(defn p
  [m]
  (merge default-params m))

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

(fact "refine points adds point when appropriate"
  (refine-points (p {:refine-polygon-thresh 1.0}) [[0 0] [1 1] [0 2]]) => (fn [points] (= 4 (count points)))
  (refine-points (p {:refine-polygon-thresh 0.0}) [[0 0] [1 1] [0 2]]) => (fn [points] (= 3 (count points))))

(fact "add-polygon adds polygon"
  (add-polygon (p {:add-polygon-thresh 1.0}) [1 2 3]) => (fn [polys] (= 4 (count polys))))

(fact "add-noisy-midpoint adds midpoint"
  (add-noisy-midpoint (p {:point-noise 0.0}) [0 0] [1 1]) => (fn [c] (= [1 1] c))) ;; [1 1] from rounding

(fact "remove-point removes random point"
  (coarsen-points (p {:coarsen-polygon-thresh 1.0}) [[0 0] [1 1] [0 2] [4 5]]) => (fn [points] (= 3 (count points)))
  (coarsen-points (p {:coarsen-polygon-thresh 1.0}) [[0 0] [1 1] [0 2]]) => (fn [points] (= 3 (count points)))
  (coarsen-points (p {:coarsen-polygon-thresh 0.0}) [[0 0] [1 1] [0 2]]) => (fn [points] (= 3 (count points))))
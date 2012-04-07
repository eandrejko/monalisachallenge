(ns monalisachallenge.representation
  (:require [monalisachallenge.stats :as stats]))

(def default-params {
                     :width 192
                     :height 239
                     :refine-polygon-thresh 0.1
                     :coarsen-polygon-thresh 0.1
                     :polygon-color-noise 10
                     :polygon-alpha-noise 0.1
                     :background-color-noise 5
                     :initial-polygon-count 5
                     :point-noise 20
                     :mutate-point-thresh 0.1
                     :mutate-alpha-thresh 0.1
                     :mutate-color-thresh 0.1
                     :mutate-back-color-thresh 0.1
                     :add-polygon-thresh 0.1
                     :remove-polygon-thresh 0.1
                     :limits {
                              :refine-polygon-thresh [0 0.2]
                              :coarsen-polygon-thresh [0 0.2]
                              :polygon-alpha-noise [0 0.2]
                              :polygon-color-noise [0 30]
                              :background-color-noise [0 10]
                              :point-noise [0 50]
                              :mutate-point-thresh [0 0.2]
                              :mutate-alpha-thresh [0 0.2]
                              :mutate-color-thresh [0 0.2]
                              :mutate-back-color-thresh [0 0.2]
                              :add-polygon-thresh [0 0.2]
                              :remove-polygon-thresh [0 0.2]
                              }})

(defn random-polygon
  "produces a random polygon"
  [params]
  (let [width    (get params :width)
        height   (get params :height)
        color    [(rand-int 255) (rand-int 255) (rand-int 255)]
        vertices 4;;(rand-int 25)
        points   (repeatedly vertices #(vector (rand-int width) (rand-int height)))
        alpha    (stats/urand 0.5 1.0)]
    {:color color
     :alpha alpha
     :points points}))

(defn random-representation
  [params]
  {:width (:width params)
   :height (:height params)
   :polygons (repeatedly (get params :initial-polygon-count)
                         (partial random-polygon params))
   :background [(rand-int 255) (rand-int 255) (rand-int 255)]})


(defn threshold-fn
  "returns (fn x) with probability thresh
   returns x with probability 1 - thresh"
  [thresh x f]
  (if (> (rand) thresh)
    x
    (f x)))

(defn mutate-point
  [params point]
  (let [add-noise (fn [p]
                    (let [[x y] p
                          x     (stats/add-int-noise (get params :point-noise) x)
                          y     (stats/add-int-noise (get params :point-noise) y)
                          x     (stats/clamp x 0 (get params :width))
                          y     (stats/clamp y 0 (get params :height))]
                      [x y]))]
    (threshold-fn (:mutate-point-thresh params) point add-noise)))

(def max-vertices 10)
(def min-vertices 3)

(defn add-noisy-midpoint
  "adds midpoint with noise to pair of points a,b"
  [params a b]
  (let [[x1 y1] a
        [x2 y2] b
        x       (/ (+ x1 x2) 2.0)
        y       (/ (+ y1 y2) 2.0)]
    (mutate-point (assoc params :mutate-point-thresh 1.0) [x y])))

(defn refine-points
  [params points]
  (let [thresh (get params :refine-polygon-thresh)]
    (if (and (< (rand) thresh)
             (< (count points) max-vertices))
      (let [[a b] (split-at (-> (count points)
                                (dec)
                                (rand-int)
                                (inc))
                            points)]
        (concat a 
                (list (add-noisy-midpoint params (last a) (first b)))
                b))
      points)))

(defn random-projection
  "removes random single element from coll"
  [coll]
  (let [[a b] (split-at (-> (count coll)
                            (rand-int))
                        coll)]
    (concat a (rest b))))

(defn coarsen-points
  [params points]
  (let [thresh (get params :coarsen-polygon-thresh)]
    (if (and (< (rand) thresh)
             (> (count points) min-vertices))
      (random-projection points)
      points)))

(def min-alpha 0.3)
(def max-alpha 0.8)

(defn mutate-color
  [params color alpha]
  (let [[r g b] color
        color-noise (fn [x] (-> (stats/add-int-noise (get params :polygon-color-noise) x)
                               (stats/clamp 0 255)))
        r       (threshold-fn (:mutate-color-thresh params) r color-noise)
        g       (threshold-fn (:mutate-color-thresh params) g color-noise)
        b       (threshold-fn (:mutate-color-thresh params) b color-noise)
        alpha-noise (fn [x]
                      (-> (stats/add-noise
                           (get params :polygon-alpha-noise) x)
                          (stats/clamp min-alpha max-alpha)))
        alpha   (threshold-fn (:mutate-alpha-thresh params) alpha alpha-noise)]
    [[r g b] alpha]))

(defn mutate-polygon
  [params polygon]
  (let [[color alpha] (mutate-color params (:color polygon) (:alpha polygon))
        points  (:points polygon)
        points  (->> points
                     (map (partial mutate-point params)))
        points  (->> points 
                     ((partial refine-points params)))
        points  (->> points 
                     ((partial coarsen-points params)))]
    {:color color
     :alpha alpha
     :points points}))


(def max-polygons 50)
(def min-polygons 3)

(defn add-polygon
  [params polygons]
  (if (< (count polygons) max-polygons)
    (let [[a b] (split-at (rand-int (count polygons)) polygons)]
      (concat a
              (list (random-polygon params))
              b))
    polygons))

(defn remove-polygon
  [polygons]
  (if (> (count polygons) min-polygons)
    (random-projection polygons)
    polygons))

(defn mutate-polygons
  [params polygons]
  (let [polygons (threshold-fn (:add-polygon-thresh params)
                               polygons
                               (partial add-polygon params))
        polygons (threshold-fn (:remove-polygon-thresh params)
                               polygons
                               remove-polygon)
        polygons (map (partial mutate-polygon params) polygons)]
    polygons))

(defn mutate-representation
  [params repr]
  (let [[r g b] (get repr :background)
        color-noise (fn [x] (-> (stats/add-int-noise (get params :background-color-noise) x)
                               (stats/clamp 0 255)))
        r       (threshold-fn (:mutate-back-color-thresh params) r color-noise)
        g       (threshold-fn (:mutate-back-color-thresh params) g color-noise)
        b       (threshold-fn (:mutate-back-color-thresh params) b color-noise)]
    (merge repr
           {:background [r g b]
            :polygons (mutate-polygons params (get repr :polygons))})))

(ns monalisachallenge.representation
  (:require [monalisachallenge.stats :as stats]))

(def default-params {
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


(defn mutate-point
  [params point]
  (let [[x y] point
        x     (stats/add-int-noise (get params :point-noise) x)
        y     (stats/add-int-noise (get params :point-noise) y)
        x     (stats/clamp x 0 (get params :width))
        y     (stats/clamp y 0 (get params :height))]
    [x y]))

(def max-vertices 25)

(defn refine-points
  [params points]
  (let [thresh (get params :refine-polygon-thresh)]
    (if (and (< (rand) thresh)
             (< (count points) max-vertices))
      (let [[a b] (split-at (rand-int (count points))
                            points)]
        (concat a
                (list (mutate-point params (first b)))
                b))
      points)))

(defn mutate-polygon
  [params polygon]
  (let [[r g b] (get polygon :color)
        r       (-> (stats/add-int-noise (get params :polygon-color-noise) r)
                    (stats/clamp 0 255))
        g       (-> (stats/add-int-noise (get params :polygon-color-noise) g)
                    (stats/clamp 0 255))
        b       (-> (stats/add-int-noise (get params :polygon-color-noise) b)
                    (stats/clamp 0 255))
        alpha   (-> (stats/add-noise (get params :polygon-alpha-noise) (get polygon :alpha))
                    (stats/clamp 0.0 1.0))
        points  (->> (get polygon :points)
                     (map (partial mutate-point params))
                     ((partial refine-points params)))]
    {:color [r g b]
     :alpha alpha
     :points points}))


(def max-polygons 50)

(defn mutate-polygons
  [params polygons]
  (let [thresh (get params :add-polygon-thresh)]
    (if (and (< (rand) thresh)
             (< (count polygons) max-polygons))
      (conj polygons (random-polygon params))
      (map (partial mutate-polygon params) polygons))))

(defn mutate-representation
  [params repr]
  (let [[r g b] (get repr :background)
        r       (-> (stats/add-int-noise (get params :background-color-noise) r)
                    (stats/clamp 0 255))
        g       (-> (stats/add-int-noise (get params :background-color-noise) g)
                    (stats/clamp 0 255))
        b       (-> (stats/add-int-noise (get params :background-color-noise) b)
                    (stats/clamp 0 255))]
    (merge repr
           {:background [r g b]
            :polygons (mutate-polygons params (get repr :polygons))})))

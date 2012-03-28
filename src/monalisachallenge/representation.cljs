(ns monalisachallenge.representation)

(defn urand
  "uniform random number betwen a and b"
  [a b]
  (+ a (* (rand) (- b a))))

(defn random-polygon
  "produces a random polygon"
  [params]
  (let [width    (get params :width)
        height   (get params :height)
        color    [(rand-int 255) (rand-int 255) (rand-int 255)]
        vertices 4;;(rand-int 25)
        points   (repeatedly vertices #(vector (rand-int width) (rand-int height)))
        alpha    (urand 0.5 1.0)]
    {:color color
     :alpha alpha
     :points points}))

(defn random-representation
  [params]
  {:polygons (repeatedly (get params :initial-polygon-count)
                         (partial random-polygon params))
   :background [(rand-int 255) (rand-int 255) (rand-int 255)]})


(defn random-normal
  [var]
  (let [u (rand)
        v (rand)]
    (* var
       (Math/sqrt
        (* -2 (Math/log u)))
       (Math/cos (* 2 Math/PI v)))))

(defn add-int-noise
  [var x]
  (Math/round (+ x (random-normal var))))

(defn add-noise
  [var x]
  (+ x (random-normal var)))

(defn clamp
  "clamps x between a and b"
  [x a b]
  (min (max x a) b))

(defn mutate-point
  [params point]
  (let [[x y] point
        x     (add-int-noise (get params :point-noise) x)
        y     (add-int-noise (get params :point-noise) y)
        x     (clamp x 0 (get params :width))
        y     (clamp y 0 (get params :height))]
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
        r       (-> (add-int-noise (get params :polygon-color-noise) r)
                    (clamp 0 255))
        g       (-> (add-int-noise (get params :polygon-color-noise) g)
                    (clamp 0 255))
        b       (-> (add-int-noise (get params :polygon-color-noise) b)
                    (clamp 0 255))
        alpha   (-> (add-noise (get params :polygon-alpha-noise) (get polygon :alpha))
                    (clamp 0.0 1.0))
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
        r       (-> (add-int-noise (get params :background-color-noise) r)
                    (clamp 0 255))
        g       (-> (add-int-noise (get params :background-color-noise) g)
                    (clamp 0 255))
        b       (-> (add-int-noise (get params :background-color-noise) b)
                    (clamp 0 255))]
    {:background [r g b]
     :polygons (mutate-polygons params (get repr :polygons))}))

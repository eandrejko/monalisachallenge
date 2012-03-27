;; this is a prototype of evolutionary optimization in the client
;; to demonstrate a working proof of concept


(ns monalisachallenge.client
  (:require
   [goog.dom :as dom]
   [goog.color :as colors]
   [goog.events :as events]
   [goog.graphics :as graphics]
   [monalisachallenge.loss :as loss]))

(def width 192)
(def height 239)

(defn build-canvas
  []
  (doto (graphics/CanvasGraphics. (str width) (str height))
    (.render (dom/getElement "representation"))))

(def g (build-canvas))

(defn color-as-hex
  "converts rgb triple to hex string"
  [color]
  (let [[r g b] color]
    (colors/rgbArrayToHex (array r g b))))

(defn draw-polygon
  "draws a polygon"
  [g polygon]
  (let [path   (graphics/Path.)
        fill   (color-as-hex (get polygon :color))
        points (get polygon :points)
        alpha  (get polygon :alpha)
        [[x y] & points] points]
    (.moveTo path x y)
    (doall (map (fn [[x y]] (.lineTo path x y)) points))
    (.lineTo path x y)
    (.drawPath g path
               nil
               (graphics/SolidFill. fill alpha))))

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

(defn get-image-data
  "gets image data from canvas element"
  [node]
  (-> node
      (.getContext "2d")
      (.getImageData 0 0 width height)
      (.-data)))

(defn measure-loss
  [pixel-array-a pixel-array-b]
  (->> (map #(Math/abs (- (aget pixel-array-a %) (aget pixel-array-b %))) (range (* width height 4)))
         (reduce +)))

(defn random-representation
  [params]
  {:polygons (repeatedly (get params :initial-polygon-count) (partial random-polygon params))
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

(defn measure-representation
  [reference canvas repr i]
  (.clear g true)
  (.setSize g width height)
  (.drawRect g 0 0 width height
             (graphics/Stroke. 1 "#444")
             (graphics/SolidFill. (color-as-hex (get repr :background)) 1))
  (doall (map (partial draw-polygon g) (get repr :polygons)))
  (loss/l2 (get-image-data reference)
           (get-image-data g)))

(defn best-descendant
  [repr params reference canvas]
  (let [descendants (repeatedly 5 (partial mutate-representation params repr))
        descendants (conj descendants repr)
        ranked      (->> descendants
                         (map #(vector
                                %
                                (measure-representation reference canvas % 0)))
                         (doall))
        _ (if (< (rand) 0.1)
            (do
              ;; (.log js/console (pr-str (map last ranked)))
              ;; (.log js/console (pr-str (get (ffirst ranked) :polygons)))
              ))
        ]
    
    (->> ranked
         (sort-by last)
         (ffirst))))

(defn iterate
  [params]
  (let [reference   (-> (dom/$$ "canvas")
                        (aget 1))
        canvas      (-> (dom/$$ "canvas")
                        (aget 0))]
    (swap! best best-descendant params reference canvas)
    ;;(.log js/console (measure-loss a b))
    (swap! counter inc)
    (-> (dom/getElement "info")
        (dom/setTextContent (str (count (get @best :polygons)) " "
                            (measure-representation reference canvas @best 0) " " @counter)))))

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

(def counter (atom 0))
(def best (atom (random-representation params)))

(defn ^:export start
  []
  (doto (graphics/CanvasGraphics. (str width) (str height))
    (.render (dom/getElement "reference"))
    (.drawImage 0 0 width height "/img/mona-lisa-head-192.jpg"))
  (let [timer  (goog.Timer. 1500)]
    (do (iterate params)
      (. timer (start))
      (events/listen timer goog.Timer/TICK (partial iterate params)))))

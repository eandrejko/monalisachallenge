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

(def g (doto (graphics/CanvasGraphics. (str width) (str height))
         (.render (dom/getElement "representation"))))

(defn color-as-hex
  "converts rgb triple to hex string"
  [color]
  (let [[r g b] color]
    (colors/rgbArrayToHex (array r g b))))

(defn draw-polygon
  "draws a polygon"
  [polygon]
  (let [path   (graphics/Path.)
        fill   (color-as-hex (get polygon :color))
        points (get polygon :points)
        alpha  (get polygon :alpha)
        [[x y] & points] points]
    (.moveTo path x y)
    (doall (map (fn [[x y]] (.lineTo path x y)) points))
    (.lineTo path x y)
    (.drawPath g path nil (graphics/SolidFill. fill alpha))))

(defn random-polygon
  "produces a random polygon"
  []
  (let [color    [(rand-int 255) (rand-int 255) (rand-int 255)]
        vertices 4;;(rand-int 25)
        points   (repeatedly vertices #(vector (rand-int width) (rand-int height)))
        alpha    (rand)]
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
  []
  {:polygons (repeatedly 25 random-polygon)
   :background [(rand-int 255) (rand-int 255) (rand-int 255)]})

(def counter (atom 0))
(def best (atom (random-representation)))

(defn add-int-noise
  [x]
  (+ x (- (rand-int 51) 25)))

(defn add-noise
  [x]
  (+ x (- (* (rand) 0.2) 0.1)))

(defn clamp
  "clamps x between a and b"
  [x a b]
  (min (max x a) b))

(defn mutate-point
  [point]
  (let [[x y] point
        x     (add-int-noise x)
        y     (add-int-noise y)
        x     (clamp x 0 width)
        y     (clamp y 0 height)]
    [x y]))

(def max-vertices 25)

(defn refine-points
  [points]
  (if (and (< (rand) 0.1)
           (< (count points) max-vertices))
    (let [[a b] (split-at (rand-int (count points)))]
      (concat a
              (list (mutate-point (first b)))
              b))
    points))

(defn mutate-polygon
  [polygon]
  (let [[r g b] (get polygon :color)
        r       (-> (add-int-noise r)
                    (clamp 0 255))
        g       (-> (add-int-noise g)
                    (clamp 0 255))
        b       (-> (add-int-noise b)
                    (clamp 0 255))
        alpha   (-> (add-noise (get polygon :alpha))
                    (clamp 0.0 1.0))
        points  (->> (get polygon :points)
                     (map mutate-point)
                     (refine-points))]
    {:color [r g b]
     :alpha alpha
     :points points}))

(def max-polygons 50)

(defn mutate-polygons
  [polygons]
  (if (and (< (rand) 0.1)
           (< (count polygons) max-polygons))
    (conj polygons (random-polygon))
    (map mutate-polygon polygons)))

(defn mutate-representation
  [repr]
  (let [[r g b] (get repr :background)
        r       (-> (add-int-noise r)
                    (clamp 0 255))
        g       (-> (add-int-noise g)
                    (clamp 0 255))
        b       (-> (add-int-noise)
                    (clamp 0 255))]
    {:background [r g b]
     :polygons (mutate-polygons (get repr :polygons))}))

(defn measure-representation
  [reference canvas repr]
  (.clear g true)
  (.setSize g width height)
  (.drawRect g 0 0 width height
             (graphics/Stroke. 1 "#444")
             (graphics/SolidFill. (color-as-hex (get repr :background)) 1))
  ;;(.log js/console "measure-representation" reference canvas)
  (doall (map draw-polygon (get repr :polygons)))
  ;;(.log js/console "measure-representation" reference canvas)
  (loss/l1 (get-image-data reference)
           (get-image-data canvas)))

(defn best-descendant
  [repr reference canvas]
  (let [descendants (repeatedly 5 (partial mutate-representation repr))
        descendants (conj descendants repr)]
    (->> descendants
         (map #(vector % (measure-representation reference canvas %)))
         (sort-by last)
         (ffirst))))

(defn iterate
  []
  (let [reference   (-> (dom/$$ "canvas")
                        (aget 1))
        canvas      (-> (dom/$$ "canvas")
                        (aget 0))]
    (swap! best best-descendant reference canvas)
    ;;(.log js/console (measure-loss a b))
    (swap! counter inc)
    (-> (dom/getElement "info")
        (dom/setTextContent (str (measure-representation reference canvas @best) " " @counter)))))

(defn ^:export start
  []
  (doto (graphics/CanvasGraphics. (str width) (str height))
    (.render (dom/getElement "reference"))
    (.drawImage 0 0 width height "/img/mona-lisa-head-192.jpg"))
  (let [timer (goog.Timer. 1500)]
    (do (iterate)
      (. timer (start))
      (events/listen timer goog.Timer/TICK iterate))))

;; this is a prototype of evolutionary optimization in the client
;; to demonstrate a working proof of concept


(ns monalisachallenge.client
  (:use [monalisachallenge.representation :only [random-representation mutate-representation]])
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

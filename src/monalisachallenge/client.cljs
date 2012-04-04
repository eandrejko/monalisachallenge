;; this is a prototype of evolutionary optimization in the client
;; to demonstrate a working proof of concept


(ns monalisachallenge.client
  (:require
   [goog.dom :as dom]
   [goog.color :as colors]
   [goog.events :as events]
   [goog.graphics :as graphics] 
   [goog.net.XhrIo :as xhr]
   [monalisachallenge.loss :as loss]
   [cljs.reader :as reader]
   [monalisachallenge.hyper :as hyper]
   [monalisachallenge.representation :as repr]))

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
  (let [descendants (repeatedly 5 (partial repr/mutate-representation params repr))
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

(defn make-js-map
  "makes a javascript map from a clojure one"
  [cljmap]
  (let [out (js-obj)]
    (doall (map #(aset out (name (first %)) (second %)) cljmap))
    out))

(defn set-best
  [e]
  (swap! best
         (fn [best]
           (or
            (:representation (reader/read-string (.getResponseText (.-currentTarget e))))
            (repr/random-representation params)))))

(defn iterate
  [params]
  (let [reference   (-> (dom/$$ "canvas")
                        (aget 1))
        canvas      (-> (dom/$$ "canvas")
                        (aget 0))]
    (if-let [prev-best @best]
      (let [p (hyper/mutate @current-params)]
        ;;(.log js/console (pr-str params))
        (swap! best best-descendant p reference canvas)
        ;;(.log js/console (measure-loss a b))
        (swap! counter inc)
        (if (= 0 (mod @counter 100))
            (xhr/send "/representation/" set-best))
        (if (not= prev-best @best)
          (do
            (swap! current-params (fn [_] p))
            (.log js/console (pr-str (dissoc p :last :limits)))
            (xhr/send "/representation/" set-best "POST" (pr-str @best) (make-js-map {"Content-type" "text/json"}))))
        (-> (dom/getElement "info")
            (dom/setTextContent (str (count (get @best :polygons)) " "
                                     (measure-representation reference canvas @best 0) " " @counter)))))))

(def params {
             :width 192
             :height 239
             :refine-polygon-thresh 0.1
             :polygon-color-noise 10
             :polygon-alpha-noise 0.1
             :add-polygon-thresh 0.05
             :background-color-noise 10
             :initial-polygon-count 5
             :point-noise 10
             :limits {
                      :refine-polygon-thresh [0 1]
                      :polygon-alpha-noise [0 1]
                      :polygon-color-noise [0 30]
                      :add-polygon-thresh [0 1]
                      :background-color-noise [0 15]
                      :point-noise [0 50]
                      }
             })

(def counter (atom 0))
(def best (atom nil))
(def current-params (atom params))

(defn ^:export start
  []
  (doto (graphics/CanvasGraphics. (str width) (str height))
    (.render (dom/getElement "reference"))
    (.drawImage 0 0 width height "/img/mona-lisa-head-192.jpg"))
  (xhr/send "/representation/" set-best)
  (let [timer  (goog.Timer. 700)]
    (do (iterate params)
        (. timer (start))
        (events/listen timer goog.Timer/TICK (partial iterate params)))))

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

(defn draw-representation
  [repr]
  (.clear g true)
  (.setSize g width height)
  (.drawRect g 0 0 width height
             (graphics/Stroke. 1 "#444")
             (graphics/SolidFill. (color-as-hex (get repr :background)) 1))
  (doall (map (partial draw-polygon g) (get repr :polygons))))

(defn measure-representation
  [reference canvas repr]
  (draw-representation repr)
  (loss/l2 (get-image-data reference)
           (get-image-data g)))

(defn best-descendant
  [repr params reference canvas]
  (let [descendants (repeatedly 5 (partial repr/mutate-representation params repr))
        descendants (conj descendants repr)
        ranked      (->> descendants
                         (map #(vector
                                %
                                (measure-representation reference canvas %)))
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
  (let [result (reader/read-string (.getResponseText (.-currentTarget e)))]
    (do
      (swap! best
             (fn [best]
               (or
                (:representation result)
                (repr/random-representation @current-params))))
      (swap! current-params
             (fn [params]
               (or (:hyper (:representation result))
                   params))))))

(defn reference-canvas
  []
  (-> (dom/$$ "canvas")
      (aget 1)))

(defn representation-canvas
  []
  (-> (dom/$$ "canvas")
      (aget 0)))

(defn iterate
  [params]
  (let [reference   (reference-canvas)
        canvas      (representation-canvas)]
    (if-let [prev-best @best]
      (let [p (hyper/mutate @current-params)]
        ;;(.log js/console (pr-str params))
        (swap! best best-descendant p reference canvas)
        ;;(.log js/console (measure-loss a b))
        (swap! counter inc)
        (if (not= prev-best @best)
          (do
            (swap! current-params (fn [_] p))
            ;;(.log js/console (pr-str (dissoc p :last :limits)))
            ;;(.log js/console (pr-str prev-best) (pr-str @best))
            (let [results {:representation @best
                           :hyper @current-params
                           :parent prev-best}]
              (xhr/send "/representation/" set-best "POST" (pr-str results)
                        (make-js-map {"Content-type" "text/json"})))
            (.log js/console (count (:polygons @best))))
          (if (= 0 (mod @counter 100))
            (xhr/send "/representation/" set-best)))
        (-> (dom/getElement "info")
            (dom/setTextContent (str (count (get @best :polygons)) " "
                                     (measure-representation reference canvas @best) " " @counter)))))))

(def counter (atom 0))
(def best (atom nil))
(def current-params (atom repr/default-params))

(defn ^:export init
  []
  (doto (graphics/CanvasGraphics. (str width) (str height))
    (.render (dom/getElement "reference"))
    (.drawImage 0 0 width height "/img/mona-lisa-head-192.jpg"))
  ;;(swap! best (fn [best] (repr/random-representation @current-params)))
  ;;(draw-representation @best)
  (xhr/send "/representation/" (fn [res] (set-best res) (draw-representation @best)))
  )

(defn ^:export start
  []
  (.log js/console "Starting")
  (let [timer  (goog.Timer. 500)]
    (do (iterate params)
        (. timer (start))
        (events/listen timer goog.Timer/TICK (partial iterate params)))))

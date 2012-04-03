(ns monalisachallenge.rendering
  (:import [java.awt Graphics Color Polygon]
           [java.awt.image BufferedImage]
           [javax.imageio ImageIO]
           [java.io ByteArrayOutputStream ByteArrayInputStream]))

(defn draw-polyon
  [#^Graphics graphics polygon]
  (let [points  (:points polygon)
        [r g b] (:color polygon)
        alpha   (try (Math/round (* (:alpha polygon) 255)) (catch java.lang.IllegalArgumentException e (* (:alpha polygon 255))))
        color   (Color. r g b alpha)
        polygon (Polygon. (int-array (map first points))
                          (int-array (map last points))
                          (count points))]
    (doto graphics
      (.setColor color)
      (.fillPolygon polygon))))

(defn draw-polygons
  "draws polygons to Graphics"
  [#^Graphics graphics polygons]
  (doall (->> polygons
              (map (fn [polygon] (draw-polyon graphics polygon)))))
  graphics)

(defn render-representation
  "renders representation as a BufferedImage"
  [repr]
  (let [width    (:width repr)
        height   (:height repr)
        [r g b]  (:background repr)
        img      (BufferedImage. width height BufferedImage/TYPE_INT_RGB)
        graphics (.createGraphics img)
        graphics (doto graphics
                   (.setColor (Color. r g b))
                   (.fillRect 0 0 width height))]
    (draw-polygons graphics (:polygons repr))
    img))

(defn make-input-stream
  "writes rendered representation to InputStream in PNG format"
  [#^BufferedImage img]
  (let [out (ByteArrayOutputStream.)]
    (ImageIO/write img "png" out)
    (ByteArrayInputStream. (.toByteArray out))))


(ns aloops.graphics
  (:require [quil.core :as q]
            [aloops.loopsapi :as loopsapi]
            [aloops.oscapi :as oscapi]))

(declare mundi)
(declare splash)

(defn setup-graphics []
  (q/color-mode :hsb 360 100 100 100)
  (q/text-align :left :center)
  ;; TODO añadir alguna tipografía en concreto?
  )

(defn load-resources []
  (intern 'aloops.graphics 'mundi (q/load-image "resources/1_BDatos/mundi_1728x1080.jpg"))
  (intern 'aloops.graphics 'splash (q/load-image "resources/1_BDatos/splash_1280x800.png")))
;; Parece que definir una variable def dentro de defn está muy mal.
;; he buscado una alternativa: declare + intern
;; supongo que también podría haber usado alter-var-root, pero no sé qué es mejor
;; Importante!!! No olvidar que antes de pasar a uberjar tengo que quitar resources/ del path.

(defn adapt-to-frame [state]
  (let [ratio (/ (q/width) (q/height))
        img-sz (:img-sz state)]
    (if (>= ratio 1.6)
      (assoc state :img-sz (assoc img-sz
                             :ix (/ (- (q/width) (* (q/height) 1.6)) 2)
                             :iy 0
                             :img-width (* (q/height) 1.6)
                             :img-height (q/height)))
      (assoc state :img-sz (assoc img-sz
                             :ix 0
                             :iy (/ (- (q/height) (/ (q/width) 1.6)) 2)
                             :img-width (q/width)
                             :img-height (/ (q/width) 1.6))))))

(defn draw-background [ix iy iwidth iheight]
    ;; background color = image background color
    (q/background 0 0 17)

    ;; image
    (q/image mundi ix iy iwidth iheight)

    ;; Grey strip behind album covers
    (q/no-stroke)
    (q/fill 50) ;; gris oscuro, fondo de las carátulas.
    (q/rect 0 0 (q/width) (+ (/ iheight 5) (/ (- (q/height) iheight) 2))) ;;rectángulo donde van las portadas

    ;; Credits
    (q/text-size (* iwidth 0.01))
    (q/fill 250)
    (q/text "www.antropoloops.com" (+ ix (* iwidth 0.015)) (+ iy (* iheight 0.95)))
    (q/fill 150)
    (q/text "www.mi-mina.com" (+ ix (* iwidth 0.015)) (+ iy (* iheight 0.97))))

;; En el caso de las funciones que se ejecutan dentro de draw no tenemos que preocuparnos de
;; devolver el estado porque no es tenido en cuenta para nada.

(defn draw-splash-screen [ix iy iwidth iheight]
  (if @loopsapi/ready?
    nil
    (q/image splash ix iy iwidth iheight)))

;; Graphic elements

(defn abanica [d factor h s b]
  (let [diam1 (* factor (cond
                          (<= d 40) (* d (/ 3 4))
                          (and (> d 40) (<= d 70)) (/ (- (* 4 d) 70) 3)
                          (and (> d 70) (<= d 80)) (- (* d 5) 280)
                          (> d 80) 120))
        diam2 (* factor (cond
                          (<= d 40) (* d 2)
                          (> d 40) (* d 2.5)))
        steps 20
        start (fn [x] (- (* (/ 3 2) Math/PI) (* x (/ (* 2 Math/PI) steps))))
        stop (* (/ 3 2) Math/PI)
        line-length (* (/ diam2 2) 1.1)
        ]
    (doseq [i (range steps)]
      (q/fill h s b 25)
      (q/no-stroke)
      (q/arc 0 0 diam1 diam1 (start i) stop)
      (q/fill h s b 2)
      (q/arc 0 0 diam2 diam2 (start i) stop))
    (q/stroke h s b)
    (q/stroke-weight 1)
    (q/line 0 0 0 (- line-length))
    ))

;; TODO The code below isn't DRY, It works, but I don't like it. Refactor it. Maybe using Plumbing and Graph?
;; Podría juntarlo todo en una sola función con un let enorme. Cómo conseguir seguir teniendo distintas
;; funciones para que quede claro que cada una se ocupa de una cosa distinta.

(defn draw-abanica-in-place [loop-index ix iy iwidth iheight factor state]
  (let [info (loop-index @oscapi/loops-info)
        track (keyword (str (first (name loop-index))))
        tempo (:tempo state)
        volume (* 100 (get-in state [:tracks-info track :volume]))
        x (+ ix (* factor (:x info)))
        y (+ iy (* factor (:y info)))
        w (q/radians (/ (q/millis) (/ (* (:loopend info) 60 1000) (* tempo 360))))]
    (q/push-matrix)
    (q/translate x y)
    (q/rotate w)
    (abanica volume factor (:color-h info)(:color-s info)(:color-b info))
    (q/pop-matrix)))

(defn draw-album-covers [loop-index ix iy iwidth iheight]
  (let [info (loop-index @oscapi/loops-info)
        sz (/ iheight 5)
        track (read-string (str (first (name loop-index))))
        pos-x (+ ix (* sz track))
        offset (/ sz 23)]
    (q/image (:image info) pos-x iy sz sz)
    (q/fill (:color-h info)(:color-s info)(:color-b info))
    (q/rect pos-x (+ iy sz) sz (/ sz 9))
    (q/fill 0 0 17)
    (q/text (:lugar info) (+ pos-x offset) (+ iy sz offset))
    (q/fill (:color-h info)(:color-s info)(:color-b info))
    (q/text (:fecha info) (+ pos-x offset) (+ iy sz offset (/ sz 9)))))
;; TODO añadir transparencia cuando baja el volumen de 45

(defn draw-lines [loop-index ix iy iwidth iheight factor]
  (let [info (loop-index @oscapi/loops-info)
        sz (/ iheight 5)
        track (read-string (str (first (name loop-index))))
        pos-x (+ ix (* sz track))
        offset (/ sz 23)
        x1 (+ (+ pos-x offset) (/ (q/text-width (:fecha info)) 2))
        y1 (+ iy sz (* 2 (/ sz 9)))
        x2 (+ ix (* factor (:x info)))
        y2 (+ iy (* factor (:y info)))]
    (q/stroke (:color-h info)(:color-s info)(:color-b info))
    (q/stroke-weight 2)
    (q/line x1 y1 x2 y2)
    (q/no-stroke)))















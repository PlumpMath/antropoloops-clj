(ns aloops.main
  (:import [oscP5 OscP5 OscMessage]
           [netP5 NetAddress])
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [aloops.osc :as osc]
            [aloops.oscapi :as oscapi]
            [aloops.graphics :as g]))



(def initial-width 1200)
(def initial-height 750)

(defn setup []
  (g/load-resources)
  (q/frame-rate 3)
  ;; initial state
  {:ix 0
   :iy 0
   :img-width initial-width
   :img-height initial-height}
  )



(defn update-state [state]
  (g/adapt-to-frame state)
  )


(defn draw [state]
  ;(apply q/background (:bg-color state))
  (g/draw-background state)
  )

(defn mouse-clicked [state event]
  (oscapi/async-request-info-for-all-clips)
  state)

(defn osc-event [state message]
  (println "pasando por osc-event-fn. mensage: " message)
  (println "pasando por osc-event-fn. path:" (.addrPattern message))
  (oscapi/process-osc-event message)
  )

(q/defsketch papplet
             :title "osc"
             :setup setup
             :draw draw
             :size [initial-width initial-height] ;; :fullscreen
             :update update-state
             :mouse-clicked mouse-clicked
             :osc-event osc-event
             ;; :diplay 1
             :features [:keep-on-top
                        ;:exit-on-close
                        :no-bind-output
                        :resizable
                        ;; :present
                        ]
             :middleware [m/fun-mode])

(oscapi/init-oscP5-communication papplet)

(defn -main [& args] )

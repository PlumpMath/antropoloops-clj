(ns aloops.oscapi
  (:require [quil.core :as q]
            [aloops.osc :as osc]
            [aloops.bd :as bd]))

;; No tengo muy clara cuál sería la diferencia entre este ns y loopsapi

(def loops-info (atom {}))
;; De momento he definido loops-info como un atom. No sé si hubiera gran cantidad de clips, por ejemplo, en una sesión entera
;; estaría garantizado que se añaden todos los clips o podría quedarse alguno fuera si el mensaje de respuesta de ableton
;; llega cuando todavía el atom no ha terminado de actualizarse por el mensaje anterior.
;; En loops-info guardo toda la información que pido una vez al comienzo de la sesión y no la vuelvo a pedir más a no
;; ser que se cambie de sesión. (habría que resetear la aplicación entera)

;; Un atom para saber cuándo han llegado todos los mensajes requeridos por "/live/name/clip"
;; En loops-info voy a guardar también loopend, ya que es un valor que en principio no va a cambiar tampoco a lo largo de la sesión
;; Sólo puedo guardar algo nuevo en loops-info una vez que me he asegurado que ya ha recibido toda la información
;; Además, para solicitar los loopends de cada clip, necesito saber qué clips están cargados porque la query se hace por cada slot, es decir,
;; tengo que adjuntar el track y el clip en cada mensaje.

(def clip-info-received? (atom false))


;; Iniciar comunicación con Ableton ********************************************************
(defn init-oscP5-communication [papplet]
  (osc/init-oscP5 papplet))


;; Funciones para preguntar a ableton ********************************************************
(defn async-request-info-for-all-clips []
  (osc/send-osc-message (osc/make-osc-message "/live/name/clip")))

(defn async-request-clip-state []
  (doseq [i (keys @loops-info)]
    (let [a (read-string (str (first (name i))))
          b (read-string (str (second (name i))))]
      (-> (osc/make-osc-message "/live/clip/info")
          (.add (int-array [a b]))
          (osc/send-osc-message)))))

(defn async-request-clips-loopend []
  (doseq [i (keys @loops-info)]
    (let [a (read-string (str (first (name i))))
          b (read-string (str (second (name i))))]
      (-> (osc/make-osc-message "/live/clip/loopend_id")
          (.add (int-array [a b]))
          (osc/send-osc-message)))))

(defn async-request-tempo []
  (-> (osc/make-osc-message "/live/tempo")
      (osc/send-osc-message)))

(defn async-request-volume-mute-solo []
  (doseq [paths ["/live/volume" "/live/mute" "/live/solo"]
          track (range 0 8)]
    (-> (osc/make-osc-message paths)
        (.add track)
        (osc/send-osc-message))))


;; Funciones para procesar los mensajes decibidos de Ableton ********************************************************

(defn load-loops-info [state message]
  ;; TODO: check that exist a place and a song if not throw an exception
  (let [[track clip nombre] (.arguments message)
        bd-song (first (filter #(= (:nombreArchivo %) nombre) bd/loops)) ;; pido el first porque el resultado es una secuencia de un elemento ({})
        bd-lugar (first (filter #(= (:lugar %) (:lugar bd-song)) bd/lugares))
        aloop {:track track
               :clip clip
               :nombre nombre
               :titulo (:titulo bd-song)
               :album (:album bd-song)
               :artista (:artista bd-song)
               :fecha (:fecha bd-song)
               :color-s (q/random 50 100 )
               :color-b (q/random 80 100)
               :color-h (condp = track ;; Hay que pasarlo a integer?
                          0 (q/random 105 120)
                          1 (q/random 145 160)
                          2 (q/random 300 315)
                          3 (q/random 330 345)
                          4 (q/random 195 210)
                          5 (q/random 230 245)
                          6 (q/random 25 40)
                          7 (q/random 50 65))
               :image (q/load-image (str "resources/0_portadas/" nombre ".jpg"))
               :lugar (:lugar bd-lugar)
               :x (:coordX bd-lugar)
               :y (:coordY bd-lugar)}
        index (keyword (str (:track aloop) (:clip aloop)))]
    (println "loading loops info for" nombre "(track" track "clip" clip ")")
    (swap! loops-info assoc index aloop)
    state)) ;; Devuelve el estado sin modificarlo

(defn load-clip-state [state message]
  (let [[track clip clip-state] (.arguments message)
        index (keyword (str track clip))]
    (assoc-in state [:loops-state index] clip-state)))

(defn load-clips-loopend [state message]
  (let [[track clip loopend] (.arguments message)
        index (keyword (str track clip))]
    (swap! loops-info assoc-in [index :loopend] loopend)
    state)) ;; Devuelve el estado sin modificarlo

(defn load-track-info [state message]
  (let [[track track-state] (.arguments message)
        track-index (keyword (str track))
        track-property (keyword (clojure.string/replace (.addrPattern message) #"/live/" ""))]
    (assoc-in state [:tracks-info track-index track-property] track-state)))

(defn load-tempo [state message]
  (assoc state :tempo (first (.arguments message))))


;; Función principal que procesa todos los mensajes recibidos de Ableton en :osc-event
(defn process-osc-event [state message]
  (let [path (osc/get-address-pattern message)]
    (condp = path
      "/live/name/clip"        (load-loops-info state message)
      "/live/name/clip/done"   (do (println (first (.arguments message))) (reset! clip-info-received? true) state)
      "/live/clip/info"        (load-clip-state state message)
      "/live/clip/loopend"     (load-clips-loopend state message)
                               ;; Aunque la pregunta es con /live/clip/loopend_id, la respuesta es con /live/clip/loopend
      "/live/volume"           (load-track-info state message)
      "/live/solo"             (load-track-info state message)
      "/live/mute"             (load-track-info state message)
      "/live/tempo"            (load-tempo state message)
      "/live/play"             (do (println "general play state" (first (.arguments message))) state)
      "/live/stop"             (do (println "general stop state" (first (.arguments message))) state)

      (do (println "not mapped. path: " (osc/get-address-pattern message))))))












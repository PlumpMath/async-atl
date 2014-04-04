(ns async-atl.core
  (:require-macros [cljs.core.async.macros
                    :refer [go]])
  (:require [cljs.core.async
             :refer [chan <! >! alts! timeout]
             :as async]
            [async-atl.util :refer [clear log]]))


;; core.async does communication over channels.


;; writing to a channel blocks unless there is a reader
(let [c (chan)]
  (go
   (log "We got here")
   (>! c :val)
   (log "We won't get here")))

(clear)

;; writing and reading can be done in any order
(let [c (chan)]
  (go
   (log "About to send")
   (>! c :val)
   (log "Just sent"))
  (go
   (log "About to receive")
   (log (str  "I received " (<! c)))))

(clear)

(let [c (chan)]
  (go
   (log "About to receive")
   (log (str  "I received " (<! c))))
    (go
   (log "About to send")
   (>! c :val)
   (log "Just sent")))

(clear)

;; timeouts can be used to delay things
(let [c (chan)]
(go
 (>! c "wait for it..."))
(go
 (<! (timeout 2000))
 (log (<! c))))

;; use alts! to implement a timeout
(let [c (chan)
      t (timeout 5000)]
  (go
   (let [[val port] (alts! [c t])]
     (if (= port t)
       (log "operation timed out")
       (log val)))))

(clear)

;; timeouts and loops can be used to create timers
(let [c (chan)]
  (go
   (dotimes [n 10]
     (<! (timeout 1000))
     (>! c n)))
  (go
   (while true
     (log (<! c)))))





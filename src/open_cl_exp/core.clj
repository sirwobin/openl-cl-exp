(ns open-cl-exp.core
  (:require [clojure.java.io :as io]
            [uncomplicate.commons
             [core :refer [with-release info]]
             [utils :refer [direct-buffer]]]
            [uncomplicate.clojurecl
             [core :refer :all]
             [info :refer [endian-little]]
             [toolbox :refer [decent-platform]]]))

(defn read-write-long-via-gpu
  "Run a function on the first available GPU setting a long param and expecting to read it back."
  [x]
  (when (-> x type (not= Long))
    (throw (ex-info "x is not a long." {:value x})))

  (with-release [dev        (-> (platforms) decent-platform devices first) ; (first (devices (decent-platform (platforms))))
                 ctx        (context [dev])
                 cqueue     (command-queue ctx dev)
                 my-buffer  (cl-buffer ctx 16 :read-write)
                 my-program (as-> "put-long-into-buf.cl" $
                                  (io/resource $)
                                  (slurp $)
                                  (program-with-source ctx [$])
                                  (build-program! $))
                 my-kernel  (kernel my-program "put_long_into_buf")]
    (let [result (long-array 2)]
      (set-args! my-kernel x my-buffer)
      ;(set-arg! my-kernel 0 x)  ; causes error CL_INVALID_ARG_VALUE
      ;(set-arg! my-kernel 1 my-buffer)
      (enq-kernel! cqueue my-kernel (work-size [1]))
      (enq-read! cqueue my-buffer result)
      (vec result))))

(comment
  (read-write-long-via-gpu 12345))
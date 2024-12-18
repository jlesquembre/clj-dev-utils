(ns local-utils
  (:require
    [clojure.reflect :refer [reflect]]
    [clojure.string :as string]
    [clojure.pprint :as pprint]
    [lambdaisland.deep-diff2 :as ddiff]
    [com.gfredericks.dot-slash-2 :as dot-slash-2]
    [hashp.core]
    [snitch.core :as snitch]
    [portal.api :as p]
    [nrepl.cmdline]
    [clojure.tools.namespace.repl :as c.t.n.r]))

;; ------------
;;
;; Global utils
;;
;; ------------

(c.t.n.r/disable-reload!)

(require '[sc.api :refer [letsc defsc]])

(defmacro log
  "Useful to print multiple variables at once"
  [& xs]
  `(array-map ~@(mapcat #(vector (keyword %) %) xs)))

(defmacro letsc*
  "Like letsc, but uses the last (most recently evaluated) EP id."
  [& body]
  `(letsc ~(sc.api/last-ep-id) ~@body))

(defmacro defsc*
  "Like defsc, but uses the last (most recently evaluated) EP id."
  []
  `(defsc ~(sc.api/last-ep-id)))


(defn logsc
  "Shows information about scope.capture bindings"
  []
  (:sc.ep/local-bindings (sc.api/ep-info)))

; From
; https://github.com/jorinvo/clj-scratch
(defn jmethods
  "Print methods of a Java object"
  [o]
  (->> o
      reflect
      :members
      (filter :exception-types)
      (sort-by :name)
      (map #(select-keys % [:name :parameter-types]))))

(defn start-nrepl
  []
  (nrepl.cmdline/-main "--middleware" "[cider.nrepl/cider-middleware]"))

(defn diff-pp
  "Compare two values recursively."
  [expected actual]
  (ddiff/pretty-print (ddiff/diff expected actual)))

(defn spit-pp
  "Spit pretty-printed object"
  [f content & options]
  (if options
    (spit f (with-out-str (pprint/pprint content)) options)
    (spit f (with-out-str (pprint/pprint content)))))


(def portal nil)

(defn get-system
  []
  (try
    (deref (requiring-resolve 'donut.system.repl.state/system))
    (catch java.io.FileNotFoundException _
      (println "ERROR Did you include donut.system in your deps.edn file?"))))


(defn system*
  "Get a specific component instance. With no arguments returns set of all
  component names."
  [& args]
  (try
    (let [instance (requiring-resolve 'donut.system/instance)
          my-system (deref (requiring-resolve 'donut.system.repl.state/system))]
      (if (empty? args)
        (instance my-system)
        (instance my-system args)))
    (catch java.io.FileNotFoundException _
      (println "ERROR Did you include donut.system in your deps.edn file?"))))


(defn system-fn*
  "Like system*, but returns a function that will return the component instance"
  []
  (try
    (let [instance (requiring-resolve 'donut.system/instance)
          my-system (deref (requiring-resolve 'donut.system.repl.state/system))]
      (fn
        ([] (instance my-system))
        ([& args] (instance my-system args))))
    (catch java.io.FileNotFoundException _
      (println "ERROR Did you include donut.system in your deps.edn file?"))))

(deftype SystemHelper []
  clojure.lang.ILookup
   (valAt [_ k] ((system-fn*) k))
  clojure.lang.IFn
   (applyTo [this xs] (clojure.lang.AFn/applyToHelper  this xs))
   (invoke [_] ((system-fn*)))
   (invoke [_ arg1] ((system-fn*) arg1))
   (invoke [_ arg1 arg2] ((system-fn*) arg1 arg2))
   (invoke [_ arg1 arg2 arg3] ((system-fn*) arg1 arg2 arg3))
   (invoke [_ arg1 arg2 arg3 arg4] ((system-fn*) arg1 arg2 arg3 arg4))
   (invoke [_ arg1 arg2 arg3 arg4 arg5] ((system-fn*) arg1 arg2 arg3 arg4 arg5)))

(def system-helper (->SystemHelper))

(defn open-portal
  []
  (alter-var-root #'portal (fn [_] (p/open))))

;; See
;; https://github.com/gfredericks/dot-slash-2
(defn my-dot-slash []
  (dot-slash-2/!
   `{. [{:var clojure.repl/doc
         :name ~'doc}
        {:var clojure.repl/source
         :name ~'source}

        ; {:var donut.system.repl.state/system
        ;  :name ~'system}

        ; {:var get-system
        ;  :name ~'system*}

        {:var system-helper
         :name ~'system}

        {:var lambdaisland.deep-diff2/diff
         :name ~'diff}
        diff-pp
        spit-pp

        {:var letsc*
         :name ~'letsc}
        {:var defsc*
         :name ~'defsc}
        sc.api/spy
        logsc

        jmethods

        log

        ; clojure.tools.namespace.repl/refresh
        clojure.repl/apropos
        clojure.repl/dir
        clojure.java.shell/sh

        com.gfredericks.repl/pp

        portal
        open-portal
        {:var portal.api/clear
         :name ~'pclear}

        {:var snitch/defn*
         :name ~'defn}

        {:var snitch/defmethod*
         :name ~'defmethod}

        {:var snitch/*fn
         :name ~'fn}

        {:var snitch/*let
         :name ~'let}]}))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn init
  [{:keys [main args exec nrepl extra-requires] :as options
    :or {nrepl true
         exec false}}]
  (println "INFO Loading local utils...")
  (my-dot-slash)

  (when main
    (let [my-ns (-> main namespace symbol)]
      (println (str "INFO Loading " my-ns))
      (require my-ns))
    (when exec
      (println (str "INFO Executing " main))
      (apply (resolve main) args)))

  (when (:portal options)
    (open-portal))
    ; (alter-var-root #'portal (fn [_] (p/open))))
  (add-tap #'p/submit)

  ;; Moved to reload-system fn
  ; (try
  ;   ((requiring-resolve 'malli.dev/start!))
  ;   (catch java.io.FileNotFoundException _))

  (set! *warn-on-reflection* true)

  (when extra-requires
    (->> (string/split (str extra-requires) #",")
         (map (comp vector symbol string/trim))
         (map (fn [lib]
                (println (str "Extra require -> (require '" lib ")"))
                lib))
         (run! require)))

  (when nrepl
    (start-nrepl)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
;; Invoked after system reload by Conjure configuration
(defn reload-system
  []

  (try
    ; From
    ; https://github.com/metosin/malli/blob/39ccfef96b54beb3d862b1eab5f5be90ec0f4456/src/malli/dev.clj#L18-L44
    ((requiring-resolve 'malli.instrument/instrument!) {:report ((requiring-resolve 'malli.dev.pretty/reporter))})
    ((requiring-resolve 'malli.clj-kondo/emit!))
    (println "INFO Malli intrumentation reloaded...")
    (catch java.io.FileNotFoundException _))

  (try
    (if (not (contains? (methods @(requiring-resolve 'donut.system/named-system))
                        :donut.system/repl))
      (println "INFO No donut.system/repl defined, skip system reload")
      (do
        ((requiring-resolve 'donut.system.repl/stop))
        ((requiring-resolve 'donut.system.repl/start))
        (println "INFO Donut system reloaded...")))
    (catch java.io.FileNotFoundException _))

  (try
    ((requiring-resolve 'integrant.repl/reset))
    (println "INFO integrant system reloaded...")
    (catch java.io.FileNotFoundException _)))

;; Legacy utils, when I was using user.clj
;;
;; -----------
;;
;; clj -A:user
;;
;; -----------

;; Trick to load all user.clj files in path
;; Usage: clj -A:user:other-alias
;; See
;; https://clojureverse.org/t/how-are-user-clj-files-loaded/3842/2
;; https://github.com/gfredericks/user.clj

; (defn- static-classpath-dirs
;   []
;   (mapv #(.getCanonicalPath  %) (classpath/classpath-directories)))

; (defn user-clj-paths
;   []
;   (->> (static-classpath-dirs)
;     (map #(io/file % "user.clj"))
;     (filter #(.exists %))))


; (defn load-user!
;   [f]
;   (try
;     (println (str "Loading " f))
;     (load-file (str f))
;     (catch Exception e
;       (binding [*out* *err*]
;         (printf "WARNING: Exception while loading %s\n" f)
;         (println e)))))


; (defn load-all-user!
;   []
;   (let [paths (user-clj-paths)]
;     (println (str "Loading " (first paths)))
;     (doall
;       (map load-user! (rest paths)))))

; (load-all-user!)

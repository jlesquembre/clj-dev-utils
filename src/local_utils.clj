(ns local-utils
  (:require
    [clojure.reflect :refer [reflect]]
    [clojure.string :as string]
    [clojure.pprint :as pprint]
    [lambdaisland.deep-diff2 :as ddiff]
    [com.gfredericks.dot-slash-2 :as dot-slash-2]
    [hashp.core]
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

;; See
;; https://github.com/gfredericks/dot-slash-2
(defn my-dot-slash []
  (dot-slash-2/!
   `{. [{:var clojure.repl/doc
         :name ~'doc}
        {:var clojure.repl/source
         :name ~'source}

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
        {:var portal.api/clear
         :name ~'pclear}]}))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn init
  [{:keys [main args exec nrepl extra-requires] :as options
    :or {nrepl true
         exec false}}]
  (println "Loading local utils...")
  (my-dot-slash)

  (when main
    (let [my-ns (-> main namespace symbol)]
      (println (str "Loading " my-ns))
      (require my-ns))
    (when exec
      (println (str "Executing " main))
      (apply (resolve main) args)))

  (when (:portal options)
    (alter-var-root #'portal (fn [_] (p/open))))
  (add-tap #'p/submit)

  (try
    ((requiring-resolve 'malli.dev/start!))
    (catch java.io.FileNotFoundException _))

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
(defn reload-system
  []
  (try
    ((requiring-resolve 'donut.system.repl/stop))
    ((requiring-resolve 'donut.system.repl/start))
    (catch java.io.FileNotFoundException _))

  (try
    ((requiring-resolve 'integrant.repl/reset))
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

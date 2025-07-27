;; Copyright (c) Stuart Sierra, 2012. All rights reserved. The use and
;; distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this
;; distribution. By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license. You must not
;; remove this notice, or any other, from this software.

(ns ^{:author "Stuart Sierra, modified for ClojureCLR by David Miller"
      :doc "Read and track namespace information from files"}
  clojure.tools.namespace.file
  (:require [clojure.clr.io :as io]                    ;;; clojure.java.io
            [clojure.tools.namespace.parse :as parse]
            [clojure.tools.namespace.track :as track])
  (:import (clojure.lang PushbackTextReader)))          ;;; (java.io PushbackReader)))

(defn read-file-ns-decl
  "Attempts to read a (ns ...) declaration from file, and returns the
  unevaluated form. Returns nil if ns declaration cannot be found.
  read-opts is passed through to tools.reader/read."
  ([file]
   (read-file-ns-decl file nil))
  ([file read-opts]
   (with-open [rdr (PushbackTextReader. (io/text-reader file))]                ;;; PushbackReader.  io/reader
     (parse/read-ns-decl rdr read-opts))))

(declare is-file? is-directory?)

(defn file-with-extension?
  "Returns true if the java.io.File represents a file whose name ends
  with one of the Strings in extensions."
  {:added "0.3.0"}
  [^System.IO.FileInfo file extensions]                                    ;;; ^java.io.File 
  (and (is-file? file)                                                         ;;; (.isFile file)  java.io.File conflates regular files and directors.  Not so with FileInfo
       (let [extn (.Extension file)]                                           ;;; name (.getName file)
         (some #(= extn %) extensions))))                                      ;;; #(.endsWith name %)

(def ^{:added "0.3.0"}
  clojure-extensions
  "File extensions for Clojure (JVM) files."
  (list ".clj" ".cljc"))

(def ^{:added "0.3.0"}
  clojurescript-extensions
  "File extensions for ClojureScript files."
  (list ".cljs" ".cljc"))

(def ^{:added "0.3.0"}
  clojure-clr-extensions
  "File extensions for Clojure (CLR) files."
  (list ".cljr" ".cljc" ".clj" ))

(defn clojure-file?
  "Returns true if the java.io.File represents a file which will be
  read by the Clojure (JVM) compiler."
  [^System.IO.FileSystemInfo file]                                         ;;; java.io.File
  (file-with-extension? file clojure-extensions))

(defn clojurescript-file?
  "Returns true if the java.io.File represents a file which will be
  read by the Clojure (JVM) compiler."
  [^System.IO.FileSystemInfo file]                                         ;;; java.io.File
  (file-with-extension? file clojurescript-extensions))

(defn clojure-clr-file?
  "Returns true if the java.io.File represents a file which will be
  read by the Clojure (JVM) compiler."
  [^System.IO.FileSystemInfo file]                                         ;;; java.io.File
  (file-with-extension? file clojure-clr-extensions))

;; Dealing with FileInfo.Equals is reference-based, not structuaral  -- via Brandon Correa

(defn- files= [file-1 file-2]
  (= (.-FullName file-1) (.-FullName file-2)))

(defn some-file [coll file]
  (reduce #(when (files= file %2) (reduced %2)) nil coll))
  
(defn into-files [files others]
  (into files (remove #(some-file files %) others)))

(defn- dissoc-files [m files]
  (when m
    (select-keys m (remove #(some-file files %) (keys m)))))

(defn- get-file [filemap file]
  (reduce #(when (files= file (first %2)) (reduced (second %2))) nil filemap))

(defn- files->symbols [tracker files]
  (let [filemap (::filemap tracker {})]
    (keep #(get-file filemap %) files)))

(defn- merge-file-map [m other]
  (merge (dissoc-files m (keys other)) other))

(defn- distinct-files [files]
  (reduce #(-> (disj %1 (some-file %1 %2))
               (conj %2)) #{} files))  

;; 
 
;;; Dependency tracker

(defn- files-and-deps [files read-opts]
  (reduce (fn [m file]
            (if-let [decl (read-file-ns-decl file read-opts)]
              (let [deps (parse/deps-from-ns-decl decl)
                    name (parse/name-from-ns-decl decl)]
                (-> m
                    (assoc-in [:depmap name] deps)
                    (assoc-in [:filemap file] name)))
              m))
          {} (distinct-files files)))                                      ;;; files
 
 
(def ^:private merge-map (fnil merge-file-map {}))                         ;;;  (fnil merge {})

(defn add-files
  "Reads ns declarations from files; returns an updated dependency
  tracker with those files added. read-opts is passed through to
  tools.reader."
  ([tracker files]
   (add-files tracker files nil))
  ([tracker files read-opts]
   (let [{:keys [depmap filemap]} (files-and-deps files read-opts)]
     (-> tracker
         (track/add depmap)
         (update ::filemap merge-map filemap)))))                          ;;; (update-in [::filemap] merge-map filemap)

(defn remove-files
  "Returns an updated dependency tracker with files removed. The files
  must have been previously added with add-files."
  [tracker files]
  (-> tracker
      (track/remove (files->symbols tracker files))                        ;;; (track/remove (keep (::filemap tracker {}) files))
      (update ::filemap dissoc-files files)))                              ;;; (update-in [::filemap] #(apply dissoc % files))

;;;  Added

(defn is-file? [^System.IO.FileSystemInfo file]
  (not= (enum-and (.Attributes file) System.IO.FileAttributes/Directory) System.IO.FileAttributes/Directory))

(defn is-directory? [^System.IO.FileSystemInfo file]
  (= (enum-and (.Attributes file) System.IO.FileAttributes/Directory) System.IO.FileAttributes/Directory))  

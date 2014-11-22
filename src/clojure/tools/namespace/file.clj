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
  unevaluated form.  Returns nil if read fails, or if the first form
  is not a ns declaration."
  [file]
  (with-open [rdr (PushbackTextReader. (io/text-reader file))]       ;;; PushbackReader.  io/reader
    (parse/read-ns-decl rdr)))
(declare is-file? is-directory?)
(defn clojure-file?
  "Returns true if the java.io.File represents a normal Clojure source
  file."
  [^System.IO.FileSystemInfo file]                                         ;;; java.io.File
  (and (is-file? file)   ;;; (.isFile file)
       (.EndsWith (.Name file) ".clj")))                                ;;; .endsWith  .getName 

;;; Dependency tracker

(defn- files-and-deps [files]
  (reduce (fn [m file]
            (if-let [decl (read-file-ns-decl file)]
              (let [deps (parse/deps-from-ns-decl decl)
                    name (second decl)]
                (-> m
                    (assoc-in [:depmap name] deps)
                    (assoc-in [:filemap file] name)))
              m))
          {} files))

(defn add-files
  "Reads ns declarations from files; returns an updated dependency
  tracker with those files added."
  [tracker files]
  (let [{:keys [depmap filemap]} (files-and-deps files)]
    (-> tracker
        (track/add depmap)
        (update-in [::filemap] (fnil merge {}) filemap))))

(defn remove-files
  "Returns an updated dependency tracker with files removed. The files
  must have been previously added with add-files."
  [tracker files]
  (-> tracker
      (track/remove (keep (::filemap tracker {}) files))
      (update-in [::filemap] #(apply dissoc % files))))

;;;  Added

(defn is-file? [^System.IO.FileSystemInfo file]
  (not= (enum-and (.Attributes file) System.IO.FileAttributes/Directory) System.IO.FileAttributes/Directory))
  
(defn is-directory? [^System.IO.FileSystemInfo file]
  (= (enum-and (.Attributes file) System.IO.FileAttributes/Directory) System.IO.FileAttributes/Directory))  
  

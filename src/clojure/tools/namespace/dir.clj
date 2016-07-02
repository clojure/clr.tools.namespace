;; Copyright (c) Stuart Sierra, 2012. All rights reserved. The use and
;; distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this
;; distribution. By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license. You must not
;; remove this notice, or any other, from this software.

(ns ^{:author "Stuart Sierra, modified for ClojureCLR by David Miller"
      :doc "Track namespace dependencies and changes by monitoring
  file-modification timestamps"}
  clojure.tools.namespace.dir
  (:require [clojure.tools.namespace.file :as file]
            [clojure.tools.namespace.track :as track]
			;;;[clojure.java.classpath :refer [classpath-directories]]
            [clojure.clr.io :as io]                                  ;;; clojure.java.io
            [clojure.set :as set]
            [clojure.string :as string])
  (:import (System.IO DirectoryInfo FileSystemInfo Path) (System.Text.RegularExpressions Regex)))    ;;; (java.io File) (java.util.regex Pattern)
(declare make-dir-info)
(defn- find-files [dirs]
  (->> dirs
       (map make-dir-info)                         ;;; (map io/file)
       (filter #(.Exists ^DirectoryInfo %))             ;;; #(.exists ^File %)
       (mapcat file-seq)
       (filter file/clojure-file?)
       ))                                               ;;;  (map #(.getCanonicalFile ^File %))  -- no equivalent?

(defn- modified-files [tracker files]
  (filter #(DateTime/op_LessThan ^DateTime (::time tracker 0) (.LastWriteTimeUTC ^FileSystemInfo %)) files))         ;;; (.lastModified ^File %)

(defn- deleted-files [tracker files]
  (set/difference (::files tracker #{}) (set files)))

(defn- update-files [tracker deleted modified]
  (let [now (DateTime/UtcNow)]                                             ;;; (System/currentTimeMillis)
    (-> tracker
        (update-in [::files] #(if % (apply disj % deleted) #{}))
        (file/remove-files deleted)
        (update-in [::files] into modified)
        (file/add-files modified)
        (assoc ::time now))))

(defn- dirs-on-classpath []                                                    ;;; This has been replaced in JVM by a call to clojure.java.classpath/classpath-directories.  We don't have that, so we're leaving this in
  (filter file/is-directory?                                                   ;;; #(.isDirectory ^File %)
          (map #(DirectoryInfo. ^String %)                                     ;;; #(File. ^String %)
               (string/split
                (Environment/GetEnvironmentVariable "CLOJURE_LOAD_PATH")       ;;; (System/getProperty "java.class.path")
                (Regex. (str "\\" System.IO.Path/PathSeparator))))))           ;;; (Pattern/compile (Pattern/quote File/pathSeparator))))))

(defn scan
  "Scans directories for files which have changed since the last time
  'scan' was run; update the dependency tracker with
  new/changed/deleted files.

  If no dirs given, defaults to all directories on the classpath."
  [tracker & dirs]
  (let [ds (or (seq dirs) (dirs-on-classpath))                              ;;; (classpath-directories)
        files (find-files ds)
        deleted (seq (deleted-files tracker files))
        modified (seq (modified-files tracker files))]
    (if (or deleted modified)
      (update-files tracker deleted modified)
      tracker)))

(defn scan-all
  "Scans directories for all Clojure source files and updates the
  dependency tracker to reload files. If no dirs given, defaults to
  all directories on the classpath."
  [tracker & dirs]
  (let [ds (or (seq dirs) (dirs-on-classpath))                              ;;; (classpath-directories)
        files (find-files ds)
        deleted (seq (deleted-files tracker files))]
    (update-files tracker deleted files)))
	
;;; ADDED

(defn- make-dir-info 
  ^DirectoryInfo [x]
  (cond 
    (instance? DirectoryInfo x) x
	(string? x) (DirectoryInfo. ^String x)
	:default (DirectoryInfo. (str x))))
	
	
  

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
            [clojure.tools.namespace.find :as find]
            [clojure.tools.namespace.track :as track]
			;;;[clojure.java.classpath :refer [classpath-directories]]
            [clojure.clr.io :as io]                                  ;;; clojure.java.io
            [clojure.set :as set]
            [clojure.string :as string])
  (:import (System.IO DirectoryInfo FileSystemInfo Path) (System.Text.RegularExpressions Regex)))    ;;; (java.io File) (java.util.regex Pattern)
(declare make-dir-info)
(defn- find-files [dirs platform]
  (->> dirs
       (map make-dir-info)                         ;;; (map io/file)
       ;;;                                         ;;; I have no idea if this is necessary. (map #(.getCanonicalFile ^File %))
       (filter #(.Exists ^DirectoryInfo %))             ;;; #(.exists ^File %)
       (mapcat #(find/find-sources-in-dir % platform))
       ))                                          ;;; ditto:  (map #(.getCanonicalFile ^File %))

(defn- modified-since-tracked? [tracker file]
  (if-let [time (::time tracker)]
    (DateTime/op_LessThan time (.LastWriteTimeUtc ^FileSystemInfo file))
    true))

(defn- modified-files [tracker files]
  (filter (partial modified-since-tracked? tracker) files))         ;;; (.lastModified ^File %)

(defn- deleted-files [tracker files]
  (set/difference (::files tracker #{}) (set files)))

(defn- update-files [tracker deleted modified {:keys [read-opts]}]
  (let [now (DateTime/UtcNow)]                                             ;;; (System/currentTimeMillis)
    (-> tracker
        (update-in [::files] #(if % (apply disj % deleted) #{}))
        (file/remove-files deleted)
        (update-in [::files] into modified)
        (file/add-files modified read-opts)
        (assoc ::time now))))

(defn- dirs-on-classpath []                                                    ;;; This has been replaced in JVM by a call to clojure.java.classpath/classpath-directories.  We don't have that, so we're leaving this in
  (filter file/is-directory?                                                   ;;; #(.isDirectory ^File %)
          (map #(DirectoryInfo. ^String %)                                     ;;; #(File. ^String %)
               (string/split
                (Environment/GetEnvironmentVariable "CLOJURE_LOAD_PATH")       ;;; (System/getProperty "java.class.path")
                (Regex. (str "\\" System.IO.Path/PathSeparator))))))           ;;; (Pattern/compile (Pattern/quote File/pathSeparator))))))

(defn scan-files
  "Scans files to find those which have changed since the last time
  'scan-files' was run; updates the dependency tracker with
  new/changed/deleted files.

  files is the collection of files to scan.

  Optional third argument is map of options:

    :platform  Either clj (default) or cljs, both defined in
               clojure.tools.namespace.find, controls reader options for 
               parsing files.

    :add-all?  If true, assumes all extant files are modified regardless
               of filesystem timestamps."
  {:added "0.3.0"}
  ([tracker files] (scan-files tracker files nil))
  ([tracker files {:keys [platform add-all?]}]
   (let [deleted (seq (deleted-files tracker files))
         modified (if add-all?
                    files
                    (seq (modified-files tracker files)))]
     (if (or deleted modified)
       (update-files tracker deleted modified platform)
       tracker))))

(defn scan-dirs
  "Scans directories for files which have changed since the last time
  'scan-dirs' or 'scan-files' was run; updates the dependency tracker
  with new/changed/deleted files.

  dirs is the collection of directories to scan, defaults to all
  directories on Clojure's classpath.

  Optional third argument is map of options:

    :platform  Either clj (default) or cljs, both defined in 
               clojure.tools.namespace.find, controls file extensions 
               and reader options.

    :add-all?  If true, assumes all extant files are modified regardless
               of filesystem timestamps."
  {:added "0.3.0"}
  ([tracker] (scan-dirs tracker nil nil))
  ([tracker dirs] (scan-dirs tracker dirs nil))
  ([tracker dirs {:keys [platform add-all?] :as options}]
   (let [ds (or (seq dirs) (dirs-on-classpath))]                           ;;; (classpath-directories)
     (scan-files tracker (find-files ds platform) options))))

(defn scan
  "DEPRECATED: replaced by scan-dirs.

  Scans directories for Clojure (.clj, .cljc) source files which have
  changed since the last time 'scan' was run; update the dependency
  tracker with new/changed/deleted files.

  If no dirs given, defaults to all directories on the classpath."
    {:added "0.2.0"
    :deprecated "0.3.0"}
  [tracker & dirs]
  (scan-dirs tracker dirs {:platform find/cljr}))                         ;;; find/clj  -- is this correct?

(defn scan-all
  "DEPRECATED: replaced by scan-dirs.

  Scans directories for all Clojure source files and updates the
 dependency tracker to reload files. If no dirs given, defaults to
  all directories on the classpath."
  {:added "0.2.0"
   :deprecated "0.3.0"}[tracker & dirs]
  (scan-dirs tracker dirs {:platform find/cljr :add-all? true}))          ;;; find/clj  -- is this correct?
	
;;; ADDED

(defn- make-dir-info 
  ^DirectoryInfo [x]
  (cond 
    (instance? DirectoryInfo x) x
	(string? x) (DirectoryInfo. ^String x)
	:default (DirectoryInfo. (str x))))

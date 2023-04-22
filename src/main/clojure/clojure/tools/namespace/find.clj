;; Copyright (c) Stuart Sierra, 2012. All rights reserved. The use and
;; distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this
;; distribution. By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license. You must not
;; remove this notice, or any other, from this software.

(ns 
  ^{:author "Stuart Sierra, modified for ClojureCLR by David Miller",
     :doc "Search for namespace declarations in directories and JAR files."} 
  clojure.tools.namespace.find
  (:require                                                             ;;;  Doesn't exist for us: [clojure.java.classpath :as classpath]
            [clojure.clr.io :as io]                                     ;;; clojure.java.io
            [clojure.set :as set]
            [clojure.tools.namespace.file :as file]
            [clojure.tools.namespace.parse :as parse])
  (:import (System.IO TextReader                                       ;;; (java.io File FileReader BufferedReader PushbackReader
                     DirectoryInfo FileSystemInfo)                     ;;;        InputStreamReader)
          (clojure.lang PushbackTextReader)))                          ;;; (java.util.jar JarFile JarEntry)))

(set! *warn-on-reflection* true)

(def ^{:added "0.3.0"}
  clj
  "Platform definition of file extensions and reader options for
  Clojure (.clj and .cljc) source files."
  {:read-opts parse/clj-read-opts
   :extensions file/clojure-extensions})

(def ^{:added "0.3.0"}
  cljs
  "Platform definition of file extensions and reader options for
  ClojureScript (.cljs and .cljc) source files."
  {:read-opts parse/cljs-read-opts
   :extensions file/clojurescript-extensions})

(def ^{:added "0.3.0"}
  cljr
  "Platform definition of file extensions and reader options for
  ClojureCLR (.cljr and .cljc) source files."
  {:read-opts parse/cljr-read-opts
   :extensions file/clojure-clr-extensions})

(defmacro ^:private ignore-reader-exception
  "If body throws an exception caused by a syntax error (from
  tools.reader), returns nil. Rethrows other exceptions."
  [& body]
  `(try ~@body
        (catch Exception e#
          (if (= :reader-exception (:type (ex-data e#)))
            nil
            (throw e#)))))
 
;;; Finding namespaces in a directory tree

(defn- sort-files-breadth-first
  [files]
  (sort-by #(.FullName ^FileSystemInfo %) files))                   ;#(.getAbsolutePath ^File %)
  
(defn find-sources-in-dir
  "Searches recursively under dir for source files. Returns a sequence
  of File objects, in breadth-first sort order.

  Optional second argument is either clj (default) or cljs, both
  defined in clojure.tools.namespace.find."
  {:added "0.3.0"}
  ([dir]
   (find-sources-in-dir dir nil))
  ([^DirectoryInfo dir platform]                               ;;; ^File
   (let [{:keys [extensions]} (or platform clj)]
     (->> (file-seq dir)
          (filter #(file/file-with-extension? % extensions))
          sort-files-breadth-first))))

(defn find-clojure-sources-in-dir
   "DEPRECATED: replaced by find-sources-in-dir

  Searches recursively under dir for Clojure source files (.clj, .cljc).
  Returns a sequence of File objects, in breadth-first sort order."            ;;; SystemFileInfo
  {:added "0.2.0"
   :deprecated "0.3.0"}  
  [^FileSystemInfo dir]                                                        ;;; ^File
  (find-sources-in-dir dir clj))

(defn find-ns-decls-in-dir
  "Searches dir recursively for (ns ...) declarations in Clojure
  source files; returns the unevaluated ns declarations.
  The symbol name in the returned ns declaration will contain metadata
  for the corresponding directory and located file within at keys
  :dir and :file.
  
  Optional second argument platform is either clj (default) or cljs,
  both defined in clojure.tools.namespace.find."
  {:added "0.2.0"}
  ([dir] (find-ns-decls-in-dir dir nil))
  ([dir platform]
   (keep #(ignore-reader-exception
           (let [[_ nom & more] (file/read-file-ns-decl % (:read-opts platform))]
             (list* 'ns (with-meta nom
                          {:dir (.Name ^System.IO.DirectoryInfo dir) :file (.Name ^System.IO.FileInfo %)}) ;; .getName ^java.io.File x 2
                    more)))
          (find-sources-in-dir dir platform))))
		  
(defn find-namespaces-in-dir
  "Searches dir recursively for (ns ...) declarations in Clojure
   source files; returns the symbol names of the declared namespaces.

  Optional second argument platform is either clj (default) or cljs,
  both defined in clojure.tools.namespace.find."
  {:added "0.3.0"}
  ([dir] (find-namespaces-in-dir dir nil))
  ([dir platform]
   (map parse/name-from-ns-decl (find-ns-decls-in-dir dir platform))))

;;; Finding namespaces in JAR files

(defn- ends-with-extension
  [^String filename extensions]
  (some #(.EndsWith filename ^String %) extensions))                              ;;; .endsWith, add type hint

(defn sources-in-jar
  "Returns a sequence of source file names found in the JAR file.

  Optional second argument platform is either clj (default) or cljs,
  both defined in clojure.tools.namespace.find."
  {:added "0.3.0"}
  ([jar-file]
   (sources-in-jar jar-file nil))
  ([jar-file platform]                           ;;; [^JarFile jar-file platform]
   nil))                                         ;;; (let [{:keys [extensions]} (or platform clj)]
                                                 ;;; (filter #(ends-with-extension % extensions)
                                                 ;;;      (classpath/filenames-in-jar jar-file)))

(defn clojure-sources-in-jar
  "DEPRECATED: replaced by sources-in-jar

  Returns a sequence of filenames ending in .clj or .cljc found in the
  JAR file."
  {:added "0.2.0"
   :deprecated "0.3.0"}
  [jar-file]
  (sources-in-jar jar-file clj))             

(defn read-ns-decl-from-jarfile-entry
  "Attempts to read a (ns ...) declaration from the named entry in the
  JAR file, and returns the unevaluated form. Returns nil if read
  fails due to invalid syntax or if a ns declaration cannot be found.
  The symbol name in the returned ns declaration will contain metadata
  for the corresponding jar filename and located file within at keys
  :jar and :file.
  
  Optional third argument platform is either clj (default) or cljs,
  both defined in clojure.tools.namespace.find."
  ([jarfile entry-name]
   (read-ns-decl-from-jarfile-entry jarfile entry-name nil))
  ([jarfile ^String entry-name platorm]                                             ;;; [^JarFile jarfile ^String entry-name platform]
    nil))                                                                           ;;; (let [{:keys [read-opts]} (or platform clj)]
                                                                                    ;;;   (with-open [rdr (PushbackReader.
                                                                                    ;;;                    (io/reader
                                                                                    ;;;                     (.getInputStream jarfile (.getEntry jarfile entry-name))))]
                                                                                    ;;;       (ignore-reader-exception
                                                                                    ;;;         (let [[_ nom & more] (parse/read-ns-decl rdr read-opts)]
                                                                                    ;;;           (list* 'ns (with-meta nom
                                                                                    ;;;                        {:jar (.getName ^java.io.File jarfile) :file entry-name})
                                                                                    ;;;                  more)))))))
(defn find-ns-decls-in-jarfile
  "Searches the JAR file for source files containing (ns ...)
  declarations; returns the unevaluated ns declarations.

  Optional second argument platform is either clj (default) or cljs,
  both defined in clojure.tools.namespace.find."
  ([jarfile]
   (find-ns-decls-in-jarfile jarfile nil))
  ([jarfile platform]                                                              ;;; ^JarFile 
   (keep #(read-ns-decl-from-jarfile-entry jarfile % platform)
         (sources-in-jar jarfile platform))))

(defn find-namespaces-in-jarfile
  "Searches the JAR file for platform source files containing (ns ...)
  declarations.  Returns a sequence of the symbol names of the
  declared namespaces.

  Optional second argument platform is either clj (default) or cljs,
  both defined in clojure.tools.namespace.find."
  ([jarfile]
   (find-namespaces-in-jarfile jarfile nil))
  ([jarfile platform]                                                              ;;; ^JarFile 
   (map parse/name-from-ns-decl (find-ns-decls-in-jarfile jarfile platform))))


;;; Finding namespaces

(defn find-ns-decls
  "Searches a sequence of java.io.File objects (both directories and
  JAR files) for platform source files containing (ns...)
  declarations. Returns a sequence of the unevaluated ns declaration
  forms. Use with clojure.java.classpath to search Clojure's
  classpath.

  Optional second argument platform is either clj (default) or cljs,
  both defined in clojure.tools.namespace.find."
  ([files]
   (find-ns-decls files nil))
  ([files platform]
   (concat
    (mapcat #(find-ns-decls-in-dir % platform)
            (filter file/is-directory? files))                                  ;;;  #(.isDirectory ^File %)
                                                                                ;;;(mapcat #(find-ns-decls-in-jarfile % platform)
                                                                                ;;;        (map #(JarFile. (io/file %))
       )))                                                                      ;;;             (filter classpath/jar-file? files)))

(defn find-namespaces
  "Searches a sequence of java.io.File objects (both directories and
  JAR files) for platform source files containing (ns...)
  declarations. Returns a sequence of the symbol names of the declared
  namespaces. Use with clojure.java.classpath to search Clojure's
  classpath.

  Optional second argument platform is either clj (default) or cljs,
  both defined in clojure.tools.namespace.find."
  ([files]
   (find-namespaces files nil))
  ([files platform]
   (map parse/name-from-ns-decl (find-ns-decls files platform))))
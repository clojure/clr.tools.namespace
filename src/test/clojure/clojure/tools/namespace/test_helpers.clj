(ns clojure.tools.namespace.test-helpers
  "Utilities to help with testing files and namespaces."
  (:require [clojure.clr.io :as io]                                                      ;;; clojure.java.io
            [clojure.string :as string])
  (:import (System.IO Path File FileInfo DirectoryInfo TextWriter Directory)))           ;;; (java.io Closeable File Writer PrintWriter)

(defn create-temp-dir
  "Creates and returns a new temporary directory java.io.File."
  [name]
  (let [temp-filename (Path/Combine (Path/GetTempPath) name)                   ;;;  temp-file (File/createTempFile name nil)
        _ (when (or (File/Exists temp-filename)(Directory/Exists temp-filename))
      		(Directory/Delete temp-filename true))                                ;;; (.delete temp-file)
       dir (Directory/CreateDirectory temp-filename)]                          ;;; (.mkdirs temp-file)
    (println "Temporary directory" (Path/GetFullPath temp-filename))           ;;; (.getAbsolutePath temp-file)
    dir))                                                                      ;;; temp-file

(defn- write-contents
  "Writes contents into writer. Strings are written as-is via println,
  other types written as by prn."
  [^TextWriter writer contents]                                                               ;;; ^Writer
  {:pre [(sequential? contents)]}
  (binding [*out* writer]                                                                     ;;; (PrintWriter. writer)
    (doseq [content contents]
      (if (string? content)
        (println content)
        (prn content))
      (newline))))

;;; DM: Added
(defn- coerce-to-file-path 
  "Creates a path (string) from a vector of strings and possible a beginning DirectoryInfo"
  [path]
  (let [coerce (fn [x] 
				(cond
				  (instance? DirectoryInfo x) (.FullName ^DirectoryInfo x)
				  (instance? FileInfo x) (.FullName ^FileInfo x)
				  (instance? String x) x
				  :otherwise (str x)))]
	(Path/Combine (into-array String (map coerce path)))))
;;; DM:
	  
(defn create-file
  "Creates a file from a vector of path elements. Writes contents into
  the file. Elements of contents may be data, written via prn, or
  strings, written directly."
  [path contents]
  {:pre [(vector? path)]}
  (let [path (coerce-to-file-path path)                                               ;;; ^File file (apply io/file path)
        ^FileInfo fi (FileInfo. path)]                                                           ;;; (when-let [parent (.getParentFile file)]
    (when-not (.Exists fi) (Directory/CreateDirectory (.. fi Directory FullName)))     ;;; (.mkdirs parent))
    (with-open [wtr (io/text-writer fi)]                                               ;;;  io/writer file
      (write-contents wtr contents))
    fi))                                                                             ;;; file

(defn- sym->path
  "Converts a symbol name into a vector of path parts, not including
  file extension."
  [symbol]
  {:pre [(symbol? symbol)]
   :post [(vector? %)]}
  (-> (name symbol)
      (string/replace \- \_)
      (string/split #"\.")))

(defn- source-path
  "Returns a vector of path components for namespace named sym,
  with given file extension (keyword)."
  [sym extension]
  (let [path (sym->path sym)
        basename (peek path)
        filename (str basename \. (name extension))]
    (conj (pop path) filename)))

(defn create-source
  "Creates a file at the correct path under base-dir for a namespace
  named sym, with file extension (keyword), containing a ns
  declaration which :require's the dependencies (symbols). Optional
  contents written after the ns declaration as by write-contents."
  ([base-dir sym extension]
   (create-source base-dir sym extension nil nil))
  ([base-dir sym extension dependencies]
   (create-source base-dir sym extension dependencies nil))  
  ([base-dir sym extension dependencies contents]
   (let [full-path (into [base-dir] (source-path sym extension))
         ns-decl (if (seq dependencies)
                   (list 'ns sym (list* :require dependencies))
                   (list 'ns sym))]
     (create-file full-path (into [ns-decl] contents)))))

(defn create-headless-source
  "Creates a file at the correct path under base-dir for a file that
  declares in-ns for namespace named sym, with file extension (keyword)
  and will also require the dependencies (symbols). Optional contents
  written after the ns declaration as by write-contents."
  ([base-dir sym extension]
   (create-headless-source base-dir sym extension nil nil))
  ([base-dir sym extension dependencies]
   (create-headless-source base-dir sym extension dependencies nil))
  ([base-dir sym extension dependencies contents]
   (let [full-path (into [base-dir] (source-path sym extension))
         ins-decl (list 'in-ns (list 'quote sym))
         deps-decl (when (seq dependencies)
                     (map #(list 'require `(quote ~%)) dependencies))]
     (create-file full-path (filter identity (concat [ins-decl] deps-decl contents))))))
	 
(defn same-files?
  "True if files-a and files-b contain the same canonical File's,
  regardless of order."
  [files-a files-b]
  (= (sort (map #(.FullName ^FileInfo %) files-a))                   ;;; .getCanonicalPath ^File
     (sort (map #(.FullName ^FileInfo %) files-b))))                 ;;; .getCanonicalPath ^File

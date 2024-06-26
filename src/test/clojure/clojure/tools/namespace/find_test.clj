(ns clojure.tools.namespace.find-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.tools.namespace.test-helpers :as help]
            [clojure.tools.namespace.find :as find])
  )                                                      ;;; (:import (java.io File))

(deftest t-find-clj-and-cljc-files
  "main.clj depends on one.cljc which depends on two.clj.
  two.cljs also exists but should not be returned"
  (let [dir (help/create-temp-dir "t-find-clj-and-cljc-files")
        main-clj (help/create-source dir 'example.main :clj '[example.one])
        one-cljc (help/create-source dir 'example.one :cljc '[example.two])
        two-clj (help/create-source dir 'example.two :clj)
        two-cljs (help/create-source dir 'example.two :cljs)]
    (is (help/same-files?
         [main-clj one-cljc two-clj]
         (find/find-sources-in-dir dir)))))

(deftest t-find-cljs-and-cljc-files
  "main.cljs depends on one.cljc which depends on two.cljs.
  two.clj also exists but should not be returned"
  (let [dir (help/create-temp-dir "t-find-cljs-and-cljc-files")
        main-cljs (help/create-source dir 'example.main :cljs '[example.one])
        one-cljc (help/create-source dir 'example.one :cljc '[example.two])
        two-clj (help/create-source dir 'example.two :clj)
        two-cljs (help/create-source dir 'example.two :cljs)]
    (is (help/same-files?
         [main-cljs one-cljc two-cljs]
         (find/find-sources-in-dir dir find/cljs)))))
		 
(deftest t-find-ns-decl-meta
  (let [dir (help/create-temp-dir "t-find-clj-and-cljc-files")
        main-clj (help/create-source dir 'example.main :clj '[example.one])
        one-cljc (help/create-source dir 'example.one :cljc '[example.two])
        two-clj (help/create-source dir 'example.two :clj)
        two-cljs (help/create-source dir 'example.two :cljs)
        headless-clj (help/create-headless-source dir 'example.headless :clj)]
    (is (every? #{(.Name ^System.IO.DirectoryInfo dir)}                 ;;   .getName ^java.io.File
         (map #(-> % second meta :dir)
              (find/find-ns-decls [dir]))))))		 
			  
;;; DM: added
(deftest t-find-cljr-and-cljc-files
  "main.cljr depends on one.cljc which depends on two.cljr.
  two.clj also exists but should not be returned"
  (let [dir (help/create-temp-dir "t-find-cljr-and-cljc-files")
        main-cljr (help/create-source dir 'example.main :cljr '[example.one])
        one-cljc (help/create-source dir 'example.one :cljc '[example.two])
        two-cljs (help/create-source dir 'example.two :cljs)
        two-cljr (help/create-source dir 'example.two :cljr)]
    (is (help/same-files?
         [main-cljr one-cljc two-cljr]
         (find/find-sources-in-dir dir find/cljr)))))			  
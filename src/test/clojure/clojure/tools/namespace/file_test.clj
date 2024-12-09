(ns clojure.tools.namespace.file-test
    (:require [clojure.test :refer [deftest is]]
      [clojure.tools.namespace.dependency :as dep]
      [clojure.tools.namespace.file :as file]
      [clojure.tools.namespace.test-helpers :as help]
      [clojure.tools.namespace.track :as track])
    (:import (System.IO FileInfo)))
	
;; Tests compliments of Brandon Correa

(deftest t-add-no-files
  (let [tracker (file/add-files (track/tracker) nil)]
    (is (= (dep/->MapDependencyGraph {} {}) (::track/deps tracker)))
    (is (= {} (::file/filemap tracker)))
    (is (= '() (::track/unload tracker)))
    (is (= '() (::track/load tracker)))))

(deftest t-add-one-file
  (let [dir     (help/create-temp-dir "t-add-one-file")
        one-clj (help/create-source dir 'example.one :clj)
        tracker (file/add-files (track/tracker) [one-clj])]
    (is (= (dep/->MapDependencyGraph {} {}) (::track/deps tracker)))
    (is (= {one-clj 'example.one} (::file/filemap tracker)))
    (is (= (list 'example.one) (::track/unload tracker)))
    (is (= (list 'example.one) (::track/load tracker)))))

(deftest t-add-file-with-dependency
  (let [dir      (help/create-temp-dir "t-add-file-with-dependency")
        main-clj (help/create-source dir 'example.main :clj '[example.one])
        tracker  (file/add-files (track/tracker) [main-clj])]
    (is (= {'example.main #{'example.one}} (:dependencies (::track/deps tracker))))
    (is (= {'example.one #{'example.main}} (:dependents (::track/deps tracker))))
    (is (= {main-clj 'example.main} (::file/filemap tracker)))
    (is (= (list 'example.main) (::track/unload tracker)))
    (is (= (list 'example.main) (::track/load tracker)))))

(deftest t-add-file-that-already-exists
  (let [dir        (help/create-temp-dir "t-add-file-that-already-exists")
        file-ref-1 (help/create-source dir 'example.main :clj)
        file-ref-2 (FileInfo. (.-FullName file-ref-1))
        tracker    (-> (track/tracker)
                       (file/add-files [file-ref-1])
                       (file/add-files [file-ref-2]))]
       (is (= {} (:dependencies (::track/deps tracker))))
       (is (= {} (:dependents (::track/deps tracker))))
       (is (= {file-ref-2 'example.main} (::file/filemap tracker)))
       (is (= (list 'example.main) (::track/unload tracker)))
       (is (= (list 'example.main) (::track/load tracker)))))

(deftest t-add-file-that-already-exists-in-the-same-call
  (let [dir        (help/create-temp-dir "t-add-file-that-already-exists-in-the-same-call")
        file-ref-1 (help/create-source dir 'example.main :clj)
        file-ref-2 (FileInfo. (.-FullName file-ref-1))
        tracker    (-> (track/tracker)
                       (file/add-files [file-ref-1 file-ref-2]))]
    (is (= {} (:dependencies (::track/deps tracker))))
    (is (= {} (:dependents (::track/deps tracker))))
    (is (= {file-ref-2 'example.main} (::file/filemap tracker)))
    (is (= (list 'example.main) (::track/unload tracker)))
    (is (= (list 'example.main) (::track/load tracker)))))

(deftest t-remove-no-files-from-empty-tracker
  (let [tracker (file/remove-files {} nil)]
    (is (= (dep/->MapDependencyGraph {} {}) (::track/deps tracker)))
    (is (nil? (::file/filemap tracker)))
    (is (= '() (::track/unload tracker)))
    (is (= '() (::track/load tracker)))))

(deftest t-remove-file-with-dependency-from-filemap
  (let [dir        (help/create-temp-dir "t-remove-file-with-dependency-from-filemap")
        file-ref-1 (help/create-source dir 'example.main :clj '[example.one])
        file-ref-2 (FileInfo. (.-FullName file-ref-1))
        tracker    (-> (track/tracker)
                       (file/add-files [file-ref-1])
                       (file/remove-files [file-ref-2]))]
       (is (= {} (:dependencies (::track/deps tracker))))
       (is (= {'example.one #{}} (:dependents (::track/deps tracker))))
       (is (= {} (::file/filemap tracker)))
       (is (= (list 'example.main) (::track/unload tracker)))
       (is (= (list) (::track/load tracker)))))
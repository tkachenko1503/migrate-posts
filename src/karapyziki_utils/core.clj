(ns karapyziki-utils.core
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [karapyziki-utils.renderer :as rndr]
            [clj-time.format :as f]))

; Convert json string to clojure data
; String -> Map
(def json-str->clj
  #(json/read-str % :key-fn keyword))

; Returns clojure data from json file
; String -> Map
(def get-json-from-resource
  (comp json-str->clj slurp))

; Pick interested values from original article
; Map -> Map
(defn get-article-info
  "Pick props from article map"
  [{:keys [title content picture showDate]}]
  {:title title
   :content content
   :picture picture
   :date (:iso showDate)})

; Date formatters
(def iso-formatter (f/formatters :date-time))
(def name-date-format (f/formatter "yyyy-MM-dd"))

; Convert date to readable format
; String -> String
(def format-date-for-name
 #(->> %
   (f/parse iso-formatter)
   (f/unparse name-date-format)))

; Converts date field in article map
; Map -> Map
(def update-article-date
  #(update-in % [:date] format-date-for-name))

; Returns list of articles content
; Map -> List
(def extract-articles
  #(->> %
    (:results)
    (map get-article-info)
    (map update-article-date)))

; Template function
; Map -> String
(def articles-template
  (rndr/renderer-fn "---\n"
                    "layout: post\n"
                    "title: " (:title %) "\n"
                    "date: " (:date %) "\n"
                    "description: " "\n"
                    "image: " (:picture %) "\n"
                    "image-sm: " "\n"
                    "---\n"
                    (:content %)))

; Prepare article name
; String -> String
(def normalize-name
  #(-> %
    (str/trim)
    (str/lower-case)
    (str/replace #"[\.,\"'\-)]" "")
    (str/replace #"\s" "-")))

; Generate article name
; Map -> String
(defn get-article-name
  [{:keys [date title]}]
  (str date "-" (normalize-name title)))

; Returns name and content of article
; Map -> Vector
(def get-article-name-and-content
  (juxt get-article-name
        articles-template))

; Writes article to file
(defn write-article
  [[name content]]
  (spit
    (str "resources/result/" name ".markdown")
    content))

; Removes file
(def rm-files
  #(doseq [file %]
    (->> file
      (.getName)
      (str "resources/result/")
      (io/delete-file))))

; Removes all files from dir
(def cleanup-dir
  #(-> "resources/result/"
    (io/file)
    (file-seq)
    (rest)
    (rm-files)))

; Removes old files and write new
(defn write-to-fs
  [articles]
  (do
    (cleanup-dir)
    (map write-article articles)))

(defn -main
  []
  (->> "resources/Article.json"
    (get-json-from-resource)
    (extract-articles)
    (map get-article-name-and-content)
    (write-to-fs)))

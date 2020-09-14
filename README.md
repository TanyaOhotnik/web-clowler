# web-crawler

The application starts at localhost:8080, you will be redirected to the main path - /search

# Input data
- root URL, where we should start a search. Constraints: notNull, size [10,256]
- searched text, what we should search for. Constraints: notNull, size [1,256]
- thread count, amount of threads we should use for download. Constraints: notNull, size [1,6]
- maximum scanned URLs, amount of URLs to be download (it does not matter if a download was successful or not). Constraints: notNull, size [1,256]

# Assumptions
- we do not need to persist search requests and results, hence all cached in a hash map
- we do not need to store a file on our file system, but it can be done for long-running searches and big HTMLs
- only text/html downloaded from web services
- search text using java built-in API. (I tried used Lucene, but in memory indexing effective only for a small amount of document, also it creates 'millions' of 1024 arrays, which is not effective while we are not storing files on FS)
- HTTP protocol is used

# Comments
It is impossible to make concurrent search and download of URLs and at the same time and build an ordered tree (because in each moment we do not know if a level of a tree fully searched or not), applicable breadth-first search. We potentially can store a list of futures, but it is not effective because we will hold references on all downloaded strings. In my solution downloaded string can be removed by GC after text search, which relies on the queue of downloaded documents. For all this stuff responsible *com.tetianaokhotnik.webcrawler.service.impl.ConcurrentSearchService*, as well as *com.tetianaokhotnik.webcrawler.service.impl.task.DownloadRunnable* and *com.tetianaokhotnik.webcrawler.service.impl.task.SearchRunnable*

For Breadth-first search created *com.tetianaokhotnik.webcrawler.service.impl.SimpleSearchService* which at first step downloads all the documents and creates a queue (on every level we are waiting for the completion of all downloads - otherwise it is impossible to store the order of leaves in a queue). After download, we can search by this queue thus performing a Breadth-first search.

package com.tetianaokhotnik.webcrawler.service.impl;


import com.tetianaokhotnik.webcrawler.model.DownloadedDocument;
import com.tetianaokhotnik.webcrawler.model.SearchRequest;
import com.tetianaokhotnik.webcrawler.model.SearchStatus;
import com.tetianaokhotnik.webcrawler.service.ISearchService;
import com.tetianaokhotnik.webcrawler.service.impl.task.DownloadRunnable;
import com.tetianaokhotnik.webcrawler.service.impl.task.SearchRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class ConcurrentSearchService implements ISearchService
{
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentSearchService.class);

    public static final String POISON_PILL = "POISON_PILL";
    //TODO to be replaced with persistence
    private static final Map<String, Map<String, SearchStatus>> allSearchCache = new HashMap<>();
    private static final Map<String, SearchRequest> searchRequestCache = new HashMap<>();

    @Value("${download.pages.timeout.seconds}")
    long finishAllDownloadsTimeout;

    @Override
    public void startSearch(SearchRequest searchRequest)
    {
        final Map<String, SearchStatus> statusesByUrl = new ConcurrentHashMap<>();

        allSearchCache.put(searchRequest.getGuid(), statusesByUrl);
        searchRequestCache.put(searchRequest.getGuid(), searchRequest);

        final BlockingQueue<String> urlsQueue = new LinkedBlockingQueue<>();
        final BlockingQueue<DownloadedDocument> documentsQueue = new LinkedBlockingQueue<>();

        urlsQueue.add(searchRequest.getStartUrl());

        final ExecutorService rootExecutorService = Executors.newCachedThreadPool();
        final ExecutorService downloadTaskExecutorsService =
                Executors.newFixedThreadPool(searchRequest.getThreadCount());

        Integer maxScannedUrls = searchRequest.getMaxScannedUrls();

        rootExecutorService.submit(new DownloadRunnable(urlsQueue, documentsQueue, downloadTaskExecutorsService,
                statusesByUrl, maxScannedUrls));
        rootExecutorService.submit(new SearchRunnable(urlsQueue, documentsQueue, statusesByUrl, searchRequest));
    }

    @Override
    public List<SearchStatus> getSearchStatus(String searchRequest)
    {
        Map<String, SearchStatus> stringSearchStatusMap = allSearchCache.get(searchRequest);
        if (stringSearchStatusMap != null)
        {
            return new LinkedList<>(stringSearchStatusMap.values());
        }
        return new LinkedList<>();
    }

    @Override
    public SearchRequest getSearchRequest(String searchGuid)
    {
        return searchRequestCache.get(searchGuid);
    }
}

package com.tetianaokhotnik.webcrawler.service.impl;


import com.tetianaokhotnik.webcrawler.model.DownloadedDocument;
import com.tetianaokhotnik.webcrawler.model.SearchRequest;
import com.tetianaokhotnik.webcrawler.model.SearchStatus;
import com.tetianaokhotnik.webcrawler.service.ISearchService;
import com.tetianaokhotnik.webcrawler.service.impl.task.DownloadRunnable;
import com.tetianaokhotnik.webcrawler.service.impl.task.SearchRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class ConcurrentSearchService implements ISearchService
{
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentSearchService.class);

    public static final String POISON_PILL = "POISON_PILL";
    //TODO to be replaced with persistence
    private static final Map<String, List<SearchStatus>> allSearchCache = new LinkedHashMap<>();


    @Override
    public void startSearch(SearchRequest searchRequest)
    {
        final Map<String, SearchStatus> statuses = new LinkedHashMap<>();

        final BlockingQueue<String> urlsQueue = new LinkedBlockingQueue<>();
        final BlockingQueue<DownloadedDocument> documentsQueue = new LinkedBlockingQueue<>();

        urlsQueue.add(searchRequest.getStartUrl());

        final ExecutorService rootExecutorService = Executors.newCachedThreadPool();
        final ExecutorService downloadTaskExecutorsService =
                Executors.newFixedThreadPool(searchRequest.getThreadCount());

        Integer maxScannedUrls = searchRequest.getMaxScannedUrls();
        rootExecutorService.submit(new DownloadRunnable(urlsQueue, documentsQueue, downloadTaskExecutorsService,
                maxScannedUrls));
        rootExecutorService.submit(new SearchRunnable(urlsQueue, documentsQueue, maxScannedUrls));
    }

    @Override
    public List<SearchStatus> getSearchStatus(String searchRequest)
    {
        return allSearchCache.get(searchRequest);
    }
}

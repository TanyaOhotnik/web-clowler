package com.tetianaokhotnik.webcrawler.service.impl.task;

import com.tetianaokhotnik.webcrawler.model.DownloadedDocument;
import com.tetianaokhotnik.webcrawler.service.impl.ConcurrentSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class SearchRunnable implements Runnable
{
    private static final Logger logger = LoggerFactory.getLogger(SearchRunnable.class);

    private BlockingQueue<String> urlsQueue;
    private BlockingQueue<DownloadedDocument> documentQueue;
    private int maxUrls;

    public SearchRunnable(BlockingQueue<String> urlsQueue, BlockingQueue<DownloadedDocument> documentQueue,
                          int maxUrls)
    {
        this.urlsQueue = urlsQueue;
        this.documentQueue = documentQueue;
        this.maxUrls = maxUrls;
    }

    @Override
    public void run()
    {
        int foundUrlsSum = 0;
        while (true)
        {
            final DownloadedDocument documentBody = documentQueue.poll();
            if (DownloadedDocument.isPoisonPill(documentBody))
            {
                break;
            }
            if (documentBody != null)
            {
                boolean needToSearchUrls = foundUrlsSum < maxUrls;
                if (needToSearchUrls)
                {
                    List<String> foundUrls = searchUrls(documentBody);

                    urlsQueue.addAll(foundUrls);
                    foundUrlsSum += foundUrls.size();
                } else {
                    logger.info("Links limit reached, but text should be processed");
                }

                int matches = searchText(documentBody);
                //TODO manage search status here
            }
            //TODO handle case when links limit not reached, but all docs scanned
        }
        //let url consumer know, that all links scanned
        logger.info("Adding poison pill");
        urlsQueue.add(ConcurrentSearchService.POISON_PILL);
    }

    private int searchText(DownloadedDocument downloadedDocument)
    {
        //search for matches, return amount of matches
        return 0;
    }

    private List<String> searchUrls(DownloadedDocument downloadedDocument)
    {
        return new LinkedList<>();
    }

}

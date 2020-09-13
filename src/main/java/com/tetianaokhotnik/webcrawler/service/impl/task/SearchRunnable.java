package com.tetianaokhotnik.webcrawler.service.impl.task;

import com.tetianaokhotnik.webcrawler.model.DownloadedDocument;
import com.tetianaokhotnik.webcrawler.service.impl.ConcurrentSearchService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public class SearchRunnable implements Runnable
{
    private static final String HREF = "href";
    private static final String A_HREF = "a[href]";
    private static final String HASHTAG = "#";
    private static final String FORWARDS_SLASH = "/";

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
                } else
                {
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
        final Document document = Jsoup.parse(downloadedDocument.getContent());
        final Elements elements = document.select(A_HREF);
        final String rootUrl = downloadedDocument.getUrl();

        LinkedList<String> resolvedLinks =
                elements.stream()
                        .map(e -> e.attr(HREF))
                        .filter(Objects::nonNull)
                        .filter(this::isNotRecoverable)
                        .map((String urlFromWebPage) -> appendRelativeLinks(urlFromWebPage, rootUrl))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedList::new));

        logger.info("Found {} links from {}", resolvedLinks.size(), rootUrl);

        return resolvedLinks;
    }

    private String appendRelativeLinks(String relativeUrl, String rootUrlStr)
    {
        if (!relativeUrl.startsWith(FORWARDS_SLASH))
        {
            return relativeUrl;
        }

        try
        {
            logger.info("Trying to convert {} to valid url", relativeUrl);

            URL rootUrl = new URL(rootUrlStr);
            URL resolvedUrl = new URL(rootUrl.getProtocol(), rootUrl.getHost(), rootUrl.getPort(), relativeUrl);

            return resolvedUrl.toString();
        }
        catch (MalformedURLException e)
        {
            logger.warn("Cannot convert provided url to valid", e);
            return null;
        }
    }

    /**
     * @param url - url to check
     * @return true, if link cannot be recovered, e.g. anchor links, root link, javascript action
     */
    private boolean isNotRecoverable(String url)
    {
        return !url.startsWith(HASHTAG) && !url.equals(FORWARDS_SLASH) && !url.equals("javascript:void(0)");
    }
}

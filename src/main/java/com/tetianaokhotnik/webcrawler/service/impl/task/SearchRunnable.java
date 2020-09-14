package com.tetianaokhotnik.webcrawler.service.impl.task;

import com.tetianaokhotnik.webcrawler.model.DownloadedDocument;
import com.tetianaokhotnik.webcrawler.model.SearchRequest;
import com.tetianaokhotnik.webcrawler.model.SearchStatus;
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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SearchRunnable implements Runnable
{
    private static final Logger logger = LoggerFactory.getLogger(SearchRunnable.class);

    private static final String HREF = "href";
    private static final String A_HREF = "a[href]";
    private static final String HASHTAG = "#";
    private static final String FORWARDS_SLASH = "/";

    private BlockingQueue<String> urlsQueue;
    private BlockingQueue<DownloadedDocument> documentQueue;
    private Map<String, SearchStatus> statuses;
    private SearchRequest searchRequest;

    public SearchRunnable(BlockingQueue<String> urlsQueue,
                          BlockingQueue<DownloadedDocument> documentQueue,
                          Map<String, SearchStatus> statuses,
                          SearchRequest searchRequest)
    {
        this.urlsQueue = urlsQueue;
        this.documentQueue = documentQueue;
        this.statuses = statuses;
        this.searchRequest = searchRequest;
    }

    @Override
    public void run()
    {
        int foundUrlsSum = 0;
        while (true)
        {
            final DownloadedDocument downloadedDocument = documentQueue.poll();

            if (DownloadedDocument.isPoisonPill(downloadedDocument))
            {
                logger.info("Received poison pill, queue size {}", documentQueue.size());
                break;
            }
            if (downloadedDocument != null)
            {
                logger.debug("Processing downloaded document {}", downloadedDocument.getUrl());
                final String documentUrl = downloadedDocument.getUrl();

                updateStatus(documentUrl, SearchStatus.Status.ANALYZING, null, null);

                boolean needToSearchUrls = foundUrlsSum < searchRequest.getMaxScannedUrls();
                if (needToSearchUrls)
                {
                    List<String> foundUrls = searchUrls(downloadedDocument);

                    urlsQueue.addAll(foundUrls);
                    foundUrlsSum += foundUrls.size();
                } else
                {
                    logger.debug("Links limit reached, but text should be processed");
                }

                int matches = searchText(downloadedDocument, searchRequest.getSearchedText());

                updateStatus(documentUrl, SearchStatus.Status.DONE, null, matches);
            }
            //TODO handle case when links limit not reached, but all docs scanned
        }
        //let url consumer know, that all links scanned
        logger.info("Adding poison pill");
        urlsQueue.add(ConcurrentSearchService.POISON_PILL);
    }

    private void updateStatus(String currentUrl, SearchStatus.Status status, String comment, Integer matches)
    {

        statuses.compute(currentUrl, (key, value) ->
        {
            SearchStatus newSearchStatus = new SearchStatus();
            newSearchStatus.setUrl(currentUrl);
            newSearchStatus.setStatus(status);
            newSearchStatus.setStatusDetails(comment);
            newSearchStatus.setMatches(matches);
            return newSearchStatus;
        });

    }

    private int searchText(DownloadedDocument downloadedDocument, String searchedText)
    {
        if (downloadedDocument == null || downloadedDocument.getContent().isEmpty())
        {
            return 0;
        }

        final String documentBody = downloadedDocument.getContent();

        final Matcher matcher = Pattern.compile(searchedText).matcher(documentBody);

        int count = 0;
        while (matcher.find())
        {
            count++;
        }

        return count;
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

        logger.debug("Found {} links from {}", resolvedLinks.size(), rootUrl);

        return resolvedLinks;
    }

    private String appendRelativeLinks(String relativeUrl, String rootUrlStr)
    {
        try
        {
            //TODO add apache utils to check validity without exception
            logger.debug("Trying to convert {} to valid url", relativeUrl);

            URL rootUrl = new URL(rootUrlStr);

            if (!relativeUrl.startsWith(FORWARDS_SLASH))
            {
                return relativeUrl;
            }

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
        return !url.startsWith(HASHTAG) && !url.equals(FORWARDS_SLASH) && !url.startsWith("javascript:void(0)");
    }
}

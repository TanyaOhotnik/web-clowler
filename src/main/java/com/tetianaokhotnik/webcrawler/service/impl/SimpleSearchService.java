package com.tetianaokhotnik.webcrawler.service.impl;


import com.tetianaokhotnik.webcrawler.model.DownloadedDocument;
import com.tetianaokhotnik.webcrawler.model.SearchRequest;
import com.tetianaokhotnik.webcrawler.model.SearchStatus;
import com.tetianaokhotnik.webcrawler.service.ISearchService;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Simple implementation of search service with concurrent operation for download ONLY
 * All other code is not handles edge cases and is not tested properly, it's intended only to demonstrate general idea,
 * that's why here are some code duplication from {@link ConcurrentSearchService} and its dependencies
 */
@Service
public class SimpleSearchService implements ISearchService
{
    public static final Logger logger = LoggerFactory.getLogger(SimpleSearchService.class);

    private static final String HREF = "href";
    private static final String A_HREF = "a[href]";
    private static final String HASHTAG = "#";
    private static final String FORWARDS_SLASH = "/";

    private static final Map<String, Map<String, SearchStatus>> allSearchCache = new HashMap<>();
    private static final Map<String, SearchRequest> searchRequestCache = new HashMap<>();

    public void startSearch(SearchRequest searchRequest)
    {
        Map<String, SearchStatus> statuses = new LinkedHashMap<>();

        final Queue<String> urlsQueue = new LinkedList<>();
        final Queue<DownloadedDocument> documentsQueue = new LinkedList<>();
        int processedUrls = 0;
        urlsQueue.add(searchRequest.getStartUrl());

        final ExecutorService downloadExecutorsService = Executors.newFixedThreadPool(searchRequest.getThreadCount());

        while (processedUrls < searchRequest.getMaxScannedUrls() || urlsQueue.isEmpty())
        {
            String currentUrl = urlsQueue.poll();

            if (currentUrl == null)
            {
                //urlsQueue population is not concurrent, so it is impossible to miss value here
                return;
            }

            List<CompletableFuture<DownloadedDocument>> downloadedDocumentCompletableFutures = new LinkedList<>();
            for (String url : urlsQueue)
            {
                CompletableFuture<DownloadedDocument> downloadedDocumentCompletableFuture =
                        CompletableFuture.supplyAsync(new DownloadSupplier(url), downloadExecutorsService);
                downloadedDocumentCompletableFutures.add(downloadedDocumentCompletableFuture);
            }

            //wait for all futures to be completed to get ordered list of tree leaves
            CompletableFuture.allOf(downloadedDocumentCompletableFutures.toArray(new CompletableFuture[0])).join();

            for (CompletableFuture<DownloadedDocument> currentFuture : downloadedDocumentCompletableFutures)
            {
                try
                {
                    final DownloadedDocument downloadedDocument = currentFuture.get();

                    List<String> foundLinks = searchUrls(downloadedDocument);

                    urlsQueue.addAll(foundLinks);
                    documentsQueue.add(downloadedDocument);

                    processedUrls++;
                }
                catch (Exception e)
                {
                    logger.debug("Error getting future result", e);
                }

            }
            //now we have complete graph and can simply iterate over it (it is already populates for Breadth-first search)

            while (documentsQueue.peek() != null)
            {
                DownloadedDocument downloadedDocument = documentsQueue.poll();
                searchText(downloadedDocument, searchRequest.getSearchedText());
            }

        }

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
        if (!relativeUrl.startsWith(FORWARDS_SLASH))
        {
            return relativeUrl;
        }

        try
        {
            logger.debug("Trying to convert {} to valid url", relativeUrl);

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
        return !url.startsWith(HASHTAG) && !url.equals(FORWARDS_SLASH) && !url.startsWith("javascript:void(0)");
    }

    public static class DownloadSupplier implements Supplier<DownloadedDocument>
    {
        private String currentUrl;

        public DownloadSupplier(String currentUrl)
        {
            this.currentUrl = currentUrl;
        }

        @Override
        public DownloadedDocument get()
        {
            if (currentUrl == null)
            {
                return new DownloadedDocument(null, null, "null url");
            }

            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setCookieSpec(CookieSpecs.STANDARD)
                            .build())
                    .build();)
            {
                HttpGet request = new HttpGet(currentUrl);
                request.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE);

                try (CloseableHttpResponse response = httpClient.execute(request);)
                {

                    if (org.springframework.http.HttpStatus.valueOf(response.getStatusLine().getStatusCode()).is2xxSuccessful())
                    {
                        HttpEntity entity = response.getEntity();
                        if (entity != null)
                        {
                            String result = EntityUtils.toString(entity);
                            return new DownloadedDocument(currentUrl, result, null);
                        }
                    }

                    final String errorStatus = String.format("Failed with %s",
                            response.getStatusLine().getStatusCode());
                    return new DownloadedDocument(currentUrl, null, errorStatus);
                }
            }
            catch (Exception e)
            {
                final String errorStatus = String.format("Failed with %s", e.getMessage());

                logger.debug("Download failed", e);
                return new DownloadedDocument(currentUrl, null, errorStatus);
            }
        }

    }

}

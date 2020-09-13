package com.tetianaokhotnik.webcrawler.service.impl.task;

import com.tetianaokhotnik.webcrawler.model.DownloadedDocument;
import com.tetianaokhotnik.webcrawler.model.SearchStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.tetianaokhotnik.webcrawler.service.impl.ConcurrentSearchService.POISON_PILL;

public class DownloadRunnable implements Runnable
{
    private static final Logger logger = LoggerFactory.getLogger(DownloadRunnable.class);

    private BlockingQueue<String> urlsQueue;
    private BlockingQueue<DownloadedDocument> documentQueue;
    private ExecutorService executorService;
    private Map<String, SearchStatus> statuses;
    private int maxScannedUrls;

    public DownloadRunnable(BlockingQueue<String> urlsQueue,
                            BlockingQueue<DownloadedDocument> documentQueue,
                            ExecutorService executorService,
                            Map<String, SearchStatus> statuses,
                            int maxScannedUrls)
    {
        this.urlsQueue = urlsQueue;
        this.documentQueue = documentQueue;
        this.executorService = executorService;
        this.maxScannedUrls = maxScannedUrls;
        this.statuses = statuses;
    }

    public void run()
    {
        int processedUrls = 0;

        while (processedUrls < maxScannedUrls)
        {
            String currentUrl = urlsQueue.poll();

            if (POISON_PILL.equals(currentUrl))
            {
                break;
            }

            if (currentUrl != null)
            {
                logger.info("Processing {}", currentUrl);

                updateStatus(currentUrl, SearchStatus.Status.IN_PROGRESS, null);

                CompletableFuture<DownloadedDocument> downloadResult =
                        CompletableFuture.supplyAsync(new DownloadSupplier(currentUrl), executorService);

                downloadResult.thenApply((downloadedDocument) ->
                {
                    logger.info("Download done for {}", downloadedDocument.getUrl());

                    if (downloadedDocument.getContent() != null)
                    {
                        updateStatus(currentUrl, SearchStatus.Status.DOWNLOADED, null);
                        return documentQueue.add(downloadedDocument);
                    }

                    updateStatus(currentUrl, SearchStatus.Status.ERROR, downloadedDocument.getError());

                    return false;
                });

                processedUrls++;
            }
        }

        logger.info("Queued processing for {} urls, limit reached", processedUrls);

        try
        {
            logger.info("Waiting for all tasks to be done");
            executorService.shutdown();
            executorService.awaitTermination(3, TimeUnit.MINUTES);
            //let document consumer know, then all docs downloaded
            documentQueue.add(DownloadedDocument.createPoisonPill());
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void updateStatus(String currentUrl, SearchStatus.Status status, String comment)
    {
        SearchStatus cachedSearchStatus = statuses.get(currentUrl);

        SearchStatus newSearchStatus = new SearchStatus();
        newSearchStatus.setUrl(currentUrl);
        newSearchStatus.setStatus(status);
        newSearchStatus.setStatusDetails(comment);

        statuses.replace(currentUrl, cachedSearchStatus, newSearchStatus);
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

                    if (HttpStatus.valueOf(response.getStatusLine().getStatusCode()).is2xxSuccessful())
                    {
                        HttpEntity entity = response.getEntity();
                        if (entity != null)
                        {
                            String result = EntityUtils.toString(entity);
                            return new DownloadedDocument(currentUrl, result, null);
                        }
                    }

                    final String errorStatus = String.format("Failed with %s", response.getStatusLine().getStatusCode());
                    return new DownloadedDocument(currentUrl, null, errorStatus);
                }
            }
            catch (Exception e)
            {
                final String errorStatus = String.format("Failed with %s", e.getMessage());

                logger.info("Download failed", e);
                return new DownloadedDocument(currentUrl, null, errorStatus);
            }
        }

    }
}


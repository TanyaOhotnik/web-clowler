package com.tetianaokhotnik.webcrawler.service.impl.task;

import com.tetianaokhotnik.webcrawler.model.DownloadedDocument;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private int maxScannedUrls;
    private int processedUrls;

    public DownloadRunnable(BlockingQueue<String> urlsQueue,
                            BlockingQueue<DownloadedDocument> documentQueue,
                            ExecutorService executorService,
                            int maxScannedUrls)
    {
        this.urlsQueue = urlsQueue;
        this.documentQueue = documentQueue;
        this.executorService = executorService;
        this.maxScannedUrls = maxScannedUrls;
    }

    public void run()
    {
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

                CompletableFuture<DownloadedDocument> downloadResult =
                        CompletableFuture.supplyAsync(new DownloadSupplier(currentUrl), executorService);

                downloadResult.thenApply((downloadedDocument) ->
                {
                    logger.info("Download done for {}", downloadedDocument.getUrl());

                    if (downloadedDocument.getContent() != null)
                    {
                        return documentQueue.add(downloadedDocument);
                    }
                    return false;
                });

                processedUrls++;
            }
        }

        documentQueue.add(DownloadedDocument.createPoisonPill());
        logger.info("Queued processing for {} urls, limit reached", processedUrls);

        try
        {
            logger.info("Waiting for all tasks to be done");
            executorService.awaitTermination(3, TimeUnit.MINUTES);
            //let document consumer know, then all docs downloaded
            documentQueue.add(DownloadedDocument.createPoisonPill());
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
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
                return new DownloadedDocument();
            }

            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setCookieSpec(CookieSpecs.STANDARD).build())
                    .build();)
            {
                HttpGet request = new HttpGet(currentUrl);
                try (CloseableHttpResponse response = httpClient.execute(request);)
                {

                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
                    {
                        HttpEntity entity = response.getEntity();
                        if (entity != null)
                        {
                            String result = EntityUtils.toString(entity);
                            return new DownloadedDocument(result, currentUrl);
                        }
                    }

                    return new DownloadedDocument();
                }
            }
            catch (Exception e)
            {
                return new DownloadedDocument();
            }
        }

    }
}


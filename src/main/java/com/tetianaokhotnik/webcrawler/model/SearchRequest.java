package com.tetianaokhotnik.webcrawler.model;

public class SearchRequest
{
    private String guid;
    private String startUrl;
    private String searchedText;
    private Integer threadCount;
    private Integer maxScannedUrls;

    public SearchRequest(String guid, String startUrl, String searchedText, Integer threadCount, Integer maxScannedUrls)
    {
        this.guid = guid;
        this.startUrl = startUrl;
        this.searchedText = searchedText;
        this.threadCount = threadCount;
        this.maxScannedUrls = maxScannedUrls;
    }

    public String getGuid()
    {
        return guid;
    }

    public void setGuid(String guid)
    {
        this.guid = guid;
    }

    public String getStartUrl()
    {
        return startUrl;
    }

    public void setStartUrl(String startUrl)
    {
        this.startUrl = startUrl;
    }

    public String getSearchedText()
    {
        return searchedText;
    }

    public void setSearchedText(String searchedText)
    {
        this.searchedText = searchedText;
    }

    public Integer getThreadCount()
    {
        return threadCount;
    }

    public void setThreadCount(Integer threadCount)
    {
        this.threadCount = threadCount;
    }

    public Integer getMaxScannedUrls()
    {
        return maxScannedUrls;
    }

    public void setMaxScannedUrls(Integer maxScannedUrls)
    {
        this.maxScannedUrls = maxScannedUrls;
    }

    @Override
    public String toString()
    {
        return "SearchRequest{" +
                "startUrl='" + startUrl + '\'' +
                ", searchedText='" + searchedText + '\'' +
                ", threadCount=" + threadCount +
                ", maxScannedUrls=" + maxScannedUrls +
                '}';
    }
}

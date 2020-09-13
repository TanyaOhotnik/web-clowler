package com.tetianaokhotnik.webcrawler.model;

public class SearchStatus
{
    private String searchGuid;
    private String url;
    private Status status;
    private String statusDetails;
    private Integer matches;

    public SearchStatus()
    {
    }

    public SearchStatus(String searchGuid, String url, Status status, String statusDetails, int matches)
    {
        this.searchGuid = searchGuid;
        this.url = url;
        this.status = status;
        this.statusDetails = statusDetails;
        this.matches = matches;
    }

    public String getSearchGuid()
    {
        return searchGuid;
    }

    public void setSearchGuid(String searchGuid)
    {
        this.searchGuid = searchGuid;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public Status getStatus()
    {
        return status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public String getStatusDetails()
    {
        return statusDetails;
    }

    public void setStatusDetails(String statusDetails)
    {
        this.statusDetails = statusDetails;
    }

    public Integer getMatches()
    {
        return matches;
    }

    public void setMatches(Integer matches)
    {
        this.matches = matches;
    }

    public enum Status
    {
        IN_PROGRESS,
        DOWNLOADED,
        ANALYZING,
        DONE,
        ERROR,

    }
}
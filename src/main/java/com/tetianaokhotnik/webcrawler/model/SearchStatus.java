package com.tetianaokhotnik.webcrawler.model;

public class SearchStatus
{
    String searchGuid;
    String url;
    Status status;
    String statusDetails;

    public enum Status
    {
        IN_PROGRESS,
        DONE,
        ERROR,

    }
}
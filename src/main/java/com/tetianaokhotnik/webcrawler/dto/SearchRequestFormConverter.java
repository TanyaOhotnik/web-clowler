package com.tetianaokhotnik.webcrawler.dto;

import com.tetianaokhotnik.webcrawler.model.SearchRequest;

import java.util.UUID;

public class SearchRequestFormConverter
{
    public static SearchRequest searchRequestFormToSearchRequest(SearchRequestForm searchRequestForm)
    {
        return searchRequestFormToSearchRequest(searchRequestForm, false);
    }

    public static SearchRequest searchRequestFormToSearchRequest(SearchRequestForm searchRequestForm,
                                                                 boolean generateGuid)
    {
        //TODO should be done at persistence layer
        final String guid = generateGuid
                ? UUID.randomUUID().toString()
                : null;

        SearchRequest searchRequest = new SearchRequest(
                guid,
                searchRequestForm.getStartUrl(),
                searchRequestForm.getSearchedText(),
                searchRequestForm.getThreadCount(),
                searchRequestForm.getMaxScannedUrls()
        );

        return searchRequest;
    }
}

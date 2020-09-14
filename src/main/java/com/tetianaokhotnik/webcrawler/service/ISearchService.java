package com.tetianaokhotnik.webcrawler.service;


import com.tetianaokhotnik.webcrawler.model.SearchRequest;
import com.tetianaokhotnik.webcrawler.model.SearchStatus;

import java.util.List;

public interface ISearchService
{
    /**
     * Start async processing of provided requests
     * @param searchRequest
     */
    void startSearch(SearchRequest searchRequest);

    /**
     *
     * @param searchGuid
     * @return list of {@link SearchStatus} corresponding provided guid
     */
    List<SearchStatus> getSearchStatus(String searchGuid);

    /**
     *
     * @param searchGuid
     * @return {@link SearchRequest} by provided guid
     */
    SearchRequest getSearchRequest(String searchGuid);
}

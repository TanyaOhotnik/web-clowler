package com.tetianaokhotnik.webcrawler.service;


import com.tetianaokhotnik.webcrawler.model.SearchRequest;
import com.tetianaokhotnik.webcrawler.model.SearchStatus;

import java.util.List;

public interface ISearchService
{
     void startSearch(SearchRequest searchRequest);

     List<SearchStatus> getSearchStatus(String searchGuid);
}

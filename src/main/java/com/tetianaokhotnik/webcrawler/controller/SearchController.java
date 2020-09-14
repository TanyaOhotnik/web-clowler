package com.tetianaokhotnik.webcrawler.controller;


import com.tetianaokhotnik.webcrawler.dto.SearchRequestForm;
import com.tetianaokhotnik.webcrawler.dto.SearchRequestFormConverter;
import com.tetianaokhotnik.webcrawler.model.SearchRequest;
import com.tetianaokhotnik.webcrawler.model.SearchStatus;
import com.tetianaokhotnik.webcrawler.service.ISearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import java.util.List;

@Controller
public class SearchController
{
    private static final String SEARCH_REQUEST_KEY = "searchRequest";
    private final String SEARCH_GUID_KEY = "searchGuid";
    private final String SEARCH_STATUSES_KEY = "searchStatuses";

    @Autowired
    @Qualifier("ConcurrentSearchService")
    private ISearchService searchService;

    @GetMapping("/search")
    public String search(Model model)
    {
        model.addAttribute(SEARCH_REQUEST_KEY, new SearchRequestForm());

        return "index";
    }

    @PostMapping("/search")
    public String startSearch(@Valid @ModelAttribute SearchRequestForm searchRequestInput,
                              BindingResult bindingResult,
                              Model model)
    {
        if (bindingResult.hasErrors())
        {
            return "index";
        }

        final SearchRequest searchRequest =
                SearchRequestFormConverter
                        .searchRequestFormToSearchRequest(searchRequestInput,true);
        final String requestGuid = searchRequest.getGuid();

        searchService.startSearch(searchRequest);

        model.addAttribute(SEARCH_GUID_KEY, requestGuid);

        return "redirect:search/" + requestGuid;
    }

    @GetMapping("/search/{guid}")
    public String startSearch(@PathVariable("guid") String guid, Model model)
    {
        final List<SearchStatus> searchStatus = searchService.getSearchStatus(guid);
        final SearchRequest searchRequest = searchService.getSearchRequest(guid);

        model.addAttribute(SEARCH_GUID_KEY, guid);
        model.addAttribute(SEARCH_STATUSES_KEY, searchStatus);
        model.addAttribute(SEARCH_REQUEST_KEY, searchRequest);

        return "status";
    }

    @GetMapping("/error")
    public String error()
    {
        return "error";
    }

}

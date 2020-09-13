package com.tetianaokhotnik.webcrawler.controller;


import com.tetianaokhotnik.webcrawler.dto.SearchRequestForm;
import com.tetianaokhotnik.webcrawler.dto.SearchRequestFormConverter;
import com.tetianaokhotnik.webcrawler.model.SearchRequest;
import com.tetianaokhotnik.webcrawler.model.SearchStatus;
import com.tetianaokhotnik.webcrawler.service.ISearchService;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    ISearchService searchService;

    @GetMapping("/search")
    public String search(Model model)
    {
        model.addAttribute("searchRequest", new SearchRequestForm());
        return "index";
    }

    @PostMapping("/search")
    public String startSearch(@Valid @ModelAttribute SearchRequestForm searchRequestInput, BindingResult bindingResult,
                              Model model)
    {
        if (bindingResult.hasErrors())
        {
            return "index";
        }

        SearchRequest searchRequest = SearchRequestFormConverter.searchRequestFormToSearchRequest(searchRequestInput,
                true);

        searchService.startSearch(searchRequest);

        String requestGuid = searchRequest.getGuid();
        model.addAttribute("searchGuid", requestGuid);

        return "redirect:search/" + requestGuid;
    }

    @GetMapping("/search/{guid}")
    public String startSearch(@PathVariable("guid") String guid, Model model)
    {

        List<SearchStatus> searchStatus = searchService.getSearchStatus(guid);
        SearchRequest searchRequest = searchService.getSearchRequest(guid);

        model.addAttribute("searchGuid", guid);
        model.addAttribute("searchStatuses", searchStatus);
        model.addAttribute("searchRequest", searchRequest);

        return "status";
    }

    @GetMapping("/error")
    public String error()
    {
        return "error";
    }

}

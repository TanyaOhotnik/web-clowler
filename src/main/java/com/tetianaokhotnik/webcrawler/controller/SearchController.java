package com.tetianaokhotnik.webcrawler.controller;


import com.tetianaokhotnik.webcrawler.dto.SearchRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import java.util.UUID;

@Controller
public class SearchController
{

    @GetMapping("/search")
    public String search(Model model)
    {
        model.addAttribute("searchRequest", new SearchRequest());
        return "index";
    }

    @PostMapping("/search")
    public String startSearch(@Valid @ModelAttribute SearchRequest searchRequest, BindingResult bindingResult, Model model)
    {
        if (bindingResult.hasErrors()) {
            return "index";
        }
        String searchGuid = UUID.randomUUID().toString();
        //TODO start search
        model.addAttribute("searchGuid", searchGuid);

        return "redirect:search/" + searchGuid;
    }

    @GetMapping("/search/{guid}")
    public String startSearch(@PathVariable("guid")String guid, Model model)
    {

        //TODO start search
        model.addAttribute("searchGuid", guid);
        return "status";
    }

}

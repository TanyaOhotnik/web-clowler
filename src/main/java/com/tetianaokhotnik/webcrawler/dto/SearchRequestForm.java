package com.tetianaokhotnik.webcrawler.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;;

/**
 * Request form DTO wth validation constrains
 */
public class SearchRequestForm
{
    @NotNull
    @Size(min = 1, max = 256)
    @Pattern(regexp = "((((https|http):(?:\\/\\/)?)(?:[-;:&=\\+\\$,\\w]+@)?[A-Za-z0-9.-]+|(?:www.|[-;:&=\\+\\$," +
            "\\w]+@)[A-Za-z0-9.-]+)((?:\\/[\\+~%\\/.\\w-_]*)?\\??(?:[-\\+=&;%@.\\w_]*)#?(?:[.\\!\\/\\\\w]*))?)",
            message = "URL does not match valid URL pattern")
    private String startUrl;

    @NotNull
    @Size(min = 1, max = 256)
    private String searchedText;

    @NotNull
    @Min(1)
    @Max(6)
    private Integer threadCount;

    @NotNull
    @Min(1)
    @Max(256)
    private Integer maxScannedUrls;

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

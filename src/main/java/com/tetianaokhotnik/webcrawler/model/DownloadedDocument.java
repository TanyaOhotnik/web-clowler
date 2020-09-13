package com.tetianaokhotnik.webcrawler.model;

public class DownloadedDocument
{
    private static final String POISON_PILL = "POISON_PILL";

    private String content;
    private String url;

    public DownloadedDocument()
    {
    }

    public DownloadedDocument(String content, String url)
    {
        this.content = content;
        this.url = url;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public static DownloadedDocument createPoisonPill()
    {
        return new DownloadedDocument(POISON_PILL, POISON_PILL);
    }

    public static boolean isPoisonPill(DownloadedDocument document)
    {
        if (document == null)
        {
            return false;
        }

        return POISON_PILL.equals(document.content);
    }
}

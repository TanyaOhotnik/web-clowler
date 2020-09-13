package com.tetianaokhotnik.webcrawler.model;

public class DownloadedDocument
{
    private static final String POISON_PILL = "POISON_PILL";

    private String content;
    private String url;
    private String error;

    public DownloadedDocument()
    {
    }

    public DownloadedDocument(String url, String content, String error)
    {
        this.url = url;
        this.content = content;
        this.error = error;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
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

    public String getError()
    {
        return error;
    }

    public void setError(String error)
    {
        this.error = error;
    }

    public static DownloadedDocument createPoisonPill()
    {
        return new DownloadedDocument(POISON_PILL, POISON_PILL, null);
    }

    public static boolean isPoisonPill(DownloadedDocument document)
    {
        if (document == null)
        {
            return false;
        }

        return POISON_PILL.equals(document.content);
    }

    public static boolean isEmpty(DownloadedDocument document)
    {
        return document.getContent() == null || !document.getContent().isEmpty();
    }
}

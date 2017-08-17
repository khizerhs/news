
package com.khizerhs.news.api.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Response {

    @SerializedName("docs")
    @Expose
    private List<NewsArticle> newsArticles = null;
    @SerializedName("meta")
    @Expose
    private Meta meta;

    public List<NewsArticle> getNewsArticles() {
        return newsArticles;
    }

    public void setNewsArticles(List<NewsArticle> newsArticles) {
        this.newsArticles = newsArticles;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

}

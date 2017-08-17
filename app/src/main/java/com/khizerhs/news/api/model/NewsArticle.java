
package com.khizerhs.news.api.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NewsArticle {

    public final static String IMAGE = "image";
    public final static String NYTIMES_BASE_URI = "http://www.nytimes.com/";


    @SerializedName("web_url")
    @Expose
    private String webUrl;

    @SerializedName("headline")
    @Expose
    private Headline headline;

    @SerializedName("multimedia")
    @Expose
    private List<Multimedia> multimedia = null;


    private String error = null;

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public Headline getHeadline() {
        return headline;
    }

    public void setHeadline(Headline headline) {
        this.headline = headline;
    }

    public List<Multimedia> getMultimedia() {
        return multimedia;
    }

    public void setMultimedia(List<Multimedia> multimedia) {
        this.multimedia = multimedia;
    }

    public String getArticleImageUrl() {
        String imageUrl = "";
        for (Multimedia m : this.getMultimedia()) {
            if (m.getType().equals(IMAGE) && m.getSubtype().equals("thumbnail")) {
                imageUrl = NYTIMES_BASE_URI + m.getUrl();
                break;
            }
        }
        return imageUrl;
    }

    public String getError() {
        return error;
    }

    public void setError(String error){
        this.error = error;
    }
}

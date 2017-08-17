package com.khizerhs.news.api;

import com.khizerhs.news.api.model.ResponseWrapper;

import java.util.Map;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;

/**
 * Created by KhizerHasan on 8/11/2017.
 */

public interface ArticleSearchAPI {

    @GET("svc/search/v2/articlesearch.json")
    Observable<ResponseWrapper> getArticles(@QueryMap Map<String, String> queryParams);


}

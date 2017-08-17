package com.khizerhs.news.api;

import android.text.TextUtils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by KhizerHasan on 8/11/2017.
 */

public class ArticleSearchService {

    public static ArticleSearchAPI createArticleSearchService(final String apiKey) {
        Retrofit.Builder builder =
                new Retrofit.Builder()
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create())
                        .baseUrl("https://api.nytimes.com/");

        if (!TextUtils.isEmpty(apiKey)) {
            OkHttpClient client =
                    new OkHttpClient.Builder()
                            .addInterceptor(new Interceptor() {
                                @Override
                                public Response intercept(Chain chain) throws IOException {
                                    Request request = chain.request();
                                    Request newReq = request
                                                    .newBuilder()
                                                    .addHeader("api-key", apiKey)
                                                    .build();
                                    return chain.proceed(newReq);
                                }
                            }).build();
            builder.client(client);
        }

        return builder.build().create(ArticleSearchAPI.class);

    }
}
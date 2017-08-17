package com.khizerhs.news;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.khizerhs.news.api.model.NewsArticle;
import com.khizerhs.news.util.DebouncedOnClickListener;
import com.khizerhs.news.util.OnArticleClickListener;
import com.khizerhs.news.util.OnLoadNewListener;
import com.khizerhs.news.util.OnNetworkErrorClickListener;

import java.util.List;

/**
 * Created by KhizerHasan on 8/10/2017.
 */

public class NewsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;
    private final int VIEW_TYPE_ERROR = 2;

    private OnLoadNewListener onLoadNewListener;
    private boolean isLoading;
    private List<NewsArticle> newsArticleList;
    private int visibleThreshold = 5;
    private int lastVisibleItem, totalItemCount;
    private OnArticleClickListener onArticleClickListener;
    private OnNetworkErrorClickListener onNetworkErrorClickListener;
    private Context context;

    public NewsRecyclerAdapter(RecyclerView recyclerView, List<NewsArticle> newsArticleList, Context context) {
        this.newsArticleList = newsArticleList;
        this.onArticleClickListener = (OnArticleClickListener) context;
        this.onNetworkErrorClickListener = (OnNetworkErrorClickListener) context;
        this.context = context;

        // Do detect the end of scroll and get further data to display
        final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                totalItemCount = linearLayoutManager.getItemCount();
                lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                if (!isLoading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                    if (onLoadNewListener != null) {
                        onLoadNewListener.onLoadNew();
                    }
                    isLoading = true;
                }
            }
        });
    }


    public void setOnLoadNewListener(OnLoadNewListener onLoadNewListener) {
        this.onLoadNewListener = onLoadNewListener;
    }

    @Override
    public int getItemViewType(int position) {
        // if the news article has the parameter error set, then there is an network error
        if (newsArticleList.get(position) == null)
            return VIEW_TYPE_LOADING;
        else if (newsArticleList.get(position).getError() != null)
            return VIEW_TYPE_ERROR;
        else
            return VIEW_TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false);
            return new NewsViewHolder(view);
        } else if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        } else if (viewType == VIEW_TYPE_ERROR) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_error, parent, false);
            return new NetworkErrorViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof NewsViewHolder) {
            NewsViewHolder newsViewHolder = (NewsViewHolder) holder;
            NewsArticle newsArticleItem = newsArticleList.get(position);
            if (newsArticleItem != null) {
                newsViewHolder.headline.setText(newsArticleItem.getHeadline().getMain());
                Glide.with(context)
                        .load(newsArticleItem.getArticleImageUrl())
                        .placeholder(R.drawable.nyt_placeholder_black)
                        .into(newsViewHolder.thumbnail);
            }
        } else if (holder instanceof NetworkErrorViewHolder) {
            NetworkErrorViewHolder networkErrorViewHolder = (NetworkErrorViewHolder) holder;
            NewsArticle newsArticleItem = newsArticleList.get(position);
            if (newsArticleItem != null)
                networkErrorViewHolder.networkErrorTextView.setText("Error while talking to server \n"
                        + newsArticleItem.getError() + "\nTap me to try again");
            else
                networkErrorViewHolder.networkErrorTextView.setText("Something went wrong.\nTap me to try again");
        } else if (holder instanceof LoadingViewHolder) {
            LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
        }
    }

    @Override
    public int getItemCount() {
        return newsArticleList == null ? 0 : newsArticleList.size();
    }

    public void setLoaded() {
        isLoading = false;
    }

    private class LoadingViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public LottieAnimationView lottieProgressBar;

        public LoadingViewHolder(View view) {
            super(view);
            lottieProgressBar = (LottieAnimationView) view.findViewById(R.id.lottie_loading);
        }

        @Override
        public void onClick(View view) {
            // do nothing
        }
    }


    private class NetworkErrorViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView networkErrorTextView;

        public NetworkErrorViewHolder(View view) {
            super(view);
            networkErrorTextView = (TextView) view.findViewById(R.id.network_error_textView);
            view.setOnClickListener(new DebouncedOnClickListener(1000, this));
        }

        @Override
        public void onClick(View view) {
            onNetworkErrorClickListener.onNetworkErrorClick();
        }
    }


    private class NewsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ImageView thumbnail;
        public TextView headline;

        public NewsViewHolder(View view) {
            super(view);
            thumbnail = (ImageView) view.findViewById(R.id.thumbnail_news);
            headline = (TextView) view.findViewById(R.id.headline_news);
            view.setOnClickListener(new DebouncedOnClickListener(1000, this));
        }

        @Override
        public void onClick(View view) {
            onArticleClickListener.onArticleClicked(view, this.getLayoutPosition());
        }
    }

}

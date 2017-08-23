package com.khizerhs.news;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.khizerhs.news.api.ArticleSearchAPI;
import com.khizerhs.news.api.ArticleSearchService;
import com.khizerhs.news.api.model.NewsArticle;
import com.khizerhs.news.api.model.ResponseWrapper;
import com.khizerhs.news.util.OnArticleClickListener;
import com.khizerhs.news.util.OnNetworkErrorClickListener;
import com.khizerhs.news.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements OnArticleClickListener,OnNetworkErrorClickListener {

    private List<NewsArticle> newsArticleList;
    private NewsRecyclerAdapter newsRecyclerAdapter;
    private int pageNumber;
    private String query;
    private RecyclerView recyclerView;
    private android.support.v7.widget.SearchView searchView;
    private CardView noResultsCardView;
    private TextView noResultsTextView;
    private CardView noNetworkCardView;
    private TextView noNetworkTextView;
    private boolean isGettingData;
    private boolean gettingDataFromScroll;



    private Snackbar snackbar;
    private CountDownTimer countDownTimer;
    private int multiplier;
    private BroadcastReceiver networkChangeReceiver;
    private LinearLayout linearLayout;
    private boolean getDataFailed;


    private LottieAnimationView loadingAnimation;

    private ArticleSearchAPI articleSearchService;
    private CompositeDisposable disposables;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        isGettingData = false;
        gettingDataFromScroll = false;

        linearLayout = (LinearLayout) findViewById(R.id.main_linear_layout);

        loadingAnimation =(LottieAnimationView) findViewById(R.id.loading_animation);
        loadingAnimation.setVisibility(View.VISIBLE);

        // CardView to display when the search return no results
        noResultsCardView =(CardView) findViewById(R.id.no_results);
        noResultsTextView = (TextView) findViewById(R.id.no_results_textView);

        //CardView to display when there is no network and no data in the recycler view
        noNetworkCardView =(CardView) findViewById(R.id.no_network);
        noNetworkTextView = (TextView) findViewById(R.id.no_network_textView);

        articleSearchService = ArticleSearchService.createArticleSearchService("YOUR_API_KEY");
        disposables = new CompositeDisposable();

        networkChangeReceiver = new NetworkChangeReceiver();

        // getting 1st page as default.
        pageNumber = 1;

        //As soon as the app loads, it loads some articles sorted by date to
        query = null;

        //Multiplier for the snackbar. Initally snack bar starts at 15 and then multiplies with the retry number.
        multiplier = 1;

        //Flag to keep track of failures when getting data.
        getDataFailed = false;
        setupRecyclerView();
        Log.d("Khizer-debug", "getData from onCreate");
        getData(pageNumber, query);

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Registering Broadcast receivers to receive network loss broadcast
        // snack bar with retry-backoff is displayed when no internet
        registerReceiver(networkChangeReceiver, new IntentFilter(
                "android.net.conn.CONNECTIVITY_CHANGE"));
    }

    @Override
    protected void onStop() {
        // Un-Registering the broadcast receivers
        super.onStop();
        unregisterReceiver(networkChangeReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // cancelling all the subscriptions to the Observables.
        disposables.dispose();
        if(countDownTimer!=null)
            countDownTimer.cancel();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d("Khizer-debug", "I got a new intent");
        super.onNewIntent(intent);

        // We are worried about only the Intents which are generated when the search query is submitted
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
            pageNumber = 1;

            newsArticleList.clear();
            newsRecyclerAdapter.notifyDataSetChanged();

            loadingAnimation.setVisibility(View.VISIBLE);

            Log.d("Khizer-debug", "getData from on New Intent");
            getData(pageNumber, query);

            if (searchView != null)
                searchView.clearFocus();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.search, menu);

        //Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView =
                (android.support.v7.widget.SearchView) menu.findItem(R.id.menu_search).getActionView();

        //As the current activity is searchable
        searchView.setSearchableInfo(searchManager.getSearchableInfo(this.getComponentName()));
        return true;
    }

    //Method to make a call to the server and get the articles data
    public void getData(int pageNumber, String query) {
        if(isGettingData)
            return;

        if(gettingDataFromScroll) {
            gettingDataFromScroll = false;

            recyclerView.post(() -> {
                newsArticleList.add(null);
                newsRecyclerAdapter.notifyItemInserted(newsArticleList.size() - 1);
            });

        }

        if (noResultsCardView != null && noResultsCardView.getVisibility() == View.VISIBLE)
            noResultsCardView.setVisibility(View.GONE);

        if (noNetworkCardView != null && noNetworkCardView.getVisibility() == View.VISIBLE) {
            noNetworkCardView.setVisibility(View.GONE);
        }

        Log.d("Khizer-debug", "getting data for page" + pageNumber);
        HashMap<String, String> articleSearchQueries = new HashMap<>();
        articleSearchQueries.put("page", String.valueOf(pageNumber));
        if (query != null)
            articleSearchQueries.put("q", query);
        else
            articleSearchQueries.put("sort", "newest");

        // When no network, don't make call to the backend, so return
        if (!Util.isNetworkAvailable(MainActivity.this)) {
            getDataFailed = true;
            if (noNetworkCardView != null
                    && (newsArticleList == null || newsArticleList.size() == 0)) {

                // This Animation is visible only when there are no articles in the recycler view
                if(loadingAnimation!= null && loadingAnimation.getVisibility() == View.VISIBLE);
                loadingAnimation.setVisibility(View.GONE);

                // This card is visible only when there are no articles in the recycler view
                noNetworkTextView.setText("Cannot connect to internet");
                noNetworkCardView.setVisibility(View.VISIBLE);
            }
            return;
        }

        isGettingData = true;

        //RxJava call to get the articles
        //Getting a new page of articles(10 articles in each page) everytime a request is made.
        disposables.add(articleSearchService.getArticles(articleSearchQueries)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<ResponseWrapper>() {
                    @Override
                    public void onComplete() {
                        Log.d("khizer-debug", "Network call completed");
                        newsRecyclerAdapter.setLoaded();
                        getDataFailed = false;
                        isGettingData = false;
                    }

                    @Override
                    public void onError(Throwable e) {
                        getDataFailed = true;
                        isGettingData = false;
                        Log.d("khizer-debug", "Error when making a network call"+ e.getMessage());
                        // This Animation is visible only when there are no articles in the recycler view
                        if(loadingAnimation!= null && loadingAnimation.getVisibility() == View.VISIBLE);
                            loadingAnimation.setVisibility(View.GONE);

                        // If recyclerview of articles is not empty and we receive an error when getting more articles,
                        // A cardview is added at the end of the list which provides error info
                        if( newsArticleList.size() != 0 ) {

                            NewsArticle n = new NewsArticle();
                            n.setError(e.getMessage());

                            // Also remove the loading bar form the end.
                            newsArticleList.remove(newsArticleList.size() - 1);

                            // Add this object in the news list to identify where to add a error card
                            newsArticleList.add(n);
                            newsRecyclerAdapter.notifyItemChanged(newsArticleList.size() - 1);
                        }
                    }

                    @Override
                    public void onNext(ResponseWrapper response) {
                        Log.d("khizer-debug", "got the response from the server");

                        //Hide this animation as some data need to shown on the recycler view
                        if(loadingAnimation!= null && loadingAnimation.getVisibility() == View.VISIBLE);
                            loadingAnimation.setVisibility(View.GONE);

                        //Remove the progressbar from the bottom of the recycler view after done loading the next page
                        if (newsArticleList.size() != 0) {
                            // when loading for first time (when application starts), no progress bar is present so checking is size is 0
                            newsArticleList.remove(newsArticleList.size() - 1);
                            newsRecyclerAdapter.notifyItemRemoved(newsArticleList.size() -1 );
                        }
                        //Add all the articles from the next page
                        int initialSize = newsArticleList.size();
                        newsArticleList.addAll(response.getResponse().getNewsArticles());
                        newsRecyclerAdapter.notifyItemRangeInserted(initialSize,
                                newsArticleList.size() - initialSize);

                        // If the response is empty and the request is to get page 1,
                        // It is understood that there are no results for this query
                        // So show the noResults CardView
                        if(response.getResponse().getNewsArticles().size() == 0 && pageNumber ==1 ){
                            noResultsTextView.setText("No articles found for '"+query+"'\nTry searching 'Cars' or 'Sports'");
                            noResultsCardView.setVisibility(View.VISIBLE);
                        }

                        // If the response is empty but the page number is >1,
                        // It is understood that there are some articles alredy shown and
                        // there are no more articles matching the query.

                        else if(response.getResponse().getNewsArticles().size() == 0 &&(pageNumber > 1)
                                && newsArticleList.size() !=0 ){
                            //when there are no more results remove the last row (animation)
                            newsArticleList.remove(newsArticleList.size() -1);
                            newsRecyclerAdapter.notifyItemRemoved(newsArticleList.size());
                        }
                    }
                }));
    }

    private void setupRecyclerView() {
        newsArticleList = new ArrayList<>();

        //find view by id and attaching adapter for the RecyclerView
        recyclerView = (RecyclerView) findViewById(R.id.news_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        newsRecyclerAdapter = new NewsRecyclerAdapter(recyclerView, newsArticleList, this);
        recyclerView.setAdapter(newsRecyclerAdapter);

        //set load new listener for the RecyclerView adapter
        newsRecyclerAdapter.setOnLoadNewListener(() -> {
            if(!isGettingData && !getDataFailed) {
                Log.d("KHizer-debug", "OnScrollListener after scroll");
                gettingDataFromScroll = true;
                pageNumber++;
                Log.d("Khizer-debug", "getData from OnScroll");
                getData(pageNumber, query);
            }
        });

    }

    private void invalidatePreviousData() {
        newsArticleList = null;
        newsRecyclerAdapter.setOnLoadNewListener(null);
        newsRecyclerAdapter = null;
        recyclerView = null;
    }


    //Callback for click on the article view of recycler views
    @Override
    public void onArticleClicked(View v, int position) {
        NewsArticle clickedNewsArticle = newsArticleList.get(position);
        Intent openDetailIntent = new Intent(this, DetailActivity.class);
        openDetailIntent.putExtra("webUrl", clickedNewsArticle.getWebUrl());
        startActivity(openDetailIntent);
    }

    //Callback for click on the network error view of recycler views
    @Override
    public void onNetworkErrorClick() {
        newsArticleList.remove(newsArticleList.size() - 1);
        newsArticleList.add(null);
        newsRecyclerAdapter.notifyItemChanged(newsArticleList.size() - 1);
        Log.d("Khizer-debug", "getData from onNetworkErrorClick");
        getData(pageNumber,query);

    /*    newsRecyclerAdapter.setOnLoadNewListener(() -> {
            Log.d("KHizer-debug", "keading new data");
            newsArticleList.add(null);
            newsRecyclerAdapter.notifyItemInserted(newsArticleList.size() - 1);

            pageNumber++;
            getData(pageNumber, query);
        });*/
    }

    /******************
     *
     * NetworkConnectivity
     *
     *******************/

    // Receiver which receives network change broadcasts to show and hide the snackbar
    public class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Khizer-debug", "Broadcast Received");
            if (!Util.isNetworkAvailable(context)) {
                showSnackbar();
            }
        }
    }

    public void showSnackbar() {
        if (countDownTimer != null)
            countDownTimer.cancel();
        countDownTimer = setupCounter(15000 * multiplier);
        snackbar = Snackbar.make(linearLayout, "Cannot connect to internet", Snackbar.LENGTH_INDEFINITE);
        snackbar.setActionTextColor(getResources().getColor(R.color.colorPrimary))
                .setAction("Try Now", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        multiplier = 1;
                        //Snackbar is dismissed automatically
                        //check network access and retry to get the data.
                        if (Util.isNetworkAvailable(MainActivity.this)){
                            if (getDataFailed) {
                                if (loadingAnimation != null && newsArticleList.size() == 0)
                                    loadingAnimation.setVisibility(View.VISIBLE);
                                Log.d("Khizer-debug", "getData from Snackbar-Tryagain click");
                                getData(pageNumber, query);
                            }
                        } else {
                            showSnackbar();
                        }

                    }
                });

        // setting max lines to 5 to support the tablets
        View v = snackbar.getView();
        TextView tv = (TextView) v.findViewById(android.support.design.R.id.snackbar_text);
        tv.setMaxLines(5);
        snackbar.show();
        countDownTimer.start();
    }

    public void hideSnackbar() {
        if (snackbar != null) {
            snackbar.dismiss();
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            multiplier = 1;
        }
    }

    public CountDownTimer setupCounter(int timer) {
        CountDownTimer countDownTimer = new CountDownTimer(timer, 1000) {
            public void onTick(long millisUntilFinished) {
                snackbar.setText(" Cannot connect to internet.\n Retrying in " + millisUntilFinished / 1000 + " seconds");
            }
            public void onFinish() {
                multiplier *= 2;
                if (Util.isNetworkAvailable(MainActivity.this)) {
                    hideSnackbar();
                    if(getDataFailed) {
                        if (loadingAnimation != null && newsArticleList.size() == 0)
                            loadingAnimation.setVisibility(View.VISIBLE);
                        Log.d("Khizer-debug", "getData from onFinish COunter");
                        getData(pageNumber, query);
                    }
                } else {
                    showSnackbar();
                }
            }
        };
        return countDownTimer;
    }
}

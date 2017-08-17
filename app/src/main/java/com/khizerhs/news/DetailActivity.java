package com.khizerhs.news;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class DetailActivity extends AppCompatActivity {

    private String articleUrl = "";
    private ProgressBar webViewProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(myToolbar);

        webViewProgressBar = (ProgressBar) findViewById(R.id.progressbar_webview);

        //To Show the back button on the toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        WebView articleDetail = (WebView) findViewById(R.id.detail_webview);
        articleDetail.getSettings().setLoadsImagesAutomatically(true);
        articleDetail.getSettings().setJavaScriptEnabled(true);
        articleDetail.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        articleDetail.setWebViewClient(new MyWebViewClient());

        //Adding WebChromeClient to detect the progress of the page load
        articleDetail.setWebChromeClient(new MyWebChromeClient());

        // Load the URL provided in the extras
        Intent detailIntent = getIntent();
        articleUrl = detailIntent.getStringExtra("webUrl");
        articleDetail.loadUrl(articleUrl);

    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    private class MyWebChromeClient extends WebChromeClient{
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if(webViewProgressBar.getVisibility()!= View.VISIBLE) {
                webViewProgressBar.animate();
                webViewProgressBar.setVisibility(View.VISIBLE);
            }

            webViewProgressBar.setProgress(newProgress);

            if (newProgress == 100) {
                webViewProgressBar.setVisibility(View.INVISIBLE);
                webViewProgressBar.invalidate();
            }
            super.onProgressChanged(view, newProgress);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.share, menu);

        MenuItem shareArticle = menu.findItem(R.id.share_article);

        ShareActionProvider articleShare = (ShareActionProvider) MenuItemCompat.getActionProvider(shareArticle);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, articleUrl);
        articleShare.setShareIntent(shareIntent);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()){
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

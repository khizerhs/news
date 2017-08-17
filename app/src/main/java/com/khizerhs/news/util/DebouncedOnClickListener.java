package com.khizerhs.news.util;

import android.view.View;

import java.util.Date;

/**
 * Created by KhizerHasan on 8/11/2017.
 */


// To debounce the taps made within the threshold specified

public class DebouncedOnClickListener implements View.OnClickListener {
    private long lastTime;
    private int threshold;

    View.OnClickListener clickListener;

    public DebouncedOnClickListener(int threshold, View.OnClickListener clickListener) {
        this.clickListener = clickListener;
        this.threshold = threshold;
        this.lastTime = 0;
    }

    @Override
    public void onClick(View v) {
        long currentTime = new Date().getTime();
        if (currentTime - lastTime > threshold) {
            clickListener.onClick(v);
            lastTime = currentTime;
        } else {
            //do nothing
        }
    }
}

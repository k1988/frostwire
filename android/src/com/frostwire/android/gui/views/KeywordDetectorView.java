/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2017, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.android.gui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.frostwire.android.R;
import com.frostwire.search.KeywordDetector;

import java.util.Map;

/**
 * Created on 11/29/16.
 *
 * @author gubatron
 * @author aldenml
 */

public final class KeywordDetectorView extends RelativeLayout implements KeywordDetector.KeywordDetectorListener {

    private int lastNumberOfSearchesForHistogramRequest;

    public KeywordDetectorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ProductCardView, 0, 0);
        // use XML attributes if we specify any.
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        View.inflate(getContext(), R.layout.view_keyword_detector, this);
        invalidate();
        initComponents();
    }

    private void initComponents() {
        // TODO
    }

    private void updateKeyword(KeywordDetector.Feature feature, String keyword, int appearances) {
        // update our internal view and placement depending on
        // how many appearances we have.
    }

    private void removeKeyword(KeywordDetector.Feature feature, String keyword) {
        // a keyword detector should invoke this on us
        // or we could have a reference to the keyword detector of the
        // ongoing search, and after we invoke it's histogram
        // method, then we add/remove keywords from our list
        // which in turn results in adding/removing our
        // KeywordLabelView.
    }

    private void onKeywordTouched(KeywordDetector.Feature feature, String keyword) {
        // idea: keywords here could have 3 states.
        // inclusive (positive)
        // exclusive (negative)
        // neutral (not in current keyword pipeline)
        // depending on the new state we
    }

    @Override
    public void onSearchReceived(KeywordDetector detector, int numSearchesProcessed) {
        // for now we'll request a histogram update every 5 searches
        if (numSearchesProcessed > lastNumberOfSearchesForHistogramRequest && numSearchesProcessed % 5 == 0) {
            detector.setKeywordDetectorListener(this);
            lastNumberOfSearchesForHistogramRequest = numSearchesProcessed;
            detector.requestHistogramUpdate(KeywordDetector.Feature.FILE_NAME);
            detector.requestHistogramUpdate(KeywordDetector.Feature.FILE_EXTENSION);
            detector.requestHistogramUpdate(KeywordDetector.Feature.SEARCH_SOURCE);
        }
    }

    @Override
    public void onHistogramUpdate(final KeywordDetector detector, final KeywordDetector.Feature feature, final Map.Entry<String, Integer>[] histogram) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (feature.equals(KeywordDetector.Feature.FILE_NAME)) {
                    onFilenamesUpdate(histogram);
                } else if (feature.equals(KeywordDetector.Feature.FILE_EXTENSION)) {
                    onFileExtensionsUpdate(histogram);
                } else if (feature.equals(KeywordDetector.Feature.SEARCH_SOURCE)) {
                    onSearchSourcesUpdate(histogram);
                }
            }
        });
    }

    private void onSearchSourcesUpdate(Map.Entry<String, Integer>[] histogram) {
        // TODO: Update search sources tags
    }

    private void onFileExtensionsUpdate(Map.Entry<String, Integer>[] histogram) {
        // TODO: Update file extensions tags
    }

    private void onFilenamesUpdate(Map.Entry<String, Integer>[] histogram) {
        // TODO: Update file name tags
    }
}

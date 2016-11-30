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
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.frostwire.android.R;
import com.frostwire.search.KeywordDetector;
import com.frostwire.util.Ref;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * Created on 11/29/16.
 * @author gubatron
 * @author aldenml
 */

public final class KeywordDetectorView extends RelativeLayout implements KeywordDetector.KeywordDetectorListener {
    private WeakReference<KeywordDetector> detectorRef;

    private int lastNumberOfSearchesForHistogramRequest;

    public KeywordDetectorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ProductCardView, 0, 0);

    }

    public void setKeywordDetector(KeywordDetector detector) {
        if (detector != null ) {
            detectorRef = Ref.weak(detector);
        }
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

    private void updateKeyword(String keyword, int appearances) {
        // update our internal view and placement depending on
        // how many appearances we have.
    }

    private void removeKeyword(String keyword) {
        // a keyword detector should invoke this on us
        // or we could have a reference to the keyword detector of the
        // ongoing search, and after we invoke it's histogram
        // method, then we add/remove keywords from our list
        // which in turn results in adding/removing our
        // KeywordLabelView.
    }

    private void onKeywordTouched(String keyword) {
        // idea: keywords here could have 3 states.
        // inclusive (positive)
        // exclusive (negative)
        // neutral (not in current keyword pipeline)

        // depending on the new state we
    }

    @Override
    public void onSearchReceived(int numSearchesProcessed) {
        // for now we'll request a histogram update every 5 searches
        if (numSearchesProcessed > lastNumberOfSearchesForHistogramRequest && numSearchesProcessed % 5 == 0) {
            if (Ref.alive(this.detectorRef)) {
                KeywordDetector keywordDetector = this.detectorRef.get();
                keywordDetector.setKeywordDetectorListener(this);
                lastNumberOfSearchesForHistogramRequest = numSearchesProcessed;
                keywordDetector.requestHistogramUpdate();
            }
        }
    }

    @Override
    public void onHistogramUpdate(Map.Entry<String, Integer>[] histogram) {
        // here we should go over the keywords we already know
        // and update the counts on the textviews
        // if there's a keyword we haven't seen, we should add it
        // and we should also re-arrange the keyword labels depending
        // on their count
        // we should have a limit of keywords here
    }
}

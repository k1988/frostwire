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
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.frostwire.android.R;
import com.frostwire.search.KeywordDetector;
import com.frostwire.util.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on 11/29/16.
 *
 * @author gubatron
 * @author aldenml
 */

public final class KeywordDetectorView extends RelativeLayout implements KeywordDetector.KeywordDetectorListener {

    private Logger LOG = Logger.getLogger(KeywordDetectorView.class);
    private LinearLayout labelContainer;
    private Map<String, Button> labelToButton;

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
        labelToButton = new HashMap<>();
        labelContainer = (LinearLayout) findViewById(R.id.view_keyword_detector_keyword_container);
    }

    private void updateKeyword(final KeywordDetector.Feature feature, final String keyword, int appearances) {
        LOG.info("KeywordDetectorView.updateKeyword(feature="+feature+", keyword="+keyword+", appearances="+appearances+")");
        // update our internal view and placement depending on
        // how many appearances we have.

        // TEMP: for now we'll have everybody together, but perhaps it might make sense
        // to keep labels grouped by Feature. this way we can have
        // 2 labels with the same word but which mean something else. e.g. a filename that's the same
        // as the name of a search engine, or file extension, they would be filtered differently.

        // for now let's just work on search sources
        Button keywordButton = labelToButton.get(keyword);
        if (keywordButton == null) {
            keywordButton = initKeywordFilterTag(feature, keyword);
        }
        keywordButton.setText(keyword + " (" + appearances + ")");
    }

    @NonNull
    private Button initKeywordFilterTag(final KeywordDetector.Feature feature, final String keyword) {
        // TODO: A KeywordFilterTag will be a visual component with 3 states (inactive, inclusive-filtering-mode, exclusive-filtering-mode)
        Button keywordButton;
        keywordButton = new Button(getContext());
        keywordButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        keywordButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onKeywordTouched(feature, keyword);
            }
        });
        labelToButton.put(keyword, keywordButton);
        labelContainer.addView(keywordButton);
        labelContainer.invalidate();
        LOG.info("added KeywordFilterTag("+keyword+") to container");
        return keywordButton;
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
        LOG.info("onKeywordTouched("+feature+", "+ keyword+")");
    }

    @Override
    public void onSearchReceived(final KeywordDetector detector, final KeywordDetector.Feature feature, int numSearchesProcessed) {
        // for now we'll request a histogram update every 5 searches
        if (numSearchesProcessed % 5 == 0) {
            detector.requestHistogramUpdate(feature);
        }
    }

    @Override
    public void onHistogramUpdate(final KeywordDetector detector, final KeywordDetector.Feature feature, final Map.Entry<String, Integer>[] histogram) {
        LOG.info("KeywordDetectorView.onHistogramUpdate(...)");
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<String, Integer> entry : histogram) {
                    updateKeyword(feature, entry.getKey(), entry.getValue());
                }
            }
        });
    }
}

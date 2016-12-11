/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2016, FrostWire(R). All rights reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frostwire.search;

import com.frostwire.util.HistoHashMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Created on 11/26/2016
 * @author gubatron
 * @author aldenml
 */
public final class KeywordDetector {
    private static final Set<String> inconsequentials = new HashSet<>();
    private final Map<String, HistoHashMap<String>> histograms;
    private final ExecutorService threadpool;
    private KeywordDetectorListener listener;
    private int numSearchesProcessed;


    public KeywordDetector() {
        this(null);
    }

    public KeywordDetector(ExecutorService threadpool) {
        histograms = new HashMap<>();
        this.threadpool = threadpool;
    }

    public void setKeywordDetectorListener(KeywordDetectorListener listener) {
        this.listener = listener;
    }

    public void addSearchTerms(String category, String terms) {
        // tokenize
        String[] pre_tokens = terms.split("\\s");

        if (pre_tokens == null || pre_tokens.length == 0) {
            return;
        }

        // count consequential terms only
        for (String token : pre_tokens) {
            if (!inconsequentials.contains(token)) {
                updateHistogram(category, token);
            }
        }

        numSearchesProcessed++;

        if (listener != null) {
            listener.onSearchReceived(numSearchesProcessed);
        }
    }

    private void updateHistogram(String category, String token) {
        HistoHashMap<String> histogram = histograms.get(category);
        if (histogram != null) {
            histogram.update(token);
        }
    }

    public void requestHistogramUpdate(String category) {
        HistoHashMap<String> histogram = histograms.get(category);
        if (histogram != null) {
            requestHistogramUpdate(category, histogram);
        }

    }

    public void requestHistogramUpdate(final String category, final HistoHashMap<String> histogram) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onHistogramUpdate(category, histogram.histogram());
                }
            }
        };

        if (threadpool != null) {
            threadpool.submit(r);
        } else {
            new Thread(r, "Keyword-Detector::requestHistogramUpdate()").start();
        }

    }

    static {
        // english
        inconsequentials.add("to");
        inconsequentials.add("the");
        inconsequentials.add("of");
        inconsequentials.add("and");
        inconsequentials.add("that");
        inconsequentials.add("a");
        inconsequentials.add("this");
        inconsequentials.add("at");
        inconsequentials.add("on");
        inconsequentials.add("in");
        // TODO: Add more here as we start testing and getting noise
    }

    public interface KeywordDetectorListener {
        void onSearchReceived(int numSearchesProcessed);
        void onHistogramUpdate(String category, Map.Entry<String, Integer>[] histogram);
    }
}
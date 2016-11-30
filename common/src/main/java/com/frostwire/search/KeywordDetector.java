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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created on 11/26/2016
 * @author gubatron
 * @author aldenml
 */
public final class KeywordDetector {
    private static final Set<String> inconsequentials = new HashSet<>();
    private final HistoHashMap<String> histogram;
    private KeywordDetectorListener listener;
    private int numSearchesProcessed;

    public KeywordDetector() {
        histogram = new HistoHashMap<>();
    }

    public void setKeywordDetectorListener(KeywordDetectorListener listener) {
        this.listener = listener;
    }

    public void addSearchTerms(String terms) {
        // tokenize
        String[] pre_tokens = terms.split("\\s");

        if (pre_tokens == null || pre_tokens.length == 0) {
            return;
        }

        // count consequential terms only
        for (String token : pre_tokens) {
            if (!inconsequentials.contains(token)) {
                histogram.update(token);
            }
        }

        numSearchesProcessed++;

        if (listener != null) {
            listener.onSearchReceived(numSearchesProcessed);
        }
    }

    public void requestHistogramUpdate() {
        Runnable r = new Runnable() {

            @Override
            public void run() {
                Map.Entry<String, Integer>[] histogram = getHistogram();
                if (listener != null) {
                    listener.onHistogramUpdate(histogram);
                }
            }
        };
    }

    private Map.Entry<String, Integer>[] getHistogram() {
        return histogram.histogram();
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
        void onHistogramUpdate(Map.Entry<String, Integer>[] histogram);
    }
}
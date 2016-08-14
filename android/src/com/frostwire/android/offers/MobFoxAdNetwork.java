/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2016, FrostWire(R). All rights reserved.
 *
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

package com.frostwire.android.offers;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.util.Logger;
import com.mobfox.sdk.interstitialads.InterstitialAd;

import java.lang.ref.WeakReference;

/**
 * Created on 8/8/16.
 * @author gubatron
 * @author aldenml
 */
final class MobFoxAdNetwork implements AdNetwork {
    static final int INTERSTITIAL_RELOAD_MAX_RETRIES = 5;
    static int INTERSTITIAL_RELOAD_RETRIES_LEFT = INTERSTITIAL_RELOAD_MAX_RETRIES;
    private static final Logger LOG = Logger.getLogger(MobFoxAdNetwork.class);
    private static final boolean DEBUG_MODE = Offers.DEBUG_MODE;
    private static final long INTERSTITIAL_RELOAD_INTERVAL_IN_SECONDS = 20;

    private boolean started = false;
    private MobFoxInterstitialListener interstitialAdListener = null;
    private InterstitialAd interstitialAd;
    private long lastInterstitialLoadTimestamp;


    // MobFox ads require location permissions, the answer from the user is handled on
    // MainActivity.onRequestPermissionsResult() which invokes Offers.onRequestPermissionsResult()
    // This utility method needs to invoke interstitialAd.onRequestPermissionsResult() provided
    // by MobFox. If the user denies us permissions we should not try again.
    InterstitialAd getInterstitialAd() {
        return interstitialAd;
    }

    @Override
    public void initialize(Activity activity) {
        if (!enabled()) {
            if (!started()) {
                LOG.info("MobFoxAdNetwork initialize(): aborted. not enabled.");
            } else {
                // initialize can be called multiple times, we may have to stop
                // this network if we started it using a default value.
                stop(activity);
            }
            return;
        }
        if (interstitialAdListener == null) {
            interstitialAdListener = new MobFoxInterstitialListener(this, activity.getApplication());
        }
        loadNewInterstitial(activity);
        started = true;
    }

    @Override
    public void stop(Context context) {
        started = false;
        interstitialAd = null;
        interstitialAdListener = null;
    }

    @Override
    public void enable(boolean enabled) {
        Offers.AdNetworkHelper.enable(this, enabled);
    }

    @Override
    public boolean enabled() {
        return Offers.AdNetworkHelper.enabled(this);
    }

    @Override
    public boolean started() {
        return started;
    }

    @Override
    public boolean showInterstitial(WeakReference<? extends Activity> activityRef,
                                    boolean shutdownActivityAfterwards,
                                    boolean dismissActivityAfterward) {
        if (enabled() && started) {

            if (interstitialAdListener == null) {
                LOG.warn("showInterstitial() aborted. interstitial ad listener wasn't created yet, check your logic.");
                return false;
            }

            if (interstitialAdListener.isAfterBehaviorConfigured()) {
                LOG.warn("showInterstitial() aborted. after behavior was already configured, ad was just displayed or still being displayed, check your logic.");
                return false;
            }

            interstitialAdListener.shutdownAppAfter(shutdownActivityAfterwards);
            interstitialAdListener.dismissActivityAfterwards(dismissActivityAfterward);
            try {
                return interstitialAdListener.isAdReadyToDisplay() && interstitialAdListener.show(activityRef);
            } catch (Throwable e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void loadNewInterstitial(Activity activity) {
        if (interstitialAd != null &&
            interstitialAdListener != null &&
            interstitialAdListener.isAfterBehaviorConfigured()) {
            LOG.info("loadNewInterstitial aborted. Ad is good to go already.");
            return;
        }

        LOG.info("loadNewInterstitial");
        interstitialAd = new InterstitialAd(activity);
        interstitialAd.getBanner().setGetLocation(askForLocationPermissions());
        interstitialAd.setInventoryHash(Constants.MOBFOX_INVENTORY_HASH);
        interstitialAd.setListener(interstitialAdListener);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                interstitialAd.load();
                INTERSTITIAL_RELOAD_RETRIES_LEFT--;
                lastInterstitialLoadTimestamp = System.currentTimeMillis();
            }
        });
    }

    @Override
    public String getShortCode() {
        return Constants.AD_NETWORK_SHORTCODE_MOBFOX;
    }

    @Override
    public String getInUsePreferenceKey() {
        return Constants.PREF_KEY_GUI_USE_MOBFOX;
    }

    @Override
    public boolean isDebugOn() {
        return DEBUG_MODE;
    }

    void dontAskForLocationPermissions() {
        ConfigurationManager.instance().setBoolean(Constants.PREF_KEY_ADNETWORK_ASK_FOR_LOCATION_PERMISSION, false);
    }

    private boolean askForLocationPermissions() {
        return ConfigurationManager.instance().getBoolean(Constants.PREF_KEY_ADNETWORK_ASK_FOR_LOCATION_PERMISSION);
    }

    void reloadInterstitial(final Activity activity) {
        if (INTERSTITIAL_RELOAD_RETRIES_LEFT == 0) {
            LOG.info("reloadInterstitial() aborted. Exhausted retry attempts.");
            return;
        }

        final long RELOAD_INTERVAL_IN_MILLIS = (INTERSTITIAL_RELOAD_INTERVAL_IN_SECONDS*1000);
        long timeSinceLastReload = System.currentTimeMillis() - lastInterstitialLoadTimestamp;

        // wait 20 seconds to reload an ad.
        if (timeSinceLastReload > RELOAD_INTERVAL_IN_MILLIS) {
            // let's do it right away
            loadNewInterstitial(activity);
        } else {
            // let's do it when 20 seconds have passed.
            long timeLeft = RELOAD_INTERVAL_IN_MILLIS - timeSinceLastReload;
            Handler h = new Handler(activity.getMainLooper());
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadNewInterstitial(activity);
                }
            }, timeLeft);
        }
    }
}

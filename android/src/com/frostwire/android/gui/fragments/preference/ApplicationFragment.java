/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml),
 * Copyright (c) 2011-2017, FrostWire(R). All rights reserved.
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

package com.frostwire.android.gui.fragments.preference;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.widget.Toast;

import com.frostwire.android.AndroidPlatform;
import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.activities.BuyActivity;
import com.frostwire.android.gui.services.Engine;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractPreferenceFragment;
import com.frostwire.android.gui.views.preference.KitKatStoragePreference;
import com.frostwire.android.gui.views.preference.KitKatStoragePreference.KitKatStoragePreferenceDialog;
import com.frostwire.android.offers.PlayStore;
import com.frostwire.android.offers.Product;
import com.frostwire.android.offers.Products;
import com.frostwire.util.Logger;
import com.frostwire.util.Ref;

import java.lang.ref.WeakReference;
import java.util.Collection;

/**
 * @author gubatron
 * @author aldenml
 */
public final class ApplicationFragment extends AbstractPreferenceFragment {

    private static final Logger LOG = Logger.getLogger(ApplicationFragment.class);

    private static final boolean INTERNAL_BUILD = false;
    private static final int MILLISECONDS_IN_A_DAY = 86400000;

    // TODO: refactor this
    // due to the separation of fragments and activities
    public static long removeAdsPurchaseTime = 0;

    public ApplicationFragment() {
        super(R.xml.settings_application);
    }

    @Override
    protected void initComponents() {
        setupConnectSwitch();
        setupStorageOption();
        setupStore(removeAdsPurchaseTime);
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment dlg = null;
        if (preference instanceof KitKatStoragePreference) {
            dlg = KitKatStoragePreferenceDialog.newInstance(preference.getKey());
        }

        if (dlg != null) {
            dlg.setTargetFragment(this, 0);
            dlg.show(getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    private void setupConnectSwitch() {
        SwitchPreferenceCompat preference = findPreference("frostwire.prefs.internal.connect_disconnect");
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean newStatus = (boolean) newValue;
                Engine e = Engine.instance();
                if (e.isStarted() && !newStatus) {
                    disconnect();
                } else if (newStatus && (e.isStopped() || e.isDisconnected())) {
                    connect();
                }
                return true;
            }
        });

        updateConnectSwitchStatus();
    }

    private void setupStorageOption() {
        // intentional repetition of preference value here
        String kitkatKey = "frostwire.prefs.storage.path";
        String lollipopKey = "frostwire.prefs.storage.path_asf";

        PreferenceCategory category = findPreference("frostwire.prefs.general");

        if (AndroidPlatform.saf()) {
            // make sure this won't be saved for kitkat
            Preference p = findPreference(kitkatKey);
            if (p != null) {
                category.removePreference(p);
            }
            p = findPreference(lollipopKey);
            if (p != null) {
                p.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        updateStorageOptionSummary(newValue.toString());
                        return true;
                    }
                });
                updateStorageOptionSummary(ConfigurationManager.instance().getStoragePath());
            }
        } else {
            Preference p = findPreference(lollipopKey);
            if (p != null) {
                category.removePreference(p);
            }
        }
    }

    private void updateConnectSwitchStatus() {
        SwitchPreferenceCompat preference = findPreference("frostwire.prefs.internal.connect_disconnect");
        Engine e = Engine.instance();
        if (e.isStarted()) {
            setChecked(preference, true, false);
        } else if (e.isStopped() || e.isDisconnected()) {
            setChecked(preference, false, false);
        }
    }

    private void connect() {
        Engine.instance().startServices(); // internally this is an async call in libtorrent
        updateConnectSwitchStatus();
    }

    private void disconnect() {
        Engine.instance().stopServices(true); // internally this is an async call in libtorrent
        updateConnectSwitchStatus();
        UIUtils.showShortMessage(getView(), R.string.toast_on_disconnect);
    }

    private void updateStorageOptionSummary(String newPath) {
        // intentional repetition of preference value here
        String lollipopKey = "frostwire.prefs.storage.path_asf";
        if (AndroidPlatform.saf()) {
            Preference p = findPreference(lollipopKey);
            if (p != null) {
                p.setSummary(newPath);
            }
        }
    }

    private void setupStore(long purchaseTimestamp) {
        Preference p = findPreference("frostwire.prefs.offers.buy_no_ads");
        if (p != null && !Constants.IS_GOOGLE_PLAY_DISTRIBUTION) {
            PreferenceCategory category = findPreference("frostwire.prefs.other_settings");
            category.removePreference(p);
        } else if (p != null) {
            PlayStore playStore = PlayStore.getInstance();
            playStore.refresh();
            Collection<Product> purchasedProducts = Products.listEnabled(playStore, Products.DISABLE_ADS_FEATURE);
            if (purchaseTimestamp == 0 && purchasedProducts != null && purchasedProducts.size() > 0) {
                initRemoveAdsSummaryWithPurchaseInfo(p, purchasedProducts);
                //otherwise, a BuyActivity intent has been configured on application_preferences.xml
            } else if (purchaseTimestamp > 0 &&
                    (System.currentTimeMillis() - purchaseTimestamp) < 30000) {
                p.setSummary(getString(R.string.processing_payment) + "...");
                p.setOnPreferenceClickListener(null);
            } else {
                p.setSummary(R.string.remove_ads_description);
                p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        PlayStore.getInstance().endAsync();
                        Intent intent = new Intent(getActivity(), BuyActivity.class);
                        startActivityForResult(intent, BuyActivity.PURCHASE_SUCCESSFUL_RESULT_CODE);
                        return true;
                    }
                });
            }
        }
    }

    private void initRemoveAdsSummaryWithPurchaseInfo(Preference p, Collection<Product> purchasedProducts) {
        Product product = purchasedProducts.iterator().next();
        String daysLeft = "";
        // if it's a one time purchase, show user how many days left she has.
        if (!product.subscription() && product.purchased()) {
            int daysBought = Products.toDays(product.sku());
            if (daysBought > 0) {
                long timePassed = System.currentTimeMillis() - product.purchaseTime();
                int daysPassed = (int) timePassed / MILLISECONDS_IN_A_DAY;
                if (daysPassed > 0 && daysPassed < daysBought) {
                    daysLeft = " (" + getString(R.string.days_left) + ": " + String.valueOf(daysBought - daysPassed) + ")";
                }
            }
        }
        p.setSummary(getString(R.string.current_plan) + ": " + product.description() + daysLeft);
        p.setOnPreferenceClickListener(new RemoveAdsOnPreferenceClickListener(getActivity(), purchasedProducts));
    }

    private static final class RemoveAdsOnPreferenceClickListener implements Preference.OnPreferenceClickListener {

        private int clicksLeftToConsumeProducts = 20;
        private final Collection<Product> purchasedProducts;
        private WeakReference<Activity> activityRef;

        RemoveAdsOnPreferenceClickListener(Activity activity, final Collection<Product> purchasedProducts) {
            activityRef = Ref.weak(activity);
            this.purchasedProducts = purchasedProducts;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (purchasedProducts != null && !purchasedProducts.isEmpty()) {
                LOG.info("Products purchased by user:");
                for (Product p : purchasedProducts) {
                    LOG.info(" - " + p.description() + " (" + p.sku() + ")");
                }

                if (INTERNAL_BUILD) {
                    clicksLeftToConsumeProducts--;
                    LOG.info("If you click again " + clicksLeftToConsumeProducts + " times, all your ONE-TIME purchases will be forced-consumed.");
                    if (0 >= clicksLeftToConsumeProducts && clicksLeftToConsumeProducts < 11) {
                        if (clicksLeftToConsumeProducts == 0) {
                            for (Product p : purchasedProducts) {
                                if (p.subscription()) {
                                    continue;
                                }
                                PlayStore.getInstance().consume(p);
                                LOG.info(" - " + p.description() + " (" + p.sku() + ") force-consumed!");
                                UIUtils.showToastMessage(preference.getContext(),
                                        "Product " + p.sku() + " forced-consumed.",
                                        Toast.LENGTH_SHORT);
                            }
                            if (Ref.alive(activityRef)) {
                                activityRef.get().finish();
                            }
                        }
                    }
                }

                return true; // true = click was handled.
            } else {
                LOG.info("Couldn't find any purchases.");
            }
            return false;
        }
    }
}

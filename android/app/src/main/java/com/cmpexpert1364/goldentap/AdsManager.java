package com.cmpexpert1364.goldentap;

import android.app.Activity;
import android.webkit.WebView;
import android.util.Log;

import com.adivery.sdk.Adivery;
import com.adivery.sdk.AdiveryListener;

public class AdsManager {

    private static final String TAG = "GoldenTapAds";

    private static final String APP_KEY =
            "2edbcef3-2bdb-4543-98c2-8e63eeac4fd1";

    private static final String PLACEMENT_ID =
            "06825fec-bfea-4f95-8166-b1445afce02b";

    private static boolean initialized = false;

    private final Activity activity;
    private final WebView webView;

    private boolean loading = false;
    private boolean showRequested = false;
    private boolean adShowing = false;
    private boolean resultSent = false;

    public AdsManager(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
    }

    public void showRewarded() {

        activity.runOnUiThread(() -> {

            if (adShowing || showRequested) {
                return;
            }

            showRequested = true;
            resultSent = false;

            if (!initialized) {
                initializeAds();
            }

            if (!initialized) {
                failRequest();
                return;
            }

            if (Adivery.isLoaded(PLACEMENT_ID)) {
                displayAd();
            } else if (!loading) {

                loading = true;

                Adivery.prepareRewardedAd(
                        activity,
                        PLACEMENT_ID
                );
            }
        });
    }

    private void initializeAds() {

        try {

            Adivery.configure(
                    activity.getApplication(),
                    APP_KEY
            );

            Adivery.addGlobalListener(new AdiveryListener() {

                @Override
                public void onRewardedAdLoaded(String placementId) {

                    if (!PLACEMENT_ID.equals(placementId)) {
                        return;
                    }

                    activity.runOnUiThread(() -> {

                        loading = false;

                        // فقط در صورت درخواست کاربر نمایش بده
                        if (showRequested && !adShowing) {
                            displayAd();
                        }

                    });
                }

                @Override
                public void onRewardedAdClosed(
                        String placementId,
                        boolean isRewarded) {

                    if (!PLACEMENT_ID.equals(placementId)) {
                        return;
                    }

                    activity.runOnUiThread(() -> {

                        if (!adShowing) {
                            return;
                        }

                        adShowing = false;
                        showRequested = false;

                        if (!resultSent) {
                            resultSent = true;
                            sendResult(isRewarded);
                        }

                        // فقط آماده‌سازی تبلیغ بعدی
                        // بدون پخش خودکار
                        prepareNextAd();

                    });
                }

                @Override
                public void onAppOpenAdLoaded(String placementId) {
                }

                @Override
                public void onInterstitialAdLoaded(String placementId) {
                }

                @Override
                public void log(String placementId, String log) {

                    Log.d(TAG, log);
                }
            });

            initialized = true;

        } catch (Exception e) {

            Log.e(TAG, "Initialization error", e);
            failRequest();
        }
    }

    private void displayAd() {

        if (!showRequested || adShowing) {
            return;
        }

        try {

            if (!Adivery.isLoaded(PLACEMENT_ID)) {
                return;
            }

            showRequested = false;
            adShowing = true;
            resultSent = false;

            Adivery.showAd(PLACEMENT_ID);

        } catch (Exception e) {

            Log.e(TAG, "Show ad error", e);

            adShowing = false;
            failRequest();
        }
    }

    private void prepareNextAd() {

        if (loading || adShowing) {
            return;
        }

        try {

            loading = true;

            Adivery.prepareRewardedAd(
                    activity,
                    PLACEMENT_ID
            );

        } catch (Exception e) {

            loading = false;

            Log.e(TAG, "Preload error", e);
        }
    }

    private void failRequest() {

        showRequested = false;
        adShowing = false;
        loading = false;

        if (!resultSent) {
            resultSent = true;
            sendResult(false);
        }
    }

    private void sendResult(boolean completed) {

        activity.runOnUiThread(() -> {

            if (activity.isFinishing() ||
                    webView == null) {
                return;
            }

            String result = completed ? "true" : "false";

            webView.evaluateJavascript(
                    "if(window.onGoldenTapAdResult){" +
                    "window.onGoldenTapAdResult(" +
                    result +
                    ");}",
                    null
            );
        });
    }
}

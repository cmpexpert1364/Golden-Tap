package com.cmpexpert1364.goldentap;

import android.app.Activity;
import android.webkit.WebView;
import android.util.Log;

import com.adivery.sdk.Adivery;
import com.adivery.sdk.AdiveryListener;

public class AdsManager {

    private static final String APP_KEY =
            "2edbcef3-2bdb-4543-98c2-8e63eeac4fd1";

    private static final String PLACEMENT_ID =
            "06825fec-bfea-4f95-8166-b1445afce02b";

    private static boolean initialized = false;
    private static boolean loading = false;

    private final Activity activity;
    private final WebView webView;

    public AdsManager(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
    }

    public void showRewarded() {
        activity.runOnUiThread(() -> {

            if (!initialized) {
                initializeAds();
            }

            if (Adivery.isLoaded(PLACEMENT_ID)) {
                Adivery.showAd(PLACEMENT_ID);
            } else if (!loading) {
                loading = true;
                Adivery.prepareRewardedAd(activity, PLACEMENT_ID);
            } else {
                sendResult(false);
            }
        });
    }

    private void initializeAds() {
        try {
            Adivery.configure(activity.getApplication(), APP_KEY);

            Adivery.addGlobalListener(new AdiveryListener() {

                @Override
                public void onRewardedAdLoaded(String placementId) {
                    if (PLACEMENT_ID.equals(placementId)) {
                        loading = false;
                        Adivery.showAd(PLACEMENT_ID);
                    }
                }

                @Override
                public void onRewardedAdClosed(
                        String placementId,
                        boolean isRewarded) {

                    if (PLACEMENT_ID.equals(placementId)) {
                        sendResult(isRewarded);
                        Adivery.prepareRewardedAd(activity, PLACEMENT_ID);
                    }
                }

                @Override
                public void onAppOpenAdLoaded(String placementId) {
                }

                @Override
                public void onInterstitialAdLoaded(String placementId) {
                }

                @Override
                public void log(String placementId, String log) {
                    Log.d("GoldenTapAds", log);
                }
            });

            initialized = true;

        } catch (Exception e) {
            Log.e("GoldenTapAds", "Initialization error", e);
            sendResult(false);
        }
    }

    private void sendResult(boolean completed) {
        activity.runOnUiThread(() -> {
            String result = completed ? "true" : "false";

            webView.evaluateJavascript(
                    "if(window.onGoldenTapAdResult){" +
                    "window.onGoldenTapAdResult(" + result + ");}",
                    null
            );
        });
    }
}

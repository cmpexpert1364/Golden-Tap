package com.cmpexpert1364.goldentap;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;

import com.adivery.sdk.Adivery;
import com.adivery.sdk.AdiveryListener;

import org.json.JSONObject;

public class AdsManager {

    private static final String TAG = "GoldenTapAds";

    // مقادیر واقعی قبلی خودت را اینجا قرار بده
    private static final String APP_KEY = "YOUR_EXISTING_APP_KEY";
    private static final String PLACEMENT_ID = "YOUR_EXISTING_PLACEMENT_ID";

    private static final long REQUEST_TIMEOUT = 90000L;

    private final Activity activity;
    private final WebView webView;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean initialized = false;
    private boolean loading = false;
    private boolean showRequested = false;
    private boolean adShowing = false;
    private boolean resultSent = false;

    private String activeRequestId = null;

    private Runnable timeoutRunnable;

    public AdsManager(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
    }

    public void showRewarded(String requestId, String placement) {

        activity.runOnUiThread(() -> {

            if (requestId == null || requestId.trim().isEmpty()) {
                return;
            }

            if (showRequested || adShowing) {
                sendResult(requestId, false);
                return;
            }

            activeRequestId = requestId;
            showRequested = true;
            resultSent = false;

            startTimeout();

            initializeAds();

            if (Adivery.isLoaded(PLACEMENT_ID)) {
                displayAd();
            } else if (!loading) {
                prepareAd();
            }

        });
    }

    private void initializeAds() {

        if (initialized) {
            return;
        }

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

                        // فقط وقتی درخواست واقعی وجود دارد نمایش بده
                        if (showRequested && !adShowing) {
                            displayAd();
                        }

                    });
                }

                @Override
                public void onRewardedAdClosed(
                        String placementId,
                        boolean isRewarded
                ) {

                    if (!PLACEMENT_ID.equals(placementId)) {
                        return;
                    }

                    activity.runOnUiThread(() -> {

                        if (!adShowing) {
                            return;
                        }

                        adShowing = false;
                        showRequested = false;

                        cancelTimeout();

                        if (!resultSent) {
                            sendActiveResult(isRewarded);
                        }

                        activeRequestId = null;

                        // فقط آماده‌سازی تبلیغ بعدی؛ بدون نمایش خودکار
                        prepareNextAd();

                    });
                }

                @Override
                public void onAppOpenAdLoaded(String placementId) {
                    // استفاده نمی‌شود
                }

                @Override
                public void onInterstitialAdLoaded(String placementId) {
                    // استفاده نمی‌شود
                }

                @Override
                public void log(String placementId, String log) {
                    Log.d(TAG, placementId + " : " + log);
                }

            });

            initialized = true;

        } catch (Exception e) {

            Log.e(TAG, "Adivery initialization failed", e);
            failRequest();

        }
    }

    private void prepareAd() {

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

            Log.e(TAG, "Prepare ad failed", e);

            if (showRequested) {
                failRequest();
            }

        }
    }

    private void displayAd() {

        if (!showRequested || adShowing) {
            return;
        }

        if (activeRequestId == null) {
            failRequest();
            return;
        }

        if (!Adivery.isLoaded(PLACEMENT_ID)) {
            if (!loading) {
                prepareAd();
            }
            return;
        }

        try {

            // قفل قبل از فراخوانی SDK
            adShowing = true;
            showRequested = false;

            Adivery.showAd(PLACEMENT_ID);

            Log.d(TAG, "Rewarded ad displayed");

        } catch (Exception e) {

            Log.e(TAG, "Show ad failed", e);

            adShowing = false;
            showRequested = false;

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
            Log.e(TAG, "Next ad preparation failed", e);

        }
    }

    private void startTimeout() {

        cancelTimeout();

        timeoutRunnable = () -> {

            if (activeRequestId == null || resultSent) {
                return;
            }

            Log.w(TAG, "Ad request timeout");

            showRequested = false;
            adShowing = false;

            sendActiveResult(false);

            activeRequestId = null;

        };

        handler.postDelayed(
                timeoutRunnable,
                REQUEST_TIMEOUT
        );
    }

    private void cancelTimeout() {

        if (timeoutRunnable != null) {

            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;

        }
    }

    private void failRequest() {

        showRequested = false;
        adShowing = false;

        cancelTimeout();

        if (!resultSent && activeRequestId != null) {
            sendActiveResult(false);
        }

        activeRequestId = null;
    }

    private void sendActiveResult(boolean completed) {

        if (resultSent || activeRequestId == null) {
            return;
        }

        String requestId = activeRequestId;

        resultSent = true;

        sendResult(requestId, completed);
    }

    private void sendResult(
            String requestId,
            boolean completed
    ) {

        if (webView == null) {
            return;
        }

        String safeId = JSONObject.quote(requestId);

        String javascript =
                "if(typeof window.onGoldenTapAdResult === 'function'){" +
                "window.onGoldenTapAdResult(" +
                safeId + "," +
                completed + "," +
                JSONObject.quote("") +
                ");}";

        webView.post(() -> {

            if (webView != null) {
                webView.evaluateJavascript(
                        javascript,
                        null
                );
            }

        });

    }
}

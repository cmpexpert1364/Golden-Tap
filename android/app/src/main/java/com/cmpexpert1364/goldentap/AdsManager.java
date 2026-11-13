package com.cmpexpert1364.goldentap;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import android.util.Log;

import com.adivery.sdk.Adivery;
import com.adivery.sdk.AdiveryListener;

import org.json.JSONObject;

import java.util.UUID;

public class AdsManager {

    private static final String TAG = "GoldenTapAds";

    private static final String APP_KEY =
            "2edbcef3-2bdb-4543-98c2-8e63eeac4fd1";

    private static final String PLACEMENT_ID =
            "06825fec-bfea-4f95-8166-b1445afce02b";

    private static final long REQUEST_TIMEOUT = 90000;

    private final Activity activity;
    private final WebView webView;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private String activeRequestId;
    private String activePlacement = PLACEMENT_ID;

    private boolean requestPending = false;
    private boolean adShowing = false;
    private boolean resultSent = false;
    private boolean destroyed = false;

    private final Runnable timeoutRunnable = new Runnable() {
        @Override
        public void run() {

            if (requestPending && !resultSent && !adShowing) {

                Log.w(TAG, "Ad loading timed out");

                finishRequest(false);
            }
        }
    };

    public AdsManager(Activity activity, WebView webView) {

        this.activity = activity;
        this.webView = webView;

        initializeAdivery();
    }

    private void initializeAdivery() {

        try {

            Adivery.configure(
                    activity.getApplication(),
                    APP_KEY
            );

            Adivery.addGlobalListener(new AdiveryListener() {

                @Override
                public void onRewardedAdLoaded(String placementId) {

                    Log.d(TAG, "Rewarded ad loaded");

                    activity.runOnUiThread(() -> {

                        if (destroyed ||
                                !requestPending ||
                                adShowing ||
                                resultSent) {
                            return;
                        }

                        if (!activePlacement.equals(placementId)) {
                            return;
                        }

                        showLoadedAd();
                    });
                }

                @Override
                public void onRewardedAdClosed(
                        String placementId,
                        boolean isRewarded
                ) {

                    Log.d(
                            TAG,
                            "Rewarded ad closed. Rewarded: " + isRewarded
                    );

                    activity.runOnUiThread(() -> {

                        if (destroyed ||
                                !requestPending ||
                                resultSent) {
                            return;
                        }

                        if (!activePlacement.equals(placementId)) {
                            return;
                        }

                        adShowing = false;

                        // جایزه فقط با تأیید SDK ثبت می‌شود.
                        finishRequest(isRewarded);
                    });
                }

                @Override
                public void onAppOpenAdLoaded(String placementId) {
                }

                @Override
                public void onInterstitialAdLoaded(String placementId) {
                }

                @Override
                public void log(
                        String placementId,
                        String message
                ) {

                    Log.d(TAG, "Adivery: " + message);
                }
            });

            prepareNextAd();

        } catch (Exception e) {

            Log.e(TAG, "Adivery initialization error", e);
        }
    }

    public void showRewarded(
            String requestId,
            String placement
    ) {

        activity.runOnUiThread(() -> {

            if (destroyed) {
                return;
            }

            if (requestId == null ||
                    requestId.trim().isEmpty()) {

                Log.w(TAG, "Invalid request ID");
                return;
            }

            if (requestPending || adShowing) {

                Log.w(TAG, "Another ad request is active");
                return;
            }

            activeRequestId = requestId.trim();
            activePlacement = PLACEMENT_ID;

            if (placement != null &&
                    PLACEMENT_ID.equals(placement.trim())) {

                activePlacement = placement.trim();
            }

            requestPending = true;
            resultSent = false;

            handler.removeCallbacks(timeoutRunnable);

            handler.postDelayed(
                    timeoutRunnable,
                    REQUEST_TIMEOUT
            );

            try {

                if (Adivery.isLoaded(activePlacement)) {

                    showLoadedAd();

                } else {

                    Log.d(TAG, "Requesting rewarded ad");

                    Adivery.prepareRewardedAd(
                            activity,
                            activePlacement
                    );
                }

            } catch (Exception e) {

                Log.e(TAG, "Could not request rewarded ad", e);

                finishRequest(false);
            }
        });
    }

    private void showLoadedAd() {

        if (destroyed ||
                !requestPending ||
                adShowing ||
                resultSent) {
            return;
        }

        try {

            if (!Adivery.isLoaded(activePlacement)) {
                return;
            }

            adShowing = true;

            // مهلت بارگذاری پس از شروع پخش دیگر اعمال نمی‌شود.
            handler.removeCallbacks(timeoutRunnable);

            Log.d(TAG, "Showing rewarded ad");

            Adivery.showAd(activePlacement);

        } catch (Exception e) {

            Log.e(TAG, "Could not show rewarded ad", e);

            adShowing = false;

            finishRequest(false);
        }
    }

    private void finishRequest(boolean completed) {

        if (destroyed ||
                resultSent ||
                !requestPending) {
            return;
        }

        resultSent = true;
        requestPending = false;
        adShowing = false;

        handler.removeCallbacks(timeoutRunnable);

        String requestId = activeRequestId;

        activeRequestId = null;

        // شناسه محلی یکتا؛ نه شناسه تأییدشده سمت سرور Adivery.
        String transactionId = completed
                ? UUID.randomUUID().toString()
                : "";

        sendResultToWebView(
                requestId,
                completed,
                transactionId
        );

        prepareNextAd();
    }

    private void sendResultToWebView(
            String requestId,
            boolean completed,
            String transactionId
    ) {

        if (requestId == null ||
                webView == null ||
                destroyed) {
            return;
        }

        String safeRequestId = JSONObject.quote(requestId);
        String safeTransactionId = JSONObject.quote(transactionId);

        String javascript =
                "if (window.onGoldenTapAdResult) {" +
                "window.onGoldenTapAdResult(" +
                safeRequestId + "," +
                completed + "," +
                safeTransactionId +
                ");" +
                "}";

        activity.runOnUiThread(() -> {

            if (!destroyed &&
                    webView != null) {

                webView.evaluateJavascript(
                        javascript,
                        null
                );
            }
        });
    }

    public void prepareNextAd() {

        activity.runOnUiThread(() -> {

            if (destroyed ||
                    requestPending ||
                    adShowing ||
                    activity.isFinishing()) {
                return;
            }

            try {

                if (!Adivery.isLoaded(PLACEMENT_ID)) {

                    Log.d(TAG, "Preparing next rewarded ad");

                    Adivery.prepareRewardedAd(
                            activity,
                            PLACEMENT_ID
                    );
                }

            } catch (Exception e) {

                Log.e(TAG, "Preparing next ad failed", e);
            }
        });
    }

    public void destroy() {

        destroyed = true;

        handler.removeCallbacks(timeoutRunnable);

        requestPending = false;
        adShowing = false;
        resultSent = true;

        activeRequestId = null;
    }
}

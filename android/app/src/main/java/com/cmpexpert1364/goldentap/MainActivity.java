package com.cmpexpert1364.goldentap;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;
import android.annotation.SuppressLint;

import com.adivery.sdk.Adivery;
import com.adivery.sdk.AdiveryListener;

import org.json.JSONObject;

public class MainActivity extends Activity {

    private WebView webView;

    private static final String APP_KEY =
        "2edbcef3-2bdb-4543-98c2-8e63eeac4fd1";

    private static final String PLACEMENT_ID =
        "06825fec-bfea-4f95-8166-b1445afce02b";

    private String pendingRequestId = null;

    private boolean adSdkReady = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.addJavascriptInterface(
            new GoldenTapAdsBridge(),
            "GoldenTapAdsNative"
        );

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                String bridge =
                    "(function() {" +
                    "window.__goldenTapPending = {};" +
                    "window.__goldenTapCounter = 0;" +

                    "window.__goldenTapAdResult = function(id, result) {" +
                    "var resolve = window.__goldenTapPending[id];" +
                    "if(resolve) {" +
                    "delete window.__goldenTapPending[id];" +
                    "resolve(result === true);" +
                    "}" +
                    "};" +

                    "window.GoldenTapAds = {" +
                    "showRewarded: function(placement) {" +
                    "return new Promise(function(resolve) {" +
                    "var id = String(++window.__goldenTapCounter);" +
                    "window.__goldenTapPending[id] = resolve;" +
                    "GoldenTapAdsNative.showRewarded(placement, id);" +
                    "});" +
                    "}" +
                    "};" +
                    "})();";

                view.evaluateJavascript(bridge, null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        initializeAds();

        webView.loadUrl(
            "file:///android_asset/index.html"
        );
    }

    private void initializeAds() {

        try {

            Adivery.configure(
                getApplication(),
                APP_KEY
            );

            Adivery.addGlobalListener(
                new AdiveryListener() {

                    @Override
                    public void onRewardedAdClosed(
                        String placementId,
                        boolean isRewarded
                    ) {

                        if (!PLACEMENT_ID.equals(placementId)) {
                            return;
                        }

                        finishAdRequest(isRewarded);
                    }

                }
            );

            Adivery.prepareRewardedAd(
                this,
                PLACEMENT_ID
            );

            adSdkReady = true;

        } catch (Exception e) {

            adSdkReady = false;
        }
    }

    private class GoldenTapAdsBridge {

        @JavascriptInterface
        public void showRewarded(
            String rewardType,
            String requestId
        ) {

            runOnUiThread(() -> {

                if (!adSdkReady || pendingRequestId != null) {
                    sendAdResult(requestId, false);
                    return;
                }

                try {

                    if (!Adivery.isLoaded(PLACEMENT_ID)) {

                        Adivery.prepareRewardedAd(
                            MainActivity.this,
                            PLACEMENT_ID
                        );

                        sendAdResult(requestId, false);
                        return;
                    }

                    pendingRequestId = requestId;

                    Adivery.showAd(PLACEMENT_ID);

                } catch (Exception e) {

                    pendingRequestId = null;
                    sendAdResult(requestId, false);
                }

            });
        }
    }

    private void finishAdRequest(boolean rewarded) {

        if (pendingRequestId == null) {
            return;
        }

        String id = pendingRequestId;

        pendingRequestId = null;

        sendAdResult(id, rewarded);
    }

    private void sendAdResult(
        String requestId,
        boolean rewarded
    ) {

        if (webView == null) {
            return;
        }

        String js =
            "window.__goldenTapAdResult(" +
            JSONObject.quote(requestId) +
            "," +
            rewarded +
            ");";

        runOnUiThread(() ->
            webView.evaluateJavascript(js, null)
        );
    }

    @Override
    public void onBackPressed() {

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        if (webView != null) {
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}

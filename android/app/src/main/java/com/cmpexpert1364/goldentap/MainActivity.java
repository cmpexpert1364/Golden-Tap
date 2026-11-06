package com.cmpexpert1364.goldentap;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;

import com.adivery.sdk.Adivery;
import com.adivery.sdk.AdiveryListener;

public class MainActivity extends Activity {

    private static final String APP_KEY =
            "2edbcef3-2bdb-4543-98c2-8e63eeac4fd1";

    private static final String REWARDED_ID =
            "06825fec-bfea-4f95-8166-b1445afce02b";

    private WebView webView;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private String pendingCallbackId = null;
    private boolean pendingShow = false;

    private Runnable loadTimeout;

    private final AdiveryListener adListener =
            new AdiveryListener() {

        @Override
        public void onRewardedAdLoaded(String placementId) {

            if (!REWARDED_ID.equals(placementId)) return;

            Log.d("GoldenTap", "Rewarded ad loaded");

            if (pendingShow && pendingCallbackId != null) {

                pendingShow = false;

                if (loadTimeout != null) {
                    handler.removeCallbacks(loadTimeout);
                }

                Adivery.showAd(REWARDED_ID);
            }
        }

        @Override
        public void onRewardedAdShown(String placementId) {
            Log.d("GoldenTap", "Rewarded ad shown");
        }

        @Override
        public void onRewardedAdClicked(String placementId) {
            Log.d("GoldenTap", "Rewarded ad clicked");
        }

        @Override
        public void onRewardedAdClosed(
                String placementId,
                boolean isRewarded) {

            if (!REWARDED_ID.equals(placementId)) return;

            Log.d("GoldenTap", "Rewarded: " + isRewarded);

            sendResult(isRewarded);

            Adivery.prepareRewardedAd(
                    MainActivity.this,
                    REWARDED_ID
            );
        }

        @Override
        public void log(String placementId, String message) {
            Log.d("GoldenTap Adivery", message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Adivery.setLoggingEnabled(true);
        Adivery.configure(getApplication(), APP_KEY);

        Adivery.addPlacementListener(
                REWARDED_ID,
                adListener
        );

        Adivery.prepareRewardedAd(
                this,
                REWARDED_ID
        );

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.addJavascriptInterface(
                new AdBridge(),
                "AndroidAds"
        );

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(
                    WebView view,
                    String url) {

                String script =
                    "(function() {" +
                    "window.__goldenTapAdCounter=0;" +

                    "window.__goldenTapAdPending={};" +

                    "window.__goldenTapAdResult=function(id,ok) {" +
                    "var r=window.__goldenTapAdPending[id];" +
                    "if(r) {" +
                    "delete window.__goldenTapAdPending[id];" +
                    "r(!!ok);" +
                    "}" +
                    "};" +

                    "window.GoldenTapAds={" +
                    "showRewarded:function(placement) {" +
                    "return new Promise(function(resolve) {" +
                    "var id=String(++window.__goldenTapAdCounter);" +
                    "window.__goldenTapAdPending[id]=resolve;" +
                    "window.AndroidAds.showRewardedAd(" +
                    "String(placement||''),id);" +
                    "});" +
                    "}" +
                    "};" +

                    "})();";

                view.evaluateJavascript(script, null);
            }
        });

        setContentView(webView);

        webView.loadUrl(
                "file:///android_asset/index.html"
        );
    }

    public class AdBridge {

        @JavascriptInterface
        public void showRewardedAd(
                String placement,
                String callbackId) {

            runOnUiThread(() -> {

                if (callbackId == null ||
                        !callbackId.matches("\\d+")) {
                    return;
                }

                if (pendingCallbackId != null) {
                    webView.evaluateJavascript(
                            "window.__goldenTapAdResult(" +
                            callbackId + ",false)",
                            null
                    );
                    return;
                }

                pendingCallbackId = callbackId;

                if (Adivery.isLoaded(REWARDED_ID)) {

                    Adivery.showAd(REWARDED_ID);

                } else {

                    pendingShow = true;

                    Adivery.prepareRewardedAd(
                            MainActivity.this,
                            REWARDED_ID
                    );

                    loadTimeout = () -> {
                        if (pendingShow) {
                            pendingShow = false;
                            sendResult(false);
                        }
                    };

                    handler.postDelayed(loadTimeout, 45000);
                }
            });
        }
    }

    private void sendResult(boolean rewarded) {

        if (pendingCallbackId == null) return;

        String id = pendingCallbackId;
        pendingCallbackId = null;
        pendingShow = false;

        if (loadTimeout != null) {
            handler.removeCallbacks(loadTimeout);
        }

        if (webView != null) {

            webView.evaluateJavascript(
                    "window.__goldenTapAdResult('" +
                    id + "'," + rewarded + ")",
                    null
            );
        }
    }

    @Override
    protected void onDestroy() {

        handler.removeCallbacksAndMessages(null);

        Adivery.removePlacementListener(REWARDED_ID);

        if (webView != null) {
            webView.destroy();
        }

        super.onDestroy();
    }
}

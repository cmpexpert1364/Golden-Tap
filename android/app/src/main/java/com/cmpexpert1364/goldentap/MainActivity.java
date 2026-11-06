package com.cmpexpert1364.goldentap;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;

import com.adivery.sdk.Adivery;

public class MainActivity extends Activity {

    private static final String APP_KEY =
            "2edbcef3-2bdb-4543-98c2-8e63eeac4fd1";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);

        webView.addJavascriptInterface(
                new AdBridge(),
                "AndroidAds"
        );

        webView.setWebViewClient(new WebViewClient());

        setContentView(webView);

        webView.loadUrl(
                "file:///android_asset/index.html"
        );

        try {
            Adivery.configure(getApplication(), APP_KEY);
            Log.d("GoldenTap", "Adivery initialized");

        } catch (Exception e) {
            Log.e("GoldenTap", "Adivery initialization failed", e);
        }
    }

    public class AdBridge {

        @JavascriptInterface
        public void showRewardedAd(
                String placement,
                String callbackId
        ) {
            runOnUiThread(() -> {

                if (callbackId == null ||
                        !callbackId.matches("\\d+")) {
                    return;
                }

                webView.evaluateJavascript(
                        "if(window.__goldenTapAdResult){" +
                        "window.__goldenTapAdResult('" +
                        callbackId + "',false);}",
                        null
                );
            });
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

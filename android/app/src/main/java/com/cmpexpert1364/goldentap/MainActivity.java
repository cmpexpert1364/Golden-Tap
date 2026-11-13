package com.cmpexpert1364.goldentap;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;

public class MainActivity extends Activity {

    private WebView webView;
    private AdsManager adsManager;

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

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        adsManager = new AdsManager(this, webView);

        webView.addJavascriptInterface(
                new AdsBridge(),
                "AndroidGoldenTapAds"
        );

        webView.loadUrl(
                "file:///android_asset/index.html"
        );
    }

    public class AdsBridge {

        @JavascriptInterface
        public void showRewarded(
                String requestId,
                String placement
        ) {
            runOnUiThread(() -> {
                if (adsManager != null) {
                    adsManager.showRewarded(
                            requestId,
                            placement
                    );
                }
            });
        }
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

        if (adsManager != null) {
            adsManager.destroy();
            adsManager = null;
        }

        if (webView != null) {
            webView.removeJavascriptInterface(
                    "AndroidGoldenTapAds"
            );
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}

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

    adsManager = new AdsManager(this, webView);

    webView.addJavascriptInterface(
            new AdsBridge(),
            "GoldenTapAdsNative"
    );

    webView.setWebChromeClient(new WebChromeClient());

    webView.setWebViewClient(new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            view.evaluateJavascript(
                "window.GoldenTapAds = {" +
                "showRewarded: function(placement) {" +
                "return new Promise(function(resolve) {" +
                "window.onGoldenTapAdResult = resolve;" +
                "GoldenTapAdsNative.showRewarded(placement);" +
                "setTimeout(function() {" +
                "if(window.onGoldenTapAdResult) {" +
                "window.onGoldenTapAdResult(false);" +
                "window.onGoldenTapAdResult = null;" +
                "}" +
                "}, 120000);" +
                "});" +
                "}" +
                "};",
                null
            );
        }
    });

    webView.loadUrl("file:///android_asset/index.html");
}

public class AdsBridge {

    @JavascriptInterface
    public void showRewarded(String placement) {
        runOnUiThread(() -> adsManager.showRewarded());
    }
}

@Override
public void onBackPressed() {
    if (webView.canGoBack()) {
        webView.goBack();
    } else {
        super.onBackPressed();
    }
}

}

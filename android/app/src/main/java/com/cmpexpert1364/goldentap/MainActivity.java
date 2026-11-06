package com.cmpexpert1364.goldentap;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;

public class MainActivity extends Activity {

private WebView webView;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    webView = new WebView(this);

    WebSettings settings = webView.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(true);
    settings.setAllowContentAccess(true);

    webView.addJavascriptInterface(
            new AdBridge(),
            "AndroidAds"
    );

    webView.setWebViewClient(new WebViewClient());

    setContentView(webView);

    webView.loadUrl(
            "file:///android_asset/index.html"
    );
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

            String script =
                    "if(window.__goldenTapAdResult){" +
                    "window.__goldenTapAdResult('" +
                    callbackId +
                    "',false);" +
                    "}";

            webView.evaluateJavascript(script, null);
        });
    }
}

@Override
protected void onDestroy() {

    if (webView != null) {
        webView.stopLoading();
        webView.destroy();
        webView = null;
    }

    super.onDestroy();
}

}

package com.cmpexpert1364.goldentap;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.adivery.sdk.Adivery;

public class MainActivity extends Activity {

    private static final String APP_KEY =
            "2edbcef3-2bdb-4543-98c2-8e63eeac4fd1";

    private static final String REWARDED_ID =
            "06825fec-bfea-4f95-8166-b1445afce02b";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Adivery.setLoggingEnabled(true);
        Adivery.configure(getApplication(), APP_KEY);

        Adivery.prepareRewardedAd(
                getBaseContext(), REWARDED_ID
        );

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.addJavascriptInterface(
                new AdBridge(), "AndroidAds"
        );

        webView.loadUrl("file:///android_asset/index.html");

        setContentView(webView);
    }

    public class AdBridge {

        @JavascriptInterface
        public void showRewardedAd() {
            runOnUiThread(() -> {
                Adivery.showAd(REWARDED_ID);
            });
        }
    }
}

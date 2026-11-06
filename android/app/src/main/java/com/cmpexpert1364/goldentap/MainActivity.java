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

        try {
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
                        "window

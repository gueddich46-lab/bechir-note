package com.bechir.note;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.view.Gravity;

public class MainActivity extends Activity {
    private WebView web;
    private boolean authenticated = false;
    private boolean prompted = false;
    private final String url = "https://gueddich46-lab.github.io/bechir-note/";
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView splash = new TextView(this);
        splash.setText("🔐\nBECHIR NOTE\nدفترك الشخصي");
        splash.setGravity(Gravity.CENTER);
        splash.setTextSize(24);
        splash.setBackgroundColor(0xfff6f1e8);
        setContentView(splash);
        requestUnlock();
    }
    private void requestUnlock() {
        KeyguardManager km = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        if (km == null || !km.isDeviceSecure()) {
            new AlertDialog.Builder(this).setTitle("حماية الجهاز").setMessage("فعّل قفل الشاشة أو البصمة في إعدادات الهاتف لحماية التطبيق. كلمة مرور الملاحظات مطلوبة دائمًا.")
                .setPositiveButton("المتابعة", (d,w)->openNotebook()).setNegativeButton("إغلاق",(d,w)->finish()).show();
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            android.hardware.biometrics.BiometricManager bm = (android.hardware.biometrics.BiometricManager)getSystemService(Context.BIOMETRIC_SERVICE);
            if (bm != null && bm.canAuthenticate() == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS) {
                CancellationSignal signal = new CancellationSignal();
                BiometricPrompt prompt = new BiometricPrompt.Builder(this).setTitle("فتح BECHIR NOTE").setSubtitle("تحقق بالبصمة قبل إدخال كلمة مرور الدفتر")
                    .setNegativeButton("استخدام قفل الهاتف",getMainExecutor(),(d,w)->deviceUnlock())
                    .build();
                prompt.authenticate(signal,getMainExecutor(),new BiometricPrompt.AuthenticationCallback(){
                    @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r){openNotebook();}
                    @Override public void onAuthenticationError(int code,CharSequence message){if(code!=BiometricPrompt.BIOMETRIC_ERROR_NEGATIVE_BUTTON)deviceUnlock();}
                });
                return;
            }
        }
        deviceUnlock();
    }
    private void deviceUnlock(){
        KeyguardManager km=(KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        if(km==null){finish();return;}
        Intent intent=km.createConfirmDeviceCredentialIntent("فتح BECHIR NOTE","تحقق باستخدام قفل هاتفك");
        if(intent==null){finish();return;}
        startActivityForResult(intent,11);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==11){if(result==RESULT_OK)openNotebook();else finish();}
    }
    private void openNotebook(){
        if(authenticated)return;
        authenticated=true;
        web=new WebView(this);
        web.setBackgroundColor(0xfff6f1e8);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,android.webkit.WebResourceRequest req){
                Uri uri=req.getUrl();
                if("gueddich46-lab.github.io".equals(uri.getHost()) && uri.getPath().startsWith("/bechir-note/"))return false;
                startActivity(new Intent(Intent.ACTION_VIEW,uri));return true;
            }
        });
        setContentView(web);
        web.loadUrl(url);
    }
    @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){if(web!=null)web.destroy();super.onDestroy();}
}
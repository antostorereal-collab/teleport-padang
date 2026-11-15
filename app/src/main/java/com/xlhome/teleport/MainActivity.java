package com.padang.fieldutility;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;
    private LocationManager locationManager;
    private Handler mockHandler = new Handler(Looper.getMainLooper());
    private boolean isMocking = false;
    private double mockLat = -0.8971;
    private double mockLng = 100.3818;

    private final Runnable mockRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isMocking) return;
            pushMockLocation(LocationManager.GPS_PROVIDER);
            pushMockLocation(LocationManager.NETWORK_PROVIDER);
            mockHandler.postDelayed(this, 800);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setAllowFileAccess(true);

        webView.addJavascriptInterface(new MockGPSBridge(), "AndroidMockGPS");
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void setupProvider(String provider) {
        try { locationManager.removeTestProvider(provider); } catch (Exception ignored) {}
        try {
            locationManager.addTestProvider(
                provider, false, false, false, false, true, true, true,
                Criteria.POWER_LOW, Criteria.ACCURACY_FINE
            );
            locationManager.setTestProviderEnabled(provider, true);
        } catch (SecurityException se) {
            Toast.makeText(this, "Aktifkan aplikasi ini di Opsi Pengembang -> Pilih aplikasi lokasi palsu!", Toast.LENGTH_LONG).show();
            openDevOptionsIntent();
        } catch (Exception ignored) {}
    }

    private void pushMockLocation(String provider) {
        try {
            Location loc = new Location(provider);
            loc.setLatitude(mockLat);
            loc.setLongitude(mockLng);
            loc.setAltitude(14.5);
            loc.setAccuracy(3.2f);
            loc.setTime(System.currentTimeMillis());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            }
            locationManager.setTestProviderLocation(provider, loc);
        } catch (Exception ignored) {}
    }

    private void openDevOptionsIntent() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {}
        }
    }

    public class MockGPSBridge {
        @JavascriptInterface
        public void startMock(double lat, double lng, String label) {
            runOnUiThread(() -> {
                mockLat = lat;
                mockLng = lng;
                setupProvider(LocationManager.GPS_PROVIDER);
                setupProvider(LocationManager.NETWORK_PROVIDER);
                isMocking = true;
                mockHandler.removeCallbacks(mockRunnable);
                mockHandler.post(mockRunnable);
            });
        }

        @JavascriptInterface
        public void stopMock() {
            runOnUiThread(() -> {
                isMocking = false;
                mockHandler.removeCallbacks(mockRunnable);
                try { locationManager.removeTestProvider(LocationManager.GPS_PROVIDER); } catch (Exception ignored) {}
                try { locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER); } catch (Exception ignored) {}
            });
        }

        @JavascriptInterface
        public void openDevSettings() {
            runOnUiThread(() -> openDevOptionsIntent());
        }
    }
}

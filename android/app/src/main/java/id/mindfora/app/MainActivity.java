package id.mindfora.app;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.WebViewListener;

public class MainActivity extends BridgeActivity {

    private boolean splashHidden = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Must run AFTER super.onCreate: BridgeActivity switches from the launch theme to AppTheme.NoActionBar there.
        // Touching the decor view earlier inflates the window with the launch theme (splash-painted title bar).
        // Draw behind status & navigation bars on every Android version (Android 15+ forces this anyway),
        // so the web layout handles insets consistently via env(safe-area-inset-*).
        EdgeToEdge.enable(this);

        if (bridge == null) return;
        initDownloadListener();
        // Keep the animated splash until the first page (remote web or offline.html) is painted.
        // SplashScreen.launchShowDuration in capacitor.config.ts is only the upper bound.
        bridge.addWebViewListener(
            new WebViewListener() {
                @Override
                public void onPageCommitVisible(WebView view, String url) {
                    if (splashHidden) return;
                    splashHidden = true;
                    view.evaluateJavascript("window.Capacitor?.Plugins?.SplashScreen?.hide()", null);
                }
            }
        );
    }

    /**
     * Android WebView has no built-in download handler: responses with
     * Content-Disposition: attachment silently fail (Capacitor then shows errorPath).
     * Forward them to DownloadManager with the WebView session cookies so
     * auth-protected routes (training.download, exports) still work.
     */
    private void initDownloadListener() {
        bridge.getWebView().setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                if (url.startsWith("blob:")) return; // blob downloads need the JS-side Filesystem bridge
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimeType);
                request.setTitle(fileName);
                request.setDescription(getString(R.string.app_name));
                String cookies = CookieManager.getInstance().getCookie(url);
                if (cookies != null) request.addRequestHeader("Cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                if (dm != null) {
                    dm.enqueue(request);
                    Toast.makeText(this, "Mengunduh " + fileName, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Unduhan gagal dimulai", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

package id.mindfora.app;

import android.os.Bundle;
import android.webkit.WebView;
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
}

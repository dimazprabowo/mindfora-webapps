package id.mindfora.app;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Draw behind status & navigation bars on every Android version (Android 15+ forces this anyway),
        // so the web layout handles insets consistently via env(safe-area-inset-*).
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
    }
}

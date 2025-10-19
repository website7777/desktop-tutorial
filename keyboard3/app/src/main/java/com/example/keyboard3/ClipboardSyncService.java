package com.example.keyboard3;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;

public class ClipboardSyncService extends Service {

    private static final String SYNC_URL = "http://your-windows-pc-ip:8080/sync";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("clipboard_text")) {
            String text = intent.getStringExtra("clipboard_text");
            syncWithWindows(text);
        }
        return START_NOT_STICKY;
    }

    private void syncWithWindows(String text) {
        new Thread(() -> {
            try {
                URL url = new URL(SYNC_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String jsonInputString = "{\"text\": \"" + text + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                Log.d("ClipboardSync", "Sync response: " + responseCode);

            } catch (Exception e) {
                Log.e("ClipboardSync", "Sync failed", e);
            }
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
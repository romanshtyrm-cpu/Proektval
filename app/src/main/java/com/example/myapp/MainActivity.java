package com.example.myapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_CAMERA = 101;
    private CameraPreview cameraPreview;
    private VideoHttpServer httpServer;
    private TextView tvStatus, tvUrl;
    private Button btnStart, btnStop;
    private final int PORT = 8080;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        tvUrl = findViewById(R.id.tvUrl);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);

        FrameLayout holder = findViewById(R.id.camera_preview_container);
        cameraPreview = new CameraPreview(this);
        holder.addView(cameraPreview);

        btnStart.setOnClickListener(v -> startStream());
        btnStop.setOnClickListener(v -> stopStream());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQ_CAMERA
            );
        }
    }

    private void startStream() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            tvStatus.setText("Server: CAMERA permission required");
            return;
        }

        boolean ok = cameraPreview.startCameraFront();
        if (!ok) {
            tvStatus.setText("Server: camera start failed");
            return;
        }

        if (httpServer == null) {
            httpServer = new VideoHttpServer(PORT, cameraPreview);
            try {
                httpServer.start();   // ← ИСПРАВЛЕННОЕ МЕСТО
            } catch (Exception e) {
                tvStatus.setText("Server: start failed");
                return;
            }
        }

        String ip = getLocalIpAddress();
        tvUrl.setText("URL: http://" + ip + ":" + PORT + "/stream");
        tvStatus.setText("Server: ON");
    }

    private void stopStream() {
        if (httpServer != null) {
            httpServer.stop();
            httpServer = null;
        }
        cameraPreview.stopCamera();
        tvStatus.setText("Server: OFF");
    }

    private String getLocalIpAddress() {
        WifiManager wm = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }

    @Override
    protected void onDestroy() {
        stopStream();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        if (requestCode == REQ_CAMERA) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                tvStatus.setText("Server: camera permission denied");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
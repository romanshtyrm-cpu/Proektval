package com.example.myapp;

import android.util.Log;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class VideoHttpServer extends NanoHTTPD {

    private final CameraPreview cameraPreview;
    private volatile boolean running = false;
    private static final String BOUNDARY = "MJPEGBOUNDARY";

    public VideoHttpServer(int port, CameraPreview preview) {
        super(port);
        this.cameraPreview = preview;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        if ("/stream".equals(uri) || "/".equals(uri)) {
            try {
                PipedOutputStream out = new PipedOutputStream();
                PipedInputStream in = new PipedInputStream(out);

                startStreamingThread(out);

                return newChunkedResponse(
                        Response.Status.OK,
                        "multipart/x-mixed-replace; boundary=" + BOUNDARY,
                        in
                );

            } catch (IOException e) {
                return newFixedLengthResponse("Stream error");
            }
        }

        String html = "<html><body>"
                + "<h3>MJPEG Stream</h3>"
                + "<img src=\"/stream\" />"
                + "</body></html>";

        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private void startStreamingThread(PipedOutputStream out) {
        running = true;

        new Thread(() -> {
            try {
                while (running) {
                    byte[] jpeg = cameraPreview.getLatestJpeg();
                    if (jpeg != null) writeFrame(out, jpeg);
                    Thread.sleep(66); // ~15 FPS
                }
            } catch (Exception e) {
                Log.e("VideoHttpServer", "stream stopped");
            } finally {
                running = false;
                try { out.close(); } catch (IOException ignored) {}
            }
        }).start();
    }

    private void writeFrame(PipedOutputStream out, byte[] jpeg) throws IOException {
        String header = "\r\n--" + BOUNDARY + "\r\n"
                + "Content-Type: image/jpeg\r\n"
                + "Content-Length: " + jpeg.length + "\r\n\r\n";

        out.write(header.getBytes());
        out.write(jpeg);
        out.flush();
    }

    @Override
    public void stop() {
        running = false;
        super.stop();
    }

    public boolean isRunning() {
        return running;
    }
}
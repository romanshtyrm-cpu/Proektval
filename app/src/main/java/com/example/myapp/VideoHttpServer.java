package com.example.myapp;

import android.util.Log;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.io.OutputStream;

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

            return newChunkedResponse(
                    Response.Status.OK,
                    "multipart/x-mixed-replace; boundary=" + BOUNDARY,
                    new IStreamer() {
                        @Override
                        public void stream(OutputStream outputStream) throws IOException {
                            running = true;
                            try {
                                while (running) {
                                    byte[] jpeg = cameraPreview.getLatestJpeg();
                                    if (jpeg != null) {
                                        writeFrame(outputStream, jpeg);
                                    }
                                    Thread.sleep(66);
                                }
                            } catch (InterruptedException ignored) {
                            } finally {
                                running = false;
                            }
                        }
                    }
            );
        }

        String html = "<html><body>"
                + "<h3>MJPEG Stream</h3>"
                + "<img src=\"/stream\" />"
                + "</body></html>";

        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private void writeFrame(OutputStream out, byte[] jpeg) throws IOException {
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
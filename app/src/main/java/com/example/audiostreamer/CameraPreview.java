package com.example.myapp; // поменяй на свой package

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.util.List;

@SuppressWarnings("deprecation")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private byte[] latestJpeg;
    private final Object frameLock = new Object();
    private int previewWidth = 640;
    private int previewHeight = 480;

    public CameraPreview(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // open handled elsewhere
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        stopCamera();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // no-op
    }

    public boolean startCameraFront() {
        try {
            stopCamera();
            int cameraId = findFrontFacingCamera();
            if (cameraId == -1) cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
            mCamera = Camera.open(cameraId);
            Camera.Parameters params = mCamera.getParameters();
            Camera.Size best = getBestPreviewSize(params);
            if (best != null) {
                previewWidth = best.width;
                previewHeight = best.height;
                params.setPreviewSize(previewWidth, previewHeight);
            } else {
                previewWidth = 640; previewHeight = 480;
                params.setPreviewSize(previewWidth, previewHeight);
            }
            params.setPreviewFormat(ImageFormat.NV21);
            mCamera.setParameters(params);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
            return true;
        } catch (Exception e) {
            Log.e("CameraPreview", "startCameraFront error", e);
            stopCamera();
            return false;
        }
    }

    public void stopCamera() {
        if (mCamera != null) {
            try {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
            } catch (Exception ignored) {}
            mCamera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        try {
            YuvImage yuv = new YuvImage(data, ImageFormat.NV21, previewWidth, previewHeight, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // quality 60 - balance between bandwidth and quality
            yuv.compressToJpeg(new Rect(0, 0, previewWidth, previewHeight), 60, baos);
            byte[] jpeg = baos.toByteArray();
            synchronized (frameLock) {
                latestJpeg = jpeg;
            }
        } catch (Exception e) {
            // ignore frame errors
        }
    }

    public byte[] getLatestJpeg() {
        synchronized (frameLock) {
            return latestJpeg;
        }
    }

    private int findFrontFacingCamera() {
        int cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(camIdx, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return camIdx;
            }
        }
        return -1;
    }

    private Camera.Size getBestPreviewSize(Camera.Parameters params) {
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        if (sizes == null) return null;
        Camera.Size best = null;
        for (Camera.Size s : sizes) {
            if (best == null) best = s;
            else {
                int area = s.width * s.height;
                int bestArea = best.width * best.height;
                if (area <= 640*480 && area > bestArea) best = s;
            }
        }
        return best;
    }
}
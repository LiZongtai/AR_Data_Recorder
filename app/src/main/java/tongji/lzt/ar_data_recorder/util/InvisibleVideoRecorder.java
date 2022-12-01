package tongji.lzt.ar_data_recorder.util;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class InvisibleVideoRecorder {
    private static final String TAG = "InvisibleVideoRecorder";
    private final CameraCaptureSessionStateCallback cameraCaptureSessionStateCallback = new CameraCaptureSessionStateCallback();
    private final CameraDeviceStateCallback cameraDeviceStateCallback = new CameraDeviceStateCallback();
    private MediaRecorder mediaRecorder;
    private CameraManager cameraManager;
    private Context context;

    private CameraDevice cameraDevice;

    private HandlerThread handlerThread;
    private Handler handler;

    private final int CAMERA_BACK_ID = 0;

    public InvisibleVideoRecorder(Context context) {
        this.context = context;
        handlerThread = new HandlerThread("camera");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        try {
            mediaRecorder = new MediaRecorder();

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);

            final String filename = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AR_Data/" + File.separator + System.currentTimeMillis() + ".mp4";
            mediaRecorder.setOutputFile(filename);
            Log.d(TAG, "start: " + filename);

            // by using the profile, I don't think I need to do any of these manually:
//            mediaRecorder.setVideoEncodingBitRate(16000000);
//            mediaRecorder.setVideoFrameRate(30);
//            mediaRecorder.setCaptureRate(30);
//            mediaRecorder.setVideoSize(1920, 1080);
//            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
//            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

//            Log.d(TAG, "start: 1 " + CamcorderProfile.hasProfile(CameraMetadata.LENS_FACING_BACK, CamcorderProfile.QUALITY_1080P));
            // true
//            Log.d(TAG, "start: 2 " + CamcorderProfile.hasProfile(CameraMetadata.LENS_FACING_BACK, CamcorderProfile.QUALITY_HIGH_SPEED_1080P));
            // false
//            Log.d(TAG, "start: 3 " + CamcorderProfile.hasProfile(CameraMetadata.LENS_FACING_BACK, CamcorderProfile.QUALITY_HIGH));
            // true

            CamcorderProfile profile = CamcorderProfile.get(CAMERA_BACK_ID, CamcorderProfile.QUALITY_1080P);
            Log.d(TAG, "start: profile " + profile.toString());
//          start: 0 android.media.CamcorderProfile@114016694 {
//                audioBitRate: 256000
//                audioChannels: 2
//                audioCodec: 3
//                audioSampleRate: 48000
//                duration: 30
//                fileFormat: 2
//                quality: 6
//                videoBitRate: 17000000
//                videoCodec: 2
//                videoFrameHeight: 1080
//                videoFrameRate: 30
//                videoFrameWidth: 1920
//            }
            mediaRecorder.setOrientationHint(0);
            mediaRecorder.setProfile(profile);
            mediaRecorder.prepare();
        } catch (IOException e) {
            Log.d(TAG, "start: exception" + e.getMessage());
        }

    }

    public void start() {
        Log.d(TAG, "start: ");

        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraManager.openCamera(String.valueOf(CAMERA_BACK_ID), cameraDeviceStateCallback, handler);
        } catch (CameraAccessException | SecurityException e) {
            Log.d(TAG, "start: exception " + e.getMessage());
        }

    }

    public void stop() {
        Log.d(TAG, "stop: ");
        mediaRecorder.stop();
        mediaRecorder.reset();
        mediaRecorder.release();
        cameraDevice.close();
        try {
            handlerThread.join();
        } catch (InterruptedException e) {

        }
    }

    private class CameraCaptureSessionStateCallback extends CameraCaptureSession.StateCallback {
        private final static String TAG = "CamCaptSessionStCb";

        @Override
        public void onActive(CameraCaptureSession session) {
            Log.d(TAG, "onActive: ");
            super.onActive(session);
        }

        @Override
        public void onClosed(CameraCaptureSession session) {
            Log.d(TAG, "onClosed: ");
            super.onClosed(session);
        }

        @Override
        public void onConfigured(CameraCaptureSession session) {
            Log.d(TAG, "onConfigured: ");
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.d(TAG, "onConfigureFailed: ");
        }

        @Override
        public void onReady(CameraCaptureSession session) {
            Log.d(TAG, "onReady: ");
            super.onReady(session);
            try {
                CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                builder.addTarget(mediaRecorder.getSurface());
                CaptureRequest request = builder.build();
                session.setRepeatingRequest(request, null, handler);
                mediaRecorder.start();
            } catch (CameraAccessException e) {
                Log.d(TAG, "onConfigured: " + e.getMessage());

            }
        }

        @Override
        public void onSurfacePrepared(CameraCaptureSession session, Surface surface) {
            Log.d(TAG, "onSurfacePrepared: ");
            super.onSurfacePrepared(session, surface);
        }
    }

    private class CameraDeviceStateCallback extends CameraDevice.StateCallback {
        private final static String TAG = "CamDeviceStateCb";

        @Override
        public void onClosed(CameraDevice camera) {
            Log.d(TAG, "onClosed: ");
            super.onClosed(camera);
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d(TAG, "onDisconnected: ");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.d(TAG, "onError: ");
        }

        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(TAG, "onOpened: ");
            cameraDevice = camera;
            try {
                camera.createCaptureSession(Arrays.asList(mediaRecorder.getSurface()), cameraCaptureSessionStateCallback, handler);
            } catch (CameraAccessException e) {
                Log.d(TAG, "onOpened: " + e.getMessage());
            }
        }
    }

}

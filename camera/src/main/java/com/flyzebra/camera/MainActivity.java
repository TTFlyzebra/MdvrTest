package com.flyzebra.camera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.flyzebra.utils.CameraUtil;
import com.flyzebra.utils.FlyLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    private String[] mPermissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private boolean isPermission = false;
    private boolean isSurfaceReady = false;
    private final int REQUEST_CODE = 102;
    private TextureView textureView01;
    private static final HandlerThread mThread = new HandlerThread("CAMERA2");

    static {
        mThread.start();
    }

    private Handler mBackgroundHandler = new Handler(mThread.getLooper());
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private CameraManager mCameraManager;
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private String cameraID = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView01 = findViewById(R.id.textureview01);
        textureView01.setSurfaceTextureListener(this);

        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            verifyPermissions();
        } else {
            isPermission = true;
        }
    }

    public void verifyPermissions() {
        List<String> applyPerms = new ArrayList<>();
        for (String permission : mPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                applyPerms.add(permission);
            }
        }
        if (!applyPerms.isEmpty()) {
            ActivityCompat.requestPermissions(this, applyPerms.toArray(new String[applyPerms.size()]), REQUEST_CODE);
        } else {
            isPermission = true;
        }
    }

    private void openCamera() {
        FlyLog.d("openCamera");
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                mCameraManager.openCamera(cameraID, deviceCallBack, mBackgroundHandler);
            } catch (CameraAccessException e) {
                FlyLog.e(e.toString());
            }
        } else {
            FlyLog.e("no camera permission");
        }
    }

    private void closeCamera() {
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private CameraDevice.StateCallback deviceCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            try {
                mCameraDevice = cameraDevice;
                SurfaceTexture texture = textureView01.getSurfaceTexture();
                texture.setDefaultBufferSize(1280, 720);
                Surface surface = new Surface(texture);
                mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                mPreviewRequestBuilder.set(CaptureRequest.JPEG_THUMBNAIL_SIZE, new Size(1280, 720));
                Range<Integer>[] fpsRanges = CameraUtil.getCameraFps(MainActivity.this);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRanges[0]);
                mPreviewRequestBuilder.addTarget(surface);
                mCameraDevice.createCaptureSession(Arrays.asList(surface), mCaptureStateCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
                FlyLog.e(e.toString());
            }

        }

        @Override
        public void onError(CameraDevice cameraDevice, int arg1) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }
    };

    private CameraCaptureSession.StateCallback mCaptureStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            if (null == mCameraDevice) {
                return;
            }
            mCaptureSession = cameraCaptureSession;
            try {
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
            } catch (CameraAccessException e) {
                FlyLog.e(e.toString());
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession arg0) {
        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            boolean authorized = true;
            for (int num : grantResults) {
                if (num != PackageManager.PERMISSION_GRANTED) {
                    authorized = false;
                    break;
                }
            }
            if (authorized) {
                isPermission = true;
                if (isSurfaceReady) {
                    openCamera();
                }
            }
        }
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        FlyLog.d("onSurfaceTextureAvailable");
        if (isPermission) {
            openCamera();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        FlyLog.d("onSurfaceTextureDestroyed");
        closeCamera();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

    }
}
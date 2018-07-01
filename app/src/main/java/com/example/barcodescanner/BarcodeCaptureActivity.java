package com.example.barcodescanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.example.barcodescanner.detectors.SingleDetector;
import com.example.barcodescanner.graphics.BarcodeGraphic;
import com.example.barcodescanner.trackers.GraphicTracker;
import com.example.barcodescanner.trackers.TrackerFactory;
import com.example.barcodescanner.trackers.TrackerFocusFactory;
import com.example.barcodescanner.views.CameraSource;
import com.example.barcodescanner.views.CameraSourcePreview;
import com.example.barcodescanner.views.GraphicOverlay;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

public final class BarcodeCaptureActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    public static final String AutoFocus = "AutoFocus";
    public static final String UseFlash = "UseFlash";
    public static final String UseQRCode = "UseQRCode";
    public static final String BarcodeObject = "Barcode";

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<BarcodeGraphic> mGraphicCaptureOverlay;

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    private TextView mHint;
    private View mButtonClose;
    private View mButtonAutoFocus;
    private View mButtonFlash;

    private boolean mAutoFocus = true;
    private boolean mFlash = false;
    private boolean mQRCode = true;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.barcode_capture);

        boolean autoFocus = getIntent().getBooleanExtra(AutoFocus, false);
        boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);
        mQRCode = getIntent().getBooleanExtra(UseQRCode, true);

        mPreview = findViewById(R.id.preview);
        mHint = findViewById(R.id.hint);
        mGraphicCaptureOverlay = findViewById(R.id.graphicOverlay);

        if (mQRCode) {
            mGraphicCaptureOverlay.setVisibility(View.VISIBLE);
            mGraphicCaptureOverlay.isQRCode();
            mHint.setText(getString(R.string.scan_qrcode_hint));
        } else {
            mGraphicCaptureOverlay.setVisibility(View.VISIBLE);
            mHint.setText(getString(R.string.scan_code_hint));
        }

        mButtonClose = findViewById(R.id.close);
        mButtonAutoFocus = findViewById(R.id.auto_focus);
        mButtonFlash = findViewById(R.id.flash);

        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(autoFocus, useFlash);
        } else {
            requestCameraPermission();
        }

        gestureDetector = new GestureDetector(this, new CaptureGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        Snackbar.make(mGraphicCaptureOverlay, "Tap to capture. Pinch/Stretch to zoom", Snackbar.LENGTH_LONG).show();

        mButtonClose.setOnClickListener(this);
        mButtonAutoFocus.setOnClickListener(this);
        mButtonFlash.setOnClickListener(this);
    }

    private void toggleFocusButton() {
        mCameraSource.setFocusMode(mAutoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
        ((TextView)mButtonAutoFocus).setTextColor(mAutoFocus ? Color.WHITE : Color.GRAY);
    }

    private void toggleFlashButton() {
        mCameraSource.setFlashMode(mFlash ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
        ((TextView)mButtonFlash).setTextColor(mFlash ? Color.WHITE : Color.GRAY);
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.close) {
            finish();
        } else if (i == R.id.auto_focus) {
            if (mCameraSource != null && mButtonAutoFocus != null) {
                mAutoFocus = !mAutoFocus;
                toggleFocusButton();
            }
        } else if (i == R.id.flash) {
            if (mCameraSource != null && mButtonFlash != null) {
                mFlash = !mFlash;
                toggleFlashButton();
            }
        }
    }

    private void requestCameraPermission() {
        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions, RC_HANDLE_CAMERA_PERM);
            }
        };

        findViewById(R.id.topLayout).setOnClickListener(listener);
        Snackbar.make(mGraphicCaptureOverlay, R.string.permission_camera_rationale, Snackbar.LENGTH_SHORT)
                .setAction(R.string.ok, listener)
                .show();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean b = scaleGestureDetector.onTouchEvent(e);
        boolean c = gestureDetector.onTouchEvent(e);
        return b || c || super.onTouchEvent(e);
    }

    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getApplicationContext();
        BarcodeDetector.Builder barcodeBuilder = new BarcodeDetector.Builder(context);
        barcodeBuilder.setBarcodeFormats(Barcode.QR_CODE);

        BarcodeDetector barcodeDetector = barcodeBuilder.build();
        TrackerFactory barcodeFactory = new TrackerFactory(mGraphicCaptureOverlay) {
            @Override
            public Tracker<Barcode> create(Barcode barcode) {
                if (barcode != null) {
                    barcodeResult(barcode);
                }
                return super.create(barcode);
            }
        };

        SingleDetector singleDetector = new SingleDetector(barcodeDetector);
        GraphicTracker tracker = new GraphicTracker(mGraphicCaptureOverlay, new BarcodeGraphic(mGraphicCaptureOverlay));
        TrackerFocusFactory centerProcessor = new TrackerFocusFactory(singleDetector, tracker, barcodeFactory);
        singleDetector.setProcessor(centerProcessor);

        if (!barcodeDetector.isOperational()) {
            IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowStorageFilter) != null;
            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
            }
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        CameraSource.Builder builder = new CameraSource.Builder(getApplicationContext(), singleDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(2048, 4048)
                .setRequestedFps(15.0f);

        builder = builder
                .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                .setFocusMode(mAutoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);

        mCameraSource = builder.build();

        toggleFocusButton();
        toggleFlashButton();
    }

    private void barcodeResult(Barcode barcode) {
        Vibrator vb = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vb != null) {
            vb.vibrate(200);
        }

        Intent intent = new Intent(this, BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeObject, barcode);
        setResult(CommonStatusCodes.SUCCESS, intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mButtonClose != null) {
            mButtonClose.setOnClickListener(null);
        }
        if (mButtonAutoFocus != null) {
            mButtonAutoFocus.setOnClickListener(null);
        }
        if (mButtonFlash != null) {
            mButtonFlash.setOnClickListener(null);
        }
        if (mPreview != null) {
            mPreview.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(mAutoFocus, mFlash);
            return;
        }

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name))
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    private void startCameraSource() throws SecurityException {
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicCaptureOverlay);
            } catch (IOException e) {
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    private boolean onTap(float rawX, float rawY) {
        int[] location = new int[2];
        mGraphicCaptureOverlay.getLocationOnScreen(location);
        float x = (rawX - location[0]) / mGraphicCaptureOverlay.getWidthScaleFactor();
        float y = (rawY - location[1]) / mGraphicCaptureOverlay.getHeightScaleFactor();

        Barcode best = null;
        float bestDistance = Float.MAX_VALUE;
        for (BarcodeGraphic graphic : mGraphicCaptureOverlay.getGraphics()) {
            Barcode barcode = graphic.getBarcode();
            if (barcode.getBoundingBox().contains((int) x, (int) y)) {
                best = barcode;
                break;
            }
            float dx = x - barcode.getBoundingBox().centerX();
            float dy = y - barcode.getBoundingBox().centerY();
            float distance = (dx * dx) + (dy * dy);
            if (distance < bestDistance) {
                best = barcode;
                bestDistance = distance;
            }
        }

        if (best != null) {
            barcodeResult(best);
            return true;
        }
        return false;
    }

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return onTap(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
        }
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mCameraSource.doZoom(detector.getScaleFactor());
        }
    }
}

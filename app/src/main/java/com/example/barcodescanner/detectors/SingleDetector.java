package com.example.barcodescanner.detectors;

import android.util.SparseArray;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;

public class SingleDetector extends Detector<Barcode> {
    private Detector<Barcode> mDelegate;

    public SingleDetector(Detector<Barcode> delegate) {
        mDelegate = delegate;
    }

    public SparseArray<Barcode> detect(Frame frame) {
        return mDelegate.detect(frame);
    }

    public boolean isOperational() {
        return mDelegate.isOperational();
    }

    public boolean setFocus(int id) {
        return mDelegate.setFocus(id);
    }
}

package com.example.barcodescanner.trackers;

import android.util.SparseArray;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.FocusingProcessor;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

public class TrackerFocusFactory extends FocusingProcessor<Barcode> {
    private TrackerFactory barcodeFactory;

    public TrackerFocusFactory(Detector<Barcode> detector, Tracker<Barcode> tracker, TrackerFactory barcodeFactory) {
        super(detector, tracker);
        this.barcodeFactory = barcodeFactory;
    }

    @Override
    public int selectFocus(Detector.Detections<Barcode> detections) {
        SparseArray<Barcode> barcodes = detections.getDetectedItems();
        Frame.Metadata meta = detections.getFrameMetadata();
        double nearestDistance = Double.MAX_VALUE;
        int id = -1;

        for (int i = 0; i < barcodes.size(); ++i) {
            int tempId = barcodes.keyAt(i);
            Barcode barcode = barcodes.get(tempId);
            barcodeFactory.create(barcode);
            float dx = Math.abs((meta.getWidth() / 4) - barcode.getBoundingBox().centerX());
            float dy = Math.abs((meta.getHeight() / 4) - barcode.getBoundingBox().centerY());

            double distanceFromCenter =  Math.sqrt((dx * dx) + (dy * dy));

            if (distanceFromCenter < nearestDistance) {
                id = tempId;
                nearestDistance = distanceFromCenter;
            }
        }
        
        return -1;
    }
}


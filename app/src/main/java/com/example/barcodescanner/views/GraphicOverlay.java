package com.example.barcodescanner.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;
import com.example.barcodescanner.R;
import com.example.barcodescanner.utils.AndroidUtilities;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class GraphicOverlay<T extends GraphicOverlay.Graphic> extends View {
    protected final Object mLock = new Object();
    protected int mPreviewWidth;
    protected float mWidthScaleFactor = 1.0f;
    protected int mPreviewHeight;
    protected float mHeightScaleFactor = 1.0f;
    protected boolean mIsQRCode = false;
    protected int mFacing = CameraSource.CAMERA_FACING_BACK;
    protected Set<T> mGraphics = new HashSet<>();

    protected Paint mTransparentPaint;
    protected Paint mGradientPaint;
    protected Paint mBitmapPaint;
    protected Paint mSemiBlackPaint;
    protected Path mPath = new Path();
    protected Paint mPaint = new Paint();
    protected Typeface mTypeface;

    Drawable mQRcode;
    Bitmap mQRcodeBitmap;
    Drawable mBarcode;
    Bitmap mBarcodeBitmap;

    public static abstract class Graphic {
        GraphicOverlay mOverlay;

        public Graphic(GraphicOverlay overlay) {
            mOverlay = overlay;
        }

        public abstract void draw(Canvas canvas);

        public float scaleX(float horizontal) {
            return horizontal * mOverlay.mWidthScaleFactor;
        }

        public float scaleY(float vertical) {
            return vertical * mOverlay.mHeightScaleFactor;
        }

        protected float translateX(float x) {
            if (mOverlay.mFacing == CameraSource.CAMERA_FACING_FRONT) {
                return mOverlay.getWidth() - scaleX(x);
            } else {
                return scaleX(x);
            }
        }

        protected float translateY(float y) {
            return scaleY(y);
        }

        protected void postInvalidate() {
            mOverlay.postInvalidate();
        }
    }

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.Barcode, 0, 0);
        try {
            mIsQRCode = a.getBoolean(R.styleable.Barcode_qrcode, false);
        } finally {
            a.recycle();
        }
    }

    public void isQRCode() {
        mIsQRCode = true;
    }

    public void clear() {
        synchronized (mLock) {
            mGraphics.clear();
        }
        postInvalidate();
    }

    public void add(T graphic) {
        synchronized (mLock) {
            mGraphics.add(graphic);
        }
        postInvalidate();
    }

    public void remove(T graphic) {
        synchronized (mLock) {
            mGraphics.remove(graphic);
        }
        postInvalidate();
    }

    public List<T> getGraphics() {
        synchronized (mLock) {
            return new Vector(mGraphics);
        }
    }

    public float getWidthScaleFactor() {
        return mWidthScaleFactor;
    }

    public float getHeightScaleFactor() {
        return mHeightScaleFactor;
    }

    public void setCameraInfo(int previewWidth, int previewHeight, int facing) {
        synchronized (mLock) {
            mPreviewWidth = previewWidth;
            mPreviewHeight = previewHeight;
            mFacing = facing;
        }
        postInvalidate();
    }

    protected void initPaints() {
        mTransparentPaint = new Paint();
        mTransparentPaint.setColor(Color.TRANSPARENT);
        mTransparentPaint.setStrokeWidth(10);

        mSemiBlackPaint = new Paint();
        mSemiBlackPaint.setColor(Color.TRANSPARENT);
        mSemiBlackPaint.setStrokeWidth(10);

        mBitmapPaint = new Paint();

        mTypeface = Typeface.createFromAsset(getContext().getAssets(),"fonts/FontAwesome.ttf");

        mGradientPaint = new Paint();

        mQRcode = ContextCompat.getDrawable(getContext(), R.drawable.qrcode);
        mQRcodeBitmap = ((BitmapDrawable)mQRcode).getBitmap();

        mBarcode = ContextCompat.getDrawable(getContext(), R.drawable.barcode);
        mBarcodeBitmap = ((BitmapDrawable)mBarcode).getBitmap();

        start();
    }

    protected void drawFocusFrame(Canvas canvas) {
        AndroidUtilities androidUtilities = new AndroidUtilities();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        int topMargin = androidUtilities.dpToPx(90, getContext());
        int leftMargin = androidUtilities.dpToPx(30, getContext());
        int rightMargin = (int)Math.ceil(getRight() - leftMargin);
        int bottomMargin = (int)Math.ceil(getBottom() - androidUtilities.dpToPx(300, getContext()));

        mPath.reset();
        mPath.addRect(leftMargin, topMargin, rightMargin, bottomMargin, Path.Direction.CW);
        mPath.setFillType(Path.FillType.INVERSE_EVEN_ODD);

        canvas.drawRect(leftMargin, topMargin, rightMargin, bottomMargin, mTransparentPaint);
        canvas.save();

        mSemiBlackPaint.setLinearText(true);

        try {
            canvas.drawPath(mPath, mSemiBlackPaint);
            canvas.clipPath(mPath);
            canvas.drawColor(Color.parseColor("#A6000000"));
        } catch (UnsupportedOperationException e) {
            e.fillInStackTrace();
        }

        /*
         * Border
         * */

        int strokeWidth = 2;
        mPaint.setColor(Color.parseColor("#CCffffff"));
        mPaint.setStrokeWidth(strokeWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setLinearText(true);
        canvas.drawRect(leftMargin, topMargin, rightMargin, bottomMargin, mPaint);

        /*
         * Bitmap
         * */

        canvas.restore();

        ColorFilter filter = new PorterDuffColorFilter(ContextCompat.getColor(getContext(), R.color.background_black), PorterDuff.Mode.SRC_IN);
        mBitmapPaint.setColorFilter(filter);

        int imgWidth = getWidth() / 2 - 30;
        int imgHeight = mBarcodeBitmap.getHeight() - 100;

        int scanHeight = bottomMargin - topMargin;

        int leftBitmap = (canvas.getWidth() - imgWidth) / 2;
        int rightBitmap = leftBitmap + imgWidth;
        int topBitmap = topMargin + ((scanHeight - imgHeight) / 2);
        int bottomBitmap = topBitmap + (imgHeight);

        Rect src = new Rect(0, 0, mBarcodeBitmap.getWidth(), mBarcodeBitmap.getHeight());
        Rect dest = new Rect(leftBitmap, topBitmap, rightBitmap, bottomBitmap);

        canvas.drawBitmap(mBarcodeBitmap, src, dest, mBitmapPaint);

        /*
        * Bitmap gradient
        * */
        Shader shaderA = new LinearGradient(
                leftBitmap, 0, rightBitmap, 0,
                new int[] { 0x4Dffffff, 0xffffffff, 0x4Dffffff },
                new float[] { 0.0f + animatedValue, 0.1f + animatedValue, 0.2f + animatedValue },
                Shader.TileMode.REPEAT);
        mGradientPaint.setShader(shaderA);
        mGradientPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawRect(dest, mGradientPaint);
    }

    float animatedValue;
    void start() {
        ValueAnimator animation = ValueAnimator.ofFloat(-0.3f, 1.2f);
        animation.setInterpolator(new FastOutSlowInInterpolator());
        animation.setDuration(1200);
        animation.setRepeatCount(ValueAnimator.INFINITE);
        animation.setRepeatMode(ValueAnimator.REVERSE);
        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator updatedAnimation) {
                animatedValue = (float)updatedAnimation.getAnimatedValue();
                invalidate();
            }
        });
        animation.start();
    }

    protected void drawFocusFrameQRCode(Canvas canvas) {
        AndroidUtilities androidUtilities = new AndroidUtilities();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        int topMargin = androidUtilities.dpToPx(60, getContext());
        int leftMargin = androidUtilities.dpToPx(50, getContext());
        int rightMargin = (int)Math.ceil(getRight() - leftMargin);
        int bottomMargin = (int)Math.ceil(getBottom() - androidUtilities.dpToPx(280, getContext()));

        mPath.reset();
        mPath.addRect(leftMargin, topMargin, rightMargin, bottomMargin, Path.Direction.CW);
        mPath.setFillType(Path.FillType.INVERSE_EVEN_ODD);

        canvas.drawRect(leftMargin, topMargin, rightMargin, bottomMargin, mTransparentPaint);

        canvas.save();
        mSemiBlackPaint.setLinearText(true);

        try {
            canvas.drawPath(mPath, mSemiBlackPaint);
            canvas.clipPath(mPath);
            canvas.drawColor(Color.parseColor("#A6000000"));
        } catch (UnsupportedOperationException e) {
            e.fillInStackTrace();
        }

        /*
         * Border
         * */

        int strokeWidth = 2;
        mPaint.setColor(Color.parseColor("#CCffffff"));
        mPaint.setStrokeWidth(strokeWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setLinearText(true);
        canvas.drawRect(leftMargin, topMargin, rightMargin, bottomMargin, mPaint);

        /*
         * Bitmap
         * */

        canvas.restore();
        mBitmapPaint.setAlpha(200);

        int imgWidth = getWidth() / 2 - 60;
        int imgHeight = mQRcodeBitmap.getHeight() / 2 - 60;

        int scanHeight = bottomMargin - topMargin;

        int leftBitmap = (canvas.getWidth() - imgWidth) / 2;
        int rightBitmap = leftBitmap + imgWidth;
        int topBitmap = topMargin + ((scanHeight - imgHeight) / 2);
        int bottomBitmap = topBitmap + (imgHeight);

        Rect src = new Rect(0, 0, mQRcodeBitmap.getWidth(), mQRcodeBitmap.getHeight());
        Rect dest = new Rect(leftBitmap, topBitmap, rightBitmap, bottomBitmap);
        canvas.drawBitmap(mQRcodeBitmap, src, dest, mBitmapPaint);

        /*
        * Bitmap gradient
        * */
        Shader shaderA = new LinearGradient(
                leftBitmap, 0, rightBitmap, 0,
                new int[] { 0x4Dffffff, 0xffffffff, 0x4Dffffff },
                new float[] { 0.0f + animatedValue, 0.1f + animatedValue, 0.2f + animatedValue },
                Shader.TileMode.REPEAT);

        mGradientPaint.setShader(shaderA);
        mGradientPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawRect(dest, mGradientPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mIsQRCode) {
            drawFocusFrameQRCode(canvas);
        } else {
            drawFocusFrame(canvas);
        }

        synchronized (mLock) {
            if ((mPreviewWidth != 0) && (mPreviewHeight != 0)) {
                mWidthScaleFactor = (float) canvas.getWidth() / (float) mPreviewWidth;
                mHeightScaleFactor = (float) canvas.getHeight() / (float) mPreviewHeight;
            }
        }
    }
}

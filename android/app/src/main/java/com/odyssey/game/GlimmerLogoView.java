package com.odyssey.game;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Cinematic Glimmer Logo View for Code Odyssey Splash Screen.
 * Renders the 3D title logo with a brilliant diamond shimmer light sweep (inspired by CornCastle Anime App)
 * and harmonic twinkling 4-point star sparkles with cyan & purple specular highlights.
 */
public class GlimmerLogoView extends View {

    private Bitmap logoBitmap;
    private final Rect srcRect = new Rect();
    private final RectF dstRect = new RectF();

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint shimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sparkleCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sparkleGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path sparklePath = new Path();
    private final Matrix shimmerMatrix = new Matrix();

    private LinearGradient shimmerGradient;
    private final PorterDuffXfermode srcAtopXfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP);

    private ValueAnimator shimmerAnimator;
    private float shimmerFraction = 0f;
    private long startTime;

    // Sparkle focal points on the 3D Code Odyssey logo (relative coordinates [0..1])
    private static final float[][] SPARKLE_POINTS = {
            {0.732f, 0.165f, 20f, 0.0f},   // Top-right 4-point star on cosmic orbit
            {0.852f, 0.245f, 17f, 0.35f},  // Glowing planet sphere
            {0.435f, 0.490f, 15f, 0.70f},  // Code brackets </> orb
            {0.120f, 0.690f, 14f, 0.50f},  // Left orbit outer curve
            {0.280f, 0.380f, 14f, 0.85f},  // Letter C upper bevel
            {0.640f, 0.880f, 15f, 0.20f}   // ODYSSEY lower curve
    };

    public GlimmerLogoView(Context context) {
        super(context);
        init();
    }

    public GlimmerLogoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GlimmerLogoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        startTime = SystemClock.uptimeMillis();

        logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.title_code_odyssey);
        if (logoBitmap != null) {
            srcRect.set(0, 0, logoBitmap.getWidth(), logoBitmap.getHeight());
        }

        shimmerPaint.setXfermode(srcAtopXfermode);

        sparkleCorePaint.setColor(Color.WHITE);
        sparkleCorePaint.setStyle(Paint.Style.FILL);

        sparkleGlowPaint.setStyle(Paint.Style.FILL);

        setupAnimator();
    }

    private void setupAnimator() {
        shimmerAnimator = ValueAnimator.ofFloat(0f, 1f);
        shimmerAnimator.setDuration(1900);
        shimmerAnimator.setInterpolator(new LinearInterpolator());
        shimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
        shimmerAnimator.addUpdateListener(animation -> {
            shimmerFraction = (float) animation.getAnimatedValue();
            postInvalidateOnAnimation();
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (shimmerAnimator != null && !shimmerAnimator.isStarted()) {
            shimmerAnimator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (shimmerAnimator != null) {
            shimmerAnimator.cancel();
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (shimmerAnimator == null) return;
        if (visibility == VISIBLE && isAttachedToWindow()) {
            if (!shimmerAnimator.isStarted()) shimmerAnimator.start();
        } else {
            if (shimmerAnimator.isStarted()) shimmerAnimator.pause();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0 || logoBitmap == null) return;

        float bmWidth = logoBitmap.getWidth();
        float bmHeight = logoBitmap.getHeight();
        float bmAspect = bmWidth / bmHeight;

        float pad = 4f * getResources().getDisplayMetrics().density;
        float availW = Math.max(10f, w - pad * 2f);
        float availH = Math.max(10f, h - pad * 2f);
        float viewAspect = availW / availH;

        float dstW, dstH;
        if (viewAspect > bmAspect) {
            dstH = availH;
            dstW = dstH * bmAspect;
        } else {
            dstW = availW;
            dstH = dstW / bmAspect;
        }

        float left = (w - dstW) / 2f;
        float top = (h - dstH) / 2f;
        dstRect.set(left, top, left + dstW, top + dstH);

        // Precompute shimmer gradient across 42% of width
        float bandWidth = dstW * 0.42f;
        int[] shimmerColors = new int[]{
                0x00FFFFFF,
                0x305EEAD4,  // Neon Cyan Tint
                0xD0FFFFFF,  // Diamond Sheen
                0xFFFFFFFF,  // Brilliant Core White
                0xD0FFFFFF,  // Diamond Sheen
                0x30C084FC,  // Neon Purple Tint
                0x00FFFFFF
        };
        float[] positions = new float[]{0.0f, 0.22f, 0.44f, 0.50f, 0.56f, 0.78f, 1.0f};

        // Linear gradient tilted diagonally at ~25 degrees
        shimmerGradient = new LinearGradient(
                0, 0,
                bandWidth, bandWidth * 0.45f,
                shimmerColors,
                positions,
                Shader.TileMode.CLAMP
        );
        shimmerPaint.setShader(shimmerGradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dstRect.isEmpty()) return;

        long now = SystemClock.uptimeMillis();
        float elapsedSec = (now - startTime) / 1000f;

        // 1. Draw shimmer light sweep over logo silhouette
        if (logoBitmap != null && shimmerGradient != null) {
            float sweepProgress = Math.min(1.0f, shimmerFraction / 0.75f);
            float bandWidth = dstRect.width() * 0.42f;
            float totalTravel = dstRect.width() + bandWidth * 2f;
            float currentX = dstRect.left - bandWidth + (totalTravel * sweepProgress);
            float currentY = dstRect.top;

            shimmerMatrix.reset();
            shimmerMatrix.setTranslate(currentX, currentY);
            shimmerGradient.setLocalMatrix(shimmerMatrix);

            int saveCount = canvas.saveLayer(null, null);
            // Draw base logo into layer
            canvas.drawBitmap(logoBitmap, srcRect, dstRect, bitmapPaint);
            // Composite shimmer beam strictly onto logo pixels
            canvas.drawRect(dstRect, shimmerPaint);
            canvas.restoreToCount(saveCount);
        }

        // 2. Draw dynamic 4-point sparkle glimmers at logo highlights
        drawSparkles(canvas, elapsedSec);
    }

    private void drawSparkles(Canvas canvas, float elapsedSec) {
        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < SPARKLE_POINTS.length; i++) {
            float[] pt = SPARKLE_POINTS[i];
            float cx = dstRect.left + dstRect.width() * pt[0];
            float cy = dstRect.top + dstRect.height() * pt[1];
            float maxR = pt[2] * density;
            float phaseOffset = pt[3];

            // Harmonic twinkle animation: cycle period ~ 1.8s
            float phase = (elapsedSec * 3.5f + phaseOffset * 6.283f);
            float intensity = (float) Math.sin(phase);
            if (intensity <= 0.15f) continue; // Sparkle is dark in this phase

            float r = maxR * (float) Math.pow(intensity, 2.5);
            int alpha = (int) (255 * Math.min(1.0f, intensity * 1.3f));

            // Soft cyan/purple glow aura
            sparkleGlowPaint.setColor(i % 2 == 0 ? 0x805EEAD4 : 0x80C084FC);
            sparkleGlowPaint.setAlpha(alpha / 2);
            canvas.drawCircle(cx, cy, r * 0.8f, sparkleGlowPaint);

            // 4-point Diamond Star Rays
            sparklePath.reset();
            float rayThin = r * 0.18f;

            // Vertical ray
            sparklePath.moveTo(cx, cy - r);
            sparklePath.lineTo(cx + rayThin, cy);
            sparklePath.lineTo(cx, cy + r);
            sparklePath.lineTo(cx - rayThin, cy);
            sparklePath.close();

            // Horizontal ray
            sparklePath.moveTo(cx - r, cy);
            sparklePath.lineTo(cx, cy + rayThin);
            sparklePath.lineTo(cx + r, cy);
            sparklePath.lineTo(cx - rayThin, cy);
            sparklePath.close();

            // 45-degree smaller secondary diagonal rays
            float diagR = r * 0.45f;
            float diagThin = rayThin * 0.7f;
            sparklePath.moveTo(cx - diagR, cy - diagR);
            sparklePath.lineTo(cx + diagThin, cy - diagThin);
            sparklePath.lineTo(cx + diagR, cy + diagR);
            sparklePath.lineTo(cx - diagThin, cy + diagThin);
            sparklePath.close();

            sparklePath.moveTo(cx + diagR, cy - diagR);
            sparklePath.lineTo(cx + diagThin, cy + diagThin);
            sparklePath.lineTo(cx - diagR, cy + diagR);
            sparklePath.lineTo(cx - diagThin, cy - diagThin);
            sparklePath.close();

            sparkleCorePaint.setAlpha(alpha);
            canvas.drawPath(sparklePath, sparkleCorePaint);

            // Bright white central dot
            canvas.drawCircle(cx, cy, r * 0.22f, sparkleCorePaint);
        }
    }
}

package com.odyssey.game;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * "GOOD LUCK, STUDENTS!" launch cutscene for Code Odyssey.
 *
 * Flow: iris reveal from the START QUIZ button -> hyperspace warp -> 3, 2, 1 countdown ->
 * impact (flash, shockwaves, speed lines, confetti) -> GOOD LUCK, STUDENTS! slams in ->
 * 5 collectible lucky stars -> glowing BEGIN QUIZ button.
 *
 * Interactive: tap anywhere for sparkles, drag to leave a trail, tap the floating lucky stars to
 * collect them (5 = MAX LUCK), SKIP at any time, back button skips too, BEGIN QUIZ starts the quiz.
 * If nobody taps, it continues by itself after about 14 seconds.
 *
 * Usage (in the START QUIZ click listener):
 *   QuizCutsceneView.play(this, startButton, "May your answers be sharp and your streak be long.",
 *       luckyStars -> startQuizNow(luckyStars));     // luckyStars = 0..5 collected
 */
public class QuizCutsceneView extends View {

    public interface Listener { void onFinished(int luckyStars); }

    // timeline (seconds)
    private static final float T_IRIS = 0.55f, T_C3 = 0.9f, T_C2 = 1.7f, T_C1 = 2.5f, T_IMPACT = 3.3f,
            T_TITLE = 3.45f, T_SUB = 4.5f, T_BEGIN = 5.0f, T_AUTO = 14f;
    private static final int INK = 0xFF150F8F;
    private static final int NS = 150, NC = 64, NL = 5, NSL = 70;
    private static final int[] CONF = {0xFF22D3EE, 0xFFA855F7, 0xFFD946EF, 0xFFFDE047, 0xFF34D399, 0xFF3B82F6};

    private final float dp, sd;
    private final Random rnd = new Random();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint subP = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF(), beginRect = new RectF(), skipRect = new RectF();
    private final Path path = new Path();

    private Typeface tf = Typeface.create("sans-serif-black", Typeface.NORMAL);
    private Listener listener;
    private String subtitle = "May your answers be sharp and your streak be long.";
    private static final String L1 = "GOOD LUCK,", L2 = "STUDENTS!", CAPTION = "PREPARING YOUR ODYSSEY...";

    // geometry
    private float W, H, cx, cy, ox = -1f, oy = -1f, topInset, bottomInset;
    private float size1, size2, y1, y2, slotY;
    private float[] lx = new float[L2.length()];
    private StaticLayout subL;
    private Shader titleSh, beginSh;

    // time / state
    private float t, outT, clock, shake, flash, flare, sl, zoom, warp, beginPress, bannerT;
    private boolean finishing, done, confettiOn, touching, skipDown, beginDown;
    private final boolean[] fired = new boolean[5];
    private final float[] tickT = {-9f, -9f, -9f, -9f};
    private long lastNs;
    private float trailT;

    // world
    private final float[] sx = new float[NS], sy = new float[NS], sz = new float[NS];
    private final float[] cfx = new float[NC], cfy = new float[NC], cfv = new float[NC], cfr = new float[NC], cfw = new float[NC], cfs = new float[NC];
    private final int[] cfc = new int[NC];
    private final float[] slA = new float[NSL], slL = new float[NSL], slR = new float[NSL];
    private final float[] lsx = new float[NL], lsy = new float[NL], lsp = new float[NL], lsPop = new float[NL];
    private final boolean[] lsGot = new boolean[NL];
    private int stars = 0;
    private final ArrayList<float[]> parts = new ArrayList<>(); // x,y,vx,vy,life,color,size
    private final ArrayList<float[]> rings = new ArrayList<>(); // x,y,r,a
    private final ArrayList<float[]> texts = new ArrayList<>(); // x,y,life

    public QuizCutsceneView(Context c) { this(c, null); }

    public QuizCutsceneView(Context c, AttributeSet a) {
        super(c, a);
        dp = c.getResources().getDisplayMetrics().density;
        sd = c.getResources().getDisplayMetrics().scaledDensity;
        setClickable(true);
        setFocusableInTouchMode(true);
        tp.setStrokeJoin(Paint.Join.ROUND);
        subP.setTextSize(15 * sd);
        subP.setColor(0xFFC4B5FD);
        for (int i = 0; i < NS; i++) respawnStar(i, true);
        for (int i = 0; i < NSL; i++) { slA[i] = rnd.nextFloat() * 6.283f; slL[i] = 200 + rnd.nextFloat() * 520; slR[i] = 50 + rnd.nextFloat() * 120; }
        for (int i = 0; i < NC; i++) respawnConfetti(i, true);
    }

    // ------------------------------------------------------------ public API

    /** Adds the cutscene on top of the activity. origin = the START QUIZ button (the reveal starts there). */
    public static QuizCutsceneView play(Activity act, View origin, String subtitle, Listener l) {
        ViewGroup root = (ViewGroup) act.getWindow().getDecorView();
        QuizCutsceneView v = new QuizCutsceneView(act);
        if (origin != null) {
            int[] loc = new int[2];
            origin.getLocationInWindow(loc);
            v.ox = loc[0] + origin.getWidth() / 2f;
            v.oy = loc[1] + origin.getHeight() / 2f;
        }
        if (subtitle != null && !subtitle.isEmpty()) v.subtitle = subtitle;
        v.listener = l;
        root.addView(v, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return v;
    }

    public void setNeonTypeface(Typeface t) { if (t != null) { tf = t; build(); } }

    // ------------------------------------------------------------ setup

    private void respawnStar(int i, boolean anyZ) {
        sx[i] = rnd.nextFloat() * 2 - 1; sy[i] = rnd.nextFloat() * 2 - 1;
        sz[i] = anyZ ? 0.05f + rnd.nextFloat() * 0.95f : 1f;
    }

    private void respawnConfetti(int i, boolean initial) {
        cfx[i] = rnd.nextFloat() * Math.max(W, 400);
        cfy[i] = initial ? -rnd.nextFloat() * 900 : -20 * dp;
        cfv[i] = (110 + rnd.nextFloat() * 160) * dp;
        cfr[i] = rnd.nextFloat() * 6.283f; cfw[i] = (rnd.nextFloat() - 0.5f) * 8f;
        cfs[i] = (4 + rnd.nextFloat() * 5) * dp; cfc[i] = CONF[rnd.nextInt(CONF.length)];
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        lastNs = 0;
        if (Build.VERSION.SDK_INT >= 23) {
            WindowInsets wi = getRootWindowInsets();
            if (wi != null) { topInset = wi.getSystemWindowInsetTop(); bottomInset = wi.getSystemWindowInsetBottom(); }
        }
        requestFocus();
        NeonSound.play(getContext(), NeonSound.Sfx.WHOOSH);
        postInvalidateOnAnimation();
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        W = w; H = h; cx = w / 2f; cy = h / 2f;
        if (ox < 0) { ox = cx; oy = h * 0.82f; }
        for (int i = 0; i < NC; i++) if (cfy[i] < -5000) respawnConfetti(i, true);
        build();
    }

    private void build() {
        if (W <= 0) return;
        tp.setTypeface(tf);
        tp.setLetterSpacing(0.02f);
        tp.setTextSize(100f);
        float w100 = tp.measureText(L2);
        size2 = Math.min(W * 0.86f / w100 * 100f, W * 0.2f);
        tp.setTextSize(size2);
        float total = tp.measureText(L2), x0 = (W - total) / 2f;
        for (int i = 0; i < L2.length(); i++) lx[i] = x0 + tp.measureText(L2.substring(0, i));
        y2 = H * 0.30f + size2;
        size1 = Math.max(16 * sd, Math.min(size2 * 0.34f, W * 0.06f));
        y1 = y2 - size2 * 1.08f;
        titleSh = new LinearGradient(0, y2 - size2 * 0.8f, 0, y2,
                new int[]{0xFFFFFFFF, 0xFFCFFAFE, 0xFFC4B5FD, 0xFFF5D0FE}, new float[]{0f, 0.4f, 0.72f, 1f}, Shader.TileMode.CLAMP);

        subP.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        subL = new StaticLayout(subtitle, subP, (int) (W * 0.8f), Layout.Alignment.ALIGN_CENTER, 1.15f, 0f, false);

        float bw = Math.min(W * 0.74f, 360 * dp), bh = 56 * dp, bcy = H - bottomInset - 56 * dp;
        beginRect.set(cx - bw / 2f, bcy - bh / 2f, cx + bw / 2f, bcy + bh / 2f);
        beginSh = new LinearGradient(0, beginRect.top, 0, beginRect.bottom,
                new int[]{0xFF22D3EE, 0xFF2563EB, 0xFF5B21B6, 0xFFD946EF}, new float[]{0f, 0.3f, 0.68f, 1f}, Shader.TileMode.CLAMP);
        skipRect.set(W - 12 * dp - 84 * dp, topInset + 12 * dp, W - 12 * dp, topInset + 12 * dp + 32 * dp);

        slotY = beginRect.top - 30 * dp;
        float top = y2 + 26 * dp + subL.getHeight() + 40 * dp, bottom = slotY - 44 * dp;
        if (bottom < top + 20 * dp) bottom = top = (top + bottom) / 2f;
        for (int i = 0; i < NL; i++) {
            lsx[i] = W * (0.14f + 0.18f * i) + (rnd.nextFloat() - 0.5f) * W * 0.05f;
            lsy[i] = top + (bottom - top) * rnd.nextFloat();
            lsp[i] = rnd.nextFloat() * 6.283f;
        }
    }

    // ------------------------------------------------------------ effects

    private void burst(float x, float y, int n, float speed, int color) {
        for (int i = 0; i < n && parts.size() < 420; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            float sp = (80f + rnd.nextFloat() * 380f) * dp * speed, life = 0.6f + rnd.nextFloat() * 0.9f;
            parts.add(new float[]{x, y, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp, life, rnd.nextBoolean() ? 0xFFFFFF : (color & 0xFFFFFF), (1.2f + rnd.nextFloat() * 2.6f) * dp});
        }
        rings.add(new float[]{x, y, 0f, 1f});
    }

    private void tick(int n) {
        tickT[n] = t;
        rings.add(new float[]{cx, cy, 0f, 1f});
        shake = Math.max(shake, 7 * dp);
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        NeonSound.play(NeonSound.Sfx.TICK);
    }

    private void impact() {
        flash = 1f; shake = 26 * dp; zoom = 0.14f; flare = 1f; sl = 1f;
        burst(cx, cy, 130, 1.8f, 0xFDE047);
        rings.add(new float[]{cx, cy, -60 * dp, 1f});
        rings.add(new float[]{cx, cy, -130 * dp, 1f});
        confettiOn = true;
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        NeonSound.play(NeonSound.Sfx.IMPACT);
    }

    private void collect(int i) {
        lsGot[i] = true; stars++; lsPop[i] = 1f;
        burst(lsx[i], lsy[i], 28, 1.1f, 0xFDE047);
        texts.add(new float[]{lsx[i], lsy[i], 1f});
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        NeonSound.play(NeonSound.Sfx.COLLECT);
        if (stars == NL) {
            bannerT = 2.4f; flash = 0.55f; shake = 14 * dp; zoom = 0.06f;
            burst(cx, y2, 90, 1.6f, 0xFDE047);
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            NeonSound.play(NeonSound.Sfx.UNLOCK);
        }
    }

    private void skip() {
        NeonSound.play(NeonSound.Sfx.CLICK);
        if (t < T_IMPACT) { fired[0] = fired[1] = fired[2] = true; t = T_IMPACT; }
        else if (t < T_BEGIN) { t = T_BEGIN; }
    }

    private void finish() {
        if (finishing) return;
        finishing = true; outT = 0f;
        burst(beginRect.centerX(), beginRect.centerY(), 60, 1.4f, 0x67E8F9);
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        NeonSound.play(NeonSound.Sfx.LAUNCH);
    }

    // ------------------------------------------------------------ input

    @Override public boolean onKeyDown(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (t < T_BEGIN) skip(); else finish();
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (finishing) return true;
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (t < T_BEGIN && skipRect.contains(x, y)) { skipDown = true; return true; }
                if (t >= T_BEGIN && beginRect.contains(x, y)) { beginDown = true; beginPress = 1f; return true; }
                boolean hit = false;
                if (t >= T_IMPACT) {
                    for (int i = 0; i < NL; i++) {
                        if (lsGot[i] || t < T_IMPACT + 0.9f + i * 0.18f) continue;
                        float dx = x - lsx[i], dy = y - (lsy[i] + (float) Math.sin(clock * 1.3f + lsp[i]) * 6 * dp);
                        if (dx * dx + dy * dy < (46 * dp) * (46 * dp)) { collect(i); hit = true; break; }
                    }
                }
                if (!hit) { burst(x, y, 16, 0.8f, 0x67E8F9); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); }
                touching = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (touching) {
                    trailT -= 0.016f;
                    if (trailT <= 0f && parts.size() < 420) {
                        trailT = 0.03f;
                        parts.add(new float[]{x, y, (rnd.nextFloat() - 0.5f) * 60 * dp, (rnd.nextFloat() - 0.5f) * 60 * dp, 0.5f, rnd.nextBoolean() ? 0xFFFFFF : 0x67E8F9, (1f + rnd.nextFloat() * 2f) * dp});
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (skipDown && skipRect.contains(x, y)) skip();
                if (beginDown && beginRect.contains(x, y)) finish();
                skipDown = beginDown = touching = false; beginPress = 0f;
                return true;
            case MotionEvent.ACTION_CANCEL:
                skipDown = beginDown = touching = false; beginPress = 0f;
                return true;
        }
        return true;
    }

    // ------------------------------------------------------------ update

    private static float ease(float x) { x = x < 0 ? 0 : (x > 1 ? 1 : x); return x * x * (3 - 2 * x); }

    private static float clamp(float x) { return x < 0 ? 0 : (x > 1 ? 1 : x); }

    private void update(float dt) {
        clock += dt;
        if (!finishing) t += dt; else outT += dt;
        shake *= (float) Math.exp(-dt * 5f); flash *= (float) Math.exp(-dt * 1.8f);
        flare *= (float) Math.exp(-dt * 1.6f); zoom *= (float) Math.exp(-dt * 3f); sl *= (float) Math.exp(-dt * 3.2f);
        if (bannerT > 0) bannerT -= dt;
        beginPress += ((beginDown ? 1f : 0f) - beginPress) * (1f - (float) Math.exp(-dt * 20f));

        if (!finishing) {
            if (!fired[0] && t >= T_C3) { fired[0] = true; tick(3); }
            if (!fired[1] && t >= T_C2) { fired[1] = true; tick(2); }
            if (!fired[2] && t >= T_C1) { fired[2] = true; tick(1); }
            if (!fired[3] && t >= T_IMPACT) { fired[3] = true; impact(); }
            if (t >= T_AUTO) finish();
        }
        warp = finishing ? 1.4f : (t < 0.15f ? 0f : (t < T_IMPACT ? ease((t - 0.15f) / 0.8f) : 0f));

        float sp = 0.03f + warp * 2.4f, scl = Math.max(W, H) * 0.5f;
        for (int i = 0; i < NS; i++) {
            sz[i] -= sp * dt;
            if (sz[i] < 0.03f || Math.abs(sx[i] / sz[i]) > 1.3f * W / scl || Math.abs(sy[i] / sz[i]) > 1.3f * H / scl) respawnStar(i, false);
        }
        if (confettiOn) {
            for (int i = 0; i < NC; i++) {
                cfy[i] += cfv[i] * dt; cfx[i] += (float) Math.sin(clock * 2f + i) * 30 * dp * dt; cfr[i] += cfw[i] * dt;
                if (cfy[i] > H + 20 * dp) respawnConfetti(i, false);
            }
        }
        for (int i = 0; i < NL; i++) if (lsPop[i] > 0) lsPop[i] = Math.max(0f, lsPop[i] - dt * 2.2f);

        Iterator<float[]> it = parts.iterator();
        while (it.hasNext()) {
            float[] q = it.next();
            float d = (float) Math.exp(-dt * 1.6f);
            q[2] *= d; q[3] *= d; q[0] += q[2] * dt; q[1] += q[3] * dt; q[4] -= dt;
            if (q[4] <= 0f) it.remove();
        }
        it = rings.iterator();
        while (it.hasNext()) {
            float[] q = it.next();
            q[2] += 720 * dp * dt;
            if (q[2] > 0) q[3] -= dt * 1.1f;
            if (q[3] <= 0f) it.remove();
        }
        it = texts.iterator();
        while (it.hasNext()) {
            float[] q = it.next();
            q[1] -= 46 * dp * dt; q[2] -= dt * 0.9f;
            if (q[2] <= 0f) it.remove();
        }
        if (finishing && outT >= 0.65f && !done) {
            done = true;
            if (listener != null) listener.onFinished(stars);
            animate().alpha(0f).setDuration(280).withEndAction(() -> {
                if (getParent() instanceof ViewGroup) ((ViewGroup) getParent()).removeView(this);
            }).start();
        }
    }

    // ------------------------------------------------------------ drawing

    @Override protected void onDraw(Canvas c) {
        if (W <= 0) return;
        long now = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : Math.min(0.05f, (now - lastNs) / 1e9f);
        lastNs = now;
        update(dt);

        c.save();
        // iris reveal from the START QUIZ button
        float iris = finishing ? 1f : ease(t / T_IRIS);
        float maxR = (float) Math.hypot(Math.max(ox, W - ox), Math.max(oy, H - oy));
        if (iris < 1f) {
            path.reset(); path.addCircle(ox, oy, Math.max(1f, iris * maxR), Path.Direction.CW);
            c.clipPath(path);
        }
        float zm = 1f + zoom + (finishing ? outT * 0.5f : 0f);
        c.translate((rnd.nextFloat() - 0.5f) * (shake + warp * 1.5f * dp), (rnd.nextFloat() - 0.5f) * (shake + warp * 1.5f * dp));
        c.scale(zm, zm, cx, cy);

        drawBackdrop(c);
        drawStars(c);
        if (confettiOn) drawConfetti(c);
        drawCountdown(c);
        drawImpactFx(c);
        drawTitle(c);
        drawLuckyStars(c);
        drawParticles(c);
        c.restore();

        if (iris < 1f && !finishing) { // glowing edge of the iris
            p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(5 * dp);
            p.setColor(((int) (200 * (1f - iris)) << 24) | 0x67E8F9);
            c.drawCircle(ox, oy, Math.max(1f, iris * maxR), p);
        }
        drawButtons(c);

        if (flash > 0.01f) { p.reset(); p.setColor(((int) (255 * Math.min(1f, flash)) << 24) | 0xFFFFFF); c.drawRect(0, 0, W, H, p); }
        if (finishing) { // outro flash to white
            p.reset(); p.setColor(((int) (255 * ease(outT / 0.45f)) << 24) | 0xFFFFFF); c.drawRect(0, 0, W, H, p);
        }
        postInvalidateOnAnimation();
    }

    private void drawBackdrop(Canvas c) {
        p.reset(); p.setShader(new LinearGradient(0, 0, 0, H, new int[]{0xFF0B0220, 0xFF1A0638, 0xFF07010F}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(-50, -50, W + 50, H + 50, p);
        float vis = ease((t - 0.1f) / 1.2f) * (1f + flash * 0.6f);
        float big = Math.max(W, H) * 0.7f;
        float[][] b = {{0.2f, 0.25f}, {0.85f, 0.5f}, {0.4f, 0.88f}};
        int[] col = {0xC026D3, 0x7C3AED, 0x4F46E5};
        for (int i = 0; i < 3; i++) {
            float bx = b[i][0] * W + (float) Math.sin(clock * 0.3f + i * 2f) * W * 0.06f, by = b[i][1] * H + (float) Math.cos(clock * 0.25f + i) * H * 0.04f;
            int a = (int) (Math.min(1f, 0.38f * vis) * 255);
            p.reset(); p.setAntiAlias(true);
            p.setShader(new RadialGradient(bx, by, big, (a << 24) | col[i], col[i], Shader.TileMode.CLAMP));
            c.drawCircle(bx, by, big, p);
        }
    }

    private void drawStars(Canvas c) {
        float e = ease(t / 1.2f), vx = ox + (cx - ox) * e, vy = oy + (cy - oy) * e;
        float sp = 0.03f + warp * 2.4f, scl = Math.max(W, H) * 0.5f;
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < NS; i++) {
            float z = sz[i], pz = z + sp * 0.05f;
            float x2 = vx + sx[i] / z * scl, y2s = vy + sy[i] / z * scl, x1 = vx + sx[i] / pz * scl, y1s = vy + sy[i] / pz * scl;
            float al = (1f - z * 0.55f) * (0.4f + 0.6f * (0.5f + 0.5f * (float) Math.sin(clock * (1f + i % 3) + i)));
            p.setStrokeWidth(((1f - z) * 1.6f + 0.5f) * dp);
            p.setColor(((int) (255 * al) << 24) | (i % 5 == 0 ? 0xC4B5FD : 0xFFFFFF));
            c.drawLine(x1, y1s, x2, y2s, p);
        }
    }

    private void drawConfetti(Canvas c) {
        p.reset(); p.setAntiAlias(true);
        for (int i = 0; i < NC; i++) {
            c.save(); c.translate(cfx[i], cfy[i]); c.rotate((float) Math.toDegrees(cfr[i]));
            p.setColor((0xE6 << 24) | (cfc[i] & 0xFFFFFF));
            c.drawRect(-cfs[i], -cfs[i] * 0.5f, cfs[i], cfs[i] * 0.5f, p);
            c.restore();
        }
    }

    private void drawCountdown(Canvas c) {
        if (t >= T_IMPACT) return;
        // typed caption
        if (t > 0.25f) {
            int n = Math.min(CAPTION.length(), (int) ((t - 0.25f) * 22));
            float al = t > T_C3 ? 1f - ease((t - T_C3) / 0.4f) : 1f;
            tp.setTypeface(tf); tp.setShader(null); tp.setStyle(Paint.Style.FILL); tp.setTextAlign(Paint.Align.CENTER);
            tp.setLetterSpacing(0.3f); tp.setTextSize(Math.max(11 * sd, W * 0.032f));
            tp.setColor(((int) (255 * al) << 24) | 0x2DD4BF);
            c.drawText(CAPTION.substring(0, n), cx, H * 0.22f, tp);
        }
        float[] starts = {T_C3, T_C2, T_C1};
        int[][] cols = {{0xFFFFFFFF, 0xFF67E8F9}, {0xFFFFFFFF, 0xFFC4B5FD}, {0xFFFFFFFF, 0xFFF9A8D4}};
        int[] glow = {0x22D3EE, 0xA855F7, 0xF472B6};
        for (int k = 0; k < 3; k++) {
            float lt = t - starts[k];
            if (lt < 0 || lt > 0.85f) continue;
            float sc = 1.7f - 0.7f * ease(lt / 0.25f), al = lt < 0.55f ? 1f : 1f - (lt - 0.55f) / 0.3f;
            float size = Math.min(W, H) * 0.34f * sc, base = cy + size * 0.35f;
            tp.setLetterSpacing(0f);
            Shader sh = new LinearGradient(0, base - size * 0.75f, 0, base, cols[k][0], cols[k][1], Shader.TileMode.CLAMP);
            outlined(c, String.valueOf(3 - k), cx, base, size, Paint.Align.CENTER, sh, al, glow[k]);
        }
    }

    private void drawImpactFx(Canvas c) {
        if (flare > 0.02f) {
            float rr = H * 0.55f * flare + 40 * dp;
            p.reset(); p.setAntiAlias(true);
            p.setShader(new RadialGradient(cx, cy, rr, new int[]{((int) (255 * flare) << 24) | 0xFFFFFF, ((int) (128 * flare) << 24) | 0xFDE047, 0x00FDE047}, new float[]{0f, 0.3f, 1f}, Shader.TileMode.CLAMP));
            c.drawCircle(cx, cy, rr, p);
            c.save(); c.translate(cx, cy); c.scale(1f, 0.018f);
            float l = W * 0.9f;
            p.setShader(new RadialGradient(0, 0, l, ((int) (255 * flare) << 24) | 0xFFFFFF, 0x00AAC8FF, Shader.TileMode.CLAMP));
            c.drawCircle(0, 0, l, p);
            c.restore();
        }
        if (sl > 0.03f) {
            p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2 * dp);
            p.setColor(((int) (200 * sl) << 24) | 0xFFFFFF);
            float maxD = Math.max(W, H);
            for (int i = 0; i < NSL; i++) {
                float a0 = slR[i] + (1f - sl) * maxD * 0.9f;
                c.drawLine(cx + (float) Math.cos(slA[i]) * a0, cy + (float) Math.sin(slA[i]) * a0,
                        cx + (float) Math.cos(slA[i]) * (a0 + slL[i] * sl), cy + (float) Math.sin(slA[i]) * (a0 + slL[i] * sl), p);
            }
        }
        for (float[] q : rings) {
            if (q[2] <= 0) continue;
            p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(12 * dp); p.setColor(((int) (90 * Math.max(0f, q[3])) << 24) | 0x67E8F9);
            c.drawCircle(q[0], q[1], q[2], p);
            p.setStrokeWidth(2.5f * dp); p.setColor(((int) (255 * Math.max(0f, q[3])) << 24) | 0xFFFFFF);
            c.drawCircle(q[0], q[1], q[2], p);
        }
    }

    private void drawTitle(Canvas c) {
        if (t < T_TITLE - 0.1f) return;
        float oa = ease((t - T_TITLE) / 0.6f);
        float pulse = 0.5f + 0.5f * (float) Math.sin(clock * 3f);

        // orbit swoosh behind the title
        float ocy = y2 - size2 * 0.38f, rx = W * 0.47f, ry = Math.max(size2 * 0.62f, 26 * dp);
        c.save(); c.rotate(-7f, cx, ocy);
        r.set(cx - rx, ocy - ry, cx + rx, ocy + ry);
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(8 * dp); p.setColor(((int) (255 * oa) << 24) | 0x1D1A9E);
        c.drawOval(r, p);
        p.setStrokeWidth(4.5f * dp);
        p.setShader(new LinearGradient(cx - rx, 0, cx + rx, 0, new int[]{0xFF22D3EE, 0xFF3B82F6, 0xFFA855F7}, null, Shader.TileMode.CLAMP));
        p.setAlpha((int) (255 * oa));
        c.drawOval(r, p);
        double a = clock * 0.9;
        float mx = cx + (float) Math.cos(a) * rx, my = ocy + (float) Math.sin(a) * ry;
        p.reset(); p.setAntiAlias(true); p.setColor(((int) (110 * oa) << 24) | 0x38BDF8);
        c.drawCircle(mx, my, 9 * dp, p);
        p.setColor(((int) (255 * oa) << 24) | 0xFFFFFF);
        c.drawCircle(mx, my, 4 * dp, p);
        c.restore();

        // line 1
        float a1 = ease((t - (T_TITLE - 0.1f)) / 0.4f);
        tp.setLetterSpacing(0.3f);
        outlined(c, L1, cx, y1 + (1f - a1) * -16 * dp, size1, Paint.Align.CENTER,
                new LinearGradient(0, y1 - size1, 0, y1, 0xFFFFFFFF, 0xFFFDE68A, Shader.TileMode.CLAMP), a1, 0xFDE047);

        // line 2: letters slam in one by one
        tp.setLetterSpacing(0.02f);
        for (int i = 0; i < L2.length(); i++) {
            float lt = clamp((t - (T_TITLE + i * 0.06f)) / 0.35f), e = ease(lt);
            if (lt <= 0f) continue;
            String ch = String.valueOf(L2.charAt(i));
            tp.setTextSize(size2);
            float w = tp.measureText(ch), lcx = lx[i] + w / 2f, sc = 1f + 1.2f * (1f - e);
            c.save();
            c.translate(0, (1f - e) * -30 * dp);
            c.scale(sc, sc, lcx, y2 - size2 * 0.35f);
            outlined(c, ch, lx[i], y2, size2, Paint.Align.LEFT, titleSh, Math.min(1f, e * 1.4f), 0x67E8F9);
            c.restore();
        }

        // subtitle
        float sa = ease((t - T_SUB) / 0.6f);
        if (sa > 0.01f && subL != null) {
            subP.setAlpha((int) (255 * sa));
            c.save(); c.translate((W - subL.getWidth()) / 2f, y2 + 26 * dp + (1f - sa) * 14 * dp); subL.draw(c); c.restore();
        }
        // MAX LUCK banner
        if (bannerT > 0) {
            float al = Math.min(1f, bannerT / 0.4f);
            tp.setLetterSpacing(0.12f);
            outlined(c, "MAX LUCK!", cx, y2 + size2 * 0.9f + 26 * dp, size1 * 1.1f * (1f + 0.06f * pulse), Paint.Align.CENTER,
                    new LinearGradient(0, 0, 0, H, 0xFFFFFFFF, 0xFFFDE047, Shader.TileMode.CLAMP), al, 0xFDE047);
        }
    }

    private void starPath(float x, float y, float rad) {
        path.reset();
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(-90 + i * 45);
            float rr = (i % 2 == 0) ? rad : rad * 0.26f;
            float px = x + (float) Math.cos(a) * rr, py = y + (float) Math.sin(a) * rr;
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        path.close();
    }

    private void drawLuckyStars(Canvas c) {
        if (t < T_IMPACT + 0.9f) return;
        for (int i = 0; i < NL; i++) {
            if (lsGot[i] && lsPop[i] <= 0f) continue;
            float app = ease((t - (T_IMPACT + 0.9f + i * 0.18f)) / 0.4f);
            if (app <= 0f) continue;
            float x = lsx[i] + (float) Math.cos(clock * 0.9f + lsp[i]) * 5 * dp, y = lsy[i] + (float) Math.sin(clock * 1.3f + lsp[i]) * 6 * dp;
            float pulse = 0.85f + 0.15f * (float) Math.sin(clock * 4f + lsp[i]);
            float rad = 16 * dp * app * pulse * (lsGot[i] ? 1f + lsPop[i] * 1.2f : 1f);
            float al = lsGot[i] ? lsPop[i] : 1f;
            p.reset(); p.setAntiAlias(true); p.setColor(((int) (100 * al) << 24) | 0xFDE047);
            c.drawCircle(x, y, rad * 1.9f, p);
            starPath(x, y, rad * 1.25f);
            p.setColor(((int) (255 * al) << 24) | 0x150F8F); c.drawPath(path, p);
            starPath(x, y, rad);
            p.setColor(((int) (255 * al) << 24) | 0xFDE047); c.drawPath(path, p);
            starPath(x, y, rad * 0.45f);
            p.setColor(((int) (255 * al) << 24) | 0xFFFFFF); c.drawPath(path, p);
        }
        // floating "+1 LUCK"
        for (float[] q : texts) {
            tp.setLetterSpacing(0.08f);
            outlined(c, "+1 LUCK", q[0], q[1], 15 * sd, Paint.Align.CENTER,
                    new LinearGradient(0, q[1] - 15 * sd, 0, q[1], 0xFFFFFFFF, 0xFFFDE047, Shader.TileMode.CLAMP), Math.max(0f, q[2]), 0xFDE047);
        }
        // slots + prompt
        float a = ease((t - (T_IMPACT + 1.2f)) / 0.5f);
        if (a > 0f) {
            for (int i = 0; i < NL; i++) {
                float x = cx + (i - 2) * 30 * dp, rad = 9 * dp;
                boolean on = i < stars;
                if (on) { p.reset(); p.setAntiAlias(true); p.setColor(0x55FDE047); c.drawCircle(x, slotY, rad * 1.8f, p); }
                starPath(x, slotY, rad);
                p.reset(); p.setAntiAlias(true);
                if (on) { p.setColor(0xFFFDE047); c.drawPath(path, p); }
                else { p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.6f * dp); p.setColor(0xFF4C3F9A); c.drawPath(path, p); }
            }
            String label = stars == 0 ? "TAP THE LUCKY STARS!" : (stars >= NL ? "MAX LUCK!" : "LUCK  " + stars + " / " + NL);
            tp.setTypeface(tf); tp.setShader(null); tp.setStyle(Paint.Style.FILL); tp.setTextAlign(Paint.Align.CENTER);
            tp.setLetterSpacing(0.16f); tp.setTextSize(Math.max(10 * sd, W * 0.03f));
            tp.setColor(stars >= NL ? 0xFFFDE047 : 0xFFC4B5FD);
            c.drawText(label, cx, slotY - 20 * dp, tp);
        }
    }

    private void drawParticles(Canvas c) {
        p.reset(); p.setAntiAlias(true);
        for (float[] q : parts) {
            float al = Math.max(0f, Math.min(1f, q[4]));
            p.setColor(((int) (255 * al) << 24) | ((int) q[5] & 0xFFFFFF));
            c.drawCircle(q[0], q[1], q[6] * (0.4f + 0.6f * al), p);
        }
    }

    private void drawButtons(Canvas c) {
        // skip
        if (!finishing && t < T_BEGIN) {
            float rad = skipRect.height() / 2f;
            p.reset(); p.setAntiAlias(true); p.setColor(0xCC100A2E); c.drawRoundRect(skipRect, rad, rad, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.5f * dp); p.setColor(skipDown ? 0xFF22D3EE : 0xFF7C3AED);
            c.drawRoundRect(skipRect, rad, rad, p);
            tp.setTypeface(tf); tp.setShader(null); tp.setStyle(Paint.Style.FILL); tp.setTextAlign(Paint.Align.CENTER);
            tp.setLetterSpacing(0.12f); tp.setTextSize(11 * sd); tp.setColor(0xFFC4B5FD);
            c.drawText("SKIP >>", skipRect.centerX(), skipRect.centerY() - (tp.ascent() + tp.descent()) / 2f, tp);
        }
        // BEGIN QUIZ
        float e = ease((t - T_BEGIN) / 0.5f);
        if (e <= 0f || beginSh == null) return;
        float bw = beginRect.width(), bh = beginRect.height(), rad = bh * 0.3f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(clock * 3.2f);
        float sc = (1f - 0.06f * beginPress) * (1f + 0.015f * pulse);
        c.save();
        c.translate(0, (1f - e) * 60 * dp + beginPress * 3 * dp);
        c.scale(sc, sc, beginRect.centerX(), beginRect.centerY());

        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.8f * dp);
        for (int i = 1; i <= 6; i++) {
            float ex = 4.5f * dp + i * 1.7f * dp;
            p.setColor(((int) (74 * (1f - i / 7f) * (0.75f + 0.25f * pulse)) << 24) | (i % 2 == 0 ? 0xA855F7 : 0x60A5FA));
            r.set(beginRect); r.inset(-ex, -ex);
            c.drawRoundRect(r, rad + ex, rad + ex, p);
        }
        r.set(beginRect); r.offset(0, bh * 0.07f); r.inset(-4.5f * dp, -4.5f * dp);
        p.reset(); p.setAntiAlias(true); p.setColor(0xFF2A0F8F); c.drawRoundRect(r, rad + 4.5f * dp, rad + 4.5f * dp, p);
        r.set(beginRect); r.inset(-4.5f * dp, -4.5f * dp);
        p.setColor(0xFF67E8F9); c.drawRoundRect(r, rad + 4.5f * dp, rad + 4.5f * dp, p);
        r.set(beginRect); r.inset(-3f * dp, -3f * dp);
        p.setColor(INK); c.drawRoundRect(r, rad + 3f * dp, rad + 3f * dp, p);
        p.reset(); p.setAntiAlias(true); p.setShader(beginSh); c.drawRoundRect(beginRect, rad, rad, p);
        r.set(beginRect.left + bh * 0.2f, beginRect.top + bh * 0.06f, beginRect.right - bh * 0.2f, beginRect.top + bh * 0.5f);
        p.reset(); p.setAntiAlias(true);
        p.setShader(new LinearGradient(0, r.top, 0, r.bottom, 0xBFFFFFFF, 0x14FFFFFF, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, bh * 0.2f, bh * 0.2f, p);

        float st = clamp(((clock % 3.4f) / 3.4f - 0.55f) / 0.45f);
        if (st > 0f && st < 1f) {
            path.reset(); path.addRoundRect(beginRect, rad, rad, Path.Direction.CW);
            c.save(); c.clipPath(path);
            float x = beginRect.left - bh + st * (bw + 2 * bh);
            p.reset(); p.setAntiAlias(true);
            p.setShader(new LinearGradient(x, 0, x + bh, 0, new int[]{0x00FFFFFF, 0x99FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            Path band = new Path(); float sk = bh * 0.36f;
            band.moveTo(x, beginRect.top); band.lineTo(x + bh, beginRect.top); band.lineTo(x + bh - sk, beginRect.bottom); band.lineTo(x - sk, beginRect.bottom); band.close();
            c.drawPath(band, p);
            c.restore();
        }

        float ts = bh * 0.4f;
        tp.setTypeface(tf); tp.setLetterSpacing(0.05f); tp.setTextSize(ts);
        while (tp.measureText("BEGIN QUIZ") > bw * 0.8f && ts > 9 * dp) { ts -= dp * 0.5f; tp.setTextSize(ts); }
        float base = beginRect.centerY() - (tp.ascent() + tp.descent()) / 2f;
        outlined(c, "BEGIN QUIZ", beginRect.centerX(), base, ts, Paint.Align.CENTER,
                new LinearGradient(0, base - ts * 0.8f, 0, base, new int[]{0xFFFFFFFF, 0xFFCFFAFE, 0xFFC4B5FD, 0xFFF5D0FE}, new float[]{0f, 0.4f, 0.72f, 1f}, Shader.TileMode.CLAMP), 1f, 0x67E8F9);
        c.restore();
    }

    /** Logo-style text: neon glow, dark outline, gradient fill. Set tp.setLetterSpacing before calling. */
    private void outlined(Canvas c, String s, float x, float y, float size, Paint.Align al, Shader fill, float alpha, int glowRgb) {
        alpha = Math.max(0f, Math.min(1f, alpha));
        tp.setTypeface(tf); tp.setTextAlign(al); tp.setTextSize(size); tp.setShader(null);
        int a = (int) (255 * alpha);
        tp.setStyle(Paint.Style.STROKE);
        tp.setStrokeWidth(size * 0.44f); tp.setColor(((int) (0x55 * alpha) << 24) | (glowRgb & 0xFFFFFF));
        c.drawText(s, x, y, tp);
        tp.setStrokeWidth(size * 0.24f); tp.setColor((a << 24) | 0x150F8F);
        c.drawText(s, x, y + size * 0.05f, tp);
        c.drawText(s, x, y, tp);
        tp.setStyle(Paint.Style.FILL); tp.setShader(fill); tp.setAlpha(a);
        c.drawText(s, x, y, tp);
        tp.setShader(null);
    }
}

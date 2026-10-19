package com.odyssey.game;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * Neon player card for Code Odyssey: PLAYER name + two glowing orbit rings (WIN and BEST).
 * Numbers roll, rings animate, orbs travel along the ring, and a burst plays when a value goes up.
 *
 * Usage in XML:
 *   <com.odyssey.game.PlayerStatsCard
 *       android:id="@+id/playerCard"
 *       android:layout_width="match_parent"
 *       android:layout_height="wrap_content" />
 *
 * Usage in Java:
 *   card.setPlayerName("biar");
 *   card.setStats(winFromFirebase, bestFromFirebase);   // first load, no fanfare
 *   card.incrementWin();                                 // correct answer: win+1, best auto-raises
 *   card.resetWin();                                     // wrong answer: win=0 (best is kept)
 */
public class PlayerStatsCard extends View {

    private static final int INK = 0xFF150F8F;

    private static class Stat {
        int target, prev, dir = 1;
        float roll = 1f, pop = 1f, shake = 1f;
        ValueAnimator rollA, popA, shakeA;
    }

    private final float dp;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF card = new RectF(), tmp = new RectF(), oval = new RectF();
    private final Path path = new Path();
    private final Matrix mx = new Matrix();
    private final Random rnd = new Random();
    private final ArrayList<float[]> parts = new ArrayList<>(); // x, y, vx, vy, life, color

    private Typeface tf = Typeface.create("sans-serif-black", Typeface.NORMAL);
    private String name = "PLAYER", shownName = "PLAYER", bannerText = null;
    private final Stat win = new Stat(), best = new Stat();
    private float winProg = 0f, idle = 0f, flash = 1f;
    private ValueAnimator idleA, progA, flashA;
    private long lastNs;

    private float gp, cardH, radius, D, ringW, cx1, cx2, cy, nameL, nameR, nameSize;
    private Shader rimSh, glassSh, glossSh, nameSh, sweepWin, sweepBest, innerSh, ringGlossSh, numWinSh, numBestSh;

    private final Runnable bannerOff = () -> { bannerText = null; invalidate(); };

    public PlayerStatsCard(Context c) { this(c, null); }

    public PlayerStatsCard(Context c, AttributeSet a) { this(c, a, 0); }

    public PlayerStatsCard(Context c, AttributeSet a, int s) {
        super(c, a, s);
        dp = c.getResources().getDisplayMetrics().density;
        tp.setStrokeJoin(Paint.Join.ROUND);
        updateDescription();
    }

    // ------------------------------------------------------------ public API

    public void setPlayerName(String n) {
        name = (n == null || n.trim().isEmpty()) ? "PLAYER" : n.trim();
        if (getWidth() > 0) build(getWidth());
        updateDescription();
        invalidate();
    }

    /** Set both numbers at once (use when loading from Firebase). No fanfare. best is never below win. */
    public void setStats(int w, int b) {
        w = Math.max(0, w);
        b = Math.max(Math.max(0, b), w);
        apply(w, b, false);
    }

    /** Animated update. If win goes above best, best is raised automatically with a "NEW BEST!" flash. */
    public void setWin(int w) {
        w = Math.max(0, w);
        int b = best.target;
        boolean newBest = w > b;
        if (newBest) b = w;
        apply(w, b, true);
        if (newBest) showNewBest();
    }

    /** Animated update of the best streak (never lower than the current win). */
    public void setBest(int b) {
        boolean up = b > best.target;
        apply(win.target, Math.max(Math.max(0, b), win.target), true);
        if (up) showNewBest();
    }

    public void incrementWin() { setWin(win.target + 1); }

    public void resetWin() { setWin(0); }

    public int getWin() { return win.target; }

    public int getBest() { return best.target; }

    public void setNeonTypeface(Typeface t) {
        if (t == null) return;
        tf = t;
        if (getWidth() > 0) build(getWidth());
        invalidate();
    }

    // ------------------------------------------------------------ state changes

    private void apply(int w, int b, boolean animate) {
        change(win, w, animate, cx1, 0x22D3EE);
        change(best, b, animate, cx2, 0xFBBF24);
        float tgt = best.target > 0 ? Math.min(1f, (float) win.target / best.target) : 0f;
        if (progA != null) progA.cancel();
        if (animate && isAttachedToWindow()) {
            progA = anim(winProg, tgt, 550, new DecelerateInterpolator(), a -> { winProg = (float) a.getAnimatedValue(); invalidate(); });
        } else {
            winProg = tgt;
        }
        updateDescription();
        invalidate();
    }

    private void change(Stat s, int v, boolean animate, float bx, int tint) {
        if (v == s.target) return;
        s.prev = s.target;
        s.target = v;
        s.dir = v >= s.prev ? 1 : -1;
        if (!animate || getWidth() == 0 || !isAttachedToWindow()) { s.roll = 1f; return; }
        if (s.rollA != null) s.rollA.cancel();
        s.rollA = anim(0f, 1f, 380, new DecelerateInterpolator(), a -> { s.roll = (float) a.getAnimatedValue(); invalidate(); });
        if (v > s.prev) {
            if (s.popA != null) s.popA.cancel();
            s.popA = anim(0f, 1f, 480, new AccelerateDecelerateInterpolator(), a -> { s.pop = (float) a.getAnimatedValue(); invalidate(); });
            burst(bx, cy, tint);
        } else {
            if (s.shakeA != null) s.shakeA.cancel();
            s.shakeA = anim(0f, 1f, 420, null, a -> { s.shake = (float) a.getAnimatedValue(); invalidate(); });
        }
    }

    private void showNewBest() {
        bannerText = "NEW BEST!";
        removeCallbacks(bannerOff);
        postDelayed(bannerOff, 1800);
        if (flashA != null) flashA.cancel();
        flashA = anim(0f, 1f, 1000, null, a -> { flash = (float) a.getAnimatedValue(); invalidate(); });
    }

    private void burst(float x, float y, int tint) {
        for (int i = 0; i < 20; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            float sp = (70f + rnd.nextFloat() * 170f) * dp;
            parts.add(new float[]{x, y, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp, 1f, rnd.nextBoolean() ? 0xFFFFFF : tint});
        }
    }

    private ValueAnimator anim(float a, float b, long ms, TimeInterpolator ip, ValueAnimator.AnimatorUpdateListener l) {
        ValueAnimator v = ValueAnimator.ofFloat(a, b);
        v.setDuration(ms);
        if (ip != null) v.setInterpolator(ip);
        v.addUpdateListener(l);
        v.start();
        return v;
    }

    private void updateDescription() {
        setContentDescription("Player " + name + ". Win streak " + win.target + ". Best streak " + best.target + ".");
    }

    // ------------------------------------------------------------ layout

    private void metrics(int w) {
        gp = Math.max(10 * dp, w * 0.03f);
        cardH = Math.max(92 * dp, Math.min(128 * dp, (w - 2 * gp) * 0.30f));
        radius = cardH * 0.22f;
    }

    @Override protected void onMeasure(int ws, int hs) {
        int w = resolveSize((int) (360 * dp), ws);
        metrics(w);
        setMeasuredDimension(w, (int) Math.ceil(cardH + 2 * gp));
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) { build(w); }

    private void build(int w) {
        metrics(w);
        card.set(gp, gp, w - gp, gp + cardH);
        cy = card.centerY();
        float pad = cardH * 0.14f, gap = cardH * 0.08f;
        D = Math.max(44 * dp, Math.min(cardH * 0.68f, (card.width() - 2 * pad - gap * 3 - 86 * dp) / 2f));
        ringW = D * 0.15f;
        cx2 = card.right - pad - D / 2f;
        cx1 = cx2 - D - gap;
        nameL = card.left + pad;
        nameR = cx1 - D / 2f - gap * 1.5f;

        rimSh = new LinearGradient(card.left, 0, card.right, 0,
                new int[]{0xFF22D3EE, 0xFF7C3AED, 0xFFD946EF, 0xFF22D3EE}, null, Shader.TileMode.CLAMP);
        glassSh = new LinearGradient(0, card.top, 0, card.bottom, 0xF2221270, 0xF20B0626, Shader.TileMode.CLAMP);
        glossSh = new LinearGradient(0, card.top, 0, card.top + cardH * 0.4f, 0x3DFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
        nameSh = new LinearGradient(0, card.top + cardH * 0.45f, 0, card.top + cardH * 0.8f,
                new int[]{0xFFFFFFFF, 0xFFCFFAFE, 0xFFC4B5FD}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);

        mx.setRotate(-90, 0, 0);
        sweepWin = new SweepGradient(0, 0, new int[]{0xFF22D3EE, 0xFF3B82F6, 0xFFA855F7, 0xFF3B82F6, 0xFF22D3EE},
                new float[]{0f, 0.25f, 0.5f, 0.75f, 1f});
        sweepWin.setLocalMatrix(mx);
        sweepBest = new SweepGradient(0, 0, new int[]{0xFFFDE047, 0xFFF59E0B, 0xFFEC4899, 0xFFF59E0B, 0xFFFDE047},
                new float[]{0f, 0.25f, 0.5f, 0.75f, 1f});
        sweepBest.setLocalMatrix(mx);
        float ri = D / 2f - ringW;
        innerSh = new RadialGradient(0, -ri * 0.3f, ri * 1.3f, 0xFF2B1A8A, 0xFF0B0626, Shader.TileMode.CLAMP);
        ringGlossSh = new LinearGradient(0, -ri, 0, 0, 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
        numWinSh = new LinearGradient(0, -D * 0.2f, 0, D * 0.14f,
                new int[]{0xFFFFFFFF, 0xFFCFFAFE, 0xFF93C5FD}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
        numBestSh = new LinearGradient(0, -D * 0.2f, 0, D * 0.14f,
                new int[]{0xFFFFFFFF, 0xFFFEF3C7, 0xFFFCD34D}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);

        // fit name
        float avail = Math.max(10 * dp, nameR - nameL);
        nameSize = cardH * 0.25f;
        tp.setTypeface(tf);
        tp.setLetterSpacing(0.03f);
        tp.setTextSize(nameSize);
        while (tp.measureText(name) > avail && nameSize > 11 * dp) { nameSize -= dp * 0.5f; tp.setTextSize(nameSize); }
        shownName = name;
        while (tp.measureText(shownName) > avail && shownName.length() > 2) {
            shownName = shownName.substring(0, shownName.length() - 2) + "\u2026";
        }
    }

    // ------------------------------------------------------------ lifecycle

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (idleA == null) {
            idleA = ValueAnimator.ofFloat(0f, 1f);
            idleA.setDuration(3400);
            idleA.setRepeatCount(ValueAnimator.INFINITE);
            idleA.setInterpolator(null);
            idleA.addUpdateListener(a -> { idle = (float) a.getAnimatedValue(); invalidate(); });
        }
        if (!idleA.isStarted()) idleA.start();
    }

    @Override protected void onDetachedFromWindow() {
        if (idleA != null) idleA.cancel();
        removeCallbacks(bannerOff);
        super.onDetachedFromWindow();
    }

    @Override protected void onWindowVisibilityChanged(int v) {
        super.onWindowVisibilityChanged(v);
        if (idleA == null) return;
        if (v == VISIBLE) { if (idleA.isPaused()) idleA.resume(); }
        else if (idleA.isRunning()) idleA.pause();
    }

    // ------------------------------------------------------------ drawing

    @Override protected void onDraw(Canvas c) {
        if (rimSh == null) return;
        long now = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : Math.min(0.05f, (now - lastNs) / 1e9f);
        lastNs = now;

        drawCard(c);
        drawNameBlock(c);
        drawRing(c, cx1, win, false, "WIN", sweepWin, numWinSh, winProg,
                0xFFA5F3FC, 0xFF22D3EE, 0xFF1D4ED8, 0x22D3EE);
        drawRing(c, cx2, best, true, "BEST", sweepBest, numBestSh, 1f,
                0xFFFEF3C7, 0xFFFBBF24, 0xFFD97706, 0xFBBF24);
        drawParticles(c, dt);
        drawSparkle(c);
    }

    private void drawCard(Canvas c) {
        int glow = flash < 1f ? 0xFBBF24 : 0x60A5FA;
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.8f * dp);
        for (int i = 1; i <= 6; i++) {
            float ex = 2.5f * dp + i * 1.4f * dp;
            int a = (int) (60 * (1f - i / 7f) * (flash < 1f ? 1.6f : 1f));
            p.setColor((Math.min(255, a) << 24) | (i % 2 == 0 && flash >= 1f ? 0xA855F7 : glow));
            tmp.set(card); tmp.inset(-ex, -ex);
            c.drawRoundRect(tmp, radius + ex, radius + ex, p);
        }
        tmp.set(card); tmp.inset(-2.5f * dp, -2.5f * dp);
        p.reset(); p.setAntiAlias(true); p.setShader(rimSh);
        c.drawRoundRect(tmp, radius + 2.5f * dp, radius + 2.5f * dp, p);
        p.reset(); p.setAntiAlias(true); p.setShader(glassSh);
        c.drawRoundRect(card, radius, radius, p);

        tmp.set(card.left + cardH * 0.12f, card.top + 2 * dp, card.right - cardH * 0.12f, card.top + cardH * 0.38f);
        p.reset(); p.setAntiAlias(true); p.setShader(glossSh);
        c.drawRoundRect(tmp, radius * 0.8f, radius * 0.8f, p);

        if (flash < 1f) { // gold "new best" flash on the border
            tmp.set(card); tmp.inset(-2.5f * dp, -2.5f * dp);
            p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3.5f * dp);
            p.setColor(((int) (255 * (1f - flash)) << 24) | 0xFDE047);
            c.drawRoundRect(tmp, radius + 2.5f * dp, radius + 2.5f * dp, p);
        }
    }

    private void drawNameBlock(Canvas c) {
        // orbit swoosh under the name
        path.reset();
        path.moveTo(nameL, card.bottom - cardH * 0.13f);
        path.quadTo((nameL + nameR) / 2f, card.bottom + cardH * 0.02f, nameR, card.bottom - cardH * 0.3f);
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(2 * dp);
        p.setShader(new LinearGradient(nameL, 0, nameR, 0, 0x9922D3EE, 0x99A855F7, Shader.TileMode.CLAMP));
        c.drawPath(path, p);
        p.reset(); p.setAntiAlias(true); p.setColor(0xFFA5F3FC);
        c.drawCircle(nameR, card.bottom - cardH * 0.3f, 2.4f * dp, p);

        // label (PLAYER or NEW BEST!)
        boolean nb = bannerText != null;
        tp.setTypeface(tf);
        tp.setStyle(Paint.Style.FILL); tp.setShader(null);
        tp.setTextAlign(Paint.Align.LEFT);
        tp.setLetterSpacing(0.22f);
        tp.setTextSize(cardH * (nb ? 0.105f : 0.095f));
        tp.setColor(nb ? 0xFFFDE047 : 0xFFC4B5FD);
        c.drawText(nb ? bannerText : "PLAYER", nameL, card.top + cardH * 0.34f, tp);

        outlined(c, shownName, nameL, card.top + cardH * 0.73f, nameSize, Paint.Align.LEFT, nameSh, 1f);
    }

    private void drawRing(Canvas c, float cx, Stat s, boolean orbit, String label, Shader sweep, Shader numSh,
                          float prog, int light, int mid, int dark, int tintRgb) {
        float R = D / 2f;
        float sc = 1f + 0.2f * (float) Math.sin(s.pop * Math.PI);
        float dx = (float) Math.sin(s.shake * Math.PI * 6) * (1f - s.shake) * 4 * dp;
        c.save();
        c.translate(cx + dx, cy);
        c.scale(sc, sc);

        // glow
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.8f * dp);
        for (int i = 1; i <= 5; i++) {
            p.setColor(((int) (70 * (1f - i / 6f)) << 24) | tintRgb);
            c.drawCircle(0, 0, R + i * 1.8f * dp, p);
        }
        // outline + track
        p.reset(); p.setAntiAlias(true); p.setColor(INK);
        c.drawCircle(0, 0, R + 1.5f * dp, p);
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(ringW); p.setColor(0xFF2A1F7A);
        float rm = R - ringW / 2f;
        c.drawCircle(0, 0, rm, p);

        // progress arc
        float sweepDeg = Math.max(0f, Math.min(1f, prog)) * 360f;
        oval.set(-rm, -rm, rm, rm);
        if (sweepDeg > 1f) {
            p.setShader(sweep); p.setStrokeCap(Paint.Cap.ROUND);
            c.drawArc(oval, -90, sweepDeg, false, p);
            p.setShader(null); p.setStrokeWidth(ringW * 0.26f); p.setColor(0x99FFFFFF);
            float rh = R - ringW * 0.3f;
            tmp.set(-rh, -rh, rh, rh);
            c.drawArc(tmp, -90, sweepDeg, false, p);
        }

        // glass centre
        float ri = R - ringW;
        p.reset(); p.setAntiAlias(true); p.setShader(innerSh);
        c.drawCircle(0, 0, ri, p);
        tmp.set(-ri * 0.8f, -ri * 0.92f, ri * 0.8f, -ri * 0.08f);
        p.reset(); p.setAntiAlias(true); p.setShader(ringGlossSh);
        c.drawOval(tmp, p);

        // rolling number
        c.save();
        path.reset(); path.addCircle(0, 0, ri, Path.Direction.CW);
        c.clipPath(path);
        float h = D * 0.5f;
        if (s.roll < 1f) {
            drawNum(c, String.valueOf(s.prev), -s.dir * s.roll * h, 1f - s.roll, numSh);
            drawNum(c, String.valueOf(s.target), s.dir * (1f - s.roll) * h, s.roll, numSh);
        } else {
            drawNum(c, String.valueOf(s.target), 0f, 1f, numSh);
        }
        c.restore();

        // label
        tp.setShader(null); tp.setStyle(Paint.Style.FILL);
        tp.setTextAlign(Paint.Align.CENTER); tp.setLetterSpacing(0.15f);
        tp.setTextSize(D * 0.115f); tp.setColor(0xFFC4B5FD);
        c.drawText(label, 0, D * 0.29f, tp);

        // orb travelling along the ring
        float ang = (float) Math.toRadians(orbit ? -90f + idle * 360f : -90f + sweepDeg);
        float ox = (float) Math.cos(ang) * rm, oy = (float) Math.sin(ang) * rm, or = ringW * 0.95f;
        p.reset(); p.setAntiAlias(true); p.setColor(0x66000000 | tintRgb);
        c.drawCircle(ox, oy, or * 1.5f, p);
        p.reset(); p.setAntiAlias(true);
        p.setShader(new RadialGradient(ox - or * 0.3f, oy - or * 0.35f, or * 1.4f,
                new int[]{0xFFFFFFFF, light, mid, dark}, new float[]{0f, 0.18f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(ox, oy, or, p);
        c.restore();
    }

    private void drawNum(Canvas c, String t, float yOff, float alpha, Shader sh) {
        float size = D * 0.34f;
        tp.setTypeface(tf); tp.setLetterSpacing(0f); tp.setTextSize(size);
        while (tp.measureText(t) > D * 0.5f && size > 8 * dp) { size -= dp * 0.5f; tp.setTextSize(size); }
        c.save();
        c.translate(0, yOff);
        outlined(c, t, 0, -D * 0.04f - (tp.ascent() + tp.descent()) / 2f, size, Paint.Align.CENTER, sh, alpha);
        c.restore();
    }

    private void outlined(Canvas c, String t, float x, float y, float size, Paint.Align al, Shader fill, float alpha) {
        tp.setTypeface(tf); tp.setTextAlign(al); tp.setLetterSpacing(al == Paint.Align.LEFT ? 0.03f : 0f);
        tp.setTextSize(size); tp.setShader(null); tp.setStyle(Paint.Style.STROKE);
        int a = (int) (255 * alpha);
        tp.setStrokeWidth(size * 0.44f); tp.setColor(((int) (0x55 * alpha) << 24) | 0x67E8F9);
        c.drawText(t, x, y, tp);
        tp.setStrokeWidth(size * 0.24f); tp.setColor((a << 24) | 0x150F8F);
        c.drawText(t, x, y + size * 0.05f, tp);
        c.drawText(t, x, y, tp);
        tp.setStyle(Paint.Style.FILL); tp.setShader(fill); tp.setAlpha(a);
        c.drawText(t, x, y, tp);
        tp.setShader(null);
    }

    private void drawParticles(Canvas c, float dt) {
        Iterator<float[]> it = parts.iterator();
        while (it.hasNext()) {
            float[] q = it.next();
            float drag = (float) Math.exp(-dt * 2.2f);
            q[2] *= drag; q[3] *= drag;
            q[0] += q[2] * dt; q[1] += q[3] * dt;
            q[4] -= dt * 1.5f;
            if (q[4] <= 0f) { it.remove(); continue; }
            p.reset(); p.setAntiAlias(true);
            p.setColor(((int) (255 * q[4]) << 24) | ((int) q[5] & 0xFFFFFF));
            c.drawCircle(q[0], q[1], 2.4f * dp * q[4] + dp * 0.6f, p);
        }
    }

    private void drawSparkle(Canvas c) {
        float tw = 0.8f + 0.2f * (float) Math.sin(idle * 12.566f);
        float R = cardH * 0.1f * tw, sx = card.left + cardH * 0.14f, sy = card.top;
        p.reset(); p.setAntiAlias(true); p.setColor(0x66F0ABFC);
        c.drawCircle(sx, sy, R * 1.3f, p);
        path.reset();
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(-90 + i * 45);
            float rr = (i % 2 == 0) ? R : R * 0.22f;
            float x = sx + (float) Math.cos(a) * rr, y = sy + (float) Math.sin(a) * rr;
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        path.close();
        p.reset(); p.setAntiAlias(true); p.setColor(0xFFFBCFE8);
        c.drawPath(path, p);
    }
}

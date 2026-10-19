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
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Neon card for the profile screen. Use it for achievements (UNLOCKED / LOCKED badge)
 * and for the "Account Authority" card (gold variant, no badge).
 *
 * XML:
 *   <com.odyssey.game.NeonInfoCard
 *       android:id="@+id/cardPioneer"
 *       android:layout_width="match_parent"
 *       android:layout_height="wrap_content" />
 *
 * Java:
 *   card.setIconEmoji("\uD83C\uDFC6");            // or card.setIcon(drawable)
 *   card.setTitle("CSS Pioneer");
 *   card.setSubtitle("Win your first CSS Quiz match");
 *   card.setBadge("UNLOCKED", "LOCKED");           // text shown for unlocked / locked
 *   card.setUnlocked(true, false);                 // no animation when loading
 *   card.setUnlocked(true, true);                  // animated unlock (flash + ring burst)
 *   authorityCard.setVariant(NeonInfoCard.Variant.GOLD); authorityCard.setBadge(null, null);
 */
public class NeonInfoCard extends View {

    public enum Variant {
        CYAN(new int[]{0xFF22D3EE, 0xFF3B82F6, 0xFFA855F7}, 0x22D3EE, 0xFF22D3EE, 0xFFA5F3FC),
        GOLD(new int[]{0xFFFDE047, 0xFFF59E0B, 0xFFEC4899}, 0xFBBF24, 0xFFFBBF24, 0xFFFDE68A);

        final int[] border; final int glow, mid, text;

        Variant(int[] b, int g, int m, int t) { border = b; glow = g; mid = m; text = t; }
    }

    private static final int INK = 0xFF150F8F;

    private final float dp, sd;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint titleP = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint subP = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint badgeP = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint emojiP = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF card = new RectF(), tmp = new RectF();
    private final Path path = new Path();
    private final Matrix mx = new Matrix();

    private Variant variant = Variant.CYAN;
    private Typeface tf = Typeface.create("sans-serif-black", Typeface.NORMAL);
    private String title = "", subtitle = "", badgeOn = "UNLOCKED", badgeOff = "LOCKED", emoji = null;
    private Drawable icon;
    private boolean unlocked = true;

    private StaticLayout titleL, subL;
    private float gp, pad, iconD, badgeW, badgeH, textL, contentH, innerH, cardH;
    private Shader rimSh, glassSh, glossSh, shineSh;
    private float idle, flash = 1f;
    private ValueAnimator idleA, flashA;

    public NeonInfoCard(Context c) { this(c, null); }

    public NeonInfoCard(Context c, AttributeSet a) { this(c, a, 0); }

    public NeonInfoCard(Context c, AttributeSet a, int s) {
        super(c, a, s);
        dp = c.getResources().getDisplayMetrics().density;
        sd = c.getResources().getDisplayMetrics().scaledDensity;
        titleP.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleP.setTextSize(16 * sd);
        subP.setTextSize(13 * sd);
        badgeP.setTextSize(10.5f * sd);
        badgeP.setLetterSpacing(0.08f);
        badgeP.setTextAlign(Paint.Align.CENTER);
        emojiP.setTextAlign(Paint.Align.CENTER);
    }

    // ------------------------------------------------------------ public API

    public void setTitle(String t) { title = t == null ? "" : t; refresh(); }

    public void setSubtitle(String t) { subtitle = t == null ? "" : t; refresh(); }

    public void setIconEmoji(String e) { emoji = e; icon = null; invalidate(); }

    public void setIcon(Drawable d) { icon = d; emoji = null; invalidate(); }

    public void setVariant(Variant v) { variant = v; refresh(); }

    /** Texts for the badge pill when unlocked / locked. Pass null for no badge. */
    public void setBadge(String whenUnlocked, String whenLocked) { badgeOn = whenUnlocked; badgeOff = whenLocked; refresh(); }

    public void setNeonTypeface(Typeface t) { if (t != null) { tf = t; titleP.setTypeface(t); refresh(); } }

    public boolean isUnlocked() { return unlocked; }

    public void setUnlocked(boolean u, boolean animate) {
        boolean was = unlocked;
        unlocked = u;
        if (animate && u && !was && isAttachedToWindow()) {
            if (flashA != null) flashA.cancel();
            flashA = anim(0f, 1f, 1000, new DecelerateInterpolator(), a -> { flash = (float) a.getAnimatedValue(); invalidate(); });
        }
        refresh();
    }

    private String badge() { return unlocked ? badgeOn : badgeOff; }

    private void refresh() {
        setContentDescription(title + ". " + subtitle + (badge() != null ? ". " + badge() : ""));
        requestLayout();
        invalidate();
    }

    // ------------------------------------------------------------ layout

    private void layout(int w) {
        gp = Math.max(8 * dp, w * 0.02f);
        pad = 14 * dp;
        iconD = Math.max(40 * dp, Math.min(52 * dp, w * 0.13f));
        card.left = gp; card.right = w - gp;
        String b = badge();
        badgeH = 26 * dp;
        badgeW = b != null ? badgeP.measureText(b) + 24 * dp : 0f;
        textL = card.left + pad + iconD + 12 * dp;
        float textR = card.right - pad - (b != null ? badgeW + 10 * dp : 0f);
        int tw = (int) Math.max(60 * dp, textR - textL);
        titleL = new StaticLayout(title, titleP, tw, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
        subL = subtitle.isEmpty() ? null : new StaticLayout(subtitle, subP, tw, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
        contentH = titleL.getHeight() + (subL != null ? 4 * dp + subL.getHeight() : 0f);
        innerH = Math.max(iconD, contentH);
        cardH = innerH + 2 * pad;
        card.top = gp; card.bottom = gp + cardH;
    }

    @Override protected void onMeasure(int ws, int hs) {
        int w = resolveSize((int) (360 * dp), ws);
        layout(w);
        setMeasuredDimension(w, (int) Math.ceil(cardH + 2 * gp));
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        layout(w);
        rimSh = new LinearGradient(card.left, 0, card.right, 0, variant.border, null, Shader.TileMode.CLAMP);
        glassSh = new LinearGradient(0, card.top, 0, card.bottom, 0xF01B1250, 0xF00B0626, Shader.TileMode.CLAMP);
        glossSh = new LinearGradient(0, card.top, 0, card.top + cardH * 0.4f, 0x33FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
        shineSh = new LinearGradient(0, 0, cardH, 0, new int[]{0x00FFFFFF, 0x55FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP);
    }

    // ------------------------------------------------------------ lifecycle

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (idleA == null) {
            idleA = ValueAnimator.ofFloat(0f, 1f);
            idleA.setDuration(5200);
            idleA.setRepeatCount(ValueAnimator.INFINITE);
            idleA.setInterpolator(null);
            idleA.addUpdateListener(a -> { idle = (float) a.getAnimatedValue(); if (unlocked || flash < 1f) invalidate(); });
        }
        if (!idleA.isStarted()) idleA.start();
    }

    @Override protected void onDetachedFromWindow() {
        if (idleA != null) idleA.cancel();
        if (flashA != null) flashA.cancel();
        super.onDetachedFromWindow();
    }

    @Override protected void onWindowVisibilityChanged(int v) {
        super.onWindowVisibilityChanged(v);
        if (idleA == null) return;
        if (v == VISIBLE) { if (idleA.isPaused()) idleA.resume(); }
        else if (idleA.isRunning()) idleA.pause();
    }

    private ValueAnimator anim(float a, float b, long ms, TimeInterpolator ip, ValueAnimator.AnimatorUpdateListener l) {
        ValueAnimator v = ValueAnimator.ofFloat(a, b);
        v.setDuration(ms);
        if (ip != null) v.setInterpolator(ip);
        v.addUpdateListener(l);
        v.start();
        return v;
    }

    // ------------------------------------------------------------ drawing

    @Override protected void onDraw(Canvas c) {
        if (titleL == null) return;
        float radius = 20 * dp;
        int glow = variant.glow;

        if (unlocked) {
            p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.8f * dp);
            for (int i = 1; i <= 5; i++) {
                float ex = 2f * dp + i * 1.3f * dp;
                p.setColor(((int) (52 * (1f - i / 6f)) << 24) | glow);
                tmp.set(card); tmp.inset(-ex, -ex);
                c.drawRoundRect(tmp, radius + ex, radius + ex, p);
            }
        }

        // rim + glass
        tmp.set(card); tmp.inset(-2f * dp, -2f * dp);
        p.reset(); p.setAntiAlias(true);
        if (unlocked) p.setShader(rimSh); else p.setColor(0xFF2E2570);
        c.drawRoundRect(tmp, radius + 2 * dp, radius + 2 * dp, p);
        p.reset(); p.setAntiAlias(true);
        if (unlocked) p.setShader(glassSh); else p.setColor(0xF2100C2B);
        c.drawRoundRect(card, radius, radius, p);
        tmp.set(card.left + radius, card.top + 2 * dp, card.right - radius, card.top + cardH * 0.38f);
        p.reset(); p.setAntiAlias(true); p.setShader(glossSh);
        c.drawRoundRect(tmp, radius * 0.6f, radius * 0.6f, p);

        // slow shine, unlocked only
        if (unlocked) {
            float st = Math.max(0f, Math.min(1f, (idle - 0.55f) / 0.45f));
            if (st > 0f && st < 1f) {
                path.reset(); path.addRoundRect(card, radius, radius, Path.Direction.CW);
                c.save(); c.clipPath(path);
                float x = card.left - cardH + st * (card.width() + 2 * cardH);
                mx.setTranslate(x, 0); shineSh.setLocalMatrix(mx);
                p.reset(); p.setAntiAlias(true); p.setShader(shineSh);
                Path band = new Path();
                float sk = cardH * 0.3f;
                band.moveTo(x, card.top); band.lineTo(x + cardH, card.top);
                band.lineTo(x + cardH - sk, card.bottom); band.lineTo(x - sk, card.bottom); band.close();
                c.drawPath(band, p);
                c.restore();
            }
        }

        drawIcon(c);

        // text
        c.save();
        c.translate(textL, card.top + pad + (innerH - contentH) / 2f);
        titleP.setColor(unlocked ? 0xFFFFFFFF : 0xFF7A76A6);
        titleL.draw(c);
        if (subL != null) {
            c.translate(0, titleL.getHeight() + 4 * dp);
            subP.setColor(unlocked ? 0xFFB9B4E8 : 0xFF5A5784);
            subL.draw(c);
        }
        c.restore();

        drawBadge(c);

        // unlock flash
        if (flash < 1f) {
            tmp.set(card); tmp.inset(-2f * dp, -2f * dp);
            p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3.5f * dp);
            p.setColor(((int) (255 * (1f - flash)) << 24) | 0xFDE047);
            c.drawRoundRect(tmp, radius + 2 * dp, radius + 2 * dp, p);
            float ix = card.left + pad + iconD / 2f, iy = card.centerY();
            p.setStrokeWidth(2.5f * dp);
            p.setColor(((int) (200 * (1f - flash)) << 24) | 0xFFFFFF);
            c.drawCircle(ix, iy, iconD * 0.5f + flash * card.width() * 0.45f, p);
        }
    }

    private void drawIcon(Canvas c) {
        float ix = card.left + pad + iconD / 2f, iy = card.centerY(), R = iconD / 2f;
        int ringCol = unlocked ? variant.mid : 0xFF3A2F7A;
        if (unlocked) {
            p.reset(); p.setAntiAlias(true); p.setColor(0x55000000 | variant.glow);
            c.drawCircle(ix, iy, R * 1.25f, p);
        }
        p.reset(); p.setAntiAlias(true); p.setColor(INK);
        c.drawCircle(ix, iy, R + 1.5f * dp, p);
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2f * dp); p.setColor(ringCol);
        c.drawCircle(ix, iy, R - 1f * dp, p);
        p.reset(); p.setAntiAlias(true);
        p.setShader(new RadialGradient(ix, iy - R * 0.3f, R * 1.3f, unlocked ? 0xFF2B1A8A : 0xFF1A1548, 0xFF0B0626, Shader.TileMode.CLAMP));
        c.drawCircle(ix, iy, R - 2.5f * dp, p);

        int alpha = unlocked ? 255 : 110;
        if (emoji != null) {
            emojiP.setTextSize(iconD * 0.52f);
            emojiP.setAlpha(alpha);
            c.drawText(emoji, ix, iy - (emojiP.ascent() + emojiP.descent()) / 2f, emojiP);
        } else if (icon != null) {
            int h = (int) (iconD * 0.56f);
            icon.setBounds((int) (ix - h / 2f), (int) (iy - h / 2f), (int) (ix + h / 2f), (int) (iy + h / 2f));
            icon.setAlpha(alpha);
            icon.draw(c);
        }
    }

    private void drawBadge(Canvas c) {
        String b = badge();
        if (b == null) return;
        float right = card.right - pad, cy = card.centerY();
        tmp.set(right - badgeW, cy - badgeH / 2f, right, cy + badgeH / 2f);
        float r = badgeH / 2f;
        if (unlocked) {
            p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.6f * dp);
            for (int i = 1; i <= 3; i++) {
                p.setColor(((int) (46 * (1f - i / 4f)) << 24) | variant.glow);
                RectF g = new RectF(tmp); g.inset(-i * 1.4f * dp, -i * 1.4f * dp);
                c.drawRoundRect(g, r + i * 1.4f * dp, r + i * 1.4f * dp, p);
            }
        }
        p.reset(); p.setAntiAlias(true); p.setColor(unlocked ? 0xFF120B35 : 0xFF100C2B);
        c.drawRoundRect(tmp, r, r, p);
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.8f * dp);
        if (unlocked) p.setShader(new LinearGradient(tmp.left, 0, tmp.right, 0, variant.border, null, Shader.TileMode.CLAMP));
        else p.setColor(0xFF3A2F7A);
        c.drawRoundRect(tmp, r, r, p);

        badgeP.setTypeface(tf);
        float base = cy - (badgeP.ascent() + badgeP.descent()) / 2f;
        if (unlocked) {
            badgeP.setStyle(Paint.Style.STROKE); badgeP.setStrokeWidth(badgeP.getTextSize() * 0.28f);
            badgeP.setColor(0x55000000 | variant.glow);
            c.drawText(b, tmp.centerX(), base, badgeP);
        }
        badgeP.setStyle(Paint.Style.FILL);
        badgeP.setColor(unlocked ? variant.text : 0xFF6B6794);
        c.drawText(b, tmp.centerX(), base, badgeP);
    }
}

package com.odyssey.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;

/**
 * Tiny sound-effect player for Code Odyssey (SoundPool = instant, low latency, many at once).
 * Needs the 10 files in res/raw/ : sfx_click.wav, sfx_tab.wav, sfx_whoosh.wav, sfx_success.wav,
 * sfx_error.wav, sfx_unlock.wav, sfx_tick.wav, sfx_impact.wav, sfx_collect.wav, sfx_launch.wav
 *
 * Usage:
 *   NeonSound.init(this);                       // once, e.g. in MainActivity.onCreate()
 *   NeonSound.play(NeonSound.Sfx.CLICK);        // anywhere
 *   NeonSound.play(context, NeonSound.Sfx.TAB); // also works without init (auto-inits)
 *   NeonSound.setMuted(true);                   // remembered between launches
 */
public final class NeonSound {

    public enum Sfx {
        CLICK(R.raw.sfx_click),      // any button press
        TAB(R.raw.sfx_tab),          // nav tab / segmented tabs / selecting an option
        WHOOSH(R.raw.sfx_whoosh),    // dialogs and panels opening, transitions
        SUCCESS(R.raw.sfx_success),  // correct answer, picked a difficulty, saved
        ERROR(R.raw.sfx_error),      // wrong answer, locked item, invalid input
        UNLOCK(R.raw.sfx_unlock),    // achievement unlocked, new best streak
        TICK(R.raw.sfx_tick),        // countdown 3-2-1
        IMPACT(R.raw.sfx_impact),    // cutscene impact / big moment
        COLLECT(R.raw.sfx_collect),  // lucky star, +1 win
        LAUNCH(R.raw.sfx_launch);    // begin quiz / hyperjump

        final int res;

        Sfx(int res) { this.res = res; }
    }

    private static final String PREFS = "neon_sound";
    private static SoundPool pool;
    private static final int[] ids = new int[Sfx.values().length];
    private static final boolean[] loaded = new boolean[Sfx.values().length];
    private static boolean inited = false, muted = false;
    private static float volume = 0.85f;
    private static SharedPreferences prefs;

    private NeonSound() { }

    public static synchronized void init(Context c) {
        if (inited) return;
        Context app = c.getApplicationContext();
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        muted = prefs.getBoolean("muted", false);
        volume = prefs.getFloat("volume", 0.85f);

        AudioAttributes aa = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder().setMaxStreams(6).setAudioAttributes(aa).build();
        pool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status != 0) return;
            for (int i = 0; i < ids.length; i++) if (ids[i] == sampleId) loaded[i] = true;
        });
        Sfx[] all = Sfx.values();
        for (int i = 0; i < all.length; i++) ids[i] = pool.load(app, all[i].res, 1);
        inited = true;
    }

    public static void play(Context c, Sfx s) { init(c); play(s); }

    public static void play(Sfx s) {
        if (!inited || muted || pool == null) return;
        int i = s.ordinal();
        if (!loaded[i]) return;
        pool.play(ids[i], volume, volume, 1, 0, 1f);
    }

    public static boolean isMuted() { return muted; }

    public static void setMuted(boolean m) {
        muted = m;
        if (prefs != null) prefs.edit().putBoolean("muted", m).apply();
    }

    /** 0.0 .. 1.0 */
    public static void setVolume(float v) {
        volume = Math.max(0f, Math.min(1f, v));
        if (prefs != null) prefs.edit().putFloat("volume", volume).apply();
    }

    /** Optional: call when the app is really closing. */
    public static synchronized void release() {
        if (pool != null) { pool.release(); pool = null; }
        inited = false;
        java.util.Arrays.fill(loaded, false);
    }
}

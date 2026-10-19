package com.odyssey.game;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import com.google.android.material.textfield.TextInputEditText;

import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import android.os.CountDownTimer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 100% Pure Native Android Java Activity for Code Odyssey: CSS Quest Quiz
 * Features:
 * - Looping Gameboy BGM Audio with Interactive Mute/Unmute Toggle
 * - 100% CSS Question Bank (Easy, Medium, Hard)
 * - 5 Hearts Life & Auto-Restart Level System
 * - Live Firebase Firestore Sync
 * - Interactive Button Bounce Animations & Haptics
 */
public class MainActivity extends AppCompatActivity {

    public enum AlertType {
        ERROR, SUCCESS, INFO
    }

    // --- Color Palette (Vibrant Cosmic Purple Theme) ---
    private final int COLOR_PURPLE = Color.parseColor("#8A2BE2");
    private final int COLOR_CARD_BG = Color.parseColor("#EA1B1233");
    private final int COLOR_CYAN = Color.parseColor("#5EEAD4");
    private final int COLOR_WHITE = Color.parseColor("#F8FAFC");
    private final int COLOR_MUTED = Color.parseColor("#D8B4FE");
    private final int COLOR_HEART_ACTIVE = Color.parseColor("#FF3B69");
    private final int COLOR_HEART_LOST = Color.parseColor("#4A235A");

    // --- Audio ---
    private MediaPlayer bgmPlayer;
    private boolean isMusicMuted = false;
    private MaterialButton btnToggleMusic;

    // --- Controllers & Dedicated Screen Modules ---
    private Profile profileController;
    private AdminPanel adminPanelController;
    private boolean isStartingQuizCutscene = false;

    // --- Views ---
    private ImageView ivGalaxyBg;
    private CosmicBackgroundView cosmicBgView;
    private ScrollView scrollView;
    private View layoutAuth;
    private View layoutMenu;
    private View layoutQuiz;
    private View layoutSummary;
    private View layoutHowToPlay;
    private View layoutAdminPanel;
    private NeonTutorialButton btnAdminPanel;
    private MaterialButton btnAdminBackTop, btnAdminBackBottom, btnAdminRefresh, btnAdminAddSubmit;
    private TextInputEditText etAdminAddUser;
    private TextView tvAdminActiveUser, tvAdminTotalUsers, tvAdminTotalAdmins, tvAdminTotalLogins, tvAdminUserListHeader;
    private ProgressBar pbAdminLoading;
    private LinearLayout containerAdminUsers;

    // Splash Screen Views
    private View layoutSplashScreen;
    private GlimmerLogoView glimmerLogoView;
    private boolean isSplashDismissed = false;
    private final Handler splashHandler = new Handler(Looper.getMainLooper());

    // How to Play Views
    private MaterialButton btnHowToPlayBackTop, btnHowToPlayStart, btnHowToPlayBack;

    // Auth Views
    private MaterialButton btnTabLogin, btnTabSignup, btnAuthSubmit;
    private View layoutAuthLoading;
    private TextView tvAuthLoadingText;
    private EditText etPlayerName, etPassword, etConfirmPassword;
    private View tilConfirmPassword;
    private boolean isLoginMode = true;

    // Menu & Home Views
    private PlayerStatsCard playerCard;
    private NeonQuizButton btnStartAdventure;
    private NeonTutorialButton btnHowToPlay;

    // Profile Views (Clean Dark TikTok Style)
    private ScrollView scrollProfile;
    private ScrollView scrollAdmin;
    private ScrollView scrollSettings;
    private View layoutProfile;
    private TextView tvProfileTopName, tvProfileWinStreak, tvProfileBestStreak, tvProfileRole;
    private ImageView ivProfileAvatar;
    private View cardProfileAvatar, cardAvatarPlusBadge;
    private MaterialButton btnProfileUploadPhoto, btnProfileRemovePhoto, btnProfileStartQuiz, btnProfileGoSettings, btnProfileGoAdmin, btnProfileLogout;
    private ActivityResultLauncher<String> profilePhotoPickerLauncher;

    // Settings Views
    private View layoutSettings;
    private ImageView ivSettingsMusicIcon;
    private TextView tvSettingsMusicStatus, tvSettingsHapticsStatus, tvSettingsUserPresence;
    private MaterialButton btnSettingsMusicToggle, btnSettingsHapticsToggle, btnSettingsLogout;
    private boolean isHapticsEnabled = true;

    // Bottom Navigation View
    private NeonBottomNav bottomNav;

    // Quiz Views
    private TextView tvQuizLevelBadge, tvQuizProgress, tvHeartsBar, tvHeartsCount, tvQuestionText;
    private MaterialButton btnChoiceA, btnChoiceB, btnChoiceC, btnChoiceD, btnSubmitAnswer, btnQuizBackMenu;
    private MaterialButton[] choiceButtons;

    // Quiz Timer Views
    private MaterialCardView cardQuizTimer;
    private TextView tvQuizTimer;
    private ProgressBar pbQuizTimer;
    private ImageView ivQuizTimerIcon;
    private CountDownTimer questionTimer = null;
    public static final long DEFAULT_QUESTION_TIME_LIMIT_MS = 15000L; // 15 seconds default
    private long questionTimeLimitMs = DEFAULT_QUESTION_TIME_LIMIT_MS;
    private static final long TIMER_INTERVAL_MS = 100L;

    // Summary Views
    private ImageView ivSummaryTrophy;
    private TextView tvSummaryTitle, tvSummaryScore, tvSummaryPoints, tvSummaryStreak;
    private MaterialButton btnSummaryBackMenu;

    // --- State & Database-driven Question Bank ---
    private Account currentAccount;
    private String selectedStartingDiff = "easy";
    private final String[] difficultySequence = {"easy", "medium", "hard"};

    private static final String PREF_CUSTOM_QUESTIONS = "cached_custom_questions_json";
    private static final Map<String, List<Question>> questionBank = new HashMap<>();

    static {
        questionBank.put("easy", new ArrayList<>());
        questionBank.put("medium", new ArrayList<>());
        questionBank.put("hard", new ArrayList<>());
    }

    private int levelMaxHearts = 5;
    private int currentHearts = 5;
    private List<String> activeDifficulties = new ArrayList<>();
    private int currentDiffIdx = 0;
    private List<Question> currentLevelQuestions = new ArrayList<>();
    private int currentQuestionIdx = 0;
    private int currentScoreInLevel = 0;
    private int totalCorrectAllLevels = 0;
    private int totalQuestionsAllLevels = 0;
    private int totalPointsEarned = 0;
    private int selectedChoiceIdx = -1;
    private int correctChoiceIdx = -1;

    private Vibrator vibrator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.core.splashscreen.SplashScreen splashScreen = androidx.core.splashscreen.SplashScreen.installSplashScreen(this);
        splashScreen.setOnExitAnimationListener(splashScreenViewProvider -> splashScreenViewProvider.remove());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            if (bottomNav != null) {
                bottomNav.setBottomInset(insets.bottom);
            }
            return windowInsets;
        });

        profileController = new Profile(this);
        adminPanelController = new AdminPanel(this);

        initAudio();
        profileController.init();
        initViews();
        adminPanelController.init();
        setupAuthListeners();
        setupMenuListeners();
        setupQuizListeners();

        SharedPreferences prefs = getSharedPreferences("odyssey_prefs", MODE_PRIVATE);
        questionTimeLimitMs = prefs.getLong("quiz_time_limit_ms", DEFAULT_QUESTION_TIME_LIMIT_MS);

        // Load cached quiz questions and sync from Firestore
        List<Question> cachedQuestions = loadCustomQuestionsFromCache();
        if (!cachedQuestions.isEmpty()) {
            syncCustomQuestions(cachedQuestions);
        }

        FirebaseHelper.getAllCustomQuestions(new FirebaseHelper.Callback<List<Question>>() {
            @Override
            public void onSuccess(List<Question> questions) {
                if (questions != null) {
                    syncCustomQuestions(questions);
                }
            }

            @Override
            public void onError(String error) {
                // Keep local cache if offline
            }
        });

        // Sync admin-configured quiz timer from Firestore
        FirebaseHelper.getQuizTimerSeconds(new FirebaseHelper.Callback<Integer>() {
            @Override
            public void onSuccess(Integer seconds) {
                if (seconds != null && seconds >= 5) {
                    setQuestionTimeLimitSeconds(seconds);
                }
            }

            @Override
            public void onError(String error) {}
        });

        String lastUser = prefs.getString("last_user", "");
        if (!lastUser.isEmpty() && etPlayerName != null) {
            etPlayerName.setText(lastUser);
        }

        showScreen(layoutAuth);
    }

    private void initAudio() {
        NeonSound.init(this);
        try {
            bgmPlayer = MediaPlayer.create(this, R.raw.bgm_gameboy);
            if (bgmPlayer != null) {
                bgmPlayer.setLooping(true);
                bgmPlayer.setVolume(0.8f, 0.8f);
                // Audio stays quiet during splash screen; starts ONLY after splash dismiss completes
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startBgmAudio() {
        if (isSplashDismissed && !isMusicMuted && bgmPlayer != null && !bgmPlayer.isPlaying()) {
            try {
                bgmPlayer.start();
            } catch (Exception ignored) {}
        }
    }

    public void toggleMusic() {
        if (bgmPlayer == null) {
            initAudio();
            if (isSplashDismissed) {
                startBgmAudio();
            }
        } else {
            if (isMusicMuted) {
                isMusicMuted = false;
                startBgmAudio();
                Toast.makeText(this, "Music Playing 🎵", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    bgmPlayer.pause();
                } catch (Exception ignored) {}
                isMusicMuted = true;
                Toast.makeText(this, "Music Muted 🔇", Toast.LENGTH_SHORT).show();
            }
        }
        updateMusicUi();
    }

    public void updateMusicUi() {
        if (btnSettingsMusicToggle != null) {
            btnSettingsMusicToggle.setText(!isMusicMuted ? "ON" : "OFF");
            btnSettingsMusicToggle.setBackgroundTintList(ColorStateList.valueOf(!isMusicMuted ? Color.parseColor("#10B981") : Color.parseColor("#EF4444")));
        }
        if (ivSettingsMusicIcon != null) {
            ivSettingsMusicIcon.setImageResource(!isMusicMuted ? R.drawable.ic_volume_up : R.drawable.ic_volume_off);
            ivSettingsMusicIcon.setImageTintList(ColorStateList.valueOf(!isMusicMuted ? COLOR_CYAN : Color.parseColor("#EF4444")));
        }
        if (tvSettingsMusicStatus != null) {
            tvSettingsMusicStatus.setText(!isMusicMuted ? "Playing 8-bit space soundtrack 🎵" : "Soundtrack muted 🔇");
        }
        if (btnToggleMusic != null) {
            btnToggleMusic.setText(!isMusicMuted ? "MUSIC: ON" : "MUSIC: OFF");
            btnToggleMusic.setIconResource(!isMusicMuted ? R.drawable.ic_volume_up : R.drawable.ic_volume_off);
            btnToggleMusic.setTextColor(!isMusicMuted ? COLOR_CYAN : COLOR_MUTED);
            btnToggleMusic.setIconTint(ColorStateList.valueOf(!isMusicMuted ? COLOR_CYAN : COLOR_MUTED));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cosmicBgView != null) {
            cosmicBgView.setSensorParallaxEnabled(true);
        }
        if (currentAccount != null) {
            FirebaseHelper.setOnlineStatus(currentAccount.username, true);
        }
        // ONLY resume music if splash screen is ALREADY DISMISSED!
        if (isSplashDismissed) {
            startBgmAudio();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cancelQuestionTimer();
        if (cosmicBgView != null) {
            cosmicBgView.setSensorParallaxEnabled(false);
        }
        if (currentAccount != null) {
            FirebaseHelper.setOnlineStatus(currentAccount.username, false);
        }
        if (bgmPlayer != null && bgmPlayer.isPlaying()) {
            try {
                bgmPlayer.pause();
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onDestroy() {
        NeonSound.release();
        cancelQuestionTimer();
        super.onDestroy();
        if (bgmPlayer != null) {
            try {
                bgmPlayer.stop();
                bgmPlayer.release();
            } catch (Exception ignored) {}
            bgmPlayer = null;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (cosmicBgView != null) {
            cosmicBgView.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void initViews() {
        ivGalaxyBg = findViewById(R.id.ivGalaxyBg);
        if (ivGalaxyBg != null) {
            ivGalaxyBg.setVisibility(View.GONE); // Let CosmicBackgroundView render directly without overdraw
        }
        cosmicBgView = findViewById(R.id.cosmicBgView);
        scrollView = findViewById(R.id.scrollView);

        layoutAuth = findViewById(R.id.layoutAuth);
        layoutMenu = findViewById(R.id.layoutMenu);
        layoutQuiz = findViewById(R.id.layoutQuiz);
        layoutSummary = findViewById(R.id.layoutSummary);

        // Splash Screen Setup
        layoutSplashScreen = findViewById(R.id.layoutSplashScreen);
        View splashLogoContainer = findViewById(R.id.splashLogoContainer);
        glimmerLogoView = findViewById(R.id.glimmerLogoView);
        if (layoutSplashScreen != null) {
            layoutSplashScreen.setOnClickListener(v -> dismissSplashScreen());
            if (splashLogoContainer != null) {
                splashLogoContainer.setAlpha(0f);
                splashLogoContainer.setScaleX(0.85f);
                splashLogoContainer.setScaleY(0.85f);
                splashLogoContainer.animate()
                        .alpha(1f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(600)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
            splashHandler.postDelayed(this::dismissSplashScreen, 2800);
        }
        // Auth
        btnTabLogin = findViewById(R.id.btnTabLogin);
        btnTabSignup = findViewById(R.id.btnTabSignup);
        etPlayerName = findViewById(R.id.etPlayerName);
        etPassword = findViewById(R.id.etPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnAuthSubmit = findViewById(R.id.btnAuthSubmit);
        layoutAuthLoading = findViewById(R.id.layoutAuthLoading);
        tvAuthLoadingText = findViewById(R.id.tvAuthLoadingText);

        // Menu & Home Status
        playerCard = findViewById(R.id.playerCard);
        btnStartAdventure = findViewById(R.id.btnStartAdventure);
        btnHowToPlay = findViewById(R.id.btnHowToPlay);
        if (btnHowToPlay != null) {
            btnHowToPlay.setHearts(currentHearts);
        }

        // Modular Screens: Profile, Admin, Settings
        scrollProfile = findViewById(R.id.scrollProfile);
        scrollAdmin = findViewById(R.id.scrollAdmin);
        scrollSettings = findViewById(R.id.scrollSettings);

        // Bottom Navigation View
        bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setListener((id, changed) -> {
                if (!changed) return;
                NeonSound.play(NeonSound.Sfx.TAB);
                switch (id) {
                    case NeonBottomNav.HOME:
                        openMainMenu();
                        break;
                    case NeonBottomNav.ADMIN:
                        openAdminPanel();
                        break;
                    case NeonBottomNav.PROFILE:
                        openProfileScreen();
                        break;
                }
            });
        }

        // Quiz
        tvQuizLevelBadge = findViewById(R.id.tvQuizLevelBadge);
        tvQuizProgress = findViewById(R.id.tvQuizProgress);
        tvHeartsBar = findViewById(R.id.tvHeartsBar);
        tvHeartsCount = findViewById(R.id.tvHeartsCount);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        btnChoiceA = findViewById(R.id.btnChoiceA);
        btnChoiceB = findViewById(R.id.btnChoiceB);
        btnChoiceC = findViewById(R.id.btnChoiceC);
        btnChoiceD = findViewById(R.id.btnChoiceD);
        btnSubmitAnswer = findViewById(R.id.btnSubmitAnswer);
        btnQuizBackMenu = findViewById(R.id.btnQuizBackMenu);
        choiceButtons = new MaterialButton[]{btnChoiceA, btnChoiceB, btnChoiceC, btnChoiceD};

        // Quiz Timer
        cardQuizTimer = findViewById(R.id.cardQuizTimer);
        tvQuizTimer = findViewById(R.id.tvQuizTimer);
        pbQuizTimer = findViewById(R.id.pbQuizTimer);
        ivQuizTimerIcon = findViewById(R.id.ivQuizTimerIcon);

        // Summary
        ivSummaryTrophy = findViewById(R.id.ivSummaryTrophy);
        tvSummaryTitle = findViewById(R.id.tvSummaryTitle);
        tvSummaryScore = findViewById(R.id.tvSummaryScore);
        tvSummaryPoints = findViewById(R.id.tvSummaryPoints);
        tvSummaryStreak = findViewById(R.id.tvSummaryStreak);
        btnSummaryBackMenu = findViewById(R.id.btnSummaryBackMenu);

        // How to Play Screen Views & Listeners
        layoutHowToPlay = findViewById(R.id.layoutHowToPlay);
        btnHowToPlayBackTop = findViewById(R.id.btnHowToPlayBackTop);
        btnHowToPlayStart = findViewById(R.id.btnHowToPlayStart);
        btnHowToPlayBack = findViewById(R.id.btnHowToPlayBack);

        if (btnHowToPlayBackTop != null) {
            btnHowToPlayBackTop.setOnClickListener(v -> animateClick(btnHowToPlayBackTop, this::openMainMenu));
        }
        if (btnHowToPlayBack != null) {
            btnHowToPlayBack.setOnClickListener(v -> animateClick(btnHowToPlayBack, this::openMainMenu));
        }
        if (btnHowToPlayStart != null) {
            btnHowToPlayStart.setOnClickListener(v -> animateClick(btnHowToPlayStart, this::promptDifficultySelection));
        }

        // Quick Admin Panel shortcut on Home card
        btnAdminPanel = findViewById(R.id.btnAdminPanel);
        if (btnAdminPanel != null) {
            btnAdminPanel.setVariant(NeonTutorialButton.Variant.GOLD);
            btnAdminPanel.setGlyph("\u2605");
            btnAdminPanel.hideHearts();
            btnAdminPanel.setOnClickListener(v -> animateClick(btnAdminPanel, this::openAdminPanel));
        }

        // Initialize default difficulty
        selectedStartingDiff = "easy";

        // Load saved preferences
        isHapticsEnabled = getSharedPreferences("odyssey_prefs", MODE_PRIVATE).getBoolean("haptics_enabled", true);
        updateMusicUi();
        updateHapticsUi();
    }

    public Account getCurrentAccount() {
        return currentAccount;
    }

    public boolean isStartingQuizCutscene() {
        return isStartingQuizCutscene;
    }

    public void setStartingQuizCutscene(boolean starting) {
        this.isStartingQuizCutscene = starting;
    }

    private void showScreen(View screenToShow) {
        if (screenToShow != layoutQuiz) {
            cancelQuestionTimer();
        }
        layoutAuth.setVisibility(View.GONE);
        layoutMenu.setVisibility(View.GONE);
        layoutQuiz.setVisibility(View.GONE);
        layoutSummary.setVisibility(View.GONE);
        if (layoutHowToPlay != null) layoutHowToPlay.setVisibility(View.GONE);
        if (scrollAdmin != null) scrollAdmin.setVisibility(View.GONE);
        if (scrollProfile != null) scrollProfile.setVisibility(View.GONE);
        if (scrollSettings != null) scrollSettings.setVisibility(View.GONE);

        boolean isHome = (screenToShow == layoutMenu || screenToShow == layoutHowToPlay || screenToShow == layoutSummary);
        boolean isProfile = (screenToShow == scrollProfile);
        boolean isAdmin = (screenToShow == scrollAdmin);
        boolean isSettings = (screenToShow == scrollSettings);
        boolean isQuiz = (screenToShow == layoutQuiz);
        boolean isAuth = (screenToShow == layoutAuth);

        // ScrollView hosts Home, Auth, Quiz, HowToPlay, Summary
        if (scrollView != null) {
            scrollView.setVisibility((isHome || isQuiz || isAuth) ? View.VISIBLE : View.GONE);
            if (isHome || isQuiz || isAuth) scrollView.smoothScrollTo(0, 0);
        }

        // Live Cosmic Background: ONLY on Home, Auth, Quiz, HowToPlay, Summary. NEVER on Profile or Admin!
        if (cosmicBgView != null) {
            cosmicBgView.setVisibility((isHome || isQuiz || isAuth) ? View.VISIBLE : View.GONE);
        }
        if (ivGalaxyBg != null) {
            ivGalaxyBg.setVisibility(View.GONE);
        }

        // Code Odyssey Title Banner: ONLY on Home (layoutMenu) and Auth. NEVER on Profile, Admin, Settings, or Quiz!
        View ivTitleBanner = findViewById(R.id.ivTitleBanner);
        View tvSubTitle = findViewById(R.id.tvSubTitle);
        if (ivTitleBanner != null) {
            ivTitleBanner.setVisibility((screenToShow == layoutMenu || isAuth) ? View.VISIBLE : View.GONE);
        }
        if (tvSubTitle != null) {
            tvSubTitle.setVisibility((screenToShow == layoutMenu || isAuth) ? View.VISIBLE : View.GONE);
        }

        screenToShow.setVisibility(View.VISIBLE);
        if (isProfile && scrollProfile != null) scrollProfile.smoothScrollTo(0, 0);
        if (isAdmin && scrollAdmin != null) scrollAdmin.smoothScrollTo(0, 0);
        if (isSettings && scrollSettings != null) scrollSettings.smoothScrollTo(0, 0);

        // Control Bottom Nav visibility and selection
        if (bottomNav != null) {
            if (isAuth || isQuiz || isSettings) {
                bottomNav.setVisibility(View.GONE);
            } else {
                bottomNav.setVisibility(View.VISIBLE);
                boolean userIsAdmin = (currentAccount != null && FirebaseHelper.isUserAdmin(currentAccount));
                bottomNav.setAdminVisible(userIsAdmin);
                if (isHome) {
                    bottomNav.setSelected(NeonBottomNav.HOME, false);
                } else if (isAdmin) {
                    bottomNav.setSelected(NeonBottomNav.ADMIN, false);
                } else if (isProfile) {
                    bottomNav.setSelected(NeonBottomNav.PROFILE, false);
                }
            }
        }
    }

    public void openProfileScreen() {
        if (profileController != null) {
            profileController.render(currentAccount);
        }
        showScreen(scrollProfile);
    }

    public void openAdminPanel() {
        if (adminPanelController != null) {
            adminPanelController.open();
        }
    }

    public void showAdminScreenDirect() {
        showScreen(scrollAdmin);
    }

    public void showSettingsScreenDirect() {
        showScreen(scrollSettings);
    }

    public void setCosmicWarp(boolean enable) {
        if (cosmicBgView != null) {
            cosmicBgView.setWarp(enable);
        }
    }

    public void setCosmicTheme(CosmicBackgroundView.Theme theme) {
        if (cosmicBgView != null) {
            cosmicBgView.setTheme(theme);
        }
    }

    public boolean isMediumUnlocked() {
        if (currentAccount == null) return false;
        if (FirebaseHelper.isUserAdmin(currentAccount)) return true;
        if (currentAccount.unlockedMedium) return true;
        return getSharedPreferences("odyssey_prefs", MODE_PRIVATE)
                .getBoolean("unlocked_medium_" + currentAccount.username, false);
    }

    public boolean isHardUnlocked() {
        if (currentAccount == null) return false;
        if (FirebaseHelper.isUserAdmin(currentAccount)) return true;
        if (currentAccount.unlockedHard) return true;
        return getSharedPreferences("odyssey_prefs", MODE_PRIVATE)
                .getBoolean("unlocked_hard_" + currentAccount.username, false);
    }

    public void unlockMediumTier() {
        if (currentAccount == null) return;
        if (!currentAccount.unlockedMedium) {
            currentAccount.unlockedMedium = true;
            getSharedPreferences("odyssey_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("unlocked_medium_" + currentAccount.username, true)
                    .apply();
            FirebaseHelper.saveAccount(currentAccount, null);
        }
    }

    public void unlockHardTier() {
        if (currentAccount == null) return;
        if (!currentAccount.unlockedHard) {
            currentAccount.unlockedHard = true;
            getSharedPreferences("odyssey_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("unlocked_hard_" + currentAccount.username, true)
                    .apply();
            FirebaseHelper.saveAccount(currentAccount, null);
        }
    }

    public void promptDifficultySelection() {
        boolean isMedUnlocked = isMediumUnlocked();
        boolean isHardUnlocked = isHardUnlocked();
        String hint = !isMedUnlocked ? "Clear / Perfect Easy quiz to unlock"
                : (!isHardUnlocked ? "Clear / Perfect Medium quiz to unlock" : null);

        NeonDifficultyPicker.show(this, new boolean[]{true, isMedUnlocked, isHardUnlocked}, hint,
                new NeonDifficultyPicker.Listener() {
                    @Override
                    public void onPick(int level) {
                        String diff = level == 0 ? "easy" : (level == 1 ? "medium" : "hard");
                        startAdventureForDifficulty(diff);
                    }

                    @Override
                    public void onCancel() {
                    }
                });
    }

    public void toggleHaptics() {
        isHapticsEnabled = !isHapticsEnabled;
        getSharedPreferences("odyssey_prefs", MODE_PRIVATE).edit().putBoolean("haptics_enabled", isHapticsEnabled).apply();
        updateHapticsUi();
        if (isHapticsEnabled) vibrateDevice(50);
    }

    public void updateHapticsUi() {
        if (btnSettingsHapticsToggle != null) {
            btnSettingsHapticsToggle.setText(isHapticsEnabled ? "ON" : "OFF");
            btnSettingsHapticsToggle.setBackgroundTintList(ColorStateList.valueOf(isHapticsEnabled ? Color.parseColor("#10B981") : Color.parseColor("#64748B")));
        }
        if (tvSettingsHapticsStatus != null) {
            tvSettingsHapticsStatus.setText(isHapticsEnabled ? "Tactile vibration active" : "Vibration disabled");
        }
    }

    public void logoutUser() {
        if (currentAccount != null) {
            FirebaseHelper.setOnlineStatus(currentAccount.username, false);
            currentAccount = null;
        }
        if (btnAdminPanel != null) btnAdminPanel.setVisibility(View.GONE);
        if (bottomNav != null) bottomNav.setAdminVisible(false);
        if (etPassword != null) etPassword.setText("");
        if (etConfirmPassword != null) etConfirmPassword.setText("");
        setAuthLoading(false);
        showScreen(layoutAuth);
        Toast.makeText(this, "Logged out safely", Toast.LENGTH_SHORT).show();
    }

    // --- Interactive Bounce Animations ---
    public void animateClick(View v, Runnable onEnd) {
        if (v == null) {
            if (onEnd != null) onEnd.run();
            return;
        }
        NeonSound.play(NeonSound.Sfx.CLICK);
        vibrateDevice(20);
        if (cosmicBgView != null) {
            int[] loc = new int[2];
            v.getLocationOnScreen(loc);
            float cx = loc[0] + v.getWidth() / 2f;
            float cy = loc[1] + v.getHeight() / 2f;
            cosmicBgView.burst(cx, cy);
        }
        v.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(50)
                .withEndAction(() -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(50).start())
                .start();
        if (onEnd != null) {
            v.postDelayed(onEnd, 45);
        }
    }

    private void startNebulaAmbientMotion() {
        // Redundant animation disabled to free up CPU and eliminate background overdraw
    }

    // --- Authentication Flow ---
    private void setupAuthListeners() {
        if (btnTabLogin != null) btnTabLogin.setOnClickListener(v -> animateClick(btnTabLogin, () -> setAuthMode(true)));
        if (btnTabSignup != null) btnTabSignup.setOnClickListener(v -> animateClick(btnTabSignup, () -> setAuthMode(false)));
        if (btnAuthSubmit != null) btnAuthSubmit.setOnClickListener(v -> animateClick(btnAuthSubmit, this::handleAuthSubmit));
        if (btnToggleMusic != null) btnToggleMusic.setOnClickListener(v -> toggleMusic());
    }

    private void setAuthMode(boolean login) {
        isLoginMode = login;
        setAuthLoading(false);
        if (login) {
            btnTabLogin.setBackgroundTintList(ColorStateList.valueOf(COLOR_PURPLE));
            btnTabLogin.setTextColor(COLOR_WHITE);
            btnTabLogin.setIconTint(ColorStateList.valueOf(COLOR_WHITE));
            btnTabSignup.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            btnTabSignup.setTextColor(COLOR_MUTED);
            btnTabSignup.setIconTint(ColorStateList.valueOf(COLOR_MUTED));
            btnAuthSubmit.setText("LOG IN");
            if (tilConfirmPassword != null) tilConfirmPassword.setVisibility(View.GONE);
        } else {
            btnTabSignup.setBackgroundTintList(ColorStateList.valueOf(COLOR_PURPLE));
            btnTabSignup.setTextColor(COLOR_WHITE);
            btnTabSignup.setIconTint(ColorStateList.valueOf(COLOR_WHITE));
            btnTabLogin.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            btnTabLogin.setTextColor(COLOR_MUTED);
            btnTabLogin.setIconTint(ColorStateList.valueOf(COLOR_MUTED));
            btnAuthSubmit.setText("CREATE ACCOUNT");
            if (tilConfirmPassword != null) tilConfirmPassword.setVisibility(View.VISIBLE);
        }
    }

    private void setAuthLoading(boolean loading) {
        if (layoutAuthLoading != null) {
            layoutAuthLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (tvAuthLoadingText != null && loading) {
            tvAuthLoadingText.setText(isLoginMode ? "LOGGING IN..." : "CREATING ACCOUNT...");
        }
        if (btnAuthSubmit != null) {
            btnAuthSubmit.setEnabled(!loading);
            if (!loading) {
                btnAuthSubmit.setText(isLoginMode ? "LOG IN" : "CREATE ACCOUNT");
            } else {
                btnAuthSubmit.setText("");
            }
        }
    }

    private void handleAuthSubmit() {
        String name = etPlayerName.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String confirmPass = (etConfirmPassword != null) ? etConfirmPassword.getText().toString().trim() : "";

        if (name.isEmpty() || pass.isEmpty()) {
            showErrorAlert("Missing Info", "Please enter both Player Name and Password.");
            return;
        }

        if (!isLoginMode) {
            if (confirmPass.isEmpty()) {
                showErrorAlert("Confirm Password Required", "Please re-enter your password to confirm.");
                return;
            }
            if (!pass.equals(confirmPass)) {
                showErrorAlert("Password Mismatch", "Passwords do not match! Please check your password and confirm password.");
                return;
            }
        }

        setAuthLoading(true);

        FirebaseHelper.getAccount(name, new FirebaseHelper.Callback<Account>() {
            @Override
            public void onSuccess(Account existing) {
                if (isLoginMode) {
                    if (existing == null) {
                        setAuthLoading(false);
                        showErrorAlert("User Not Found", "Player '" + name + "' was not found.\nPlease switch to 'SIGN UP' to register.");
                    } else if (!existing.password.equals(pass)) {
                        setAuthLoading(false);
                        showErrorAlert("Invalid Password", "Incorrect password for player '" + name + "'. Please try again.");
                    } else {
                        setAuthLoading(false);
                        currentAccount = existing;
                        FirebaseHelper.recordLogin(currentAccount);
                        openMainMenu();
                    }
                } else {
                    // Sign up mode
                    if (existing != null) {
                        if (existing.password.equals(pass)) {
                            setAuthLoading(false);
                            currentAccount = existing;
                            FirebaseHelper.recordLogin(currentAccount);
                            Toast.makeText(MainActivity.this, "Welcome back! Logged in as " + name, Toast.LENGTH_SHORT).show();
                            openMainMenu();
                        } else {
                            setAuthLoading(false);
                            showErrorAlert("Player Name Exists", "This player name is already registered with a different password.");
                        }
                    } else {
                        // Register new user
                        Account newAcc = new Account(name.toLowerCase(), name, pass);
                        FirebaseHelper.saveAccount(newAcc, new FirebaseHelper.Callback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                setAuthLoading(false);
                                currentAccount = newAcc;
                                FirebaseHelper.recordLogin(currentAccount);
                                showSuccessAlert("Registration Complete!", "Welcome aboard, " + name + "! Your adventure profile is ready.", "START PLAYING", MainActivity.this::openMainMenu);
                            }

                            @Override
                            public void onError(String error) {
                                setAuthLoading(false);
                                showErrorAlert("Registration Error", "Failed to create account. Please check your connection and try again.");
                            }
                        });
                    }
                }
            }

            @Override
            public void onError(String error) {
                setAuthLoading(false);
                showErrorAlert("Connection Error", "Could not connect to the server. Please check your internet connection.");
            }
        });
    }

    // --- Main Menu ---
    private void setupMenuListeners() {
        if (btnStartAdventure != null) {
            btnStartAdventure.setOnClickListener(v -> {
                if (isStartingQuizCutscene) return;
                isStartingQuizCutscene = true;
                if (cosmicBgView != null) {
                    int[] loc = new int[2];
                    btnStartAdventure.getLocationOnScreen(loc);
                    float cx = loc[0] + btnStartAdventure.getWidth() / 2f;
                    float cy = loc[1] + btnStartAdventure.getHeight() / 2f;
                    cosmicBgView.burst(cx, cy);
                }
                QuizCutsceneView.play(this, v, "May your CSS be clean and your answers correct.", luckyStars -> {
                    isStartingQuizCutscene = false;
                    promptDifficultySelection();
                });
            });
        }
        if (btnHowToPlay != null) {
            btnHowToPlay.setOnClickListener(v -> animateClick(btnHowToPlay, () -> showScreen(layoutHowToPlay)));
        }
        if (btnAdminPanel != null) {
            btnAdminPanel.setOnClickListener(v -> animateClick(btnAdminPanel, this::openAdminPanel));
        }
    }

    public void openMainMenu() {
        if (currentAccount != null) {
            SharedPreferences prefs = getSharedPreferences("odyssey_prefs", MODE_PRIVATE);
            prefs.edit().putString("last_user", currentAccount.name).apply();

            // Sync unlocked tiers with local storage
            currentAccount.unlockedMedium = currentAccount.unlockedMedium
                    || prefs.getBoolean("unlocked_medium_" + currentAccount.username, false)
                    || FirebaseHelper.isUserAdmin(currentAccount);
            currentAccount.unlockedHard = currentAccount.unlockedHard
                    || prefs.getBoolean("unlocked_hard_" + currentAccount.username, false)
                    || FirebaseHelper.isUserAdmin(currentAccount);

            if (playerCard != null) {
                playerCard.setPlayerName(currentAccount.name);
                playerCard.setStats(currentAccount.currentWinStreak, currentAccount.bestWinStreak);
            }
            if (btnHowToPlay != null) {
                btnHowToPlay.setHearts(currentHearts);
            }

            // Online status in real time
            FirebaseHelper.setOnlineStatus(currentAccount.username, true);

            // Only 'biar' and designated admins see the Admin Control Panel button!
            boolean isAdmin = FirebaseHelper.isUserAdmin(currentAccount);
            if (btnAdminPanel != null) {
                btnAdminPanel.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            }
            if (bottomNav != null) {
                bottomNav.setAdminVisible(isAdmin);
            }
        } else {
            if (btnAdminPanel != null) btnAdminPanel.setVisibility(View.GONE);
            if (bottomNav != null) bottomNav.setAdminVisible(false);
        }
        showScreen(layoutMenu);
    }

    private void selectDifficulty(String diff) {
        selectedStartingDiff = diff;
    }

    private void showHowToPlayDialog() {
        showScreen(layoutHowToPlay);
    }

    // --- Quiz Engine & 5 Hearts Life System ---
    private void setupQuizListeners() {
        for (int i = 0; i < choiceButtons.length; i++) {
            final int idx = i;
            choiceButtons[i].setOnClickListener(v -> {
                selectChoice(idx);
                animateClick(choiceButtons[idx], null);
            });
        }

        btnSubmitAnswer.setOnClickListener(v -> animateClick(btnSubmitAnswer, this::handleSubmitAnswer));
        btnSummaryBackMenu.setOnClickListener(v -> animateClick(btnSummaryBackMenu, () -> {
            cancelQuestionTimer();
            openMainMenu();
        }));
        if (btnQuizBackMenu != null) {
            btnQuizBackMenu.setOnClickListener(v -> animateClick(btnQuizBackMenu, () -> {
                cancelQuestionTimer();
                openMainMenu();
            }));
        }
    }

    public void startAdventureForDifficulty(String diff) {
        String targetDiff = (diff != null) ? diff.toLowerCase().trim() : "easy";
        selectedStartingDiff = targetDiff;

        List<Question> pool = questionBank.get(targetDiff);
        if (pool == null || pool.isEmpty()) {
            boolean isAdmin = currentAccount != null && FirebaseHelper.isUserAdmin(currentAccount);
            String title = "NO " + targetDiff.toUpperCase() + " QUESTIONS";
            String msg = "There are currently no questions added for " + targetDiff.toUpperCase() + " mode in the database.\n\n"
                    + (isAdmin ? "As an Admin, tap below to open the Admin Portal and add questions for " + targetDiff.toUpperCase() + " mode."
                              : "Please ask an administrator to add questions for " + targetDiff.toUpperCase() + " mode in the Admin Portal!");
            showErrorAlert(title, msg, isAdmin ? "ADMIN PORTAL" : "OK", () -> {
                if (isAdmin && adminPanelController != null) {
                    adminPanelController.open();
                } else {
                    openMainMenu();
                }
            });
            return;
        }

        // Strictly isolate difficulty: questions added to Easy ONLY appear in Easy!
        activeDifficulties = new ArrayList<>(Collections.singletonList(targetDiff));
        currentDiffIdx = 0;
        totalCorrectAllLevels = 0;
        totalQuestionsAllLevels = 0;
        totalPointsEarned = 0;

        startLevel(targetDiff);
    }

    private void startAdventure() {
        startAdventureForDifficulty(selectedStartingDiff);
    }

    private void startLevel(String diff) {
        String cleanDiff = (diff != null) ? diff.toLowerCase().trim() : "easy";
        List<Question> pool = questionBank.get(cleanDiff);

        if (pool == null || pool.isEmpty()) {
            boolean isAdmin = currentAccount != null && FirebaseHelper.isUserAdmin(currentAccount);
            String title = "NO " + cleanDiff.toUpperCase() + " QUESTIONS";
            String msg = "The " + cleanDiff.toUpperCase() + " question bank is currently empty.\n\n"
                    + (isAdmin ? "As an administrator, please open the Admin Portal to add questions for " + cleanDiff.toUpperCase() + " difficulty."
                              : "Please ask an administrator to add questions for " + cleanDiff.toUpperCase() + " difficulty in the Admin Portal!");
            showErrorAlert(title, msg, isAdmin ? "ADMIN PORTAL" : "OK", () -> {
                if (isAdmin && adminPanelController != null) {
                    adminPanelController.open();
                } else {
                    openMainMenu();
                }
            });
            return;
        }

        currentLevelQuestions = new ArrayList<>(pool);
        Collections.shuffle(currentLevelQuestions);
        // Play up to 30 questions per session from the active difficulty pool
        if (currentLevelQuestions.size() > 30) {
            currentLevelQuestions = new ArrayList<>(currentLevelQuestions.subList(0, 30));
        }

        // 70% passing requirement: allowed mistakes is 30% of total questions (at least 5 hearts)
        levelMaxHearts = Math.max(5, (int) Math.ceil(currentLevelQuestions.size() * 0.30));
        currentHearts = levelMaxHearts;
        updateHeartsBar();

        currentQuestionIdx = 0;
        currentScoreInLevel = 0;

        tvQuizLevelBadge.setText("LEVEL: " + cleanDiff.toUpperCase() + " (CSS)");
        showScreen(layoutQuiz);

        loadCurrentQuestion();
    }

    private void updateHeartsBar() {
        int filledHearts = (levelMaxHearts > 0) ? (int) Math.round(((double) currentHearts / levelMaxHearts) * 5.0) : 5;
        filledHearts = Math.max(0, Math.min(5, filledHearts));

        if (btnHowToPlay != null) {
            btnHowToPlay.setHearts(filledHearts);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append("♥ ");
        }

        SpannableString span = new SpannableString(sb.toString().trim());
        for (int i = 0; i < 5; i++) {
            int start = i * 2;
            int end = start + 1;
            int color = (i < filledHearts) ? COLOR_HEART_ACTIVE : COLOR_HEART_LOST;
            span.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvHeartsBar.setText(span);
        tvHeartsCount.setText(" (" + currentHearts + "/" + levelMaxHearts + " Hearts)");
    }

    private void loadCurrentQuestion() {
        cancelQuestionTimer();
        selectedChoiceIdx = -1;
        if (scrollView != null) {
            scrollView.smoothScrollTo(0, 0);
        }
        Question q = currentLevelQuestions.get(currentQuestionIdx);

        tvQuizProgress.setText("Q " + (currentQuestionIdx + 1) + "/" + currentLevelQuestions.size());
        tvQuestionText.setText("[Q" + (currentQuestionIdx + 1) + "/" + currentLevelQuestions.size() + "] " + q.q);

        // Shuffle options
        List<String> shuffled = new ArrayList<>(q.options);
        String correctText = q.options.get(q.correct);
        Collections.shuffle(shuffled);
        correctChoiceIdx = shuffled.indexOf(correctText);

        for (int i = 0; i < choiceButtons.length; i++) {
            char letter = (char) ('A' + i);
            choiceButtons[i].setText(letter + ". " + shuffled.get(i));
            choiceButtons[i].setBackgroundTintList(ColorStateList.valueOf(COLOR_CARD_BG));
            choiceButtons[i].setStrokeColor(ColorStateList.valueOf(Color.parseColor("#582B8C")));
        }

        startQuestionTimer();
    }

    private void selectChoice(int idx) {
        selectedChoiceIdx = idx;
        for (int i = 0; i < choiceButtons.length; i++) {
            if (i == idx) {
                choiceButtons[i].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#3D1E6D")));
                choiceButtons[i].setStrokeColor(ColorStateList.valueOf(COLOR_CYAN));
            } else {
                choiceButtons[i].setBackgroundTintList(ColorStateList.valueOf(COLOR_CARD_BG));
                choiceButtons[i].setStrokeColor(ColorStateList.valueOf(Color.parseColor("#582B8C")));
            }
        }
    }

    private void handleSubmitAnswer() {
        if (selectedChoiceIdx == -1) {
            Toast.makeText(this, "Please choose an answer option first!", Toast.LENGTH_SHORT).show();
            return;
        }

        cancelQuestionTimer();
        boolean isCorrect = (selectedChoiceIdx == correctChoiceIdx);
        char correctLetter = (char) ('A' + correctChoiceIdx);

        if (isCorrect) {
            currentScoreInLevel++;
            vibrateDevice(50);
            if (playerCard != null) {
                playerCard.incrementWin();
            }
            if (currentAccount != null) {
                currentAccount.currentWinStreak = (playerCard != null) ? playerCard.getWin() : (currentAccount.currentWinStreak + 1);
                currentAccount.bestWinStreak = Math.max(currentAccount.bestWinStreak, currentAccount.currentWinStreak);
                if (profileController != null) {
                    profileController.checkAndUnlockAchievements(currentAccount);
                }
            }
            showSuccessAlert("Correct Answer!", "Excellent! That is the right CSS answer.", "NEXT QUESTION", this::advanceQuestion);
        } else {
            // Deduct 1 Heart
            currentHearts--;
            updateHeartsBar();
            vibrateDevice(200);
            if (playerCard != null) {
                playerCard.resetWin();
            }
            if (currentAccount != null) {
                currentAccount.currentWinStreak = 0;
            }

            if (currentHearts <= 0) {
                // Out of hearts! Repeat level from Question 1!
                vibrateDevice(500);
                String levelName = activeDifficulties.get(currentDiffIdx).toUpperCase();

                showErrorAlert("OUT OF HEARTS (0/" + levelMaxHearts + ")!", "Incorrect! The correct answer was: " + correctLetter + "\n\n"
                        + "You lost all " + levelMaxHearts + " hearts on " + levelName + " level!\n"
                        + "Restarting " + levelName + " level from Question 1...", "RETRY " + levelName, () -> {
                    // Automatically repeat the current level from Question 1!
                    startLevel(activeDifficulties.get(currentDiffIdx));
                });
            } else {
                showErrorAlert("Incorrect Answer", "The correct answer was: " + correctLetter + "\n\n"
                        + "You lost 1 heart! Remaining: " + currentHearts + " / " + levelMaxHearts + " hearts.", "CONTINUE", this::advanceQuestion);
            }
        }
    }

    private void startQuestionTimer() {
        cancelQuestionTimer();

        int totalSeconds = (int) (questionTimeLimitMs / 1000L);
        if (pbQuizTimer != null) {
            pbQuizTimer.setMax((int) questionTimeLimitMs);
            pbQuizTimer.setProgress((int) questionTimeLimitMs);
            pbQuizTimer.setProgressTintList(ColorStateList.valueOf(COLOR_CYAN));
        }
        if (tvQuizTimer != null) {
            tvQuizTimer.setText(totalSeconds + "s");
            tvQuizTimer.setTextColor(COLOR_CYAN);
        }
        if (cardQuizTimer != null) {
            cardQuizTimer.setStrokeColor(COLOR_CYAN);
        }

        questionTimer = new CountDownTimer(questionTimeLimitMs, TIMER_INTERVAL_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isFinishing() || isDestroyed()) return;

                int secondsRemaining = (int) Math.ceil(millisUntilFinished / 1000.0);
                if (tvQuizTimer != null) {
                    tvQuizTimer.setText(secondsRemaining + "s");
                }
                if (pbQuizTimer != null) {
                    pbQuizTimer.setProgress((int) millisUntilFinished);
                }

                if (secondsRemaining <= 4) {
                    int dangerColor = Color.parseColor("#EF4444");
                    if (tvQuizTimer != null) tvQuizTimer.setTextColor(dangerColor);
                    if (pbQuizTimer != null) pbQuizTimer.setProgressTintList(ColorStateList.valueOf(dangerColor));
                    if (cardQuizTimer != null) cardQuizTimer.setStrokeColor(dangerColor);

                    if (millisUntilFinished % 1000 < 120 && secondsRemaining > 0) {
                        vibrateDevice(25);
                    }
                }
            }

            @Override
            public void onFinish() {
                if (isFinishing() || isDestroyed()) return;
                if (tvQuizTimer != null) tvQuizTimer.setText("0s");
                if (pbQuizTimer != null) pbQuizTimer.setProgress(0);
                handleQuestionTimeout();
            }
        }.start();
    }

    private void cancelQuestionTimer() {
        if (questionTimer != null) {
            questionTimer.cancel();
            questionTimer = null;
        }
    }

    private void handleQuestionTimeout() {
        cancelQuestionTimer();
        vibrateDevice(300);

        // Deduct 1 Heart
        currentHearts--;
        updateHeartsBar();
        if (playerCard != null) {
            playerCard.resetWin();
        }
        if (currentAccount != null) {
            currentAccount.currentWinStreak = 0;
        }

        char correctLetter = (char) ('A' + correctChoiceIdx);

        if (currentHearts <= 0) {
            vibrateDevice(500);
            String levelName = activeDifficulties.get(currentDiffIdx).toUpperCase();

            showErrorAlert("OUT OF HEARTS (0/" + levelMaxHearts + ")!", "Time expired on this question!\nThe correct answer was: " + correctLetter + "\n\n"
                    + "You lost all " + levelMaxHearts + " hearts on " + levelName + " level!\n"
                    + "Restarting " + levelName + " level from Question 1...", "RETRY " + levelName, () -> {
                startLevel(activeDifficulties.get(currentDiffIdx));
            });
        } else {
            showErrorAlert("TIME EXPIRED (-1 Heart)", "Time's up for this question!\nThe correct answer was: " + correctLetter + "\n\n"
                    + "You lost 1 heart! Remaining: " + currentHearts + " / " + levelMaxHearts + " hearts.", "CONTINUE", this::advanceQuestion);
        }
    }

    public long getQuestionTimeLimitMs() {
        return questionTimeLimitMs;
    }

    public int getQuestionTimeLimitSeconds() {
        return (int) (questionTimeLimitMs / 1000L);
    }

    public void setQuestionTimeLimitSeconds(int seconds) {
        if (seconds < 5) seconds = 5;
        if (seconds > 300) seconds = 300;
        this.questionTimeLimitMs = seconds * 1000L;
        getSharedPreferences("odyssey_prefs", MODE_PRIVATE)
                .edit()
                .putLong("quiz_time_limit_ms", this.questionTimeLimitMs)
                .apply();
    }

    public void syncCustomQuestions(List<Question> questions) {
        if (questions == null) questions = new ArrayList<>();
        saveCustomQuestionsToCache(questions);

        if (questionBank.containsKey("easy") && questionBank.get("easy") != null) {
            questionBank.get("easy").clear();
        } else {
            questionBank.put("easy", new ArrayList<>());
        }

        if (questionBank.containsKey("medium") && questionBank.get("medium") != null) {
            questionBank.get("medium").clear();
        } else {
            questionBank.put("medium", new ArrayList<>());
        }

        if (questionBank.containsKey("hard") && questionBank.get("hard") != null) {
            questionBank.get("hard").clear();
        } else {
            questionBank.put("hard", new ArrayList<>());
        }

        for (Question q : questions) {
            String diff = q.difficulty != null ? q.difficulty.toLowerCase().trim() : "easy";
            List<Question> bank = questionBank.get(diff);
            if (bank != null) {
                bank.add(q);
            } else {
                questionBank.get("easy").add(q);
            }
        }
    }

    public void addCustomQuestionToBank(Question q) {
        if (q == null) return;
        String diff = q.difficulty != null ? q.difficulty.toLowerCase().trim() : "easy";
        if (!questionBank.containsKey(diff)) {
            questionBank.put(diff, new ArrayList<>());
        }
        List<Question> bank = questionBank.get(diff);
        if (bank != null) {
            boolean exists = false;
            for (Question existing : bank) {
                if (existing.id != null && existing.id.equals(q.id)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                bank.add(q);
            }
        }

        List<Question> cached = loadCustomQuestionsFromCache();
        for (int i = 0; i < cached.size(); i++) {
            if (cached.get(i).id.equals(q.id)) {
                cached.remove(i);
                break;
            }
        }
        cached.add(q);
        saveCustomQuestionsToCache(cached);
    }

    public void removeCustomQuestionFromBank(String questionId) {
        if (questionId == null || questionId.isEmpty()) return;
        for (List<Question> list : questionBank.values()) {
            for (int i = 0; i < list.size(); i++) {
                if (questionId.equals(list.get(i).id)) {
                    list.remove(i);
                    break;
                }
            }
        }

        List<Question> cached = loadCustomQuestionsFromCache();
        for (int i = 0; i < cached.size(); i++) {
            if (questionId.equals(cached.get(i).id)) {
                cached.remove(i);
                break;
            }
        }
        saveCustomQuestionsToCache(cached);
    }

    private void saveCustomQuestionsToCache(List<Question> questions) {
        try {
            JSONArray arr = new JSONArray();
            if (questions != null) {
                for (Question q : questions) {
                    arr.put(q.toJson());
                }
            }
            getSharedPreferences("odyssey_prefs", MODE_PRIVATE)
                    .edit()
                    .putString(PREF_CUSTOM_QUESTIONS, arr.toString())
                    .apply();
        } catch (Exception ignored) {}
    }

    private List<Question> loadCustomQuestionsFromCache() {
        List<Question> list = new ArrayList<>();
        try {
            String jsonStr = getSharedPreferences("odyssey_prefs", MODE_PRIVATE)
                    .getString(PREF_CUSTOM_QUESTIONS, "[]");
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                Question q = Question.fromJson(arr.getJSONObject(i));
                if (q != null) {
                    list.add(q);
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    public Map<String, List<Question>> getQuestionBank() {
        return questionBank;
    }

    private void advanceQuestion() {
        currentQuestionIdx++;
        if (currentQuestionIdx < currentLevelQuestions.size()) {
            loadCurrentQuestion();
        } else {
            // Level questions completed! Enforce strict 70% passing score requirement
            String currentLevel = activeDifficulties.get(currentDiffIdx);
            int totalQ = currentLevelQuestions.size();
            int correctQ = currentScoreInLevel;
            int passingScore = (int) Math.ceil(totalQ * 0.70);
            int pct = totalQ > 0 ? (int) Math.round(((double) correctQ / totalQ) * 100.0) : 0;
            boolean passed = (correctQ >= passingScore);

            if (!passed) {
                // FAILED: Below 70% passing requirement
                vibrateDevice(300);
                if (playerCard != null) {
                    playerCard.resetWin();
                }
                if (currentAccount != null) {
                    currentAccount.currentWinStreak = 0;
                }

                String title = "LEVEL NOT PASSED (" + pct + "%)";
                String msg = "You scored " + correctQ + " / " + totalQ + " (" + pct + "%).\n\n"
                        + "A minimum passing score of 70% (" + passingScore + "/" + totalQ + " correct) is required to unlock and advance to the next level!\n\n"
                        + "Please try again to master " + currentLevel.toUpperCase() + " mode!";

                showErrorAlert(title, msg, "RETRY " + currentLevel.toUpperCase(), () -> {
                    startLevel(currentLevel);
                });
                return;
            }

            // PASSED: Score >= 70%
            int ptsPerQ = currentLevel.equalsIgnoreCase("hard") ? 20 : currentLevel.equalsIgnoreCase("medium") ? 15 : 10;
            totalCorrectAllLevels += correctQ;
            totalQuestionsAllLevels += totalQ;
            totalPointsEarned += (correctQ * ptsPerQ);

            // Progression Tier Unlocks (Only unlocked upon achieving 70%+ passing score):
            if ("easy".equalsIgnoreCase(currentLevel)) {
                unlockMediumTier();
            } else if ("medium".equalsIgnoreCase(currentLevel)) {
                unlockHardTier();
            }

            currentDiffIdx++;
            if (currentDiffIdx < activeDifficulties.size()) {
                String nextLevel = activeDifficulties.get(currentDiffIdx).toUpperCase();
                showSuccessAlert("CSS Level Passed! 🎓", "Great work! You passed " + currentLevel.toUpperCase() + " level with " + correctQ + "/" + totalQ + " (" + pct + "%).\n\n"
                        + "Passing score requirement (70%) satisfied!\n"
                        + "Advancing to " + nextLevel + " level with fresh hearts!", "ENTER " + nextLevel, () -> startLevel(activeDifficulties.get(currentDiffIdx)));
            } else {
                finishQuest();
            }
        }
    }

    private void finishQuest() {
        int totalQuestions = totalQuestionsAllLevels > 0 ? totalQuestionsAllLevels : (activeDifficulties.size() * 5);
        int passingScore = (int) Math.ceil(totalQuestions * 0.70);
        boolean won = (totalCorrectAllLevels >= passingScore);
        int overallPct = totalQuestions > 0 ? (int) Math.round(((double) totalCorrectAllLevels / totalQuestions) * 100.0) : 0;

        if (won) {
            for (String d : activeDifficulties) {
                if ("easy".equalsIgnoreCase(d)) {
                    unlockMediumTier();
                } else if ("medium".equalsIgnoreCase(d)) {
                    unlockHardTier();
                }
            }
        }

        if (currentAccount != null) {
            if (playerCard != null) {
                if (won) {
                    currentAccount.currentWinStreak = playerCard.getWin();
                    currentAccount.bestWinStreak = Math.max(currentAccount.bestWinStreak, playerCard.getBest());
                } else {
                    currentAccount.currentWinStreak = 0;
                    playerCard.resetWin();
                }
            } else {
                if (won) {
                    currentAccount.currentWinStreak++;
                    currentAccount.bestWinStreak = Math.max(currentAccount.bestWinStreak, currentAccount.currentWinStreak);
                } else {
                    currentAccount.currentWinStreak = 0;
                }
            }
        }

        if (won) {
            if (profileController != null && currentAccount != null) {
                profileController.checkAndUnlockAchievements(currentAccount);
            }
            String finishedLevel = activeDifficulties.isEmpty() ? "EASY" : activeDifficulties.get(0).toUpperCase();
            if ("EASY".equals(finishedLevel)) {
                tvSummaryTitle.setText("EASY CONQUERED! 🏆");
            } else if ("MEDIUM".equals(finishedLevel)) {
                tvSummaryTitle.setText("MEDIUM CONQUERED! 🏆");
            } else {
                tvSummaryTitle.setText("HARD CONQUERED! 👑");
            }
            tvSummaryTitle.setTextColor(COLOR_CYAN);
            if (ivSummaryTrophy != null) {
                ivSummaryTrophy.setImageResource(R.drawable.ic_trophy);
                ivSummaryTrophy.setImageTintList(ColorStateList.valueOf(COLOR_CYAN));
            }
        } else {
            tvSummaryTitle.setText("KEEP GROWING!");
            tvSummaryTitle.setTextColor(COLOR_MUTED);
            if (ivSummaryTrophy != null) {
                ivSummaryTrophy.setImageResource(R.drawable.ic_help_outline);
                ivSummaryTrophy.setImageTintList(ColorStateList.valueOf(COLOR_MUTED));
            }
        }

        // Sync streak to Firebase Firestore
        if (currentAccount != null) {
            currentAccount.bestWinStreak = Math.max(currentAccount.bestWinStreak, currentAccount.currentWinStreak);
            FirebaseHelper.saveAccount(currentAccount, null);
        }

        tvSummaryScore.setText("Correct Answers: " + totalCorrectAllLevels + " / " + totalQuestions + " (" + overallPct + "% - Passing: 70%)");
        tvSummaryPoints.setText("Points Earned: " + totalPointsEarned);
        tvSummaryStreak.setText("Win Streak: " + (currentAccount != null ? currentAccount.currentWinStreak : 0) + " (Best: " + (currentAccount != null ? currentAccount.bestWinStreak : 0) + ")");

        showScreen(layoutSummary);
    }

    public void vibrateDevice(long millis) {
        if (!isHapticsEnabled) return;
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                vibrator.vibrate(millis);
            } catch (Exception ignored) {}
        }
    }

    private void showCustomAlert(AlertType type, String title, String message, String positiveBtnText, Runnable onPositive) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .create();

        View view = getLayoutInflater().inflate(R.layout.dialog_custom_alert, null);
        dialog.setView(view);

        MaterialCardView cardRoot = view.findViewById(R.id.cardDialogRoot);
        MaterialCardView cardIcon = view.findViewById(R.id.cardAlertIcon);
        ImageView ivIcon = view.findViewById(R.id.ivAlertIcon);
        TextView tvTitle = view.findViewById(R.id.tvAlertTitle);
        TextView tvMessage = view.findViewById(R.id.tvAlertMessage);
        MaterialButton btnPositive = view.findViewById(R.id.btnAlertPositive);

        tvTitle.setText(title);
        tvMessage.setText(message);
        if (positiveBtnText != null && !positiveBtnText.isEmpty()) {
            btnPositive.setText(positiveBtnText);
        }

        switch (type) {
            case ERROR:
                NeonSound.play(NeonSound.Sfx.ERROR);
                cardRoot.setStrokeColor(Color.parseColor("#FF3B69"));
                cardIcon.setStrokeColor(Color.parseColor("#FF3B69"));
                cardIcon.setCardBackgroundColor(Color.parseColor("#331220"));
                ivIcon.setImageResource(R.drawable.ic_error_outline);
                ivIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FF3B69")));
                tvTitle.setTextColor(Color.parseColor("#FF3B69"));
                btnPositive.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF3B69")));
                btnPositive.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#FFA0B8")));
                break;
            case SUCCESS:
                NeonSound.play(NeonSound.Sfx.SUCCESS);
                cardRoot.setStrokeColor(Color.parseColor("#5EEAD4"));
                cardIcon.setStrokeColor(Color.parseColor("#5EEAD4"));
                cardIcon.setCardBackgroundColor(Color.parseColor("#0C2925"));
                ivIcon.setImageResource(R.drawable.ic_check_circle);
                ivIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#5EEAD4")));
                tvTitle.setTextColor(Color.parseColor("#5EEAD4"));
                btnPositive.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
                btnPositive.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#6EE7B7")));
                break;
            case INFO:
            default:
                NeonSound.play(NeonSound.Sfx.WHOOSH);
                cardRoot.setStrokeColor(Color.parseColor("#A855F7"));
                cardIcon.setStrokeColor(Color.parseColor("#C084FC"));
                cardIcon.setCardBackgroundColor(Color.parseColor("#22143D"));
                ivIcon.setImageResource(R.drawable.ic_help_outline);
                ivIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#C084FC")));
                tvTitle.setTextColor(Color.parseColor("#5EEAD4"));
                btnPositive.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#8A2BE2")));
                btnPositive.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#C084FC")));
                break;
        }

        btnPositive.setOnClickListener(v -> {
            animateClick(btnPositive, () -> {
                dialog.dismiss();
                if (onPositive != null) onPositive.run();
            });
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }

    public void showErrorAlert(String title, String message) {
        showCustomAlert(AlertType.ERROR, title, message, "GOT IT", null);
    }

    private void showErrorAlert(String title, String message, Runnable onOk) {
        showCustomAlert(AlertType.ERROR, title, message, "CONTINUE", onOk);
    }

    private void showErrorAlert(String title, String message, String btnText, Runnable onOk) {
        showCustomAlert(AlertType.ERROR, title, message, btnText, onOk);
    }

    private void showSuccessAlert(String title, String message, Runnable onOk) {
        showCustomAlert(AlertType.SUCCESS, title, message, "CONTINUE", onOk);
    }

    public void showSuccessAlert(String title, String message, String btnText, Runnable onOk) {
        showCustomAlert(AlertType.SUCCESS, title, message, btnText, onOk);
    }

    private void showInfoAlert(String title, String message) {
        showCustomAlert(AlertType.INFO, title, message, "OK", null);
    }

    private void showInfoAlert(String title, String message, Runnable onOk) {
        showCustomAlert(AlertType.INFO, title, message, "OK", onOk);
    }

    private void showSimpleAlert(String title, String message) {
        showCustomAlert(AlertType.INFO, title, message, "OK", null);
    }

    private void dismissSplashScreen() {
        if (isSplashDismissed || layoutSplashScreen == null) return;
        isSplashDismissed = true;
        splashHandler.removeCallbacksAndMessages(null);

        layoutSplashScreen.animate()
                .alpha(0f)
                .scaleX(1.04f)
                .scaleY(1.04f)
                .setDuration(400)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    layoutSplashScreen.setVisibility(View.GONE);
                    // ONLY start audio playback now that the splash screen animation is completely done
                    startBgmAudio();
                })
                .start();
    }

    public android.graphics.drawable.GradientDrawable createBadgeDrawable(int bgColor, int strokeColor) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        gd.setCornerRadius(10 * getResources().getDisplayMetrics().density);
        gd.setColor(bgColor);
        gd.setStroke((int) (1 * getResources().getDisplayMetrics().density), strokeColor);
        return gd;
    }

    @Override
    public void onBackPressed() {
        if (!isSplashDismissed && layoutSplashScreen != null && layoutSplashScreen.getVisibility() == View.VISIBLE) {
            dismissSplashScreen();
            return;
        }

        if (profileController != null && profileController.isSidebarOpen()) {
            profileController.closeSidebar(null);
            return;
        }
        if (scrollAdmin != null && scrollAdmin.getVisibility() == View.VISIBLE) {
            openMainMenu();
        } else if (scrollSettings != null && scrollSettings.getVisibility() == View.VISIBLE) {
            openProfileScreen();
        } else if (scrollProfile != null && scrollProfile.getVisibility() == View.VISIBLE) {
            openMainMenu();
        } else if (layoutHowToPlay != null && layoutHowToPlay.getVisibility() == View.VISIBLE) {
            openMainMenu();
        } else if (layoutQuiz != null && layoutQuiz.getVisibility() == View.VISIBLE) {
            cancelQuestionTimer();
            openMainMenu();
        } else if (layoutSummary != null && layoutSummary.getVisibility() == View.VISIBLE) {
            openMainMenu();
        } else if (layoutMenu != null && layoutMenu.getVisibility() == View.VISIBLE) {
            logoutUser();
        } else {
            super.onBackPressed();
        }
    }
}


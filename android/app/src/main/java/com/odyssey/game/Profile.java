package com.odyssey.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulated Profile Controller for Code Odyssey
 * Features:
 * - TikTok-Style Matte Dark Profile
 * - Custom Profile Photo Picking & Cloud Sync (Firestore + SharedPreferences + Local Storage)
 * - Photo Removal & Reset
 * - Persistent Achievements Tracking (Firestore + SharedPreferences)
 * - TikTok-Style Right Sliding Sidebar Menu & Settings & Privacy Page
 */
public class Profile {

    public static class AchievementItem {
        public final String id;
        public final String icon;
        public final String title;
        public final String desc;

        public AchievementItem(String id, String icon, String title, String desc) {
            this.id = id;
            this.icon = icon;
            this.title = title;
            this.desc = desc;
        }
    }

    public static final List<AchievementItem> ALL_ACHIEVEMENTS = Arrays.asList(
            new AchievementItem("first_win", "🏆", "CSS Pioneer", "Win your first CSS Quiz match"),
            new AchievementItem("streak_3", "🔥", "Code On Fire", "Achieve a 3-game win streak"),
            new AchievementItem("streak_5", "⚡", "CSS Dominator", "Achieve a 5-game win streak"),
            new AchievementItem("streak_10", "🌟", "Legendary Odyssey", "Achieve a 10-game win streak"),
            new AchievementItem("admin_honor", "🛡️", "Command Authority", "Hold Odyssey Administrator privileges")
    );

    private final MainActivity activity;

    // Profile Screen Views
    private View scrollProfile;
    private TextView tvProfileTopName;
    private NeonAvatarView avatar;
    private NeonTutorialButton btnProfileUploadPhoto;
    private NeonTutorialButton btnProfileRemovePhoto;
    private PlayerStatsCard statsCard;
    private NeonInfoCard cardProfileAuthority;
    private NeonInfoCard cardAchPioneer;
    private NeonInfoCard cardAchFire;
    private NeonInfoCard cardAchDominator;
    private NeonInfoCard cardAchLegendary;
    private NeonInfoCard cardAchAdmin;
    private NeonQuizButton btnProfileStartQuiz;
    private TextView tvAchievementsHeader;
    private LinearLayout containerAchievements;
    private ImageView ivProfileMenu;

    // Sidebar Drawer Views
    private View layoutSidebarDrawer;
    private View viewSidebarScrim;
    private View layoutSidebarPanel;
    private View btnSidebarClose;
    private View btnSidebarQuiz;
    private View btnSidebarAdmin;
    private View btnSidebarSettings;
    private View btnSidebarLogout;

    // Full-Screen Settings Page Views
    private View scrollSettings;
    private View btnSettingsBack;
    private ImageView ivSettingsMusicIcon;
    private TextView tvSettingsMusicStatus;
    private MaterialButton btnSettingsMusicToggle;
    private TextView tvSettingsHapticsStatus;
    private MaterialButton btnSettingsHapticsToggle;
    private TextView tvSettingsUserPresence;
    private MaterialButton btnSettingsLogout;

    private ActivityResultLauncher<String> photoPickerLauncher;

    public Profile(MainActivity activity) {
        this.activity = activity;
    }

    public void init() {
        // Register image picker launcher
        photoPickerLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && activity.getCurrentAccount() != null) {
                        saveAvatar(uri);
                    }
                }
        );

        // Bind Profile Views
        scrollProfile = activity.findViewById(R.id.scrollProfile);
        tvProfileTopName = activity.findViewById(R.id.tvProfileTopName);
        avatar = activity.findViewById(R.id.avatar);
        btnProfileUploadPhoto = activity.findViewById(R.id.btnProfileUploadPhoto);
        btnProfileRemovePhoto = activity.findViewById(R.id.btnProfileRemovePhoto);
        statsCard = activity.findViewById(R.id.statsCard);
        cardProfileAuthority = activity.findViewById(R.id.cardProfileAuthority);

        cardAchPioneer = activity.findViewById(R.id.cardAchPioneer);
        cardAchFire = activity.findViewById(R.id.cardAchFire);
        cardAchDominator = activity.findViewById(R.id.cardAchDominator);
        cardAchLegendary = activity.findViewById(R.id.cardAchLegendary);
        cardAchAdmin = activity.findViewById(R.id.cardAchAdmin);

        btnProfileStartQuiz = activity.findViewById(R.id.btnProfileStartQuiz);
        tvAchievementsHeader = activity.findViewById(R.id.tvAchievementsHeader);
        containerAchievements = activity.findViewById(R.id.containerAchievements);
        ivProfileMenu = activity.findViewById(R.id.ivProfileMenu);

        // Configure Profile Buttons & Cards
        if (btnProfileUploadPhoto != null) {
            btnProfileUploadPhoto.setText("CHANGE PHOTO");
            btnProfileUploadPhoto.setGlyph("+");
            btnProfileUploadPhoto.hideHearts();
            btnProfileUploadPhoto.setVariant(NeonTutorialButton.Variant.CYAN);
        }
        if (btnProfileRemovePhoto != null) {
            btnProfileRemovePhoto.setText("REMOVE");
            btnProfileRemovePhoto.setGlyph("\u00D7");
            btnProfileRemovePhoto.hideHearts();
            btnProfileRemovePhoto.setVariant(NeonTutorialButton.Variant.RED);
        }
        if (cardProfileAuthority != null) {
            cardProfileAuthority.setVariant(NeonInfoCard.Variant.GOLD);
            cardProfileAuthority.setBadge(null, null);
            cardProfileAuthority.setTitle("Account Authority");
            cardProfileAuthority.setIconEmoji("\uD83D\uDEE1");
        }
        setupAchievementCards();

        // Bind Sidebar Views
        layoutSidebarDrawer = activity.findViewById(R.id.layoutSidebarDrawer);
        viewSidebarScrim = activity.findViewById(R.id.viewSidebarScrim);
        layoutSidebarPanel = activity.findViewById(R.id.layoutSidebarPanel);
        btnSidebarClose = activity.findViewById(R.id.btnSidebarClose);
        btnSidebarQuiz = activity.findViewById(R.id.btnSidebarQuiz);
        btnSidebarAdmin = activity.findViewById(R.id.btnSidebarAdmin);
        btnSidebarSettings = activity.findViewById(R.id.btnSidebarSettings);
        btnSidebarLogout = activity.findViewById(R.id.btnSidebarLogout);

        // Bind Settings Views
        scrollSettings = activity.findViewById(R.id.scrollSettings);
        btnSettingsBack = activity.findViewById(R.id.btnSettingsBack);
        ivSettingsMusicIcon = activity.findViewById(R.id.ivSettingsMusicIcon);
        tvSettingsMusicStatus = activity.findViewById(R.id.tvSettingsMusicStatus);
        btnSettingsMusicToggle = activity.findViewById(R.id.btnSettingsMusicToggle);
        tvSettingsHapticsStatus = activity.findViewById(R.id.tvSettingsHapticsStatus);
        btnSettingsHapticsToggle = activity.findViewById(R.id.btnSettingsHapticsToggle);
        tvSettingsUserPresence = activity.findViewById(R.id.tvSettingsUserPresence);
        btnSettingsLogout = activity.findViewById(R.id.btnSettingsLogout);

        setupListeners();
    }

    private void setupAchievementCards() {
        if (cardAchPioneer != null) {
            cardAchPioneer.setIconEmoji("🏆");
            cardAchPioneer.setTitle("CSS Pioneer");
            cardAchPioneer.setSubtitle("Win your first CSS Quiz match");
            cardAchPioneer.setBadge("UNLOCKED", "LOCKED");
        }
        if (cardAchFire != null) {
            cardAchFire.setIconEmoji("🔥");
            cardAchFire.setTitle("Code On Fire");
            cardAchFire.setSubtitle("Achieve a 3-game win streak");
            cardAchFire.setBadge("UNLOCKED", "LOCKED");
        }
        if (cardAchDominator != null) {
            cardAchDominator.setIconEmoji("⚡");
            cardAchDominator.setTitle("CSS Dominator");
            cardAchDominator.setSubtitle("Achieve a 5-game win streak");
            cardAchDominator.setBadge("UNLOCKED", "LOCKED");
        }
        if (cardAchLegendary != null) {
            cardAchLegendary.setIconEmoji("🌟");
            cardAchLegendary.setTitle("Legendary Odyssey");
            cardAchLegendary.setSubtitle("Achieve a 10-game win streak");
            cardAchLegendary.setBadge("UNLOCKED", "LOCKED");
        }
        if (cardAchAdmin != null) {
            cardAchAdmin.setIconEmoji("🛡️");
            cardAchAdmin.setTitle("Command Authority");
            cardAchAdmin.setSubtitle("Hold Odyssey Administrator privileges");
            cardAchAdmin.setBadge("UNLOCKED", "LOCKED");
        }
    }

    private void setupListeners() {
        View.OnClickListener uploadPhotoAction = v -> {
            if (photoPickerLauncher != null) {
                photoPickerLauncher.launch("image/*");
            }
        };

        if (avatar != null) {
            avatar.setOnBadgeClickListener(uploadPhotoAction);
            avatar.setOnClickListener(uploadPhotoAction);
        }
        if (btnProfileUploadPhoto != null) {
            btnProfileUploadPhoto.setOnClickListener(uploadPhotoAction);
        }
        if (btnProfileRemovePhoto != null) {
            btnProfileRemovePhoto.setOnClickListener(v -> removeAvatar());
        }

        if (btnProfileStartQuiz != null) {
            btnProfileStartQuiz.setOnClickListener(v -> {
                if (activity.isStartingQuizCutscene()) return;
                activity.setStartingQuizCutscene(true);
                QuizCutsceneView.play(activity, v, "May your CSS be clean and your answers correct.", luckyStars -> {
                    activity.setStartingQuizCutscene(false);
                    activity.promptDifficultySelection();
                });
            });
        }

        // Open Sidebar Menu when hamburger menu icon clicked
        if (ivProfileMenu != null) {
            ivProfileMenu.setOnClickListener(v -> openSidebar());
        }

        // Sidebar Actions
        if (viewSidebarScrim != null) {
            viewSidebarScrim.setOnClickListener(v -> closeSidebar(null));
        }
        if (btnSidebarClose != null) {
            btnSidebarClose.setOnClickListener(v -> closeSidebar(null));
        }
        if (btnSidebarQuiz != null) {
            btnSidebarQuiz.setOnClickListener(v -> closeSidebar(() -> {
                if (activity.isStartingQuizCutscene()) return;
                activity.setStartingQuizCutscene(true);
                QuizCutsceneView.play(activity, v, "May your CSS be clean and your answers correct.", luckyStars -> {
                    activity.setStartingQuizCutscene(false);
                    activity.promptDifficultySelection();
                });
            }));
        }
        if (btnSidebarAdmin != null) {
            btnSidebarAdmin.setOnClickListener(v -> closeSidebar(() -> activity.openAdminPanel()));
        }
        if (btnSidebarSettings != null) {
            btnSidebarSettings.setOnClickListener(v -> closeSidebar(this::openSettings));
        }
        if (btnSidebarLogout != null) {
            btnSidebarLogout.setOnClickListener(v -> closeSidebar(() -> activity.logoutUser()));
        }

        // Settings Screen Actions
        if (btnSettingsBack != null) {
            btnSettingsBack.setOnClickListener(v -> closeSettings());
        }
        if (btnSettingsMusicToggle != null) {
            btnSettingsMusicToggle.setOnClickListener(v -> activity.toggleMusic());
        }
        if (btnSettingsHapticsToggle != null) {
            btnSettingsHapticsToggle.setOnClickListener(v -> activity.toggleHaptics());
        }
        if (btnSettingsLogout != null) {
            btnSettingsLogout.setOnClickListener(v -> activity.logoutUser());
        }
    }

    public void bindUser(Account account) {
        if (account == null) return;

        if (tvProfileTopName != null) {
            tvProfileTopName.setText(account.name != null && !account.name.isEmpty() ? account.name : account.username);
        }
        if (avatar != null) {
            avatar.setInitial(account.username);
        }
        if (statsCard != null) {
            statsCard.setPlayerName(account.username);
            statsCard.setStats(account.currentWinStreak, account.bestWinStreak);
        }
        if (cardProfileAuthority != null) {
            boolean isAdmin = FirebaseHelper.isUserAdmin(account);
            boolean isSuper = "biar".equalsIgnoreCase(account.username);
            cardProfileAuthority.setSubtitle(isSuper ? "👑 Super Administrator" : (isAdmin ? "🛡️ Administrator" : "🎮 CSS Adventurer"));
        }

        // Sidebar Admin visibility
        if (btnSidebarAdmin != null) {
            boolean isAdmin = FirebaseHelper.isUserAdmin(account);
            btnSidebarAdmin.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        }

        // Load Avatar (local cache + base64 database fallback)
        loadAvatar();

        // Check & Render Achievements
        checkAndUnlockAchievements(account);
        renderAchievements(account, false);
    }

    public void render(Account account) {
        bindUser(account);
    }

    public boolean isSidebarOpen() {
        return layoutSidebarDrawer != null && layoutSidebarDrawer.getVisibility() == View.VISIBLE;
    }

    public void openSidebar() {
        if (layoutSidebarDrawer == null) return;
        layoutSidebarDrawer.setVisibility(View.VISIBLE);

        if (viewSidebarScrim != null) {
            viewSidebarScrim.setAlpha(0f);
            viewSidebarScrim.animate().alpha(1f).setDuration(220).start();
        }

        if (layoutSidebarPanel != null) {
            layoutSidebarPanel.post(() -> {
                int width = layoutSidebarPanel.getWidth() > 0 ? layoutSidebarPanel.getWidth() : 800;
                layoutSidebarPanel.setTranslationX(width);
                layoutSidebarPanel.animate()
                        .translationX(0f)
                        .setDuration(240)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            });
        }
    }

    public void closeSidebar(Runnable onClosed) {
        if (layoutSidebarDrawer == null || layoutSidebarDrawer.getVisibility() != View.VISIBLE) {
            if (onClosed != null) onClosed.run();
            return;
        }

        if (viewSidebarScrim != null) {
            viewSidebarScrim.animate().alpha(0f).setDuration(180).start();
        }

        if (layoutSidebarPanel != null) {
            int width = layoutSidebarPanel.getWidth() > 0 ? layoutSidebarPanel.getWidth() : 800;
            layoutSidebarPanel.animate()
                    .translationX(width)
                    .setDuration(180)
                    .withEndAction(() -> {
                        layoutSidebarDrawer.setVisibility(View.GONE);
                        if (onClosed != null) onClosed.run();
                    })
                    .start();
        } else {
            layoutSidebarDrawer.setVisibility(View.GONE);
            if (onClosed != null) onClosed.run();
        }
    }

    public void openSettings() {
        activity.updateMusicUi();
        activity.updateHapticsUi();
        Account current = activity.getCurrentAccount();
        if (tvSettingsUserPresence != null && current != null) {
            tvSettingsUserPresence.setText("Logged in as: " + current.name);
        }
        activity.showSettingsScreenDirect();
    }

    public void closeSettings() {
        activity.openProfileScreen();
    }

    public void saveAvatar(Uri uri) {
        Account current = activity.getCurrentAccount();
        if (current == null) return;

        try {
            InputStream is = activity.getContentResolver().openInputStream(uri);
            if (is != null) {
                Bitmap original = BitmapFactory.decodeStream(is);
                is.close();
                if (original != null) {
                    // Scale to max 256x256 thumbnail for crispness and efficient cloud storage
                    int maxDim = 256;
                    int width = original.getWidth();
                    int height = original.getHeight();
                    float ratio = Math.min((float) maxDim / width, (float) maxDim / height);
                    int newW = Math.max(1, Math.round(ratio * width));
                    int newH = Math.max(1, Math.round(ratio * height));
                    Bitmap scaled = Bitmap.createScaledBitmap(original, newW, newH, true);

                    // 1. Save local file
                    File avatarFile = new File(activity.getFilesDir(), "avatar_" + current.username + ".png");
                    FileOutputStream fos = new FileOutputStream(avatarFile);
                    scaled.compress(Bitmap.CompressFormat.PNG, 90, fos);
                    fos.close();

                    // 2. Convert to Base64 String
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                    byte[] bytes = baos.toByteArray();
                    String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);

                    // 3. Save to SharedPreferences
                    SharedPreferences prefs = activity.getSharedPreferences("odyssey_prefs", Context.MODE_PRIVATE);
                    prefs.edit().putString("avatar_base64_" + current.username, base64).apply();

                    // 4. Sync to Firestore Database
                    current.avatarBase64 = base64;
                    FirebaseHelper.saveAvatar(current.username, base64, null);

                    loadAvatar();
                    Toast.makeText(activity, "Profile photo updated & synced!", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(activity, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void removeAvatar() {
        Account current = activity.getCurrentAccount();
        if (current == null) return;

        try {
            File avatarFile = new File(activity.getFilesDir(), "avatar_" + current.username + ".png");
            if (avatarFile.exists()) {
                avatarFile.delete();
            }
            SharedPreferences prefs = activity.getSharedPreferences("odyssey_prefs", Context.MODE_PRIVATE);
            prefs.edit().remove("avatar_base64_" + current.username).apply();
            current.avatarBase64 = "";
            FirebaseHelper.saveAvatar(current.username, "", null);
        } catch (Exception ignored) {}

        if (avatar != null) {
            avatar.setImageBitmap(null);
        }
        if (btnProfileRemovePhoto != null) {
            btnProfileRemovePhoto.setVisibility(View.GONE);
        }
        Toast.makeText(activity, "Profile photo removed", Toast.LENGTH_SHORT).show();
    }

    public void loadAvatar() {
        Account current = activity.getCurrentAccount();
        if (current == null) return;

        if (avatar != null) {
            avatar.setInitial(current.username);
        }

        Bitmap customBitmap = null;

        // 1. Check local file
        try {
            File avatarFile = new File(activity.getFilesDir(), "avatar_" + current.username + ".png");
            if (avatarFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(avatarFile.getAbsolutePath());
                if (bitmap != null) {
                    customBitmap = bitmap;
                }
            }
        } catch (Exception ignored) {}

        // 2. If no local file, check SharedPreferences or Firestore base64
        if (customBitmap == null) {
            SharedPreferences prefs = activity.getSharedPreferences("odyssey_prefs", Context.MODE_PRIVATE);
            String base64 = prefs.getString("avatar_base64_" + current.username, current.avatarBase64);
            if (base64 != null && !base64.trim().isEmpty()) {
                try {
                    byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bitmap != null) {
                        customBitmap = bitmap;

                        // Save locally for future instant loading
                        File avatarFile = new File(activity.getFilesDir(), "avatar_" + current.username + ".png");
                        FileOutputStream fos = new FileOutputStream(avatarFile);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                        fos.close();
                    }
                } catch (Exception ignored) {}
            }
        }

        if (avatar != null) {
            avatar.setImageBitmap(customBitmap);
        }

        if (btnProfileRemovePhoto != null) {
            btnProfileRemovePhoto.setVisibility(customBitmap != null ? View.VISIBLE : View.GONE);
        }
    }

    public void checkAndUnlockAchievements(Account account) {
        if (account == null) return;

        SharedPreferences prefs = activity.getSharedPreferences("odyssey_prefs", Context.MODE_PRIVATE);
        String cachedStr = prefs.getString("achievements_" + account.username, "");
        Set<String> set = new HashSet<>(account.achievements);
        if (!cachedStr.isEmpty()) {
            set.addAll(Arrays.asList(cachedStr.split(",")));
        }

        boolean newlyUnlocked = false;

        if (account.currentWinStreak >= 1 || account.bestWinStreak >= 1) {
            if (set.add("first_win")) newlyUnlocked = true;
        }
        if (account.currentWinStreak >= 3 || account.bestWinStreak >= 3) {
            if (set.add("streak_3")) newlyUnlocked = true;
        }
        if (account.currentWinStreak >= 5 || account.bestWinStreak >= 5) {
            if (set.add("streak_5")) newlyUnlocked = true;
        }
        if (account.currentWinStreak >= 10 || account.bestWinStreak >= 10) {
            if (set.add("streak_10")) newlyUnlocked = true;
        }
        if (FirebaseHelper.isUserAdmin(account)) {
            if (set.add("admin_honor")) newlyUnlocked = true;
        }

        if (newlyUnlocked || account.achievements.size() != set.size()) {
            account.achievements = new ArrayList<>(set);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < account.achievements.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(account.achievements.get(i));
            }
            prefs.edit().putString("achievements_" + account.username, sb.toString()).apply();
            FirebaseHelper.saveAchievements(account.username, account.achievements, null);
            renderAchievements(account, true);
        }
    }

    public void renderAchievements(Account account) {
        renderAchievements(account, false);
    }

    public void renderAchievements(Account account, boolean animateUnlocks) {
        if (account == null) return;

        Set<String> unlockedSet = new HashSet<>(account.achievements);
        int unlockedCount = 0;

        if (cardAchPioneer != null) {
            boolean unlocked = unlockedSet.contains("first_win");
            if (unlocked) unlockedCount++;
            boolean animate = animateUnlocks && unlocked && !cardAchPioneer.isUnlocked();
            cardAchPioneer.setUnlocked(unlocked, animate);
        }
        if (cardAchFire != null) {
            boolean unlocked = unlockedSet.contains("streak_3");
            if (unlocked) unlockedCount++;
            boolean animate = animateUnlocks && unlocked && !cardAchFire.isUnlocked();
            cardAchFire.setUnlocked(unlocked, animate);
        }
        if (cardAchDominator != null) {
            boolean unlocked = unlockedSet.contains("streak_5");
            if (unlocked) unlockedCount++;
            boolean animate = animateUnlocks && unlocked && !cardAchDominator.isUnlocked();
            cardAchDominator.setUnlocked(unlocked, animate);
        }
        if (cardAchLegendary != null) {
            boolean unlocked = unlockedSet.contains("streak_10");
            if (unlocked) unlockedCount++;
            boolean animate = animateUnlocks && unlocked && !cardAchLegendary.isUnlocked();
            cardAchLegendary.setUnlocked(unlocked, animate);
        }
        if (cardAchAdmin != null) {
            boolean unlocked = unlockedSet.contains("admin_honor");
            if (unlocked) unlockedCount++;
            boolean animate = animateUnlocks && unlocked && !cardAchAdmin.isUnlocked();
            cardAchAdmin.setUnlocked(unlocked, animate);
        }

        if (tvAchievementsHeader != null) {
            tvAchievementsHeader.setText("🏆 ACHIEVEMENTS (" + unlockedCount + "/" + ALL_ACHIEVEMENTS.size() + ")");
        }
    }
}

package com.odyssey.game;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Encapsulated Admin Panel Controller for Code Odyssey
 * Features:
 * - TikTok Profile Matching Matte Dark Styling (No Live BG, Zero Distractions)
 * - Dual Segmented Controls: [ Users Directory ] and [ Quiz Manager ]
 * - Real-time User Directory & Online/Offline Presence Badges
 * - Admin Promotion & Privilege Revocation
 * - Secure Player Account Deletion
 * - Full CSS Quiz Question Management (Only Admins Can Add/Delete Questions & Set Verified Correct Answers)
 */
public class AdminPanel {

    private final MainActivity activity;

    // Admin Views
    private View scrollAdmin;
    private MaterialButton btnAdminBackTop;
    private MaterialButton btnAdminRefresh;

    // Tab Navigation
    private MaterialButton btnTabAdminUsers;
    private MaterialButton btnTabAdminQuiz;
    private View layoutAdminUsersSection;
    private View layoutAdminQuizSection;
    private boolean isQuizTabActive = false;

    // Users Tab Views
    private MaterialButton btnAdminAddSubmit;
    private TextInputEditText etAdminAddUser;
    private TextView tvAdminActiveUser;
    private TextView tvAdminTotalUsers;
    private TextView tvAdminTotalAdmins;
    private TextView tvAdminTotalLogins;
    private TextView tvAdminUserListHeader;
    private ProgressBar pbAdminLoading;
    private LinearLayout containerAdminUsers;

    // Quiz Management Tab Views
    private TextView tvAdminQuizEasyCount;
    private TextView tvAdminQuizMedCount;
    private TextView tvAdminQuizHardCount;
    private TextView tvAdminQuizCustomCount;

    private MaterialButton btnDiffEasy;
    private MaterialButton btnDiffMedium;
    private MaterialButton btnDiffHard;
    private String selectedDifficulty = "easy";

    private TextInputEditText etNewQuestionText;
    private TextInputEditText etNewOptionA;
    private TextInputEditText etNewOptionB;
    private TextInputEditText etNewOptionC;
    private TextInputEditText etNewOptionD;

    private MaterialButton btnCorrectA;
    private MaterialButton btnCorrectB;
    private MaterialButton btnCorrectC;
    private MaterialButton btnCorrectD;
    private TextView tvSelectedCorrectInfo;
    private int selectedCorrectIndex = 0; // 0=A, 1=B, 2=C, 3=D

    private MaterialButton btnSubmitNewQuestion;
    private TextView tvAdminQuizListHeader;
    private ProgressBar pbAdminQuizLoading;
    private LinearLayout containerAdminQuestions;

    // Quiz Timer Views
    private TextView tvAdminCurrentTimerBadge;
    private MaterialButton btnTimer10s;
    private MaterialButton btnTimer15s;
    private MaterialButton btnTimer20s;
    private MaterialButton btnTimer30s;
    private TextInputEditText etAdminCustomTimer;
    private MaterialButton btnAdminSaveTimer;
    private ProgressBar pbAdminTimerSaving;

    public AdminPanel(MainActivity activity) {
        this.activity = activity;
    }

    public void init() {
        scrollAdmin = activity.findViewById(R.id.scrollAdmin);
        btnAdminBackTop = activity.findViewById(R.id.btnAdminBackTop);
        btnAdminRefresh = activity.findViewById(R.id.btnAdminRefresh);

        // Tabs
        btnTabAdminUsers = activity.findViewById(R.id.btnTabAdminUsers);
        btnTabAdminQuiz = activity.findViewById(R.id.btnTabAdminQuiz);
        layoutAdminUsersSection = activity.findViewById(R.id.layoutAdminUsersSection);
        layoutAdminQuizSection = activity.findViewById(R.id.layoutAdminQuizSection);

        // Users Tab
        btnAdminAddSubmit = activity.findViewById(R.id.btnAdminAddSubmit);
        etAdminAddUser = activity.findViewById(R.id.etAdminAddUser);
        tvAdminActiveUser = activity.findViewById(R.id.tvAdminActiveUser);
        tvAdminTotalUsers = activity.findViewById(R.id.tvAdminTotalUsers);
        tvAdminTotalAdmins = activity.findViewById(R.id.tvAdminTotalAdmins);
        tvAdminTotalLogins = activity.findViewById(R.id.tvAdminTotalLogins);
        tvAdminUserListHeader = activity.findViewById(R.id.tvAdminUserListHeader);
        pbAdminLoading = activity.findViewById(R.id.pbAdminLoading);
        containerAdminUsers = activity.findViewById(R.id.containerAdminUsers);

        // Quiz Management Tab
        tvAdminQuizEasyCount = activity.findViewById(R.id.tvAdminQuizEasyCount);
        tvAdminQuizMedCount = activity.findViewById(R.id.tvAdminQuizMedCount);
        tvAdminQuizHardCount = activity.findViewById(R.id.tvAdminQuizHardCount);
        tvAdminQuizCustomCount = activity.findViewById(R.id.tvAdminQuizCustomCount);

        btnDiffEasy = activity.findViewById(R.id.btnDiffEasy);
        btnDiffMedium = activity.findViewById(R.id.btnDiffMedium);
        btnDiffHard = activity.findViewById(R.id.btnDiffHard);

        etNewQuestionText = activity.findViewById(R.id.etNewQuestionText);
        etNewOptionA = activity.findViewById(R.id.etNewOptionA);
        etNewOptionB = activity.findViewById(R.id.etNewOptionB);
        etNewOptionC = activity.findViewById(R.id.etNewOptionC);
        etNewOptionD = activity.findViewById(R.id.etNewOptionD);

        btnCorrectA = activity.findViewById(R.id.btnCorrectA);
        btnCorrectB = activity.findViewById(R.id.btnCorrectB);
        btnCorrectC = activity.findViewById(R.id.btnCorrectC);
        btnCorrectD = activity.findViewById(R.id.btnCorrectD);
        tvSelectedCorrectInfo = activity.findViewById(R.id.tvSelectedCorrectInfo);

        btnSubmitNewQuestion = activity.findViewById(R.id.btnSubmitNewQuestion);
        tvAdminQuizListHeader = activity.findViewById(R.id.tvAdminQuizListHeader);
        pbAdminQuizLoading = activity.findViewById(R.id.pbAdminQuizLoading);
        containerAdminQuestions = activity.findViewById(R.id.containerAdminQuestions);

        // Quiz Timer
        tvAdminCurrentTimerBadge = activity.findViewById(R.id.tvAdminCurrentTimerBadge);
        btnTimer10s = activity.findViewById(R.id.btnTimer10s);
        btnTimer15s = activity.findViewById(R.id.btnTimer15s);
        btnTimer20s = activity.findViewById(R.id.btnTimer20s);
        btnTimer30s = activity.findViewById(R.id.btnTimer30s);
        etAdminCustomTimer = activity.findViewById(R.id.etAdminCustomTimer);
        btnAdminSaveTimer = activity.findViewById(R.id.btnAdminSaveTimer);
        pbAdminTimerSaving = activity.findViewById(R.id.pbAdminTimerSaving);

        setupListeners();
        updateDifficultySelection("easy");
        updateCorrectAnswerSelection(0);
    }

    private void setupListeners() {
        if (btnAdminBackTop != null) {
            btnAdminBackTop.setOnClickListener(v -> activity.animateClick(btnAdminBackTop, () -> activity.openMainMenu()));
        }
        if (btnAdminRefresh != null) {
            btnAdminRefresh.setOnClickListener(v -> activity.animateClick(btnAdminRefresh, () -> {
                if (isQuizTabActive) {
                    loadQuizQuestionsData();
                } else {
                    loadData();
                }
            }));
        }

        // Tab Switching
        if (btnTabAdminUsers != null) {
            btnTabAdminUsers.setOnClickListener(v -> activity.animateClick(btnTabAdminUsers, () -> switchTab(false)));
        }
        if (btnTabAdminQuiz != null) {
            btnTabAdminQuiz.setOnClickListener(v -> activity.animateClick(btnTabAdminQuiz, () -> switchTab(true)));
        }

        // Users Tab
        if (btnAdminAddSubmit != null) {
            btnAdminAddSubmit.setOnClickListener(v -> activity.animateClick(btnAdminAddSubmit, this::handlePromoteNewAdmin));
        }

        // Quiz Difficulty Selector
        if (btnDiffEasy != null) {
            btnDiffEasy.setOnClickListener(v -> activity.animateClick(btnDiffEasy, () -> updateDifficultySelection("easy")));
        }
        if (btnDiffMedium != null) {
            btnDiffMedium.setOnClickListener(v -> activity.animateClick(btnDiffMedium, () -> updateDifficultySelection("medium")));
        }
        if (btnDiffHard != null) {
            btnDiffHard.setOnClickListener(v -> activity.animateClick(btnDiffHard, () -> updateDifficultySelection("hard")));
        }

        // Correct Choice Selector
        if (btnCorrectA != null) {
            btnCorrectA.setOnClickListener(v -> activity.animateClick(btnCorrectA, () -> updateCorrectAnswerSelection(0)));
        }
        if (btnCorrectB != null) {
            btnCorrectB.setOnClickListener(v -> activity.animateClick(btnCorrectB, () -> updateCorrectAnswerSelection(1)));
        }
        if (btnCorrectC != null) {
            btnCorrectC.setOnClickListener(v -> activity.animateClick(btnCorrectC, () -> updateCorrectAnswerSelection(2)));
        }
        if (btnCorrectD != null) {
            btnCorrectD.setOnClickListener(v -> activity.animateClick(btnCorrectD, () -> updateCorrectAnswerSelection(3)));
        }

        // Submit New Question
        if (btnSubmitNewQuestion != null) {
            btnSubmitNewQuestion.setOnClickListener(v -> activity.animateClick(btnSubmitNewQuestion, this::handleSubmitNewQuestion));
        }

        // Quiz Timer Presets & Custom
        if (btnTimer10s != null) {
            btnTimer10s.setOnClickListener(v -> activity.animateClick(btnTimer10s, () -> saveQuizTimer(10)));
        }
        if (btnTimer15s != null) {
            btnTimer15s.setOnClickListener(v -> activity.animateClick(btnTimer15s, () -> saveQuizTimer(15)));
        }
        if (btnTimer20s != null) {
            btnTimer20s.setOnClickListener(v -> activity.animateClick(btnTimer20s, () -> saveQuizTimer(20)));
        }
        if (btnTimer30s != null) {
            btnTimer30s.setOnClickListener(v -> activity.animateClick(btnTimer30s, () -> saveQuizTimer(30)));
        }
        if (btnAdminSaveTimer != null) {
            btnAdminSaveTimer.setOnClickListener(v -> activity.animateClick(btnAdminSaveTimer, () -> {
                if (etAdminCustomTimer == null || etAdminCustomTimer.getText() == null) return;
                String val = etAdminCustomTimer.getText().toString().trim();
                if (val.isEmpty()) {
                    Toast.makeText(activity, "Please enter seconds (5-300)", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    int sec = Integer.parseInt(val);
                    if (sec < 5 || sec > 300) {
                        Toast.makeText(activity, "Please choose duration between 5 and 300 seconds", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveQuizTimer(sec);
                } catch (Exception e) {
                    Toast.makeText(activity, "Invalid number entered", Toast.LENGTH_SHORT).show();
                }
            }));
        }
    }

    private void saveQuizTimer(int seconds) {
        Account current = activity.getCurrentAccount();
        if (!FirebaseHelper.isUserAdmin(current)) {
            activity.showErrorAlert("Access Denied", "Only administrators can configure the quiz timer.");
            return;
        }

        if (pbAdminTimerSaving != null) pbAdminTimerSaving.setVisibility(View.VISIBLE);

        FirebaseHelper.saveQuizTimerSeconds(current, seconds, new FirebaseHelper.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (pbAdminTimerSaving != null) pbAdminTimerSaving.setVisibility(View.GONE);
                activity.setQuestionTimeLimitSeconds(seconds);
                updateTimerUi(seconds);
                if (etAdminCustomTimer != null) etAdminCustomTimer.setText("");
                activity.showSuccessAlert("Quiz Timer Updated! ⏱️",
                        "Question countdown timer has been set to " + seconds + " seconds.\n"
                                + "All players will now receive " + seconds + " seconds per question.",
                        "EXCELLENT", null);
            }

            @Override
            public void onError(String error) {
                if (pbAdminTimerSaving != null) pbAdminTimerSaving.setVisibility(View.GONE);
                activity.showErrorAlert("Failed to Save Timer", error);
            }
        });
    }

    public void updateTimerUi(int seconds) {
        if (tvAdminCurrentTimerBadge != null) {
            tvAdminCurrentTimerBadge.setText(seconds + "s / Question");
        }

        int colorActiveBg = activity.getColor(R.color.cyan);
        int colorActiveText = Color.parseColor("#000000");
        int colorInactiveBg = Color.parseColor("#1F1F2E");
        int colorInactiveText = Color.parseColor("#FFFFFF");

        if (btnTimer10s != null) {
            boolean isThis = (seconds == 10);
            btnTimer10s.setBackgroundTintList(ColorStateList.valueOf(isThis ? colorActiveBg : colorInactiveBg));
            btnTimer10s.setTextColor(isThis ? colorActiveText : colorInactiveText);
        }
        if (btnTimer15s != null) {
            boolean isThis = (seconds == 15);
            btnTimer15s.setBackgroundTintList(ColorStateList.valueOf(isThis ? colorActiveBg : colorInactiveBg));
            btnTimer15s.setTextColor(isThis ? colorActiveText : colorInactiveText);
        }
        if (btnTimer20s != null) {
            boolean isThis = (seconds == 20);
            btnTimer20s.setBackgroundTintList(ColorStateList.valueOf(isThis ? colorActiveBg : colorInactiveBg));
            btnTimer20s.setTextColor(isThis ? colorActiveText : colorInactiveText);
        }
        if (btnTimer30s != null) {
            boolean isThis = (seconds == 30);
            btnTimer30s.setBackgroundTintList(ColorStateList.valueOf(isThis ? colorActiveBg : colorInactiveBg));
            btnTimer30s.setTextColor(isThis ? colorActiveText : colorInactiveText);
        }
    }

    public void open() {
        Account current = activity.getCurrentAccount();
        if (!FirebaseHelper.isUserAdmin(current)) {
            activity.showErrorAlert("Access Denied", "Only administrators are permitted to view the Admin Control Panel.");
            return;
        }

        if (tvAdminActiveUser != null && current != null) {
            boolean isSuper = "biar".equalsIgnoreCase(current.username);
            tvAdminActiveUser.setText("Logged in as: " + current.name + (isSuper ? " (👑 Super Admin)" : " (🛡️ Admin)"));
        }

        activity.showAdminScreenDirect();
        updateDifficultySelection("easy");
        updateCorrectAnswerSelection(0);
        switchTab(false);
        loadData();
        loadQuizQuestionsData();
    }

    private void switchTab(boolean showQuiz) {
        isQuizTabActive = showQuiz;

        if (layoutAdminUsersSection != null) {
            layoutAdminUsersSection.setVisibility(showQuiz ? View.GONE : View.VISIBLE);
        }
        if (layoutAdminQuizSection != null) {
            layoutAdminQuizSection.setVisibility(showQuiz ? View.VISIBLE : View.GONE);
        }

        if (btnTabAdminUsers != null) {
            btnTabAdminUsers.setBackgroundTintList(ColorStateList.valueOf(showQuiz ? Color.TRANSPARENT : Color.parseColor("#F59E0B")));
            btnTabAdminUsers.setTextColor(showQuiz ? activity.getColor(R.color.text_muted) : Color.parseColor("#000000"));
        }

        if (btnTabAdminQuiz != null) {
            btnTabAdminQuiz.setBackgroundTintList(ColorStateList.valueOf(showQuiz ? Color.parseColor("#F59E0B") : Color.TRANSPARENT));
            btnTabAdminQuiz.setTextColor(showQuiz ? Color.parseColor("#000000") : activity.getColor(R.color.text_muted));
        }

        if (showQuiz) {
            loadQuizQuestionsData();
        }
    }

    private void updateDifficultySelection(String diff) {
        selectedDifficulty = diff;
        int activeColor = "hard".equals(diff) ? Color.parseColor("#F59E0B") : ("medium".equals(diff) ? Color.parseColor("#8B5CF6") : Color.parseColor("#06B6D4"));

        if (btnDiffEasy != null) {
            boolean isSel = "easy".equals(diff);
            btnDiffEasy.setBackgroundTintList(ColorStateList.valueOf(isSel ? activeColor : Color.TRANSPARENT));
            btnDiffEasy.setTextColor(isSel ? Color.BLACK : activity.getColor(R.color.text_muted));
        }
        if (btnDiffMedium != null) {
            boolean isSel = "medium".equals(diff);
            btnDiffMedium.setBackgroundTintList(ColorStateList.valueOf(isSel ? activeColor : Color.TRANSPARENT));
            btnDiffMedium.setTextColor(isSel ? Color.WHITE : activity.getColor(R.color.text_muted));
        }
        if (btnDiffHard != null) {
            boolean isSel = "hard".equals(diff);
            btnDiffHard.setBackgroundTintList(ColorStateList.valueOf(isSel ? activeColor : Color.TRANSPARENT));
            btnDiffHard.setTextColor(isSel ? Color.BLACK : activity.getColor(R.color.text_muted));
        }
    }

    private void updateCorrectAnswerSelection(int choiceIdx) {
        selectedCorrectIndex = choiceIdx;
        MaterialButton[] buttons = new MaterialButton[]{btnCorrectA, btnCorrectB, btnCorrectC, btnCorrectD};
        char letter = (char) ('A' + choiceIdx);

        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] != null) {
                if (i == choiceIdx) {
                    buttons[i].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#10B981")));
                    buttons[i].setTextColor(Color.parseColor("#000000"));
                    buttons[i].setStrokeWidth(0);
                } else {
                    buttons[i].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1F1F2E")));
                    buttons[i].setTextColor(Color.parseColor("#FFFFFF"));
                    buttons[i].setStrokeColor(ColorStateList.valueOf(Color.parseColor("#3D3D52")));
                    buttons[i].setStrokeWidth((int) (1 * activity.getResources().getDisplayMetrics().density));
                }
            }
        }

        if (tvSelectedCorrectInfo != null) {
            tvSelectedCorrectInfo.setText("✓ Choice [" + letter + "] will be evaluated as the 100% correct answer.");
        }
    }

    private void handleSubmitNewQuestion() {
        Account current = activity.getCurrentAccount();
        if (!FirebaseHelper.isUserAdmin(current)) {
            activity.showErrorAlert("Access Denied", "Security check failed: Only administrators can publish quiz questions.");
            return;
        }

        if (etNewQuestionText == null || etNewOptionA == null || etNewOptionB == null || etNewOptionC == null || etNewOptionD == null) {
            return;
        }

        String qText = etNewQuestionText.getText() != null ? etNewQuestionText.getText().toString().trim() : "";
        String optA = etNewOptionA.getText() != null ? etNewOptionA.getText().toString().trim() : "";
        String optB = etNewOptionB.getText() != null ? etNewOptionB.getText().toString().trim() : "";
        String optC = etNewOptionC.getText() != null ? etNewOptionC.getText().toString().trim() : "";
        String optD = etNewOptionD.getText() != null ? etNewOptionD.getText().toString().trim() : "";

        if (qText.length() < 5) {
            activity.showErrorAlert("Invalid Question", "Please enter a complete CSS question prompt (at least 5 characters).");
            return;
        }

        if (optA.isEmpty() || optB.isEmpty() || optC.isEmpty() || optD.isEmpty()) {
            activity.showErrorAlert("Incomplete Choices", "All 4 choices (A, B, C, D) must be provided so players have full options.");
            return;
        }

        if (optA.equalsIgnoreCase(optB) || optA.equalsIgnoreCase(optC) || optA.equalsIgnoreCase(optD)
                || optB.equalsIgnoreCase(optC) || optB.equalsIgnoreCase(optD) || optC.equalsIgnoreCase(optD)) {
            activity.showErrorAlert("Duplicate Choices", "All 4 choices must be distinct to ensure accurate test evaluation.");
            return;
        }

        if (selectedCorrectIndex < 0 || selectedCorrectIndex > 3) {
            activity.showErrorAlert("Select Correct Answer", "Please tap A, B, C, or D to specify which choice is correct.");
            return;
        }

        char correctChar = (char) ('A' + selectedCorrectIndex);
        String questionId = "q_" + System.currentTimeMillis() + "_" + current.username.toLowerCase();
        List<String> options = Arrays.asList(optA, optB, optC, optD);
        Question newQ = new Question(questionId, qText, options, selectedCorrectIndex, selectedDifficulty, current.username, true, System.currentTimeMillis());

        if (pbAdminQuizLoading != null) pbAdminQuizLoading.setVisibility(View.VISIBLE);

        FirebaseHelper.saveCustomQuestion(current, newQ, new FirebaseHelper.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (pbAdminQuizLoading != null) pbAdminQuizLoading.setVisibility(View.GONE);

                // Clear input fields
                etNewQuestionText.setText("");
                etNewOptionA.setText("");
                etNewOptionB.setText("");
                etNewOptionC.setText("");
                etNewOptionD.setText("");

                // Add to live game memory bank
                activity.addCustomQuestionToBank(newQ);
                loadQuizQuestionsData();

                activity.showSuccessAlert("Question Published! 🚀",
                        "Successfully added new question to the " + selectedDifficulty.toUpperCase() + " CSS Quiz Bank!\n\n"
                                + "Correct Answer: Option " + correctChar,
                        "AWESOME", null);
            }

            @Override
            public void onError(String error) {
                if (pbAdminQuizLoading != null) pbAdminQuizLoading.setVisibility(View.GONE);
                activity.showErrorAlert("Failed to Publish", error);
            }
        });
    }

    public void loadQuizQuestionsData() {
        updateTimerUi(activity.getQuestionTimeLimitSeconds());

        // Sync timer from Firestore
        FirebaseHelper.getQuizTimerSeconds(new FirebaseHelper.Callback<Integer>() {
            @Override
            public void onSuccess(Integer seconds) {
                if (seconds != null) {
                    activity.setQuestionTimeLimitSeconds(seconds);
                    updateTimerUi(seconds);
                }
            }

            @Override
            public void onError(String error) {}
        });

        if (pbAdminQuizLoading != null) pbAdminQuizLoading.setVisibility(View.VISIBLE);

        FirebaseHelper.getAllCustomQuestions(new FirebaseHelper.Callback<List<Question>>() {
            @Override
            public void onSuccess(List<Question> questions) {
                if (pbAdminQuizLoading != null) pbAdminQuizLoading.setVisibility(View.GONE);
                if (questions == null) questions = new ArrayList<>();

                activity.syncCustomQuestions(questions);

                int customEasy = 0;
                int customMed = 0;
                int customHard = 0;

                for (Question q : questions) {
                    if ("hard".equalsIgnoreCase(q.difficulty)) customHard++;
                    else if ("medium".equalsIgnoreCase(q.difficulty)) customMed++;
                    else customEasy++;
                }

                if (tvAdminQuizEasyCount != null) tvAdminQuizEasyCount.setText(String.valueOf(customEasy));
                if (tvAdminQuizMedCount != null) tvAdminQuizMedCount.setText(String.valueOf(customMed));
                if (tvAdminQuizHardCount != null) tvAdminQuizHardCount.setText(String.valueOf(customHard));
                if (tvAdminQuizCustomCount != null) tvAdminQuizCustomCount.setText(String.valueOf(questions.size()));
                if (tvAdminQuizListHeader != null) tvAdminQuizListHeader.setText("DATABASE QUESTIONS DIRECTORY (" + questions.size() + ")");

                renderAdminQuestionCards(questions);
            }

            @Override
            public void onError(String error) {
                if (pbAdminQuizLoading != null) pbAdminQuizLoading.setVisibility(View.GONE);
                activity.showErrorAlert("Sync Warning", "Could not fetch quiz questions: " + error);
            }
        });
    }

    private void renderAdminQuestionCards(List<Question> questions) {
        if (containerAdminQuestions == null) return;
        containerAdminQuestions.removeAllViews();

        float density = activity.getResources().getDisplayMetrics().density;
        int pad14 = (int) (14 * density);
        int pad8 = (int) (8 * density);
        int pad4 = (int) (4 * density);
        int marginB = (int) (12 * density);

        if (questions == null || questions.isEmpty()) {
            TextView tvEmpty = new TextView(activity);
            tvEmpty.setText("No quiz questions found in the database.\nUse the form above to add questions for your players!");
            tvEmpty.setTextColor(Color.parseColor("#94A3B8"));
            tvEmpty.setTextSize(12);
            tvEmpty.setGravity(Gravity.CENTER);
            tvEmpty.setPadding(pad14, (int) (24 * density), pad14, (int) (24 * density));
            containerAdminQuestions.addView(tvEmpty);
            return;
        }

        // Sort: newest first
        Collections.sort(questions, (a, b) -> Long.compare(b.createdAt, a.createdAt));

        for (Question q : questions) {
            MaterialCardView card = new MaterialCardView(activity);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, marginB);
            card.setLayoutParams(cardParams);
            card.setRadius(16 * density);
            card.setCardBackgroundColor(Color.parseColor("#14141E"));
            card.setStrokeWidth((int) (1.2f * density));

            int diffColor = "hard".equalsIgnoreCase(q.difficulty) ? Color.parseColor("#F59E0B")
                    : ("medium".equalsIgnoreCase(q.difficulty) ? Color.parseColor("#8B5CF6") : Color.parseColor("#06B6D4"));
            card.setStrokeColor(diffColor);
            card.setCardElevation(4 * density);

            LinearLayout cardContent = new LinearLayout(activity);
            cardContent.setOrientation(LinearLayout.VERTICAL);
            cardContent.setPadding(pad14, pad14, pad14, pad14);

            // Row 1: Difficulty Badge, Creator tag, Delete Button
            LinearLayout row1 = new LinearLayout(activity);
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setGravity(Gravity.CENTER_VERTICAL);
            row1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            // Difficulty Badge
            TextView tvDiff = new TextView(activity);
            tvDiff.setText(q.difficulty.toUpperCase(Locale.ROOT));
            tvDiff.setTextColor(diffColor);
            tvDiff.setTextSize(10);
            tvDiff.setTypeface(null, Typeface.BOLD);
            tvDiff.setPadding((int) (8 * density), (int) (3 * density), (int) (8 * density), (int) (3 * density));
            tvDiff.setBackground(activity.createBadgeDrawable(Color.parseColor("#1C1929"), diffColor));
            row1.addView(tvDiff);

            TextView tvCreator = new TextView(activity);
            tvCreator.setText("  by @" + q.createdBy);
            tvCreator.setTextColor(Color.parseColor("#94A3B8"));
            tvCreator.setTextSize(11);
            LinearLayout.LayoutParams crParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            crParams.setMargins((int) (6 * density), 0, 0, 0);
            tvCreator.setLayoutParams(crParams);
            row1.addView(tvCreator);

            // Delete Question Button
            MaterialButton btnDeleteQ = new MaterialButton(activity);
            btnDeleteQ.setText("DELETE");
            btnDeleteQ.setTextSize(10);
            btnDeleteQ.setTextColor(Color.parseColor("#EF4444"));
            btnDeleteQ.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#281119")));
            btnDeleteQ.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#EF4444")));
            btnDeleteQ.setStrokeWidth((int) (1 * density));
            btnDeleteQ.setCornerRadius((int) (10 * density));
            btnDeleteQ.setPadding((int) (10 * density), pad4, (int) (10 * density), pad4);
            btnDeleteQ.setInsetTop(0);
            btnDeleteQ.setInsetBottom(0);

            btnDeleteQ.setOnClickListener(v -> activity.animateClick(btnDeleteQ, () -> handleDeleteCustomQuestion(q)));
            row1.addView(btnDeleteQ);

            cardContent.addView(row1);

            // Question Prompt
            TextView tvPrompt = new TextView(activity);
            tvPrompt.setText(q.q);
            tvPrompt.setTextColor(Color.WHITE);
            tvPrompt.setTextSize(14);
            tvPrompt.setTypeface(null, Typeface.BOLD);
            tvPrompt.setPadding(0, pad8, 0, pad8);
            cardContent.addView(tvPrompt);

            // Options List with Verified Correct Indicator
            LinearLayout optContainer = new LinearLayout(activity);
            optContainer.setOrientation(LinearLayout.VERTICAL);

            for (int i = 0; i < q.options.size(); i++) {
                char letter = (char) ('A' + i);
                boolean isCorrectChoice = (i == q.correct);

                TextView tvOpt = new TextView(activity);
                tvOpt.setText((isCorrectChoice ? "✓ [" + letter + "] " : "  [" + letter + "] ") + q.options.get(i));
                tvOpt.setTextSize(12);
                tvOpt.setPadding((int) (10 * density), (int) (6 * density), (int) (10 * density), (int) (6 * density));

                LinearLayout.LayoutParams optParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                optParams.setMargins(0, (int) (3 * density), 0, 0);
                tvOpt.setLayoutParams(optParams);

                if (isCorrectChoice) {
                    tvOpt.setTextColor(Color.parseColor("#10B981"));
                    tvOpt.setTypeface(null, Typeface.BOLD);
                    tvOpt.setBackground(activity.createBadgeDrawable(Color.parseColor("#063D28"), Color.parseColor("#10B981")));
                } else {
                    tvOpt.setTextColor(Color.parseColor("#CBD5E1"));
                    tvOpt.setBackground(activity.createBadgeDrawable(Color.parseColor("#161622"), Color.parseColor("#262638")));
                }
                optContainer.addView(tvOpt);
            }

            cardContent.addView(optContainer);
            card.addView(cardContent);
            containerAdminQuestions.addView(card);
        }
    }

    private void handleDeleteCustomQuestion(Question q) {
        Account current = activity.getCurrentAccount();
        if (!FirebaseHelper.isUserAdmin(current)) {
            activity.showErrorAlert("Access Denied", "Only administrators can delete quiz questions.");
            return;
        }

        new AlertDialog.Builder(activity)
                .setTitle("Delete Quiz Question")
                .setMessage("Are you sure you want to remove this question?\n\n\"" + q.q + "\"\n\nThis will remove it from the live quiz pool for all players.")
                .setPositiveButton("DELETE", (d, w) -> {
                    if (pbAdminQuizLoading != null) pbAdminQuizLoading.setVisibility(View.VISIBLE);

                    FirebaseHelper.deleteCustomQuestion(current, q.id, new FirebaseHelper.Callback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean res) {
                            if (pbAdminQuizLoading != null) pbAdminQuizLoading.setVisibility(View.GONE);
                            activity.removeCustomQuestionFromBank(q.id);
                            loadQuizQuestionsData();
                            Toast.makeText(activity, "Question removed successfully.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String err) {
                            if (pbAdminQuizLoading != null) pbAdminQuizLoading.setVisibility(View.GONE);
                            activity.showErrorAlert("Deletion Failed", err);
                        }
                    });
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    public void loadData() {
        if (pbAdminLoading != null) pbAdminLoading.setVisibility(View.VISIBLE);
        FirebaseHelper.getAllAccounts(new FirebaseHelper.Callback<List<Account>>() {
            @Override
            public void onSuccess(List<Account> accounts) {
                if (pbAdminLoading != null) pbAdminLoading.setVisibility(View.GONE);
                if (accounts == null) accounts = new ArrayList<>();

                int totalUsers = accounts.size();
                int totalAdmins = 0;
                int totalLogins = 0;

                for (Account a : accounts) {
                    if (FirebaseHelper.isUserAdmin(a)) {
                        totalAdmins++;
                    }
                    totalLogins += Math.max(1, a.loginCount);
                }

                if (tvAdminTotalUsers != null) tvAdminTotalUsers.setText(String.valueOf(totalUsers));
                if (tvAdminTotalAdmins != null) tvAdminTotalAdmins.setText(String.valueOf(totalAdmins));
                if (tvAdminTotalLogins != null) tvAdminTotalLogins.setText(String.valueOf(totalLogins));
                if (tvAdminUserListHeader != null) tvAdminUserListHeader.setText("ALL USERS DIRECTORY (" + totalUsers + ")");

                renderAdminUserCards(accounts);
            }

            @Override
            public void onError(String error) {
                if (pbAdminLoading != null) pbAdminLoading.setVisibility(View.GONE);
                activity.showErrorAlert("Sync Failed", "Could not load user directory: " + error);
            }
        });
    }

    private void handlePromoteNewAdmin() {
        if (etAdminAddUser == null) return;
        String usernameToPromote = etAdminAddUser.getText() != null ? etAdminAddUser.getText().toString().trim() : "";
        if (usernameToPromote.isEmpty()) {
            activity.showErrorAlert("Invalid Input", "Please enter a valid player username to promote.");
            return;
        }

        if (pbAdminLoading != null) pbAdminLoading.setVisibility(View.VISIBLE);

        FirebaseHelper.promoteExistingUser(usernameToPromote, new FirebaseHelper.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (pbAdminLoading != null) pbAdminLoading.setVisibility(View.GONE);
                etAdminAddUser.setText("");
                activity.showSuccessAlert("Admin Granted", "Successfully appointed @" + usernameToPromote + " as Administrator!", "OK", AdminPanel.this::loadData);
            }

            @Override
            public void onError(String error) {
                if (pbAdminLoading != null) pbAdminLoading.setVisibility(View.GONE);
                activity.showErrorAlert("Promotion Failed", error);
            }
        });
    }

    public void renderAdminUserCards(List<Account> accounts) {
        if (containerAdminUsers == null) return;
        containerAdminUsers.removeAllViews();

        float density = activity.getResources().getDisplayMetrics().density;
        int pad14 = (int) (14 * density);
        int pad8 = (int) (8 * density);
        int pad4 = (int) (4 * density);
        int marginB = (int) (12 * density);

        // Sort: Super-admin first, then admins, then alphabetically
        Collections.sort(accounts, (a, b) -> {
            boolean aSuper = "biar".equalsIgnoreCase(a.username);
            boolean bSuper = "biar".equalsIgnoreCase(b.username);
            if (aSuper) return -1;
            if (bSuper) return 1;
            boolean aAdmin = FirebaseHelper.isUserAdmin(a);
            boolean bAdmin = FirebaseHelper.isUserAdmin(b);
            if (aAdmin != bAdmin) return aAdmin ? -1 : 1;
            return a.name.compareToIgnoreCase(b.name);
        });

        for (Account user : accounts) {
            boolean isSuperAdmin = "biar".equalsIgnoreCase(user.username);
            boolean isAdmin = FirebaseHelper.isUserAdmin(user);

            MaterialCardView card = new MaterialCardView(activity);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, marginB);
            card.setLayoutParams(cardParams);
            card.setRadius(16 * density);
            card.setCardBackgroundColor(Color.parseColor("#14141E"));
            card.setStrokeWidth((int) (1.2f * density));
            card.setStrokeColor(isSuperAdmin ? Color.parseColor("#F59E0B") : (isAdmin ? Color.parseColor("#A855F7") : Color.parseColor("#292938")));
            card.setCardElevation(4 * density);

            LinearLayout cardContent = new LinearLayout(activity);
            cardContent.setOrientation(LinearLayout.VERTICAL);
            cardContent.setPadding(pad14, pad14, pad14, pad14);

            // Row 1: Name, Username, Status Pill, Role Badge
            LinearLayout row1 = new LinearLayout(activity);
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout nameCol = new LinearLayout(activity);
            nameCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            nameCol.setLayoutParams(nameParams);

            TextView tvName = new TextView(activity);
            tvName.setText(user.name);
            tvName.setTextColor(Color.WHITE);
            tvName.setTextSize(15);
            tvName.setTypeface(null, Typeface.BOLD);
            nameCol.addView(tvName);

            TextView tvUsername = new TextView(activity);
            tvUsername.setText("@" + user.username);
            tvUsername.setTextColor(Color.parseColor("#94A3B8"));
            tvUsername.setTextSize(11);
            nameCol.addView(tvUsername);

            row1.addView(nameCol);

            // Online status indicator (prominent status pill)
            TextView tvOnline = new TextView(activity);
            boolean isOnline = user.isUserActuallyOnline();
            tvOnline.setText(isOnline ? "● ONLINE" : "○ OFFLINE");
            tvOnline.setTextColor(isOnline ? Color.parseColor("#10B981") : Color.parseColor("#94A3B8"));
            tvOnline.setTextSize(10);
            tvOnline.setTypeface(null, Typeface.BOLD);
            tvOnline.setPadding((int) (10 * density), (int) (4 * density), (int) (10 * density), (int) (4 * density));
            tvOnline.setBackground(activity.createBadgeDrawable(
                    isOnline ? Color.parseColor("#063D28") : Color.parseColor("#1E293B"),
                    isOnline ? Color.parseColor("#10B981") : Color.parseColor("#64748B")
            ));
            LinearLayout.LayoutParams onlineParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            onlineParams.setMargins(0, 0, (int) (8 * density), 0);
            tvOnline.setLayoutParams(onlineParams);
            row1.addView(tvOnline);

            // Role Badge
            TextView tvBadge = new TextView(activity);
            tvBadge.setTextSize(10);
            tvBadge.setTypeface(null, Typeface.BOLD);
            tvBadge.setPadding((int) (10 * density), (int) (4 * density), (int) (10 * density), (int) (4 * density));
            if (isSuperAdmin) {
                tvBadge.setText("👑 SUPER ADMIN");
                tvBadge.setTextColor(Color.parseColor("#FBBF24"));
                tvBadge.setBackground(activity.createBadgeDrawable(Color.parseColor("#2B1D04"), Color.parseColor("#F59E0B")));
            } else if (isAdmin) {
                tvBadge.setText("🛡️ ADMIN");
                tvBadge.setTextColor(Color.parseColor("#5EEAD4"));
                tvBadge.setBackground(activity.createBadgeDrawable(Color.parseColor("#1E163B"), Color.parseColor("#5EEAD4")));
            } else {
                tvBadge.setText("🎮 PLAYER");
                tvBadge.setTextColor(Color.parseColor("#94A3B8"));
                tvBadge.setBackground(activity.createBadgeDrawable(Color.parseColor("#161622"), Color.parseColor("#333344")));
            }
            row1.addView(tvBadge);

            cardContent.addView(row1);

            // Row 2: Stats
            LinearLayout rowStats = new LinearLayout(activity);
            rowStats.setOrientation(LinearLayout.HORIZONTAL);
            rowStats.setPadding(0, pad8, 0, pad8);

            TextView tvStats = new TextView(activity);
            tvStats.setText("⚡ Streak: " + user.currentWinStreak + "  |  🏆 Best: " + user.bestWinStreak + "  |  🔑 Logins: " + Math.max(1, user.loginCount));
            tvStats.setTextColor(Color.parseColor("#5EEAD4"));
            tvStats.setTextSize(11);
            rowStats.addView(tvStats);
            cardContent.addView(rowStats);

            // Row 3: Admin Action Buttons
            if (isSuperAdmin) {
                TextView tvLocked = new TextView(activity);
                tvLocked.setText("⭐ Primary Owner & Permanent Administrator");
                tvLocked.setTextSize(11);
                tvLocked.setTextColor(Color.parseColor("#FBBF24"));
                cardContent.addView(tvLocked);
            } else if (isAdmin) {
                LinearLayout btnRow = new LinearLayout(activity);
                btnRow.setOrientation(LinearLayout.HORIZONTAL);
                btnRow.setPadding(0, pad4, 0, 0);

                MaterialButton btnRevoke = new MaterialButton(activity);
                btnRevoke.setText("REVOKE ADMIN");
                btnRevoke.setTextSize(11);
                btnRevoke.setTextColor(Color.parseColor("#EF4444"));
                btnRevoke.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2A121E")));
                btnRevoke.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#EF4444")));
                btnRevoke.setStrokeWidth((int) (1 * density));
                btnRevoke.setCornerRadius((int) (10 * density));
                btnRevoke.setPadding(pad8, pad4, pad8, pad4);
                btnRevoke.setOnClickListener(v -> activity.animateClick(btnRevoke, () -> {
                    new AlertDialog.Builder(activity)
                            .setTitle("Revoke Admin Rights")
                            .setMessage("Are you sure you want to remove admin privileges from '" + user.name + "' (@" + user.username + ")?")
                            .setPositiveButton("Revoke", (d, w) -> {
                                FirebaseHelper.setAdminStatus(user.username, false, new FirebaseHelper.Callback<Boolean>() {
                                    @Override
                                    public void onSuccess(Boolean res) {
                                        activity.showSuccessAlert("Privileges Revoked", "Removed admin access from @" + user.username, "OK", AdminPanel.this::loadData);
                                    }

                                    @Override
                                    public void onError(String err) {
                                        activity.showErrorAlert("Error", err);
                                    }
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }));
                btnRow.addView(btnRevoke);

                MaterialButton btnDelete = createDeleteButton(user, density, pad8, pad4);
                btnRow.addView(btnDelete);
                cardContent.addView(btnRow);
            } else {
                LinearLayout btnRow = new LinearLayout(activity);
                btnRow.setOrientation(LinearLayout.VERTICAL);
                btnRow.setPadding(0, pad4, 0, 0);

                MaterialButton btnPromote = new MaterialButton(activity);
                btnPromote.setText("+ PROMOTE TO ADMIN");
                btnPromote.setTextSize(11);
                btnPromote.setTextColor(Color.parseColor("#FBBF24"));
                btnPromote.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2D1E08")));
                btnPromote.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#F59E0B")));
                btnPromote.setStrokeWidth((int) (1 * density));
                btnPromote.setCornerRadius((int) (10 * density));
                btnPromote.setPadding(pad8, pad4, pad8, pad4);
                btnPromote.setOnClickListener(v -> activity.animateClick(btnPromote, () -> {
                    FirebaseHelper.setAdminStatus(user.username, true, new FirebaseHelper.Callback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean res) {
                            activity.showSuccessAlert("Admin Appointed", "@" + user.username + " is now an Odyssey Administrator!", "OK", AdminPanel.this::loadData);
                        }

                        @Override
                        public void onError(String err) {
                            activity.showErrorAlert("Promotion Failed", err);
                        }
                    });
                }));
                btnRow.addView(btnPromote);

                MaterialButton btnDelete = createDeleteButton(user, density, pad8, pad4);
                btnRow.addView(btnDelete);
                cardContent.addView(btnRow);
            }

            card.addView(cardContent);
            containerAdminUsers.addView(card);
        }
    }

    private MaterialButton createDeleteButton(Account user, float density, int pad8, int pad4) {
        MaterialButton btnDelete = new MaterialButton(activity);
        btnDelete.setText("🗑️ DELETE PLAYER ACCOUNT");
        btnDelete.setTextSize(11);
        btnDelete.setTextColor(Color.parseColor("#F87171"));
        btnDelete.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#281119")));
        btnDelete.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#DC2626")));
        btnDelete.setStrokeWidth((int) (1 * density));
        btnDelete.setCornerRadius((int) (10 * density));
        btnDelete.setPadding(pad8, pad4, pad8, pad4);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, (int) (6 * density), 0, 0);
        btnDelete.setLayoutParams(lp);

        btnDelete.setOnClickListener(v -> activity.animateClick(btnDelete, () -> {
            new AlertDialog.Builder(activity)
                    .setTitle("Delete Account: @" + user.username)
                    .setMessage("WARNING: This will permanently remove user '" + user.name + "' (@" + user.username + ") from the Code Odyssey database. This action cannot be undone.")
                    .setPositiveButton("DELETE FOREVER", (d, w) -> {
                        if (pbAdminLoading != null) pbAdminLoading.setVisibility(View.VISIBLE);
                        FirebaseHelper.deleteAccount(user.username, new FirebaseHelper.Callback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean res) {
                                if (pbAdminLoading != null) pbAdminLoading.setVisibility(View.GONE);
                                activity.showSuccessAlert("Account Deleted", "User @" + user.username + " was permanently removed.", "OK", AdminPanel.this::loadData);
                            }

                            @Override
                            public void onError(String err) {
                                if (pbAdminLoading != null) pbAdminLoading.setVisibility(View.GONE);
                                activity.showErrorAlert("Deletion Failed", err);
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }));
        return btnDelete;
    }
}

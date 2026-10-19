package com.odyssey.game;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pure Java Firebase Firestore Client for Android
 * Connects directly to project: code-odyssey-565e2
 * Features:
 * - Realtime Account Authentication
 * - Win Streak & Stats Synchronization
 * - Administrative Role Management (Biar super-admin & dynamic admins)
 * - User Directory & Login Tracking
 */
public class FirebaseHelper {
    private static final String PROJECT_ID = "code-odyssey-565e2";
    private static final String API_KEY = "AIzaSyAsVp9VcM9UH6UpOKWjk7gDLLJtWJZVgBg";
    private static final String BASE_URL = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID + "/databases/(default)/documents/users/";
    private static final String COLLECTION_URL = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID + "/databases/(default)/documents/users?pageSize=200&key=" + API_KEY;
    private static final String QUESTIONS_BASE_URL = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID + "/databases/(default)/documents/quiz_questions/";
    private static final String QUESTIONS_COLLECTION_URL = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID + "/databases/(default)/documents/quiz_questions?pageSize=200&key=" + API_KEY;
    private static final String SETTINGS_DOC_URL = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID + "/databases/(default)/documents/settings/quiz_config";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    /**
     * Checks if a given account has administrator privileges.
     * User 'biar' is the default super-admin.
     */
    public static boolean isUserAdmin(Account acc) {
        if (acc == null) return false;
        return "biar".equalsIgnoreCase(acc.username) || acc.isAdmin;
    }

    public static void getAccount(String username, Callback<Account> callback) {
        executor.execute(() -> {
            try {
                String cleanUser = URLEncoder.encode(username.trim().toLowerCase(), "UTF-8");
                URL url = new URL(BASE_URL + cleanUser + "?key=" + API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONObject fields = json.optJSONObject("fields");
                    if (fields != null) {
                        Account acc = parseAccountFromFields(username.trim().toLowerCase(), fields);
                        mainHandler.post(() -> callback.onSuccess(acc));
                        return;
                    }
                } else if (code == 404) {
                    mainHandler.post(() -> callback.onSuccess(null));
                    return;
                }
                mainHandler.post(() -> callback.onError("Server returned status: " + code));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public static void saveAccount(Account acc, Callback<Boolean> callback) {
        executor.execute(() -> {
            try {
                String cleanUser = URLEncoder.encode(acc.username.trim().toLowerCase(), "UTF-8");
                URL url = new URL(BASE_URL + cleanUser + "?key=" + API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                JSONObject fields = new JSONObject();
                fields.put("username", new JSONObject().put("stringValue", acc.username));
                fields.put("name", new JSONObject().put("stringValue", acc.name));
                fields.put("password", new JSONObject().put("stringValue", acc.password));
                fields.put("currentWinStreak", new JSONObject().put("integerValue", String.valueOf(acc.currentWinStreak)));
                fields.put("bestWinStreak", new JSONObject().put("integerValue", String.valueOf(acc.bestWinStreak)));
                fields.put("isAdmin", new JSONObject().put("booleanValue", acc.isAdmin || "biar".equalsIgnoreCase(acc.username)));
                fields.put("loginCount", new JSONObject().put("integerValue", String.valueOf(Math.max(1, acc.loginCount))));
                fields.put("isOnline", new JSONObject().put("booleanValue", acc.isOnline));
                fields.put("lastActiveMillis", new JSONObject().put("integerValue", String.valueOf(acc.lastActiveMillis > 0 ? acc.lastActiveMillis : System.currentTimeMillis())));
                fields.put("bio", new JSONObject().put("stringValue", acc.bio));
                fields.put("statusBubble", new JSONObject().put("stringValue", acc.statusBubble));
                if (acc.avatarBase64 != null) {
                    fields.put("avatarBase64", new JSONObject().put("stringValue", acc.avatarBase64));
                }
                if (acc.achievements != null && !acc.achievements.isEmpty()) {
                    StringBuilder achSb = new StringBuilder();
                    for (int i = 0; i < acc.achievements.size(); i++) {
                        if (i > 0) achSb.append(",");
                        achSb.append(acc.achievements.get(i));
                    }
                    fields.put("achievements", new JSONObject().put("stringValue", achSb.toString()));
                } else {
                    fields.put("achievements", new JSONObject().put("stringValue", ""));
                }
                fields.put("unlockedMedium", new JSONObject().put("booleanValue", acc.unlockedMedium || acc.isAdmin || "biar".equalsIgnoreCase(acc.username)));
                fields.put("unlockedHard", new JSONObject().put("booleanValue", acc.unlockedHard || acc.isAdmin || "biar".equalsIgnoreCase(acc.username)));
                if (!acc.lastLogin.isEmpty()) {
                    fields.put("lastLogin", new JSONObject().put("stringValue", acc.lastLogin));
                }

                JSONObject payload = new JSONObject();
                payload.put("fields", fields);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                boolean ok = (code >= 200 && code < 300);
                if (callback != null) {
                    mainHandler.post(() -> {
                        if (ok) callback.onSuccess(true);
                        else callback.onError("Save failed: HTTP " + code);
                    });
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Records a login event for the user, updating login count, online status, and timestamp.
     */
    public static void recordLogin(Account acc) {
        if (acc == null) return;
        acc.loginCount++;
        acc.isOnline = true;
        acc.lastActiveMillis = System.currentTimeMillis();
        acc.lastLogin = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        executor.execute(() -> {
            try {
                String cleanUser = URLEncoder.encode(acc.username.trim().toLowerCase(), "UTF-8");
                String updateUrl = BASE_URL + cleanUser + "?updateMask.fieldPaths=loginCount&updateMask.fieldPaths=lastLogin&updateMask.fieldPaths=isOnline&updateMask.fieldPaths=lastActiveMillis&key=" + API_KEY;
                URL url = new URL(updateUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                JSONObject fields = new JSONObject();
                fields.put("loginCount", new JSONObject().put("integerValue", String.valueOf(acc.loginCount)));
                fields.put("lastLogin", new JSONObject().put("stringValue", acc.lastLogin));
                fields.put("isOnline", new JSONObject().put("booleanValue", true));
                fields.put("lastActiveMillis", new JSONObject().put("integerValue", String.valueOf(acc.lastActiveMillis)));

                JSONObject payload = new JSONObject();
                payload.put("fields", fields);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                conn.getResponseCode();
            } catch (Exception ignored) {}
        });
    }

    /**
     * Updates online presence in Firestore.
     */
    public static void setOnlineStatus(String username, boolean online) {
        if (username == null || username.trim().isEmpty()) return;
        executor.execute(() -> {
            try {
                String cleanUser = URLEncoder.encode(username.trim().toLowerCase(), "UTF-8");
                long now = System.currentTimeMillis();
                String updateUrl = BASE_URL + cleanUser + "?updateMask.fieldPaths=isOnline&updateMask.fieldPaths=lastActiveMillis&key=" + API_KEY;
                URL url = new URL(updateUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                JSONObject fields = new JSONObject();
                fields.put("isOnline", new JSONObject().put("booleanValue", online));
                fields.put("lastActiveMillis", new JSONObject().put("integerValue", String.valueOf(now)));

                JSONObject payload = new JSONObject();
                payload.put("fields", fields);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                conn.getResponseCode();
            } catch (Exception ignored) {}
        });
    }

    /**
     * Fetches all registered users from Firestore for the Admin Panel.
     */
    public static void getAllAccounts(Callback<List<Account>> callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(COLLECTION_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray docs = json.optJSONArray("documents");
                    List<Account> list = new ArrayList<>();
                    if (docs != null) {
                        for (int i = 0; i < docs.length(); i++) {
                            JSONObject doc = docs.getJSONObject(i);
                            String docPath = doc.optString("name", "");
                            String docUser = docPath.contains("/") ? docPath.substring(docPath.lastIndexOf('/') + 1) : "";
                            JSONObject fields = doc.optJSONObject("fields");
                            if (fields != null) {
                                Account acc = parseAccountFromFields(docUser, fields);
                                list.add(acc);
                            }
                        }
                    }
                    mainHandler.post(() -> callback.onSuccess(list));
                    return;
                }
                mainHandler.post(() -> callback.onError("Failed to fetch users: HTTP " + code));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Deletes a user account from Firestore.
     * Prevents deletion of the primary Super Admin ('biar').
     */
    public static void deleteAccount(String username, Callback<Boolean> callback) {
        String cleanUser = username != null ? username.trim().toLowerCase() : "";
        if ("biar".equalsIgnoreCase(cleanUser)) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("Security Protection: The Super Administrator 'biar' cannot be deleted."));
            }
            return;
        }

        executor.execute(() -> {
            try {
                String encodedUser = URLEncoder.encode(cleanUser, "UTF-8");
                String deleteUrl = BASE_URL + encodedUser + "?key=" + API_KEY;
                URL url = new URL(deleteUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int code = conn.getResponseCode();
                boolean ok = (code >= 200 && code < 300);
                if (callback != null) {
                    mainHandler.post(() -> {
                        if (ok) callback.onSuccess(true);
                        else callback.onError("Failed to delete account: HTTP " + code);
                    });
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Promotes an existing registered user to Admin.
     * Guarantees that only already registered users can be granted admin status.
     * If the username does not exist in Firestore, returns an error without modifying the database.
     */
    public static void promoteExistingUser(String username, Callback<Boolean> callback) {
        String cleanUser = username != null ? username.trim().toLowerCase() : "";
        if (cleanUser.isEmpty()) {
            if (callback != null) callback.onError("Username cannot be empty.");
            return;
        }

        getAccount(cleanUser, new Callback<Account>() {
            @Override
            public void onSuccess(Account acc) {
                if (acc == null) {
                    if (callback != null) {
                        callback.onError("User '@" + cleanUser + "' not found! Only existing registered players can be granted admin privileges.");
                    }
                    return;
                }
                setAdminStatus(cleanUser, true, callback);
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError("Could not verify user existence: " + error);
            }
        });
    }

    /**
     * Promotes or revokes admin privileges for a user.
     */
    public static void setAdminStatus(String username, boolean makeAdmin, Callback<Boolean> callback) {
        executor.execute(() -> {
            try {
                String cleanUser = URLEncoder.encode(username.trim().toLowerCase(), "UTF-8");
                String updateUrl = BASE_URL + cleanUser + "?updateMask.fieldPaths=isAdmin&key=" + API_KEY;
                URL url = new URL(updateUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                JSONObject fields = new JSONObject();
                fields.put("isAdmin", new JSONObject().put("booleanValue", makeAdmin));

                JSONObject payload = new JSONObject();
                payload.put("fields", fields);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                boolean ok = (code >= 200 && code < 300);
                mainHandler.post(() -> {
                    if (ok) callback.onSuccess(true);
                    else callback.onError("Update failed: HTTP " + code);
                });
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private static Account parseAccountFromFields(String fallbackUser, JSONObject fields) {
        String username = fields.optJSONObject("username") != null ? fields.optJSONObject("username").optString("stringValue", fallbackUser) : fallbackUser;
        String name = fields.optJSONObject("name") != null ? fields.optJSONObject("name").optString("stringValue", username) : username;
        String password = fields.optJSONObject("password") != null ? fields.optJSONObject("password").optString("stringValue", "") : "";
        int currentStreak = fields.optJSONObject("currentWinStreak") != null ? Integer.parseInt(fields.optJSONObject("currentWinStreak").optString("integerValue", "0")) : 0;
        int bestStreak = fields.optJSONObject("bestWinStreak") != null ? Integer.parseInt(fields.optJSONObject("bestWinStreak").optString("integerValue", "0")) : 0;

        boolean isAdmin = false;
        if (fields.optJSONObject("isAdmin") != null) {
            isAdmin = fields.optJSONObject("isAdmin").optBoolean("booleanValue", false);
        }
        if ("biar".equalsIgnoreCase(username)) {
            isAdmin = true;
        }

        int loginCount = 1;
        if (fields.optJSONObject("loginCount") != null) {
            try {
                loginCount = Integer.parseInt(fields.optJSONObject("loginCount").optString("integerValue", "1"));
            } catch (Exception ignored) {}
        }

        String lastLogin = "";
        if (fields.optJSONObject("lastLogin") != null) {
            lastLogin = fields.optJSONObject("lastLogin").optString("stringValue", "");
        }

        boolean isOnline = false;
        if (fields.optJSONObject("isOnline") != null) {
            isOnline = fields.optJSONObject("isOnline").optBoolean("booleanValue", false);
        }

        long lastActive = 0L;
        if (fields.optJSONObject("lastActiveMillis") != null) {
            try {
                lastActive = Long.parseLong(fields.optJSONObject("lastActiveMillis").optString("integerValue", "0"));
            } catch (Exception ignored) {}
        }

        String bio = "CSS Explorer & Space Adventurer 🌌";
        if (fields.optJSONObject("bio") != null) {
            bio = fields.optJSONObject("bio").optString("stringValue", bio);
        }

        String statusBubble = "Ready to code? 🚀";
        if (fields.optJSONObject("statusBubble") != null) {
            statusBubble = fields.optJSONObject("statusBubble").optString("stringValue", statusBubble);
        }

        String avatarBase64 = "";
        if (fields.optJSONObject("avatarBase64") != null) {
            avatarBase64 = fields.optJSONObject("avatarBase64").optString("stringValue", "");
        }

        List<String> achievements = new ArrayList<>();
        if (fields.optJSONObject("achievements") != null) {
            String rawAch = fields.optJSONObject("achievements").optString("stringValue", "");
            if (!rawAch.trim().isEmpty()) {
                String[] parts = rawAch.split(",");
                for (String p : parts) {
                    String clean = p.trim();
                    if (!clean.isEmpty() && !achievements.contains(clean)) {
                        achievements.add(clean);
                    }
                }
            }
        }

        boolean unlockedMedium = false;
        if (fields.optJSONObject("unlockedMedium") != null) {
            unlockedMedium = fields.optJSONObject("unlockedMedium").optBoolean("booleanValue", false);
        }
        boolean unlockedHard = false;
        if (fields.optJSONObject("unlockedHard") != null) {
            unlockedHard = fields.optJSONObject("unlockedHard").optBoolean("booleanValue", false);
        }

        Account acc = new Account(username.trim().toLowerCase(), name, password);
        acc.currentWinStreak = currentStreak;
        acc.bestWinStreak = bestStreak;
        acc.isAdmin = isAdmin;
        acc.unlockedMedium = unlockedMedium || isAdmin || "biar".equalsIgnoreCase(username);
        acc.unlockedHard = unlockedHard || isAdmin || "biar".equalsIgnoreCase(username);
        acc.loginCount = loginCount;
        acc.lastLogin = lastLogin;
        acc.isOnline = isOnline;
        acc.lastActiveMillis = lastActive;
        acc.bio = bio;
        acc.statusBubble = statusBubble;
        acc.avatarBase64 = avatarBase64;
        acc.achievements = achievements;
        return acc;
    }

    public static void saveAvatar(String username, String avatarBase64, Callback<Boolean> callback) {
        executor.execute(() -> {
            try {
                String cleanUser = URLEncoder.encode(username.trim().toLowerCase(), "UTF-8");
                URL url = new URL(BASE_URL + cleanUser + "?updateMask.fieldPaths=avatarBase64&key=" + API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                JSONObject fields = new JSONObject();
                fields.put("avatarBase64", new JSONObject().put("stringValue", avatarBase64 != null ? avatarBase64 : ""));
                JSONObject payload = new JSONObject();
                payload.put("fields", fields);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                boolean ok = (code >= 200 && code < 300);
                if (callback != null) {
                    mainHandler.post(() -> {
                        if (ok) callback.onSuccess(true);
                        else callback.onError("HTTP " + code);
                    });
                }
            } catch (Exception e) {
                if (callback != null) mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public static void saveAchievements(String username, List<String> achievements, Callback<Boolean> callback) {
        executor.execute(() -> {
            try {
                String cleanUser = URLEncoder.encode(username.trim().toLowerCase(), "UTF-8");
                URL url = new URL(BASE_URL + cleanUser + "?updateMask.fieldPaths=achievements&key=" + API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                StringBuilder achSb = new StringBuilder();
                if (achievements != null) {
                    for (int i = 0; i < achievements.size(); i++) {
                        if (i > 0) achSb.append(",");
                        achSb.append(achievements.get(i));
                    }
                }

                JSONObject fields = new JSONObject();
                fields.put("achievements", new JSONObject().put("stringValue", achSb.toString()));
                JSONObject payload = new JSONObject();
                payload.put("fields", fields);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                boolean ok = (code >= 200 && code < 300);
                if (callback != null) {
                    mainHandler.post(() -> {
                        if (ok) callback.onSuccess(true);
                        else callback.onError("HTTP " + code);
                    });
                }
            } catch (Exception e) {
                if (callback != null) mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Saves a new or modified custom CSS question to Firestore.
     * Enforces administrator privilege verification.
     */
    public static void saveCustomQuestion(Account adminAcc, Question q, Callback<Boolean> callback) {
        if (!isUserAdmin(adminAcc)) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("Security Protection: Only administrators can create or edit quiz questions."));
            }
            return;
        }

        if (q == null || q.q == null || q.q.trim().isEmpty() || q.options == null || q.options.size() < 4) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("Invalid question data: Must provide question text and 4 choices."));
            }
            return;
        }

        if (q.id == null || q.id.trim().isEmpty()) {
            String creatorTag = (adminAcc != null && adminAcc.username != null) ? adminAcc.username.trim().toLowerCase() : "admin";
            q.id = "q_" + System.currentTimeMillis() + "_" + creatorTag;
        }

        executor.execute(() -> {
            try {
                String cleanDocId = URLEncoder.encode(q.id.trim(), "UTF-8");
                URL url = new URL(QUESTIONS_BASE_URL + cleanDocId + "?key=" + API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                JSONObject fields = new JSONObject();
                fields.put("id", new JSONObject().put("stringValue", q.id));
                fields.put("question", new JSONObject().put("stringValue", q.q.trim()));
                fields.put("difficulty", new JSONObject().put("stringValue", q.difficulty != null ? q.difficulty.toLowerCase().trim() : "easy"));
                fields.put("correct", new JSONObject().put("integerValue", String.valueOf(q.correct)));
                fields.put("createdBy", new JSONObject().put("stringValue", q.createdBy != null ? q.createdBy : (adminAcc != null ? adminAcc.username : "Admin")));
                fields.put("createdAt", new JSONObject().put("integerValue", String.valueOf(q.createdAt > 0 ? q.createdAt : System.currentTimeMillis())));
                fields.put("isCustom", new JSONObject().put("booleanValue", true));

                JSONArray optArr = new JSONArray();
                for (String opt : q.options) {
                    optArr.put(new JSONObject().put("stringValue", opt != null ? opt.trim() : ""));
                }
                fields.put("options", new JSONObject().put("arrayValue", new JSONObject().put("values", optArr)));

                JSONObject payload = new JSONObject();
                payload.put("fields", fields);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                boolean ok = (code >= 200 && code < 300);
                if (callback != null) {
                    mainHandler.post(() -> {
                        if (ok) callback.onSuccess(true);
                        else callback.onError("Failed to publish question: HTTP " + code);
                    });
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Retrieves all custom quiz questions from Firestore.
     */
    public static void getAllCustomQuestions(Callback<List<Question>> callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(QUESTIONS_COLLECTION_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray docs = json.optJSONArray("documents");
                    List<Question> questions = new ArrayList<>();
                    if (docs != null) {
                        for (int i = 0; i < docs.length(); i++) {
                            JSONObject doc = docs.getJSONObject(i);
                            String docPath = doc.optString("name", "");
                            String docId = docPath.contains("/") ? docPath.substring(docPath.lastIndexOf('/') + 1) : "";
                            JSONObject fields = doc.optJSONObject("fields");
                            if (fields != null) {
                                Question q = parseQuestionFromFields(docId, fields);
                                if (q != null) {
                                    questions.add(q);
                                }
                            }
                        }
                    }
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(questions));
                    }
                    return;
                } else if (code == 404) {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                    }
                    return;
                }

                if (callback != null) {
                    mainHandler.post(() -> callback.onError("HTTP " + code));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Deletes a custom question from Firestore.
     * Enforces administrator privilege verification.
     */
    public static void deleteCustomQuestion(Account adminAcc, String questionId, Callback<Boolean> callback) {
        if (!isUserAdmin(adminAcc)) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("Security Protection: Only administrators can delete quiz questions."));
            }
            return;
        }

        if (questionId == null || questionId.trim().isEmpty()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("Invalid question identifier."));
            }
            return;
        }

        executor.execute(() -> {
            try {
                String cleanDocId = URLEncoder.encode(questionId.trim(), "UTF-8");
                URL url = new URL(QUESTIONS_BASE_URL + cleanDocId + "?key=" + API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int code = conn.getResponseCode();
                boolean ok = (code >= 200 && code < 300);
                if (callback != null) {
                    mainHandler.post(() -> {
                        if (ok) callback.onSuccess(true);
                        else callback.onError("Failed to delete question: HTTP " + code);
                    });
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Retrieves the admin-configured quiz question timer (in seconds) from Firestore.
     * Defaults to 15 seconds if not yet configured or on 404.
     */
    public static void getQuizTimerSeconds(Callback<Integer> callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(SETTINGS_DOC_URL + "?key=" + API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONObject fields = json.optJSONObject("fields");
                    int seconds = 15;
                    if (fields != null && fields.has("timerSeconds")) {
                        JSONObject tObj = fields.optJSONObject("timerSeconds");
                        if (tObj != null) {
                            try {
                                seconds = Integer.parseInt(tObj.optString("integerValue", "15"));
                            } catch (Exception ignored) {}
                        }
                    }
                    final int finalSeconds = (seconds >= 5 && seconds <= 300) ? seconds : 15;
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(finalSeconds));
                    }
                    return;
                } else if (code == 404) {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(15));
                    }
                    return;
                }

                if (callback != null) {
                    mainHandler.post(() -> callback.onError("HTTP " + code));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Saves the admin-configured quiz question timer (in seconds) to Firestore.
     * Enforces administrator privilege verification.
     */
    public static void saveQuizTimerSeconds(Account adminAcc, int seconds, Callback<Boolean> callback) {
        if (!isUserAdmin(adminAcc)) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("Security Protection: Only administrators can configure the quiz timer."));
            }
            return;
        }

        final int validSeconds = Math.max(5, Math.min(300, seconds));

        executor.execute(() -> {
            try {
                URL url = new URL(SETTINGS_DOC_URL + "?key=" + API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                JSONObject fields = new JSONObject();
                fields.put("timerSeconds", new JSONObject().put("integerValue", String.valueOf(validSeconds)));
                fields.put("updatedBy", new JSONObject().put("stringValue", adminAcc.username));
                fields.put("updatedAt", new JSONObject().put("integerValue", String.valueOf(System.currentTimeMillis())));

                JSONObject payload = new JSONObject();
                payload.put("fields", fields);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                boolean ok = (code >= 200 && code < 300);
                if (callback != null) {
                    mainHandler.post(() -> {
                        if (ok) callback.onSuccess(true);
                        else callback.onError("Failed to update timer setting: HTTP " + code);
                    });
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    private static Question parseQuestionFromFields(String docId, JSONObject fields) {
        if (fields == null) return null;
        try {
            String id = fields.has("id") && fields.optJSONObject("id") != null ? fields.optJSONObject("id").optString("stringValue", docId) : docId;
            String prompt = "";
            if (fields.has("question") && fields.optJSONObject("question") != null) {
                prompt = fields.optJSONObject("question").optString("stringValue", "");
            } else if (fields.has("q") && fields.optJSONObject("q") != null) {
                prompt = fields.optJSONObject("q").optString("stringValue", "");
            }

            String diff = "easy";
            if (fields.has("difficulty") && fields.optJSONObject("difficulty") != null) {
                diff = fields.optJSONObject("difficulty").optString("stringValue", "easy");
            }

            String createdBy = "Admin";
            if (fields.has("createdBy") && fields.optJSONObject("createdBy") != null) {
                createdBy = fields.optJSONObject("createdBy").optString("stringValue", "Admin");
            }

            long createdAt = 0L;
            if (fields.has("createdAt") && fields.optJSONObject("createdAt") != null) {
                try {
                    createdAt = Long.parseLong(fields.optJSONObject("createdAt").optString("integerValue", "0"));
                } catch (Exception ignored) {}
            }

            int correct = 0;
            if (fields.has("correct") && fields.optJSONObject("correct") != null) {
                try {
                    correct = Integer.parseInt(fields.optJSONObject("correct").optString("integerValue", "0"));
                } catch (Exception ignored) {}
            }

            List<String> options = new ArrayList<>();
            if (fields.has("options") && fields.optJSONObject("options") != null) {
                JSONObject optObj = fields.optJSONObject("options").optJSONObject("arrayValue");
                if (optObj != null) {
                    JSONArray arr = optObj.optJSONArray("values");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject item = arr.optJSONObject(i);
                            if (item != null) {
                                options.add(item.optString("stringValue", ""));
                            }
                        }
                    }
                }
            }

            if (prompt.isEmpty() || options.size() < 4) return null;
            return new Question(id, prompt, options, correct, diff, createdBy, true, createdAt);
        } catch (Exception e) {
            return null;
        }
    }
}


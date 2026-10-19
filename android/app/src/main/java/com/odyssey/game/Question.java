package com.odyssey.game;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Question {
    public String id;
    public String q;
    public List<String> options;
    public int correct; // 0, 1, 2, or 3
    public String difficulty; // "easy", "medium", "hard"
    public String createdBy;
    public boolean isCustom;
    public long createdAt;

    // Built-in question constructor
    public Question(String q, List<String> options, int correct) {
        this("", q, options, correct, "easy", "System", false, 0L);
    }

    // Built-in question with difficulty
    public Question(String q, List<String> options, int correct, String difficulty) {
        this("", q, options, correct, difficulty, "System", false, 0L);
    }

    // Full constructor
    public Question(String id, String q, List<String> options, int correct, String difficulty, String createdBy, boolean isCustom, long createdAt) {
        this.id = id != null ? id : "";
        this.q = q != null ? q : "";
        this.options = options != null ? options : new ArrayList<>();
        this.correct = correct;
        this.difficulty = difficulty != null ? difficulty.toLowerCase() : "easy";
        this.createdBy = createdBy != null ? createdBy : "Admin";
        this.isCustom = isCustom;
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("q", q);
            JSONArray arr = new JSONArray();
            if (options != null) {
                for (String opt : options) {
                    arr.put(opt);
                }
            }
            obj.put("options", arr);
            obj.put("correct", correct);
            obj.put("difficulty", difficulty);
            obj.put("createdBy", createdBy);
            obj.put("isCustom", isCustom);
            obj.put("createdAt", createdAt);
        } catch (Exception ignored) {}
        return obj;
    }

    public static Question fromJson(JSONObject obj) {
        if (obj == null) return null;
        try {
            String id = obj.optString("id", "");
            String q = obj.optString("q", "");
            int correct = obj.optInt("correct", 0);
            String difficulty = obj.optString("difficulty", "easy");
            String createdBy = obj.optString("createdBy", "Admin");
            boolean isCustom = obj.optBoolean("isCustom", false);
            long createdAt = obj.optLong("createdAt", 0L);
            List<String> options = new ArrayList<>();
            JSONArray arr = obj.optJSONArray("options");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    options.add(arr.optString(i, ""));
                }
            }
            if (q.isEmpty() || options.size() < 4) return null;
            return new Question(id, q, options, correct, difficulty, createdBy, isCustom, createdAt);
        } catch (Exception e) {
            return null;
        }
    }
}

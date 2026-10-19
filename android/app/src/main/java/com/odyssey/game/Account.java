package com.odyssey.game;

public class Account {
    public String username;
    public String name;
    public String password;
    public int currentWinStreak = 0;
    public int bestWinStreak = 0;
    public boolean isAdmin = false;
    public int loginCount = 1;
    public String lastLogin = "";
    public boolean isOnline = false;
    public long lastActiveMillis = 0L;
    public String bio = "CSS Explorer & Space Adventurer 🌌";
    public String statusBubble = "Ready to code? 🚀";
    public String avatarBase64 = "";
    public java.util.List<String> achievements = new java.util.ArrayList<>();
    public boolean unlockedMedium = false;
    public boolean unlockedHard = false;

    public Account(String username, String name, String password) {
        this.username = (username != null) ? username.trim().toLowerCase() : "";
        this.name = (name != null) ? name.trim() : "";
        this.password = (password != null) ? password : "";
        if ("biar".equalsIgnoreCase(this.username)) {
            this.isAdmin = true;
            this.statusBubble = "Coffee or code? ☕";
            this.unlockedMedium = true;
            this.unlockedHard = true;
        }
    }

    public boolean isUserActuallyOnline() {
        if (!isOnline) return false;
        if (lastActiveMillis > 0) {
            return (System.currentTimeMillis() - lastActiveMillis) < 300000L;
        }
        return true;
    }
}

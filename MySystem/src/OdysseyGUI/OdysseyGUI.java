package OdysseyGUI;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
 import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;


/**
 *
 * @author axchel reu galangue
 */
public class OdysseyGUI extends JFrame {

    // --- Color Palette ---
    private final Color background = new Color(15, 18, 40);
    private final Color cardColor = new Color(29, 34, 67);
    private final Color purple = new Color(124, 92, 255);
    private final Color cyan = new Color(72, 211, 220);
    private final Color white = new Color(245, 246, 255);
    private final Color textMuted = new Color(180, 187, 220);

    // --- Data Classes ---
    public static class Question {
        String q;
        List<String> options;
        int correct;

        public Question(String q, List<String> options, int correct) {
            this.q = q;
            this.options = options;
            this.correct = correct;
        }
    }

    public static class Account {
        String username;
        String name;
        String password;
        int currentWinStreak = 0;
        int bestWinStreak = 0;

        public Account(String username, String name, String password) {
            this.username = username;
            this.name = name;
            this.password = password;
        }
    }

    public static class LevelResult {
        String difficulty;
        int score;
        int points;

        public LevelResult(String difficulty, int score, int points) {
            this.difficulty = difficulty;
            this.score = score;
            this.points = points;
        }
    }

    // --- Question Bank & Accounts ---
    private static final Map<String, List<Question>> questions = new HashMap<>();
    private static final Map<String, Account> accounts = new HashMap<>();
    private static final String[] difficultyOrder = {"easy", "normal", "hard"};

    private Account currentAccount;
    private String selectedCharacter;
    private String startingDifficulty;

    static {
        // Easy Questions
        questions.put("easy", Arrays.asList(
            new Question("Which keyword declares a class in Java?", Arrays.asList("object", "class", "define", "struct"), 1),
            new Question("Which CSS property changes text color?", Arrays.asList("font-style", "text-size", "color", "background"), 2),
            new Question("Which symbol ends most Java statements?", Arrays.asList(":", ".", ";", ","), 2),
            new Question("Which CSS selector targets a class named card?", Arrays.asList("#card", ".card", "card()", "*card"), 1),
            new Question("Which Java type stores true or false?", Arrays.asList("int", "String", "boolean", "char"), 2)
        ));

        // Normal Questions
        questions.put("normal", Arrays.asList(
            new Question("Which method is the usual entry point of a Java application?", Arrays.asList("start()", "main()", "runApp()", "init()"), 1),
            new Question("Which CSS layout system is designed for one-dimensional rows or columns?", Arrays.asList("Float", "Flexbox", "Table", "Position"), 1),
            new Question("Which Java keyword creates a subclass?", Arrays.asList("implements", "extends", "inherits", "superclass"), 1),
            new Question("What does display: none do in CSS?", Arrays.asList("Hides the element and removes its layout space", "Makes the element transparent", "Moves the element behind others", "Disables its text"), 0),
            new Question("Which Java collection stores unique elements?", Arrays.asList("List", "Set", "Queue", "ArrayList"), 1)
        ));

        // Hard Questions
        questions.put("hard", Arrays.asList(
            new Question("Which Java feature allows a class to implement multiple interfaces?", Arrays.asList("Multiple class inheritance", "Multiple interface implementation", "Method overloading", "Package importing"), 1),
            new Question("Which CSS function is commonly used to define a responsive grid track?", Arrays.asList("calc()", "minmax()", "translate()", "rotate()"), 1),
            new Question("What does the finally block in Java generally guarantee?", Arrays.asList("It runs after try/catch processing, even when an exception occurs", "It catches every compiler error", "It runs only when no exception occurs", "It retries the try block"), 0),
            new Question("In CSS Grid, what does 1fr represent?", Arrays.asList("One fixed pixel", "One fraction of available space", "One font size", "One row"), 1),
            new Question("Which Java concept bundles data with 55methods and restricts direct access?", Arrays.asList("Recursion", "Encapsulation", "Casting", "Iteration"), 1)
        ));
    }

    public OdysseyGUI() {
        setTitle("Odyssey - Quest Quiz");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Authenticate user on startup
        SwingUtilities.invokeLater(this::showAuthDialog);
    }

    // --- Authentication Dialog ---
    private void showAuthDialog() {
        JDialog authDialog = new JDialog(this, "Odyssey - Authentication", true);
        authDialog.setSize(400, 300);
        authDialog.setLocationRelativeTo(this);
        authDialog.setResizable(false);

        JPanel panel = new JPanel();
        panel.setBackground(background);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        JLabel title = new JLabel("PLAYER AUTHENTICATION");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        title.setForeground(cyan);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JComboBox<String> modeBox = new JComboBox<>(new String[]{"Log in", "Sign up"});
        modeBox.setMaximumSize(new Dimension(300, 35));
        modeBox.setBackground(cardColor);
        modeBox.setForeground(white);

        JTextField nameField = new JTextField();
        nameField.setMaximumSize(new Dimension(300, 35));
        nameField.setBackground(cardColor);
        nameField.setForeground(white);

        JPasswordField passField = new JPasswordField();
        passField.setMaximumSize(new Dimension(300, 35));
        passField.setBackground(cardColor);
        passField.setForeground(white);

        JButton submitBtn = createButton("CONTINUE", purple);

        panel.add(title);
        panel.add(Box.createVerticalStrut(15));
        panel.add(new JLabel("Mode:") {{ setForeground(white);setAlignmentX(Component.CENTER_ALIGNMENT); }});
        panel.add(modeBox);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JLabel("Player Name:") {{ setForeground(white);setAlignmentX(Component.CENTER_ALIGNMENT); }});
        panel.add(nameField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JLabel("Password:") {{ setForeground(white);setAlignmentX(Component.CENTER_ALIGNMENT); }});
        panel.add(passField);
        panel.add(Box.createVerticalStrut(20));
        panel.add(submitBtn);

        submitBtn.addActionListener(e -> {
            String mode = (String) modeBox.getSelectedItem();
            String name = nameField.getText().trim();
            String password = new String(passField.getPassword()).trim();

            if (name.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(authDialog, "Please fill in all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String username = name.toLowerCase();

            if ("Sign up".equals(mode)) {
                if (accounts.containsKey(username)) {
                    JOptionPane.showMessageDialog(authDialog, "Player name already exists. Logging in...", "Notice", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    Account newAcc = new Account(username, name, password);
                    accounts.put(username, newAcc);
                }
            }

            Account acc = accounts.get(username);
            if (acc == null || !acc.password.equals(password)) {
                JOptionPane.showMessageDialog(authDialog, "Invalid username or password.", "Auth Failed", JOptionPane.ERROR_MESSAGE);
            } else {
                currentAccount = acc;
                authDialog.dispose();
                showMainMenu();
            }
        });

        authDialog.setContentPane(panel);
        authDialog.setVisible(true);
    }

    // --- Main Menu Screen ---
    public void showMainMenu() {
        JPanel main = new JPanel();
        main.setBackground(background);
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(BorderFactory.createEmptyBorder(45, 100, 40, 100));

        JLabel logo = new JLabel(" ODYSSEY");
        logo.setFont(new Font("Serif", Font.BOLD, 38));
        logo.setForeground(cyan);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("QUEST QUIZ — Welcome, " + currentAccount.name + "!");
        subtitle.setFont(new Font("Arial", Font.BOLD, 15));
        subtitle.setForeground(white);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel description = new JLabel("Choose your path. Test your knowledge.");
        description.setFont(new Font("Arial", Font.PLAIN, 15));
        description.setForeground(textMuted);
        description.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton playButton = createButton("START ADVENTURE", purple);
        JButton howButton = createButton("HOW TO PLAY", cardColor);
        JButton exitButton = createButton("EXIT GAME", new Color(160, 55, 80));

        main.add(logo);
        main.add(Box.createVerticalStrut(5));
        main.add(subtitle);
        main.add(Box.createVerticalStrut(15));
        main.add(description);
        main.add(Box.createVerticalStrut(45));
        main.add(playButton);
        main.add(Box.createVerticalStrut(15));
        main.add(howButton);
        main.add(Box.createVerticalStrut(15));
        main.add(exitButton);

        playButton.addActionListener(e -> showCharacterSelection());
        howButton.addActionListener(e ->
                JOptionPane.showMessageDialog(this,
                        "Choose your guide, select difficulty, and answer quiz questions.\n"
                        + "Correct answers earn points and build your win streak!",
                        "How to Play",
                        JOptionPane.INFORMATION_MESSAGE));
        exitButton.addActionListener(e -> dispose());

        setContentPane(main);
        revalidate();
        setVisible(true);
    }

    // --- Character Selection Screen ---
    private void showCharacterSelection() {
        String[] guides = {"Java", "CSS", "Selector", "JVM"};
        String guide = (String) JOptionPane.showInputDialog(
                this,
                "Select your Odyssey Guide:",
                "Character Selection",
                JOptionPane.QUESTION_MESSAGE,
                null,
                guides,
                guides[0]
        );

        if (guide != null) {
            selectedCharacter = guide;
            showDifficultySelection();
        }
    }

    // --- Difficulty Selection Screen ---
    private void showDifficultySelection() {
        String[] choices = {"Easy", "Normal", "Hard"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Select your starting challenge level!",
                "Choose Difficulty",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                choices,
                choices[0]);

        if (choice >= 0) {
            startingDifficulty = difficultyOrder[choice];
            startQuizGameplay();
        }
    }

    // --- Gameplay Engine ---
    private void startQuizGameplay() {
        int startIndex = Arrays.asList(difficultyOrder).indexOf(startingDifficulty);
        List<String> activeDifficulties = Arrays.asList(difficultyOrder).subList(startIndex, difficultyOrder.length);

        List<LevelResult> completedLevels = new ArrayList<>();
        
        // Run through levels sequentially
        for (String level : activeDifficulties) {
            List<Question> levelQuestions = new ArrayList<>(questions.get(level));
            Collections.shuffle(levelQuestions);

            int score = runQuizLevel(level, levelQuestions);
            if (score == -1) return; // User aborted/closed

            int pointsPerAnswer = level.equals("hard") ? 20 : level.equals("normal") ? 15 : 10;
            completedLevels.add(new LevelResult(level, score, score * pointsPerAnswer));
        }

        // Tally results
        int totalCorrect = completedLevels.stream().mapToInt(l -> l.score).sum();
        int totalQuestions = completedLevels.size() * 5;
        int totalPoints = completedLevels.stream().mapToInt(l -> l.points).sum();
        boolean won = totalCorrect >= Math.ceil(totalQuestions * 0.6);

        if (won) {
            currentAccount.currentWinStreak++;
            currentAccount.bestWinStreak = Math.max(currentAccount.bestWinStreak, currentAccount.currentWinStreak);
        } else {
            currentAccount.currentWinStreak = 0;
        }

        showSummaryScreen(totalCorrect, totalQuestions, totalPoints, won);
    }

    // --- Single Level Quiz Runner Dialog/Panel ---
    private int runQuizLevel(String levelName, List<Question> levelQuestions) {
        final int[] score = {0};
        final int[] qIndex = {0};

        JDialog levelDialog = new JDialog(this, "Level: " + levelName.toUpperCase(), true);
        levelDialog.setSize(750, 500);
        levelDialog.setLocationRelativeTo(this);
        levelDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setBackground(background);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel headerLabel = new JLabel("=== LEVEL: " + levelName.toUpperCase() + " ===");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        headerLabel.setForeground(cyan);
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel questionLabel = new JLabel();
        questionLabel.setFont(new Font("Arial", Font.BOLD, 15));
        questionLabel.setForeground(white);
        questionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        ButtonGroup group = new ButtonGroup();
        JRadioButton[] optionButtons = new JRadioButton[4];
        for (int i = 0; i < 4; i++) {
            optionButtons[i] = new JRadioButton();
            optionButtons[i].setFont(new Font("Arial", Font.PLAIN, 14));
            optionButtons[i].setForeground(white);
            optionButtons[i].setBackground(cardColor);
            optionButtons[i].setFocusPainted(false);
            optionButtons[i].setAlignmentX(Component.CENTER_ALIGNMENT);
            group.add(optionButtons[i]);
        }

        JButton nextBtn = createButton("SUBMIT ANSWER", purple);

        panel.add(headerLabel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(questionLabel);
        panel.add(Box.createVerticalStrut(25));

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBackground(background);
        for (JRadioButton rb : optionButtons) {
            optionsPanel.add(rb);
            optionsPanel.add(Box.createVerticalStrut(10));
        }
        panel.add(optionsPanel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(nextBtn);

        class QuestionLoader {
            void loadNext() {
                if (qIndex[0] < levelQuestions.size()) {
                    group.clearSelection();
                    Question q = levelQuestions.get(qIndex[0]);
                    questionLabel.setText("<html><center>[Q" + (qIndex[0] + 1) + "/5] " + q.q + "</center></html>");

                    // Shuffle options
                    List<String> shuffledOptions = new ArrayList<>(q.options);
                    String correctAnswerText = q.options.get(q.correct);
                    Collections.shuffle(shuffledOptions);
                    int newCorrectIndex = shuffledOptions.indexOf(correctAnswerText);

                    for (int i = 0; i < 4; i++) {
                        optionButtons[i].setText((char)('A' + i) + ". " + shuffledOptions.get(i));
                        optionButtons[i].setActionCommand(String.valueOf(i));
                    }
                    nextBtn.putClientProperty("correctIndex", newCorrectIndex);
                    qIndex[0]++;
                } else {
                    levelDialog.dispose();
                }
            }
        }

        QuestionLoader loader = new QuestionLoader();
        loader.loadNext();

        nextBtn.addActionListener(e -> {
            int correctIdx = (int) nextBtn.getClientProperty("correctIndex");
            int selectedIdx = -1;

            for (int i = 0; i < 4; i++) {
                if (optionButtons[i].isSelected()) {
                    selectedIdx = i;
                    break;
                }
            }

            if (selectedIdx == -1) {
                JOptionPane.showMessageDialog(levelDialog, "Please select an option.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (selectedIdx == correctIdx) {
                JOptionPane.showMessageDialog(levelDialog, "Correct!", "Result", JOptionPane.INFORMATION_MESSAGE);
                score[0]++;
            } else {
                JOptionPane.showMessageDialog(levelDialog, "Incorrect! The correct answer was: " + (char)('A' + correctIdx), "Result", JOptionPane.INFORMATION_MESSAGE);
            }

            loader.loadNext();
        });

        levelDialog.setContentPane(panel);
        levelDialog.setVisible(true);

        return score[0];
    }

    // --- Results Summary Screen ---
    private void showSummaryScreen(int totalCorrect, int totalQuestions, int totalPoints, boolean won) {
        JPanel summaryPanel = new JPanel();
        summaryPanel.setBackground(background);
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(40, 80, 40, 80));

        JLabel title = new JLabel(won ? "🎉 YOU WIN!" : "🌱 KEEP GROWING!");
        title.setFont(new Font("Serif", Font.BOLD, 32));
        title.setForeground(cyan);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel info = new JLabel("<html><center>"
                + "<b>Player:</b> " + currentAccount.name + " (" + selectedCharacter + ")<br>"
                + "<b>Correct Answers:</b> " + totalCorrect + " / " + totalQuestions + "<br>"
                + "<b>Points Earned:</b> " + totalPoints + "<br>"
                + "<b>Win Streak:</b> " + currentAccount.currentWinStreak + " (Best: " + currentAccount.bestWinStreak + ")"
                + "</center></html>");
        info.setFont(new Font("Arial", Font.PLAIN, 16));
        info.setForeground(white);
        info.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton menuBtn = createButton("BACK TO MAIN MENU", purple);
        JButton exitBtn = createButton("EXIT GAME", new Color(160, 55, 80));

        summaryPanel.add(title);
        summaryPanel.add(Box.createVerticalStrut(20));
        summaryPanel.add(info);
        summaryPanel.add(Box.createVerticalStrut(30));
        summaryPanel.add(menuBtn);
        summaryPanel.add(Box.createVerticalStrut(15));
        summaryPanel.add(exitBtn);

        menuBtn.addActionListener(e -> showMainMenu());
        exitBtn.addActionListener(e -> dispose());

        setContentPane(summaryPanel);
        revalidate();
        setVisible(true);
    }

    // --- Button Factory ---
    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 15));
        button.setForeground(white);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(320, 52));
        button.setPreferredSize(new Dimension(320, 52));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OdysseyGUI().setVisible(true));
    }
}

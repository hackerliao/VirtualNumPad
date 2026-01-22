import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class VirtualNumpad extends JFrame {
    private boolean isAlwaysOnTop = false;
    private boolean isNumLockMode = true;
    private boolean showNotifications = false;
    private boolean isDarkMode = false;
    private boolean isFrostedButtons = false;
    private Set<Integer> pressedKeys = new HashSet<>();
    private Map<String, String> currentLanguage = new HashMap<>();
    private Map<String, Map<String, String>> languages = new HashMap<>();
    private String currentLangCode = "en-us";
    private String backgroundImagePath = null;

    // Configuration
    private Preferences prefs;
    private static final String PREFS_NODE = "com/virtualnumpad";
    private static final String PREF_LANGUAGE = "language";
    private static final String PREF_ALWAYS_ON_TOP = "alwaysOnTop";
    private static final String PREF_NUM_LOCK_MODE = "numLockMode";
    private static final String PREF_SHOW_NOTIFICATIONS = "showNotifications";
    private static final String PREF_DARK_MODE = "darkMode";
    private static final String PREF_BACKGROUND_IMAGE = "backgroundImage";
    private static final String PREF_FROSTED_BUTTONS = "frostedButtons";

    // Keyboard shortcuts
    private static final int TOGGLE_TOP_KEY = KeyEvent.VK_T;
    private static final int TOGGLE_MODE_KEY = KeyEvent.VK_NUMPAD0;
    private static final int TOGGLE_LANG_KEY = KeyEvent.VK_L;
    private static final int TOGGLE_THEME_KEY = KeyEvent.VK_D;

    // UI Components
    private JPanel numpadPanel;
    private JPanel mainPanel;
    private JLabel statusLabel;
    private JLabel modeLabel;
    private JComboBox<String> languageComboBox;
    private SystemTray systemTray;
    private TrayIcon trayIcon;
    private JMenuItem languageMenuItem; // 添加这个成员变量来跟踪语言菜单项

    // Font for System Tray (AWT) to fix Chinese display issues
    private Font trayFont;

    // Button configurations
    private String[][] numKeys = {
            {"7", "8", "9", "/"},
            {"4", "5", "6", "*"},
            {"1", "2", "3", "-"},
            {"0", ".", "=", "+"}
    };

    private String[][] shortcutKeys = {
            {"COPY", "PASTE", "SAVE", "CUT"},
            {"UNDO", "REDO", "NEW", "OPEN"},
            {"FIND", "REPLACE", "PRINT", "HELP"},
            {"TOGGLE_TOP", "TOGGLE_MODE", "TOGGLE_THEME", "EXIT"}
    };

    public VirtualNumpad() {
        // Load preferences
        loadPreferences();

        // Set UTF-8 encoding for file reading
        System.setProperty("file.encoding", "UTF-8");

        // Load language files
        loadLanguages();

        // Initialize UI
        initUI();

        // Setup system tray
        setupSystemTray();

        // Setup keyboard listener
        setupGlobalKeyListener();

        // Update initial status
        updateStatus();
        setAlwaysOnTop(isAlwaysOnTop);

        // Set window to not steal focus
        setFocusableWindowState(false);

        // Set window icon
        setWindowIcon();

        // Window listener to save preferences on close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Hide window instead of closing
                setVisible(false);
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                // Keep window on top even when not focused
                if (isAlwaysOnTop) {
                    setAlwaysOnTop(true);
                }
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Load user preferences
     */
    private void loadPreferences() {
        prefs = Preferences.userRoot().node(PREFS_NODE);
        currentLangCode = prefs.get(PREF_LANGUAGE, "en-us");
        isAlwaysOnTop = prefs.getBoolean(PREF_ALWAYS_ON_TOP, false);
        isNumLockMode = prefs.getBoolean(PREF_NUM_LOCK_MODE, true);
        showNotifications = prefs.getBoolean(PREF_SHOW_NOTIFICATIONS, false);
        isDarkMode = prefs.getBoolean(PREF_DARK_MODE, false);
        backgroundImagePath = prefs.get(PREF_BACKGROUND_IMAGE, null);
        isFrostedButtons = prefs.getBoolean(PREF_FROSTED_BUTTONS, false);
    }

    /**
     * Save user preferences
     */
    private void savePreferences() {
        prefs.put(PREF_LANGUAGE, currentLangCode);
        prefs.putBoolean(PREF_ALWAYS_ON_TOP, isAlwaysOnTop);
        prefs.putBoolean(PREF_NUM_LOCK_MODE, isNumLockMode);
        prefs.putBoolean(PREF_SHOW_NOTIFICATIONS, showNotifications);
        prefs.putBoolean(PREF_DARK_MODE, isDarkMode);
        prefs.putBoolean(PREF_FROSTED_BUTTONS, isFrostedButtons);
        if (backgroundImagePath != null) {
            prefs.put(PREF_BACKGROUND_IMAGE, backgroundImagePath);
        } else {
            prefs.remove(PREF_BACKGROUND_IMAGE);
        }

        try {
            prefs.flush();
        } catch (Exception e) {
            System.err.println("Error saving preferences: " + e.getMessage());
        }
    }

    /**
     * Set window icon from icon.ico file
     */
    private void setWindowIcon() {
        try {
            // Try to load icon from file in the same directory
            File iconFile = new File("icon.ico");
            if (iconFile.exists()) {
                Image image = new ImageIcon("icon.ico").getImage();
                setIconImage(image);
                System.out.println("Window icon loaded from icon.ico");
            } else {
                // Create default icon
                Image icon = createDefaultIcon();
                setIconImage(icon);
                System.out.println("Using default window icon (icon.ico not found)");
            }
        } catch (Exception e) {
            System.err.println("Error loading window icon: " + e.getMessage());
        }
    }

    /**
     * Create default icon image
     */
    private Image createDefaultIcon() {
        int size = 32;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw a keyboard icon
        g2d.setColor(Color.BLUE);
        g2d.fillRoundRect(4, 4, size-8, size-8, 8, 8);

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(10, 12, 22, 12);
        g2d.drawLine(10, 16, 22, 16);
        g2d.drawLine(10, 20, 22, 20);

        g2d.dispose();
        return image;
    }

    /**
     * Load all language files with UTF-8 encoding
     */
    private void loadLanguages() {
        // Clear existing languages
        languages.clear();

        // Try to load from languages folder
        File languagesDir = new File("languages");

        // Create languages folder if it doesn't exist
        if (!languagesDir.exists()) {
            if (languagesDir.mkdir()) {
                System.out.println("Created languages folder");
            }
        }

        if (languagesDir.exists() && languagesDir.isDirectory()) {
            loadLanguageFilesFromDirectory(languagesDir);
            System.out.println("Loading languages from languages folder");
        } else {
            // Try to load from current directory as fallback
            System.out.println("Languages folder not found, trying current directory");
            loadLanguageFilesFromDirectory(new File("."));
        }

        // Create default languages if no files were found
        if (languages.isEmpty()) {
            createDefaultLanguages();
            System.out.println("Created default languages");
        }

        // Set current language
        if (languages.containsKey(currentLangCode)) {
            currentLanguage = languages.get(currentLangCode);
        } else if (!languages.isEmpty()) {
            // If saved language not found, use the first available language
            currentLangCode = languages.keySet().iterator().next();
            currentLanguage = languages.get(currentLangCode);
            System.out.println("Saved language not found, using: " + currentLangCode);
        }

        System.out.println("Current language: " + currentLangCode);
        System.out.println("Available languages: " + languages.keySet());
    }

    /**
     * Load language files from a specific directory
     */
    private void loadLanguageFilesFromDirectory(File dir) {
        try {
            File[] files = dir.listFiles((d, name) -> {
                // Accept any .txt file as language file
                return name.toLowerCase().endsWith(".txt");
            });

            if (files != null) {
                System.out.println("Found " + files.length + " potential language files");

                for (File file : files) {
                    String fileName = file.getName();
                    String langCode = fileName.substring(0, fileName.lastIndexOf('.'));

                    // Validate language code format (xx-xx or xx_xx)
                    if (langCode.matches("^[a-z]{2}[_-][a-z]{2}$")) {
                        loadLanguageFile(langCode, file);
                    } else {
                        System.out.println("Skipping file with invalid name format: " + fileName);
                    }
                }
            } else {
                System.out.println("No files found in directory: " + dir.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Error loading language files from directory: " + dir.getAbsolutePath());
            e.printStackTrace();
        }
    }

    /**
     * Load a specific language file with UTF-8 encoding
     */
    private void loadLanguageFile(String langCode, File file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            Map<String, String> langMap = new HashMap<>();
            String line;
            int lineCount = 0;

            while ((line = reader.readLine()) != null) {
                lineCount++;
                line = line.trim();
                if (!line.isEmpty() && line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        langMap.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }

            if (!langMap.isEmpty()) {
                languages.put(langCode, langMap);
                System.out.println("Successfully loaded language: " + langCode +
                        " from " + file.getName() +
                        " (" + langMap.size() + " translations)");
            } else {
                System.out.println("Warning: Language file " + file.getName() + " is empty or invalid");
            }

        } catch (IOException e) {
            System.err.println("Error loading language file: " + file.getName());
            e.printStackTrace();
        }
    }

    /**
     * Create default language translations
     */
    private void createDefaultLanguages() {
        // English translations
        Map<String, String> enMap = new HashMap<>();
        enMap.put("window.title", "Virtual Numpad");
        enMap.put("status.label", "Status");
        enMap.put("mode.label", "Mode");
        enMap.put("top.on", "Always on Top");
        enMap.put("top.off", "Not Always on Top");
        enMap.put("mode.num", "Number Mode");
        enMap.put("mode.shortcut", "Shortcut Mode");
        enMap.put("button.toggle.top", "Toggle Top");
        enMap.put("button.toggle.mode", "Toggle Mode");
        enMap.put("button.toggle.theme", "Toggle Theme");
        enMap.put("button.background", "Set Background");
        enMap.put("button.clear.background", "Clear Background");
        enMap.put("button.frosted", "Frosted Buttons");
        enMap.put("button.copy", "Copy");
        enMap.put("button.paste", "Paste");
        enMap.put("button.save", "Save");
        enMap.put("button.cut", "Cut");
        enMap.put("button.undo", "Undo");
        enMap.put("button.redo", "Redo");
        enMap.put("button.new", "New");
        enMap.put("button.open", "Open");
        enMap.put("button.find", "Find");
        enMap.put("button.replace", "Replace");
        enMap.put("button.print", "Print");
        enMap.put("button.help", "Help");
        enMap.put("button.close", "Close");
        enMap.put("button.exit", "Exit");
        enMap.put("button.toggle_top", "Top");
        enMap.put("button.toggle_mode", "Mode");
        enMap.put("button.toggle_theme", "Theme");
        enMap.put("button.confirm", "=");
        enMap.put("message.toggle.top.on", "Window is now always on top");
        enMap.put("message.toggle.top.off", "Window is no longer always on top");
        enMap.put("message.mode.num", "Switched to Number Mode");
        enMap.put("message.mode.shortcut", "Switched to Shortcut Mode");
        enMap.put("message.theme.light", "Switched to Light Mode");
        enMap.put("message.theme.dark", "Switched to Dark Mode");
        enMap.put("message.frosted.on", "Frosted effect enabled");
        enMap.put("message.frosted.off", "Frosted effect disabled");
        enMap.put("message.input", "Input");
        enMap.put("message.execute", "Execute");
        enMap.put("message.exit.confirm", "Are you sure you want to exit?");
        enMap.put("message.language.changed", "Language changed to: ");
        enMap.put("message.background.set", "Background image set successfully");
        enMap.put("message.background.removed", "Background image removed");
        enMap.put("message.background.error", "Error loading background image");
        enMap.put("author.info", "Author: hacker_liao");
        enMap.put("language.english", "English");
        enMap.put("language.chinese", "Chinese");
        enMap.put("language.traditional_chinese", "Traditional Chinese");
        enMap.put("menu.language", "Language");
        enMap.put("menu.about", "About");
        enMap.put("menu.skins", "Skins");
        enMap.put("menu.notifications", "Notifications");
        enMap.put("menu.notifications.on", "Show Notifications");
        enMap.put("menu.notifications.off", "Hide Notifications");
        enMap.put("about.title", "About Virtual Numpad");
        enMap.put("about.version", "Version 1.0");
        enMap.put("about.features", "Features: Virtual Numpad, Window Top Toggle, NumLock Mode Switch, Multi-language Support, System Tray, Configurations, Theme Switching, Custom Background, Frosted Buttons");
        enMap.put("about.shortcuts", "Shortcuts: Ctrl+T (Toggle Top), Alt+N/NumLock (Toggle Mode), Ctrl+L (Language), Ctrl+D (Theme)");
        enMap.put("tray.restore", "Restore");
        enMap.put("tray.top.on", "Top On");
        enMap.put("tray.top.off", "Top Off");
        enMap.put("tray.mode.num", "Number Mode");
        enMap.put("tray.mode.shortcut", "Shortcut Mode");
        enMap.put("tray.theme.light", "Light Theme");
        enMap.put("tray.theme.dark", "Dark Theme");
        enMap.put("tray.background", "Set Background");
        enMap.put("tray.exit", "Exit");

        // Chinese translations
        Map<String, String> zhMap = new HashMap<>();
        zhMap.put("window.title", "虚拟数字键盘");
        zhMap.put("status.label", "状态");
        zhMap.put("mode.label", "模式");
        zhMap.put("top.on", "已置顶");
        zhMap.put("top.off", "未置顶");
        zhMap.put("mode.num", "数字模式");
        zhMap.put("mode.shortcut", "快捷键模式");
        zhMap.put("button.toggle.top", "切换置顶");
        zhMap.put("button.toggle.mode", "切换模式");
        zhMap.put("button.toggle.theme", "切换主题");
        zhMap.put("button.background", "设置背景");
        zhMap.put("button.clear.background", "清除背景");
        zhMap.put("button.frosted", "按钮雾化");
        zhMap.put("button.copy", "复制");
        zhMap.put("button.paste", "粘贴");
        zhMap.put("button.save", "保存");
        zhMap.put("button.cut", "剪切");
        zhMap.put("button.undo", "撤销");
        zhMap.put("button.redo", "重做");
        zhMap.put("button.new", "新建");
        zhMap.put("button.open", "打开");
        zhMap.put("button.find", "查找");
        zhMap.put("button.replace", "替换");
        zhMap.put("button.print", "打印");
        zhMap.put("button.help", "帮助");
        zhMap.put("button.close", "关闭");
        zhMap.put("button.exit", "退出");
        zhMap.put("button.toggle_top", "置顶");
        zhMap.put("button.toggle_mode", "模式");
        zhMap.put("button.toggle_theme", "主题");
        zhMap.put("button.confirm", "=");
        zhMap.put("message.toggle.top.on", "窗口已置顶");
        zhMap.put("message.toggle.top.off", "窗口取消置顶");
        zhMap.put("message.mode.num", "切换到数字模式");
        zhMap.put("message.mode.shortcut", "切换到快捷键模式");
        zhMap.put("message.theme.light", "切换到浅色模式");
        zhMap.put("message.theme.dark", "切换到深色模式");
        zhMap.put("message.frosted.on", "雾化效果已启用");
        zhMap.put("message.frosted.off", "雾化效果已禁用");
        zhMap.put("message.input", "输入");
        zhMap.put("message.execute", "执行");
        zhMap.put("message.exit.confirm", "确定要退出程序吗？");
        zhMap.put("message.language.changed", "语言已切换至：");
        zhMap.put("message.background.set", "背景图片设置成功");
        zhMap.put("message.background.removed", "背景图片已移除");
        zhMap.put("message.background.error", "背景图片加载失败");
        zhMap.put("author.info", "作者：hacker_liao");
        zhMap.put("language.english", "英文");
        zhMap.put("language.chinese", "中文");
        zhMap.put("language.traditional_chinese", "繁体中文");
        zhMap.put("menu.language", "语言");
        zhMap.put("menu.about", "关于");
        zhMap.put("menu.skins", "皮肤");
        zhMap.put("menu.notifications", "通知");
        zhMap.put("menu.notifications.on", "显示通知");
        zhMap.put("menu.notifications.off", "隐藏通知");
        zhMap.put("about.title", "关于虚拟数字键盘");
        zhMap.put("about.version", "版本 1.0");
        zhMap.put("about.features", "功能：虚拟数字键盘、窗口置顶切换、NumLock模式切换、多语言支持、系统托盘、配置保存、主题切换、自定义背景、按钮雾化");
        zhMap.put("about.shortcuts", "快捷键：Ctrl+T (切换置顶), Alt+N/NumLock (切换模式), Ctrl+L (语言), Ctrl+D (主题)");
        zhMap.put("tray.restore", "恢复");
        zhMap.put("tray.top.on", "置顶开");
        zhMap.put("tray.top.off", "置顶关");
        zhMap.put("tray.mode.num", "数字模式");
        zhMap.put("tray.mode.shortcut", "快捷键模式");
        zhMap.put("tray.theme.light", "浅色主题");
        zhMap.put("tray.theme.dark", "深色主题");
        zhMap.put("tray.background", "设置背景");
        zhMap.put("tray.exit", "退出");

        languages.put("en-us", enMap);
        languages.put("zh-cn", zhMap);
        currentLanguage = enMap;
    }

    /**
     * Get translation for a key
     */
    private String getTranslation(String key) {
        return currentLanguage.getOrDefault(key, key);
    }

    /**
     * Get display name for a language code
     */
    private String getLanguageDisplayName(String langCode) {
        String displayName = langCode;

        // Check if we have a translation for this language
        String translationKey = "language." + langCode.toLowerCase().replace("-", "_").replace("_", ".");
        if (currentLanguage.containsKey(translationKey)) {
            displayName = currentLanguage.get(translationKey);
        } else {
            // Try to get from the language's own map
            if (languages.containsKey(langCode)) {
                Map<String, String> langMap = languages.get(langCode);
                if (langMap.containsKey(translationKey)) {
                    displayName = langMap.get(translationKey);
                }
            }
        }

        return displayName;
    }

    /**
     * Initialize the user interface
     */
    private void initUI() {
        setTitle(getTranslation("window.title"));
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); // Hide window instead of closing
        setSize(520, 600);

        // Main panel with background support
        mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImagePath != null) {
                    try {
                        File imgFile = new File(backgroundImagePath);
                        if (imgFile.exists()) {
                            Image img = ImageIO.read(imgFile);
                            g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
                        }
                    } catch (IOException e) {
                        System.err.println("Error drawing background image: " + e.getMessage());
                    }
                }
            }
        };

        setContentPane(mainPanel);
        mainPanel.setFocusable(false);

        // Make window non-focusable to prevent stealing focus
        setFocusable(false);

        // Create menu bar
        JMenuBar menuBar = new JMenuBar();

        // Language menu
        JMenu languageMenu = new JMenu(getTranslation("menu.language"));
        languageMenu.setFocusable(false);

        // Create language combo box
        languageComboBox = new JComboBox<>();
        languageComboBox.setFocusable(false);

        // Update language combo box with available languages
        updateLanguageComboBox();

        // Add refresh languages button
        JMenuItem refreshLanguagesItem = new JMenuItem("Refresh Languages");
        refreshLanguagesItem.setFocusable(false);
        refreshLanguagesItem.addActionListener(e -> {
            // Reload languages and update combo box
            loadLanguages();
            updateLanguageComboBox();

            if (showNotifications) {
                JOptionPane.showMessageDialog(this,
                        "Language list refreshed. Found " + languages.size() + " languages.",
                        "Language Refresh",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // 使用成员变量而不是局部变量
        languageMenuItem = new JMenuItem(getTranslation("menu.language"));
        languageMenuItem.setFocusable(false);
        languageMenuItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    languageComboBox,
                    getTranslation("menu.language"),
                    JOptionPane.PLAIN_MESSAGE);
        });

        languageMenu.add(languageMenuItem);
        languageMenu.addSeparator();
        languageMenu.add(refreshLanguagesItem);

        // Skins menu (combines theme, background, and frosted effect)
        JMenu skinsMenu = new JMenu(getTranslation("menu.skins"));
        skinsMenu.setFocusable(false);

        // Theme items
        JMenuItem lightThemeItem = new JMenuItem(getTranslation("tray.theme.light"));
        lightThemeItem.setFocusable(false);
        lightThemeItem.addActionListener(e -> setLightMode());

        JMenuItem darkThemeItem = new JMenuItem(getTranslation("tray.theme.dark"));
        darkThemeItem.setFocusable(false);
        darkThemeItem.addActionListener(e -> setDarkMode());

        // Background items
        JMenuItem setBackgroundItem = new JMenuItem(getTranslation("button.background"));
        setBackgroundItem.setFocusable(false);
        setBackgroundItem.addActionListener(e -> setBackgroundImage());

        JMenuItem clearBackgroundItem = new JMenuItem(getTranslation("button.clear.background"));
        clearBackgroundItem.setFocusable(false);
        clearBackgroundItem.addActionListener(e -> clearBackgroundImage());

        // Frosted buttons item
        JCheckBoxMenuItem frostedItem = new JCheckBoxMenuItem(getTranslation("button.frosted"), isFrostedButtons);
        frostedItem.setFocusable(false);
        frostedItem.addActionListener(e -> {
            isFrostedButtons = frostedItem.isSelected();
            savePreferences();
            updateNumpadButtons();

            if (showNotifications) {
                String message = isFrostedButtons ?
                        getTranslation("message.frosted.on") :
                        getTranslation("message.frosted.off");
                JOptionPane.showMessageDialog(this,
                        message,
                        getTranslation("menu.skins"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // Add items to skins menu
        skinsMenu.add(lightThemeItem);
        skinsMenu.add(darkThemeItem);
        skinsMenu.addSeparator();
        skinsMenu.add(setBackgroundItem);
        skinsMenu.add(clearBackgroundItem);
        skinsMenu.addSeparator();
        skinsMenu.add(frostedItem);

        // Notifications menu
        JMenu notificationsMenu = new JMenu(getTranslation("menu.notifications"));
        notificationsMenu.setFocusable(false);
        JCheckBoxMenuItem notificationsItem = new JCheckBoxMenuItem(getTranslation("menu.notifications.on"), showNotifications);
        notificationsItem.setFocusable(false);
        notificationsItem.addActionListener(e -> {
            showNotifications = notificationsItem.isSelected();
            savePreferences();
        });
        notificationsMenu.add(notificationsItem);

        // About menu
        JMenu aboutMenu = new JMenu(getTranslation("menu.about"));
        aboutMenu.setFocusable(false);
        JMenuItem aboutItem = new JMenuItem(getTranslation("menu.about"));
        aboutItem.setFocusable(false);
        aboutItem.addActionListener(e -> showAboutDialog());
        aboutMenu.add(aboutItem);

        menuBar.add(languageMenu);
        menuBar.add(skinsMenu);
        menuBar.add(notificationsMenu);
        menuBar.add(aboutMenu);
        setJMenuBar(menuBar);

        // Status panel
        JPanel topPanel = new JPanel(new GridLayout(3, 1));
        topPanel.setFocusable(false);
        topPanel.setOpaque(false);
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        statusLabel.setFocusable(false);

        modeLabel = new JLabel("", SwingConstants.CENTER);
        modeLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        modeLabel.setFocusable(false);

        // Author label
        JLabel authorLabel = new JLabel(getTranslation("author.info"), SwingConstants.CENTER);
        authorLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        authorLabel.setFocusable(false);

        topPanel.add(statusLabel);
        topPanel.add(modeLabel);
        topPanel.add(authorLabel);

        // Numpad panel
        numpadPanel = new JPanel(new GridLayout(4, 4, 10, 10));
        numpadPanel.setFocusable(false);
        numpadPanel.setOpaque(false);
        numpadPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        updateNumpadButtons();

        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setFocusable(false);
        controlPanel.setOpaque(false);
        controlPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton toggleTopBtn = createRoundedButton(getTranslation("button.toggle.top"));
        toggleTopBtn.addActionListener(e -> toggleAlwaysOnTop());

        JButton toggleModeBtn = createRoundedButton(getTranslation("button.toggle.mode"));
        toggleModeBtn.addActionListener(e -> toggleNumLockMode());

        JButton toggleThemeBtn = createRoundedButton(getTranslation("button.toggle.theme"));
        toggleThemeBtn.addActionListener(e -> toggleTheme());

        controlPanel.add(toggleTopBtn);
        controlPanel.add(toggleModeBtn);
        controlPanel.add(toggleThemeBtn);

        // Add components to main window
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(numpadPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        // Apply theme
        applyTheme();
    }

    /**
     * Update language combo box with available languages
     */
    private void updateLanguageComboBox() {
        // Clear existing items
        languageComboBox.removeAllItems();

        // Get sorted list of language codes
        List<String> langCodes = new ArrayList<>(languages.keySet());
        Collections.sort(langCodes);

        // Add each language to the combo box
        for (String code : langCodes) {
            String displayName = getLanguageDisplayName(code);
            languageComboBox.addItem(displayName + " (" + code + ")");
        }

        // Set the current selection
        int currentIndex = langCodes.indexOf(currentLangCode);
        if (currentIndex >= 0) {
            languageComboBox.setSelectedIndex(currentIndex);
        }

        // 安全地移除现有的 action listeners
        for (ActionListener al : languageComboBox.getActionListeners()) {
            languageComboBox.removeActionListener(al);
        }

        // Update action listener
        languageComboBox.addActionListener(e -> {
            int selectedIndex = languageComboBox.getSelectedIndex();
            if (selectedIndex >= 0) {
                String selectedCode = langCodes.get(selectedIndex);
                changeLanguage(selectedCode);
            }
        });

        System.out.println("Language combo box updated with " + langCodes.size() + " languages");
    }

    /**
     * Create a rounded button
     */
    private JButton createRoundedButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Paint background with rounded corners
                if (getModel().isArmed()) {
                    g2.setColor(isDarkMode ? new Color(100, 100, 100, 200) : new Color(200, 200, 200, 200));
                } else if (getModel().isRollover()) {
                    g2.setColor(isDarkMode ? new Color(80, 80, 80, 200) : new Color(220, 220, 220, 200));
                } else {
                    g2.setColor(isDarkMode ? new Color(60, 60, 60, 200) : new Color(240, 240, 240, 200));
                }

                if (isFrostedButtons) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                // Paint border
                g2.setColor(isDarkMode ? Color.WHITE : Color.BLACK);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);

                // Paint text
                g2.setColor(isDarkMode ? Color.WHITE : Color.BLACK);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                Rectangle stringBounds = fm.getStringBounds(this.getText(), g2).getBounds();
                int textX = (getWidth() - stringBounds.width) / 2;
                int textY = (getHeight() - stringBounds.height) / 2 + fm.getAscent();
                g2.drawString(getText(), textX, textY);
                g2.dispose();
            }
        };

        button.setFocusable(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(120, 40));
        button.setFont(new Font("SansSerif", Font.BOLD, 12));

        return button;
    }

    /**
     * Apply current theme to UI
     */
    private void applyTheme() {
        if (isDarkMode) {
            mainPanel.setBackground(Color.DARK_GRAY);
            numpadPanel.setBackground(new Color(40, 40, 40));
            statusLabel.setForeground(Color.WHITE);
            modeLabel.setForeground(Color.LIGHT_GRAY);
        } else {
            mainPanel.setBackground(new Color(240, 240, 240));
            numpadPanel.setBackground(new Color(240, 240, 240));
            statusLabel.setForeground(Color.BLACK);
            modeLabel.setForeground(Color.BLACK);
        }

        // Update numpad buttons
        updateNumpadButtons();
    }

    /**
     * Setup system tray icon - TRAY MENUS ARE ALWAYS IN ENGLISH
     */
    private void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("System tray is not supported");
            return;
        }

        systemTray = SystemTray.getSystemTray();

        // Create tray icon image
        Image image = null;

        // Try to load icon from icon.ico file
        try {
            File iconFile = new File("icon.ico");
            if (iconFile.exists()) {
                image = new ImageIcon("icon.ico").getImage();
                System.out.println("Tray icon loaded from icon.ico");
            }
        } catch (Exception e) {
            System.err.println("Error loading tray icon: " + e.getMessage());
        }

        // If icon.ico not found, create default icon
        if (image == null) {
            image = createTrayIconImage();
            System.out.println("Using default tray icon (icon.ico not found)");
        }

        // ==========================================
        // FIX: Setup font for AWT components to support Chinese
        // ==========================================
        // Try to use Microsoft YaHei UI on Windows, otherwise Dialog
        trayFont = new Font("Microsoft YaHei UI", Font.PLAIN, 12);
        // If the font family doesn't actually exist (e.g. non-Windows), Java might default it.
        // We can check if it defaulted to "Dialog" and be explicit.
        if (!"Microsoft YaHei UI".equals(trayFont.getFamily()) && !System.getProperty("os.name").toLowerCase().contains("win")) {
            trayFont = new Font("Dialog", Font.PLAIN, 12);
        }

        // Create popup menu - ALL TRAY MENU ITEMS ARE IN ENGLISH
        // basically here i tried to switch gbk to utf-8 but failed and it wasted my whole afternoon so i decided to screw it
        //yay!!
        PopupMenu popup = new PopupMenu();
        popup.setFont(trayFont); // Apply font to popup container

        // Restore window menu item - ALWAYS ENGLISH
        MenuItem restoreItem = new MenuItem("Restore");
        restoreItem.setFont(trayFont); // Explicitly set font
        restoreItem.addActionListener(e -> {
            setVisible(true);
            setExtendedState(JFrame.NORMAL);
            toFront();
        });
        popup.add(restoreItem);

        // Separator
        popup.addSeparator();

        // Toggle always on top menu item - ALWAYS ENGLISH
        MenuItem toggleTopItem = new MenuItem(isAlwaysOnTop ? "Top Off" : "Top On");
        toggleTopItem.setFont(trayFont); // Explicitly set font
        toggleTopItem.addActionListener(e -> toggleAlwaysOnTop());
        popup.add(toggleTopItem);

        // Toggle mode menu item - ALWAYS ENGLISH
        MenuItem toggleModeItem = new MenuItem(isNumLockMode ? "Shortcut Mode" : "Number Mode");
        toggleModeItem.setFont(trayFont); // Explicitly set font
        toggleModeItem.addActionListener(e -> toggleNumLockMode());
        popup.add(toggleModeItem);

        // Toggle theme menu item - ALWAYS ENGLISH
        MenuItem toggleThemeItem = new MenuItem(isDarkMode ? "Light Theme" : "Dark Theme");
        toggleThemeItem.setFont(trayFont); // Explicitly set font
        toggleThemeItem.addActionListener(e -> toggleTheme());
        popup.add(toggleThemeItem);

        // Set background menu item - ALWAYS ENGLISH
        MenuItem backgroundItem = new MenuItem("Set Background");
        backgroundItem.setFont(trayFont); // Explicitly set font
        backgroundItem.addActionListener(e -> setBackgroundImage());
        popup.add(backgroundItem);

        // Separator
        popup.addSeparator();

        // Exit menu item - ALWAYS ENGLISH
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setFont(trayFont); // Explicitly set font
        exitItem.addActionListener(e -> {
            removeTrayIcon();
            savePreferences();
            System.exit(0);
        });
        popup.add(exitItem);

        // Create tray icon
        trayIcon = new TrayIcon(image, getTranslation("window.title"), popup);
        trayIcon.setImageAutoSize(true);

        // Add double-click listener to restore window
        trayIcon.addActionListener(e -> {
            setVisible(true);
            setExtendedState(JFrame.NORMAL);
            toFront();
        });

        // Add to system tray
        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println("Failed to add tray icon: " + e.getMessage());
        }
    }

    /**
     * Create a tray icon image
     */
    private Image createTrayIconImage() {
        // Create a simple keyboard icon
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw a keyboard icon
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRoundRect(2, 2, size-4, size-4, 4, 4);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(5, 5, 3, 3);
        g2d.fillRect(10, 5, 3, 3);
        g2d.fillRect(5, 10, 3, 3);
        g2d.fillRect(10, 10, 3, 3);

        g2d.dispose();
        return image;
    }

    /**
     * Remove tray icon
     */
    private void removeTrayIcon() {
        if (systemTray != null && trayIcon != null) {
            systemTray.remove(trayIcon);
        }
    }

    /**
     * Update numpad buttons based on current mode
     */
    private void updateNumpadButtons() {
        numpadPanel.removeAll();

        String[][] keys = isNumLockMode ? numKeys : shortcutKeys;

        for (int i = 0; i < keys.length; i++) {
            for (int j = 0; j < keys[i].length; j++) {
                String key = keys[i][j];
                String buttonText = key;

                // Translate button text for shortcut mode
                if (!isNumLockMode) {
                    buttonText = getTranslation("button." + key.toLowerCase());
                }

                JButton button = new JButton(buttonText) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        // Paint background with rounded corners
                        if (getModel().isArmed()) {
                            g2.setColor(isDarkMode ? new Color(30, 30, 30, 200) : new Color(180, 180, 180, 200));
                        } else if (getModel().isRollover()) {
                            g2.setColor(isDarkMode ? new Color(50, 50, 50, 200) : new Color(200, 200, 200, 200));
                        } else {
                            if (isDarkMode) {
                                g2.setColor(new Color(40, 40, 40, 200)); // Dark mode button color
                            } else {
                                if (isNumLockMode) {
                                    g2.setColor(new Color(200, 220, 240, 200));
                                } else {
                                    g2.setColor(new Color(240, 220, 200, 200));
                                }
                            }
                        }

                        if (isFrostedButtons) {
                            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                        }

                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                        // Paint border
                        g2.setColor(isDarkMode ? Color.WHITE : Color.BLACK);
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);

                        // Paint text
                        g2.setColor(isDarkMode ? Color.WHITE : Color.BLACK);
                        g2.setFont(getFont());
                        FontMetrics fm = g2.getFontMetrics();
                        Rectangle stringBounds = fm.getStringBounds(this.getText(), g2).getBounds();
                        int textX = (getWidth() - stringBounds.width) / 2;
                        int textY = (getHeight() - stringBounds.height) / 2 + fm.getAscent();
                        g2.drawString(getText(), textX, textY);
                        g2.dispose();
                    }
                };

                button.setFont(new Font("SansSerif", Font.BOLD, isNumLockMode ? 16 : 12));
                button.setFocusable(false);
                button.setContentAreaFilled(false);
                button.setBorderPainted(false);

                final String actionKey = key;
                button.addActionListener(e -> buttonClicked(actionKey));

                numpadPanel.add(button);
            }
        }

        numpadPanel.revalidate();
        numpadPanel.repaint();
    }

    /**
     * Handle button click events
     */
    private void buttonClicked(String key) {
        if (isNumLockMode) {
            // Number mode: simulate key press
            simulateKeyPress(key);
            if (showNotifications) {
                JOptionPane.showMessageDialog(this,
                        getTranslation("message.input") + ": " + key,
                        getTranslation("mode.num"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            // Shortcut mode: execute corresponding function
            executeShortcut(key);
        }
    }

    /**
     * Simulate key press using Robot class - FIXED VERSION for numpad keys
     */
    private void simulateKeyPress(String key) {
        try {
            Robot robot = new Robot();

            // Use numpad key codes for all keys to simulate real numpad
            switch (key) {
                case "0":
                    robot.keyPress(KeyEvent.VK_NUMPAD0);
                    robot.keyRelease(KeyEvent.VK_NUMPAD0);
                    break;
                case "1":
                    robot.keyPress(KeyEvent.VK_NUMPAD1);
                    robot.keyRelease(KeyEvent.VK_NUMPAD1);
                    break;
                case "2":
                    robot.keyPress(KeyEvent.VK_NUMPAD2);
                    robot.keyRelease(KeyEvent.VK_NUMPAD2);
                    break;
                case "3":
                    robot.keyPress(KeyEvent.VK_NUMPAD3);
                    robot.keyRelease(KeyEvent.VK_NUMPAD3);
                    break;
                case "4":
                    robot.keyPress(KeyEvent.VK_NUMPAD4);
                    robot.keyRelease(KeyEvent.VK_NUMPAD4);
                    break;
                case "5":
                    robot.keyPress(KeyEvent.VK_NUMPAD5);
                    robot.keyRelease(KeyEvent.VK_NUMPAD5);
                    break;
                case "6":
                    robot.keyPress(KeyEvent.VK_NUMPAD6);
                    robot.keyRelease(KeyEvent.VK_NUMPAD6);
                    break;
                case "7":
                    robot.keyPress(KeyEvent.VK_NUMPAD7);
                    robot.keyRelease(KeyEvent.VK_NUMPAD7);
                    break;
                case "8":
                    robot.keyPress(KeyEvent.VK_NUMPAD8);
                    robot.keyRelease(KeyEvent.VK_NUMPAD8);
                    break;
                case "9":
                    robot.keyPress(KeyEvent.VK_NUMPAD9);
                    robot.keyRelease(KeyEvent.VK_NUMPAD9);
                    break;
                case ".":
                    robot.keyPress(KeyEvent.VK_DECIMAL);
                    robot.keyRelease(KeyEvent.VK_DECIMAL);
                    break;
                case "=":
                    robot.keyPress(KeyEvent.VK_ENTER);
                    robot.keyRelease(KeyEvent.VK_ENTER);
                    break;
                case "+":
                    // Use numpad plus key
                    robot.keyPress(KeyEvent.VK_ADD);
                    robot.keyRelease(KeyEvent.VK_ADD);
                    break;
                case "-":
                    // Use numpad minus key
                    robot.keyPress(KeyEvent.VK_SUBTRACT);
                    robot.keyRelease(KeyEvent.VK_SUBTRACT);
                    break;
                case "*":
                    // Use numpad multiply key
                    robot.keyPress(KeyEvent.VK_MULTIPLY);
                    robot.keyRelease(KeyEvent.VK_MULTIPLY);
                    break;
                case "/":
                    // Use numpad divide key
                    robot.keyPress(KeyEvent.VK_DIVIDE);
                    robot.keyRelease(KeyEvent.VK_DIVIDE);
                    break;
            }
        } catch (AWTException e) {
            System.err.println("Error simulating key press: " + e.getMessage());
        }
    }

    /**
     * Execute shortcut command
     */
    private void executeShortcut(String shortcut) {
        switch (shortcut) {
            case "TOGGLE_TOP":
                toggleAlwaysOnTop();
                break;
            case "TOGGLE_MODE":
                toggleNumLockMode();
                break;
            case "TOGGLE_THEME":
                toggleTheme();
                break;
            case "COPY":
                if (showNotifications) {
                    JOptionPane.showMessageDialog(this,
                            getTranslation("message.execute") + ": " + getTranslation("button.copy"));
                }
                break;
            case "PASTE":
                if (showNotifications) {
                    JOptionPane.showMessageDialog(this,
                            getTranslation("message.execute") + ": " + getTranslation("button.paste"));
                }
                break;
            case "SAVE":
                if (showNotifications) {
                    JOptionPane.showMessageDialog(this,
                            getTranslation("message.execute") + ": " + getTranslation("button.save"));
                }
                break;
            case "CUT":
                if (showNotifications) {
                    JOptionPane.showMessageDialog(this,
                            getTranslation("message.execute") + ": " + getTranslation("button.cut"));
                }
                break;
            case "UNDO":
                if (showNotifications) {
                    JOptionPane.showMessageDialog(this,
                            getTranslation("message.execute") + ": " + getTranslation("button.undo"));
                }
                break;
            case "REDO":
                if (showNotifications) {
                    JOptionPane.showMessageDialog(this,
                            getTranslation("message.execute") + ": " + getTranslation("button.redo"));
                }
                break;
            case "NEW":
                if (showNotifications) {
                    JOptionPane.showMessageDialog(this,
                            getTranslation("message.execute") + ": " + getTranslation("button.new"));
                }
                break;
            case "OPEN":
                if (showNotifications) {
                    JOptionPane.showMessageDialog(this,
                            getTranslation("message.execute") + ": " + getTranslation("button.open"));
                }
                break;
            case "FIND":
                if (showNotifications) {
                    JOptionPane.showMessageDialog(this,
                            getTranslation("message.execute") + ": " + getTranslation("button.find"));
                }
                break;
            case "REPLACE":
                if (showNotifications) {
                    JOptionPane.showMessageDialog(this,
                            getTranslation("message.execute") + ": " + getTranslation("button.replace"));
                }
                break;
            case "PRINT":
                if (showNotifications) {
                    JOptionPane.showMessageDialog(this,
                            getTranslation("message.execute") + ": " + getTranslation("button.print"));
                }
                break;
            case "HELP":
                if (showNotifications) {
                    JOptionPane.showMessageDialog(this,
                            getTranslation("message.execute") + ": " + getTranslation("button.help"));
                }
                break;
            case "EXIT":
                int confirm = JOptionPane.showConfirmDialog(this,
                        getTranslation("message.exit.confirm"),
                        getTranslation("button.exit"),
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    removeTrayIcon();
                    savePreferences();
                    System.exit(0);
                }
                break;
        }
    }

    /**
     * Setup global keyboard listener
     */
    private void setupGlobalKeyListener() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(new KeyEventDispatcher() {
                    @Override
                    public boolean dispatchKeyEvent(KeyEvent e) {
                        int keyCode = e.getKeyCode();

                        if (e.getID() == KeyEvent.KEY_PRESSED) {
                            pressedKeys.add(keyCode);

                            // Check Ctrl+T (toggle always on top)
                            if (pressedKeys.contains(KeyEvent.VK_CONTROL) &&
                                    keyCode == TOGGLE_TOP_KEY) {
                                toggleAlwaysOnTop();
                            }

                            // Check NumLock key (toggle mode)
                            if (keyCode == KeyEvent.VK_NUM_LOCK ||
                                    (pressedKeys.contains(KeyEvent.VK_ALT) &&
                                            keyCode == KeyEvent.VK_N)) {
                                toggleNumLockMode();
                            }

                            // Check Ctrl+L (toggle language)
                            if (pressedKeys.contains(KeyEvent.VK_CONTROL) &&
                                    keyCode == TOGGLE_LANG_KEY) {
                                toggleLanguage();
                            }

                            // Check Ctrl+D (toggle theme)
                            if (pressedKeys.contains(KeyEvent.VK_CONTROL) &&
                                    keyCode == TOGGLE_THEME_KEY) {
                                toggleTheme();
                            }

                        } else if (e.getID() == KeyEvent.KEY_RELEASED) {
                            pressedKeys.remove(keyCode);
                        }

                        return false;
                    }
                });
    }

    /**
     * Toggle always on top state
     */
    private void toggleAlwaysOnTop() {
        isAlwaysOnTop = !isAlwaysOnTop;
        setAlwaysOnTop(isAlwaysOnTop);
        updateStatus();

        // Save preference
        savePreferences();

        // Update tray menu if exists - USING ENGLISH FOR TRAY
        updateTrayMenu();

        // Only show notification if enabled
        if (showNotifications) {
            String message = isAlwaysOnTop ?
                    getTranslation("message.toggle.top.on") :
                    getTranslation("message.toggle.top.off");
            JOptionPane.showMessageDialog(this,
                    message,
                    getTranslation("status.label"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Toggle between number and shortcut modes
     */
    private void toggleNumLockMode() {
        isNumLockMode = !isNumLockMode;
        updateNumpadButtons();
        updateStatus();

        // Save preference
        savePreferences();

        // Update tray menu if exists - USING ENGLISH FOR TRAY
        updateTrayMenu();

        // Only show notification if enabled
        if (showNotifications) {
            String message = isNumLockMode ?
                    getTranslation("message.mode.num") :
                    getTranslation("message.mode.shortcut");
            JOptionPane.showMessageDialog(this,
                    message,
                    getTranslation("mode.label"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Toggle between light and dark themes
     */
    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        applyTheme();

        // Save preference
        savePreferences();

        // Update tray menu if exists - USING ENGLISH FOR TRAY
        updateTrayMenu();

        // Only show notification if enabled
        if (showNotifications) {
            String message = isDarkMode ?
                    getTranslation("message.theme.dark") :
                    getTranslation("message.theme.light");
            JOptionPane.showMessageDialog(this,
                    message,
                    getTranslation("menu.skins"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Set light mode
     */
    private void setLightMode() {
        if (isDarkMode) {
            isDarkMode = false;
            applyTheme();
            savePreferences();
            updateTrayMenu();
        }
    }

    /**
     * Set dark mode
     */
    private void setDarkMode() {
        if (!isDarkMode) {
            isDarkMode = true;
            applyTheme();
            savePreferences();
            updateTrayMenu();
        }
    }

    /**
     * Set background image
     */
    private void setBackgroundImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(getTranslation("button.background"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() ||
                        f.getName().toLowerCase().endsWith(".jpg") ||
                        f.getName().toLowerCase().endsWith(".jpeg") ||
                        f.getName().toLowerCase().endsWith(".png") ||
                        f.getName().toLowerCase().endsWith(".gif");
            }

            @Override
            public String getDescription() {
                return "Image files (*.jpg, *.jpeg, *.png, *.gif)";
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            backgroundImagePath = fileChooser.getSelectedFile().getAbsolutePath();
            savePreferences();
            mainPanel.repaint();

            if (showNotifications) {
                JOptionPane.showMessageDialog(this,
                        getTranslation("message.background.set"),
                        getTranslation("menu.skins"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * Clear background image
     */
    private void clearBackgroundImage() {
        backgroundImagePath = null;
        savePreferences();
        mainPanel.repaint();

        if (showNotifications) {
            JOptionPane.showMessageDialog(this,
                    getTranslation("message.background.removed"),
                    getTranslation("menu.skins"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Toggle between available languages
     */
    private void toggleLanguage() {
        List<String> langCodes = new ArrayList<>(languages.keySet());
        if (langCodes.size() > 1) {
            int currentIndex = langCodes.indexOf(currentLangCode);
            int nextIndex = (currentIndex + 1) % langCodes.size();
            changeLanguage(langCodes.get(nextIndex));
        }
    }

    /**
     * Change to a specific language
     */
    private void changeLanguage(String langCode) {
        if (languages.containsKey(langCode)) {
            currentLangCode = langCode;
            currentLanguage = languages.get(langCode);
            updateUIForNewLanguage();

            // Save preference
            savePreferences();

            // Only show notification if enabled
            if (showNotifications) {
                String displayName = getLanguageDisplayName(langCode);

                JOptionPane.showMessageDialog(this,
                        getTranslation("message.language.changed") + displayName,
                        getTranslation("menu.language"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /**
     * Update tray menu items - ALWAYS IN ENGLISH
     */
    private void updateTrayMenu() {
        if (trayIcon != null && trayIcon.getPopupMenu() != null) {
            PopupMenu popup = trayIcon.getPopupMenu();

            // Update all menu items
            for (int i = 0; i < popup.getItemCount(); i++) {
                MenuItem item = popup.getItem(i);

                // Ensure font is set (usually set at creation, but just in case)
                if (trayFont != null) {
                    item.setFont(trayFont);
                }

                String label = item.getLabel();

                // Update labels in English only
                if (label.contains("Top On") || label.contains("Top Off")) {
                    item.setLabel(isAlwaysOnTop ? "Top Off" : "Top On");
                } else if (label.contains("Number Mode") || label.contains("Shortcut Mode")) {
                    item.setLabel(isNumLockMode ? "Shortcut Mode" : "Number Mode");
                } else if (label.contains("Light Theme") || label.contains("Dark Theme")) {
                    item.setLabel(isDarkMode ? "Light Theme" : "Dark Theme");
                }
                // Other menu items (Restore, Set Background, Exit) remain as English text
            }
        }
    }

    /**
     * Update UI for new language
     */
    private void updateUIForNewLanguage() {
        // Update window title
        setTitle(getTranslation("window.title") + " - " + getTranslation("author.info"));

        // Update menu bar
        JMenuBar menuBar = getJMenuBar();
        if (menuBar != null && menuBar.getMenuCount() >= 4) {
            menuBar.getMenu(0).setText(getTranslation("menu.language"));
            menuBar.getMenu(1).setText(getTranslation("menu.skins"));
            menuBar.getMenu(2).setText(getTranslation("menu.notifications"));
            menuBar.getMenu(3).setText(getTranslation("menu.about"));

            // 更新 languageMenuItem 的文本
            if (languageMenuItem != null) {
                languageMenuItem.setText(getTranslation("menu.language"));
            }

            // Update skins menu items
            JMenu skinsMenu = menuBar.getMenu(1);
            if (skinsMenu.getItemCount() >= 6) {
                skinsMenu.getItem(0).setText(getTranslation("tray.theme.light"));
                skinsMenu.getItem(1).setText(getTranslation("tray.theme.dark"));
                skinsMenu.getItem(3).setText(getTranslation("button.background"));
                skinsMenu.getItem(4).setText(getTranslation("button.clear.background"));
                if (skinsMenu.getItemCount() > 6) {
                    ((JCheckBoxMenuItem)skinsMenu.getItem(6)).setText(getTranslation("button.frosted"));
                }
            }

            // Update notification menu item
            JMenu notificationsMenu = menuBar.getMenu(2);
            if (notificationsMenu.getItemCount() > 0) {
                JCheckBoxMenuItem notificationsItem = (JCheckBoxMenuItem) notificationsMenu.getItem(0);
                notificationsItem.setText(showNotifications ?
                        getTranslation("menu.notifications.on") : getTranslation("menu.notifications.off"));
            }

            // Update about menu item
            JMenu aboutMenu = menuBar.getMenu(3);
            if (aboutMenu.getItemCount() > 0) {
                aboutMenu.getItem(0).setText(getTranslation("menu.about"));
            }
        }

        // Update buttons in control panel (manually recreate them with new text)
        JPanel controlPanel = (JPanel)mainPanel.getComponent(2);
        controlPanel.removeAll();

        JButton toggleTopBtn = createRoundedButton(getTranslation("button.toggle.top"));
        toggleTopBtn.addActionListener(e -> toggleAlwaysOnTop());

        JButton toggleModeBtn = createRoundedButton(getTranslation("button.toggle.mode"));
        toggleModeBtn.addActionListener(e -> toggleNumLockMode());

        JButton toggleThemeBtn = createRoundedButton(getTranslation("button.toggle.theme"));
        toggleThemeBtn.addActionListener(e -> toggleTheme());

        controlPanel.add(toggleTopBtn);
        controlPanel.add(toggleModeBtn);
        controlPanel.add(toggleThemeBtn);

        // Update author label
        ((JLabel)((JPanel)mainPanel.getComponent(0)).getComponent(2))
                .setText(getTranslation("author.info"));

        // Update numpad buttons
        updateNumpadButtons();

        // Update status
        updateStatus();

        // Update tray icon tooltip
        if (trayIcon != null) {
            trayIcon.setToolTip(getTranslation("window.title"));
        }

        // Update tray menu - BUT KEEP ENGLISH
        updateTrayMenu();

        // Repaint the window
        controlPanel.revalidate();
        controlPanel.repaint();
        revalidate();
        repaint();
    }

    /**
     * Update status labels
     */
    private void updateStatus() {
        String topStatus = isAlwaysOnTop ?
                getTranslation("top.on") :
                getTranslation("top.off");
        String modeStatus = isNumLockMode ?
                getTranslation("mode.num") :
                getTranslation("mode.shortcut");

        statusLabel.setText(getTranslation("status.label") + ": " + topStatus);
        modeLabel.setText(getTranslation("mode.label") + ": " + modeStatus);

        // Change color based on status
        if (isAlwaysOnTop) {
            statusLabel.setForeground(isDarkMode ? Color.YELLOW : Color.RED);
        } else {
            statusLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        }
    }

    /**
     * Show about dialog
     */
    private void showAboutDialog() {
        String aboutText = "<html><center>" +
                "<h2>" + getTranslation("about.title") + "</h2>" +
                "<p>" + getTranslation("about.version") + "</p>" +
                "<p><b>" + getTranslation("author.info") + "</b></p>" +
                "<p>" + getTranslation("about.features") + "</p>" +
                "<p>" + getTranslation("about.shortcuts") + "</p>" +
                "<p>Notifications: " + (showNotifications ? "Enabled" : "Disabled") + "</p>" +
                "<p>Theme: " + (isDarkMode ? "Dark" : "Light") + "</p>" +
                "<p>Background: " + (backgroundImagePath != null ? "Custom" : "Default") + "</p>" +
                "<p>Frosted Buttons: " + (isFrostedButtons ? "Enabled" : "Disabled") + "</p>" +
                "<p>Available Languages: " + languages.size() + "</p>" +
                "</center></html>";

        JOptionPane.showMessageDialog(this,
                aboutText,
                getTranslation("menu.about"),
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Main method
     */
    public static void main(String[] args) {
        // Set UTF-8 as default encoding
        System.setProperty("file.encoding", "UTF-8");

        SwingUtilities.invokeLater(() -> {
            try {
                // Set system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                // Set font to support Chinese characters
                UIManager.put("Button.font", new Font("SansSerif", Font.PLAIN, 12));
                UIManager.put("Label.font", new Font("SansSerif", Font.PLAIN, 12));
                UIManager.put("Menu.font", new Font("SansSerif", Font.PLAIN, 12));
                UIManager.put("MenuItem.font", new Font("SansSerif", Font.PLAIN, 12));

            } catch (Exception e) {
                System.err.println("Error setting look and feel: " + e.getMessage());
            }

            new VirtualNumpad();
        });
    }
}
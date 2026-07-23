package com.wmp.downloader.ui;

import com.wmp.downloader.laug.StringFormat;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.EasterEggData;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.ui.common.PathSelectionPanel;
import com.wmp.downloader.ui.settings.BasicSpecialSettings;
import com.wmp.downloader.ui.settings.BiliSettings;
import com.wmp.downloader.ui.task.DownloadTask;
import com.wmp.downloader.ui.task.createTask.CreateTaskPanel;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Downloader extends JFrame implements WindowListener {

    public static TrayIcon trayIcon;
    private static final Logger logger = Logger.getLogger(Downloader.class);
    private final List<DownloadTask> taskList = new ArrayList<>();
    private final List<DownloadTask> taskFinalyTipList = new ArrayList<>();
    private JPanel UIPanel;
    private JPanel settingsPanel;
    private JTabbedPane tabbedPane1;
    private JCheckBox isUseSSLCheckBox;
    private JButton saveButton;
    private JButton refreshButton;
    private JComboBox<String> themeComboBox;
    private JButton dataPathButton;
    private JSlider ThreadNumSlider;
    private JTextField ThreadNumLabel;
    private JPanel downloaderPanel;
    private JPanel aboutPanel;
    private JButton createTaskButton;
    private JCheckBox FlatLafCheckBox;
    private JCheckBox IconPackCheckBox;
    private JCheckBox alibabaFastjsonCheckBox;
    private JCheckBox log4jLog4jCheckBox;
    private JPanel TaskButtonPanel;
    private PathSelectionPanel pathSelectionPanel;
    private JComboBox<String> FontListComboBox;
    private JSpinner fontSizeSpinner;
    private JLabel nameLabel;
    private JCheckBox authorCheckBox;
    private JTabbedPane TasksPanel;
    private final Timer taskListener = new Timer(100, e -> {
        taskList.removeIf(DownloadTask ->
        {

            if (DownloadTask.isCanExit()) {
                TasksPanel.remove(DownloadTask);
                return true;
            } else return false;
        });
        taskList.forEach(urlDownloadTask -> {
            if (urlDownloadTask.isFinally()) {
                if (!taskFinalyTipList.contains(urlDownloadTask)) {
                    taskFinalyTipList.add(urlDownloadTask);
                    JOptionPane.showMessageDialog(this, String.format(StringFormat.translate("task", "task.download_task.success.confirm"), urlDownloadTask.getName()), StringFormat.translate("task", "task.download_task.success.confirm.title"), JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

    });
    private JButton allStartButton;
    private JButton allPauseButton;
    private PathSelectionPanel tempPathSelectionPanel;
    private JTabbedPane SpecialSettingsTabbedPane;
    private JPanel SpecialSettingsPanel;
    private JButton deleteTempFolderDataButton;
    private JCheckBox isUseClipBoardListenerCheckBox;
    private JComboBox<String> laugComboBox;
    private String lastClipboardContent = "";

    // 修改：使用定时器轮询代替 FlavorListener
    private Timer clipboardTimer;

    private CreateTaskPanel createTaskPanel = null;

    private ActionListener actionListener = e -> {
        this.setVisible(true);
        this.setState(JFrame.NORMAL);
    };

    public Downloader() {
        taskListener.start();

        this.setTitle(StringFormat.translate("common", "app_name") + " V" + DataControl.get("version", "0.0.1"));
        this.setContentPane(UIPanel);
        this.setMinimumSize(new Dimension(800, 550));

        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        IconControl.addInDynamicConverter(
                () -> this.setIconImage(IconControl.getImage("icon", 256))
        );
        this.addWindowListener(this);


        initTrayIcon();

        initMenuBar();


        IconControl.addInDynamicConverter(
                () -> {
                    var size = tabbedPane1.getFont().getSize();
                    tabbedPane1.setIconAt(tabbedPane1.indexOfComponent(downloaderPanel), IconControl.getIcon("task", size));
                    tabbedPane1.setIconAt(tabbedPane1.indexOfComponent(settingsPanel), IconControl.getIcon("settings", size));
                    tabbedPane1.setIconAt(tabbedPane1.indexOfComponent(SpecialSettingsPanel), IconControl.getIcon("settings", size));
                    tabbedPane1.setIconAt(tabbedPane1.indexOfComponent(aboutPanel), IconControl.getIcon("about", size));
                }
        );
        //任务
        initTaskComponents();
        //设置
        initSettingsComponents();
        //专项设置
        initSpecialSettingsComponents();
        //关于
        initAboutComponents();

        startClipboardListener();

        pack();
        this.setLocationRelativeTo(null);
    }

    private void initSpecialSettingsComponents() {
        var biliSettings = new BiliSettings();
        SpecialSettingsTabbedPane.addTab(biliSettings.getSettingsName(), biliSettings.getSettings());
        var fFmpegSettings = new FFmpegSettings();
        SpecialSettingsTabbedPane.addTab(fFmpegSettings.getSettingsName(), fFmpegSettings.getSettings());
    }

    private void initTrayIcon() {

        if (SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
        }

        trayIcon = new TrayIcon(IconControl.getImage("download", 256), StringFormat.translate("common", "app_name"));

        trayIcon.setImageAutoSize(true);
        IconControl.addInDynamicConverter(
                () -> trayIcon.setImage(IconControl.getImage("icon", 256))
        );

        var trayIconMenu = new PopupMenu();

        var showMenuItem = new MenuItem("show");
        showMenuItem.addActionListener(e -> {
            this.setVisible(true);
            this.setState(JFrame.NORMAL);
        });
        showMenuItem.addActionListener(actionListener);
        trayIconMenu.add(showMenuItem);

        var exitMenuItem = new MenuItem("exit");
        exitMenuItem.addActionListener(e -> System.exit(0));
        trayIconMenu.add(exitMenuItem);

        trayIcon.setPopupMenu(trayIconMenu);

        trayIcon.addActionListener(actionListener);

        if (SystemTray.isSupported()) {
            try {
                SystemTray.getSystemTray().add(trayIcon);
            } catch (AWTException e) {
                logger.error("Tray icon added failed", e);
            }
        }
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        var windowMenu = new JMenu(StringFormat.translate("download_menu_bar", "frame"));

        var alwaysOnTopCheckBox = new JCheckBoxMenuItem(StringFormat.translate("download_menu_bar", "frame.is_always_top"));
        alwaysOnTopCheckBox.addActionListener(e -> this.setAlwaysOnTop(alwaysOnTopCheckBox.isSelected()));

        windowMenu.add(alwaysOnTopCheckBox);

        windowMenu.addSeparator();

        var refreshMenuItem = new JMenuItem(StringFormat.translate("download_menu_bar", "frame.refresh"));
        refreshMenuItem.setToolTipText(StringFormat.translate("download_menu_bar", "frame.refresh.tooltip"));
        refreshMenuItem.addActionListener(e -> {
            DataControl.load();
            ThemeChanger.easyChanger();
        });
        windowMenu.add(refreshMenuItem);

        var updateFrameMenuItem = new JMenuItem(StringFormat.translate("download_menu_bar", "frame.update_frame"));
        updateFrameMenuItem.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, StringFormat.translate("download_menu_bar", "frame.update_frame.tip"), StringFormat.translate("common", "warn"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                // 修改：停止旧定时器
                if (clipboardTimer != null) {
                    clipboardTimer.stop();
                }
                this.dispose();
                // 移除已无用的 FlavorListener 相关代码
                // Toolkit.getDefaultToolkit().getSystemClipboard().removeFlavorListener(this.flavorListener);

                DataControl.load();
                ThemeChanger.easyChanger();

                new Downloader().setVisible(true);
            }
        });
        windowMenu.add(updateFrameMenuItem);

        windowMenu.addSeparator();

        var exitMenuItem = new JMenuItem(StringFormat.translate("download_menu_bar", "frame.exit"));
        exitMenuItem.addActionListener(e -> System.exit(0));
        windowMenu.add(exitMenuItem);

        menuBar.add(windowMenu);
        this.setJMenuBar(menuBar);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here


        pathSelectionPanel = new PathSelectionPanel(StringFormat.translate("common", "save_path"), DataControl.getDownloadFilePath());
        tempPathSelectionPanel = new PathSelectionPanel(StringFormat.translate("common", "temp_path"), new File(DataControl.get("TempFilePath", DataControl.getDefaultTempPath().getAbsolutePath())));

        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(DataControl.get("FontSize", 12).intValue(), 1, Integer.MAX_VALUE, 1));

    }

    private void initAboutComponents() {
        nameLabel.setText(StringFormat.translate("common", "app_name") + " V" + DataControl.get("version", "0.0.0"));
        nameLabel.putClientProperty("FlatLaf.style", "font: bold $h0.font");
        IconControl.addInDynamicConverter(
                () -> nameLabel.setIcon(IconControl.getIcon("icon", nameLabel.getFont().getSize()))
        );

        authorCheckBox.addActionListener(e -> {
            if (!authorCheckBox.isSelected()) {
                var panel = new JPanel(new BorderLayout());
                var textArea = new JTextArea("你真的要这么做吗!\n这样做真的很危险!\n不要继续呀!");
                panel.add(textArea);


                var learnMoreButton = new JButton(StringFormat.translate("common", "learn"));
                learnMoreButton.addActionListener(_ -> {

                    JOptionPane.showMessageDialog(this, "作者/程序是软件的核心部分,去除会导致异常", "了解更多", JOptionPane.WARNING_MESSAGE);
                });

                FunctionDialog.showDialog(this, StringFormat.translate("common", "warn"), panel,
                        _ -> {
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                            JOptionPane.showMessageDialog(this, "发生错误!软件开始修复修复问题...", "错误", JOptionPane.ERROR_MESSAGE);
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                            JOptionPane.showMessageDialog(this, "完成!");
                            authorCheckBox.setSelected(true);
                        },
                        new FunctionDialog.CustomButtons[]{FunctionDialog.OK_BUTTON, FunctionDialog.OK_BUTTON, FunctionDialog.OK_BUTTON}, 0,
                        new JButton[]{learnMoreButton}, FunctionDialog.NORTH_DIRECTION_RIGHT);
            }
        });
        FlatLafCheckBox.addActionListener(e -> {
            EasterEggData.canUseFlatLaf = FlatLafCheckBox.isSelected();
            ThemeChanger.easyChanger();
        });
        IconPackCheckBox.addActionListener(e -> {
            EasterEggData.canUseIcon = IconPackCheckBox.isSelected();
            IconControl.runDynamicConverters();
        });
    }

    private void initTaskComponents() {
        //初始化组件数据
        ThemeChanger.addInDynamicConverter(
                this::updateDefaultButton
        );

        //初始化组件
        createTaskButton.putClientProperty("FlatLaf.style", "font: $h2.font");
        allStartButton.putClientProperty("FlatLaf.style", "font: $h2.font");
        allPauseButton.putClientProperty("FlatLaf.style", "font: $h2.font");

        //为组件添加图标
        IconControl.addInDynamicConverter(
                () -> createTaskButton.setIcon(IconControl.getIcon("new", createTaskButton.getFont().getSize())),
                () -> allStartButton.setIcon(IconControl.getIcon("start", allStartButton.getFont().getSize())),
                () -> allPauseButton.setIcon(IconControl.getIcon("pause", allPauseButton.getFont().getSize()))

        );

        //按钮监听
        createTaskButton.addActionListener(e -> {
            createDownloadTask(null);
        });
        allStartButton.addActionListener(e -> {
            for (var urlDownloadTask : taskList) {
                if (!urlDownloadTask.isFinally()) urlDownloadTask.start();
            }
        });
        allPauseButton.addActionListener(e -> {
            for (var urlDownloadTask : taskList) {
                if (!urlDownloadTask.isFinally()) urlDownloadTask.stop();
            }
        });
    }

    private void createDownloadTask(String url) {
        //创建下载任务

        if (this.createTaskPanel == null)
            this.createTaskPanel = new CreateTaskPanel();
        if (url != null) {
            createTaskPanel.setLink(url);
        }
        var mainPanel = createTaskPanel.MainPanel;
        var learnMoreButton = new JButton(StringFormat.translate("common", "learn"));
        learnMoreButton.addActionListener(_ -> {
            var panel = new JPanel();
            var textArea = new JTextArea("""
                    由于还处于开发阶段，哔哩哔哩链接解析出的视频只能使用单线程下载
                    同时使用内置库合并文件速度很慢，建议使用本地的FFmpeg！
                    """);
            panel.add(textArea);


            FunctionDialog.showDialog(this, StringFormat.translate("common", "learn"), panel,
                    _ -> {},
                    FunctionDialog.DEFAULT_BUTTONS, 0,
                    null, FunctionDialog.NORTH_DIRECTION_RIGHT);
        });

        var SupportButton = new JButton(StringFormat.translate("common", "support"));
        SupportButton.addActionListener(_ -> {
            var panel = new JPanel();
            var textArea = new JTextArea(StringFormat.translate("common", "support_text_area"));
            panel.add(textArea);


            FunctionDialog.showDialog(this, StringFormat.translate("common", "learn"), panel,
                    _ -> {},
                    FunctionDialog.DEFAULT_BUTTONS, 0,
                    null, FunctionDialog.NORTH_DIRECTION_RIGHT);
        });


        FunctionDialog.showDialog(this, StringFormat.translate("task", "task.creat_task"), mainPanel,
                result -> {
                    if (result == FunctionDialog.RESULT_OK) {
                        createTaskPanel.getDownloadTasks().forEach(taskPanel -> {
                            taskList.add(taskPanel);
                            var name = taskPanel.getFileName();
                            if (name != null) {
                                if (name.length() > 9) {
                                    name = name.substring(0, 6) + "...";
                                }
                            } else {
                                name = StringFormat.translate("task", "task.download_task.no_name_file");
                            }
                            TasksPanel.addTab(name, taskPanel);
                            TasksPanel.revalidate();
                            TasksPanel.repaint();

                        });

                    }
                    this.createTaskPanel = null;
                }
                , FunctionDialog.OK_CANCEL_BUTTONS, 0,
                new JButton[]{learnMoreButton, SupportButton}, FunctionDialog.NORTH_DIRECTION_RIGHT);
    }

    private void initSettingsComponents() {


        //初始化组件数据
        ThemeChanger.addInDynamicConverter(
                this::updateDefaultButton
        );


        isUseSSLCheckBox.setSelected(DataControl.get("isUseSSL", false));
        isUseClipBoardListenerCheckBox.setSelected(DataControl.get("isUseClipBoardListener", false));
        ThreadNumSlider.setValue(DataControl.get("ThreadNum", 64));
        ThreadNumLabel.setText(String.valueOf(ThreadNumSlider.getValue()));

        {
            String[] laugs = new String[]{
                    "简体中文(zh_cn)", "English(en_us)", "日本語(ja_JP)", "Русский язык(ru_RU)"
            };


            var lauguage = DataControl.get("laug", "zh_cn");
            for (String laug : laugs) {
                laugComboBox.addItem(laug);

                Matcher matcher = Pattern.compile("\\((.+_.+)\\)").matcher(laug);
                if (matcher.find() && lauguage.equals(matcher.group(1))) {
                    lauguage = laug;
                }
            }



            laugComboBox.setSelectedItem(lauguage);
        }

        themeComboBox.addItem("Mac Dark");
        themeComboBox.addItem("Mac Light");
        themeComboBox.addItem("Dark");
        themeComboBox.addItem("Light");
        themeComboBox.addItem("Darcula");
        themeComboBox.addItem("IntelliJ");
        themeComboBox.addItem("System");
        themeComboBox.addItem("Windows Classic");
        themeComboBox.addItem("Metal");

        themeComboBox.setSelectedItem(DataControl.get("theme", "Mac Dark"));

        //获取所有字体
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String font : fonts) {
            FontListComboBox.addItem(font);
        }
        FontListComboBox.setSelectedItem(DataControl.get("Font", "Microsoft YaHei"));

        //为组件添加图标
        IconControl.addInDynamicConverter(
                () -> dataPathButton.setIcon(IconControl.getIcon("folder", dataPathButton.getFont().getSize()))
        );
        IconControl.addInDynamicConverter(
                () -> refreshButton.setIcon(IconControl.getIcon("refresh", refreshButton.getFont().getSize())),
                () -> saveButton.setIcon(IconControl.getIcon("save", saveButton.getFont().getSize()))
        );

        //按钮监听
        tabbedPane1.addChangeListener(e -> updateDefaultButton());

        ThreadNumSlider.addChangeListener(e -> {
            ThreadNumLabel.setText(String.valueOf(ThreadNumSlider.getValue()));
            ThreadNumLabel.setSize(ThreadNumLabel.getPreferredSize());
        });
        refreshButton.addActionListener(e -> {
            DataControl.load();
            isUseSSLCheckBox.setSelected(DataControl.get("isUseSSL", false));
            isUseClipBoardListenerCheckBox.setSelected(DataControl.get("isUseClipBoardListener", false));

            ThreadNumSlider.setValue(DataControl.get("ThreadNum", 64));
            ThreadNumLabel.setText(String.valueOf(ThreadNumSlider.getValue()));
            pathSelectionPanel.setPath(DataControl.getDownloadFilePath().getAbsolutePath());
            tempPathSelectionPanel.setPath(DataControl.get("TempFilePath", DataControl.getDataPath().getAbsolutePath()));
            FontListComboBox.setSelectedItem(DataControl.get("Font", "Microsoft YaHei"));
            fontSizeSpinner.setValue(DataControl.get("FontSize", 12));
            themeComboBox.setSelectedItem(DataControl.get("theme", "Mac Dark"));

            ThemeChanger.easyChanger();
        });
        themeComboBox.addItemListener(e -> {
            var themeStr = e.getItem().toString();
            DataControl.putAndSave("theme", themeStr);
            ThemeChanger.easyChanger();
        });
        FontListComboBox.addActionListener(e -> {
            var fontName = FontListComboBox.getSelectedItem().toString();
            DataControl.putAndSave("Font", fontName);
            ThemeChanger.easyChanger();
        });
        laugComboBox.addItemListener(e ->{
            var lauguage = e.getItem().toString();
            Matcher matcher = Pattern.compile("\\((.+_.+)\\)").matcher(lauguage);
            if (matcher.find()) {
                lauguage = matcher.group(1);
            }
            DataControl.putAndSave("laug", lauguage);
        });

        dataPathButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(DataControl.getDataPath());
            } catch (IOException ex) {
                logger.error("文件打开失败", ex);
            }
        });

        deleteTempFolderDataButton.addActionListener(e -> {
            var tempPath = DataControl.getTempPath();
            DataControl.deleteFolder(tempPath);
        });

        saveButton.addActionListener(e -> {
            DataControl.put("isUseSSL", isUseSSLCheckBox.isSelected());
            DataControl.put("isUseClipBoardListener", isUseClipBoardListenerCheckBox.isSelected());
            DataControl.put("ThreadNum", ThreadNumSlider.getValue());
            DataControl.put("DownloadFilePath", pathSelectionPanel.getPath());
            DataControl.put("TempFilePath", tempPathSelectionPanel.getPath());
            DataControl.put("FontSize", fontSizeSpinner.getValue());

            DataControl.save();
            DataControl.load();
            JOptionPane.showMessageDialog(this, StringFormat.translate("settings", "settings.save.tip"));
            ThemeChanger.easyChanger();
        });


    }


    // 修改：使用定时轮询取代 FlavorListener
    private void startClipboardListener() {
        clipboardTimer = new Timer(500, e -> {
            if (!DataControl.get("isUseClipBoardListener", false)) {
                return;
            }
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            try {
                if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                    String content = (String) clipboard.getData(DataFlavor.stringFlavor);
                    if (content != null && !content.equals(lastClipboardContent)) {
                        logger.info("剪切板更新");
                        lastClipboardContent = content;
                        String url = extractUrl(content);
                        if (url != null && isValidUrl(url)) {
                            SwingUtilities.invokeLater(() -> showLinkDetectedDialog(url));
                        }
                    }
                }
            } catch (Exception ex) {
                logger.debug("剪切板轮询异常", ex);
            }
        });
        clipboardTimer.start();
    }

    private String extractUrl(String text) {
        if (text == null) return null;
        for (String line : text.split("[\\r\\n]+")) {
            line = line.strip();
            if (line.startsWith("BV") || line.contains("bilibili.com")) {
                return line;
            }
            if (line.startsWith("http://") || line.startsWith("https://")) {
                return line;
            }
        }
        return null;
    }

    private void showLinkDetectedDialog(String url) {
        this.setVisible(true);
        tabbedPane1.setSelectedIndex(0);
        this.toFront();

        if (createTaskPanel == null) {
            createDownloadTask(url);
        }else{
            createTaskPanel.setLink(url);
        }


    }

    private boolean isValidUrl(String url) {
        try {
            if (url.startsWith("BV") || url.contains("bilibili.com")) {
                return isValidBiliUrl(url);
            }else if(url.startsWith("http")) return isHttpReachable(url);
        } catch (Exception e) {
            logger.debug("链接验证失败: " + url, e);
            return false;
        }
        return false;
    }

    private boolean isValidBiliUrl(String url) {
        try {
            String bvId = null;
            if (url.strip().startsWith("BV")) {
                bvId = url.strip();
            } else {
                var matcher = java.util.regex.Pattern.compile("(BV[A-Za-z0-9]+)").matcher(url);
                if (matcher.find()) {
                    bvId = matcher.group(1);
                }
            }
            if (bvId == null) return false;

            var conn = (HttpURLConnection) URI.create("https://api.bilibili.com/x/web-interface/view?bvid=" + bvId).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);
            String jsonText = new String(conn.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            var json = com.alibaba.fastjson.JSON.parseObject(jsonText);
            return json.getIntValue("code") == 0;
        } catch (Exception e) {
            logger.debug("B站链接验证失败: " + url, e);
            return false;
        }
    }

    private boolean isHttpReachable(String url) {
        try {
            var conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            int code = conn.getResponseCode();
            if (code >= 200 && code < 400) {
                String contentType = conn.getContentType();
                conn.disconnect();
                if (contentType == null) return true;
                String lowerType = contentType.toLowerCase();
                if (lowerType.startsWith("text/html")
                        || lowerType.startsWith("application/xhtml+xml")
                        || lowerType.startsWith("text/xml")) {
                    return false;
                }
                return true;
            }
            conn.disconnect();
            return false;
        } catch (Exception e) {
            logger.debug("HTTP链接验证失败: " + url, e);
            return false;
        }
    }

    private void updateDefaultButton() {
        int selectedIndex = tabbedPane1.getSelectedIndex();
        if (selectedIndex == tabbedPane1.indexOfComponent(downloaderPanel)) {
            getRootPane().setDefaultButton(createTaskButton);
        } else if (selectedIndex == tabbedPane1.indexOfComponent(settingsPanel)) {
            getRootPane().setDefaultButton(saveButton);
        } else if (selectedIndex == tabbedPane1.indexOfComponent(SpecialSettingsPanel)){
            if (SpecialSettingsTabbedPane.getSelectedComponent() instanceof BasicSpecialSettings.SpecialSettingsPanel specialSettingsPanel) {
                specialSettingsPanel.setDefaultButton();
            }
        }else getRootPane().setDefaultButton(null);
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        trayIcon.displayMessage("WDownLoader", "已最小化到系统托盘", TrayIcon.MessageType.INFO);
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}
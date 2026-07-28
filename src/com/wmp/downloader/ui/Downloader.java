package com.wmp.downloader.ui;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.EasterEggData;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.ui.common.PathSelectionPanel;
import com.wmp.downloader.ui.settings.BasicSpecialSettings;
import com.wmp.downloader.ui.specialSettings.BiliSettings;
import com.wmp.downloader.ui.specialSettings.FFmpegSettings;
import com.wmp.downloader.ui.specialSettings.GithubAccelerateSettings;
import com.wmp.downloader.ui.task.DownloadTask;
import com.wmp.downloader.ui.task.createTask.CreateTaskPanel;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;   // FIX 新增导入
import java.awt.event.ComponentEvent;     // FIX 新增导入
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Downloader extends JFrame implements WindowListener {

    public static Downloader mainFrame;
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
                    ToastMessage.show(this, String.format(StringFormat.translate("task", "task.download_task.success.confirm"), urlDownloadTask.getFileName()), ToastMessage.SUCCESS);
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
    private PathSelectionPanel backgroundSelectionPanel;
    private JComboBox<String> BackgroundModeComboBox;
    private JScrollPane settingsScrollPane;
    private JSlider alphaSlider;
    private JCheckBox alibabaFastjsonCheckBox;
    private JCheckBox log4jLog4jCheckBox;
    private JScrollPane aboutScrollPane;
    private String lastClipboardContent = "";

    private Timer clipboardTimer;

    private CreateTaskPanel createTaskPanel = null;

    private ActionListener actionListener = e -> {
        this.setVisible(true);
        this.setState(JFrame.NORMAL);
    };

    private JLayeredPane layeredPane = new JLayeredPane();
    private BackgroundPanel backgroundPanel;
    private JPanel backgroundPreviewPanel;
    private Timer backgroundupdateTimer = new Timer(100, e->{
        updateBackground();
        updateChildBounds(); // FIX 使用统一方法
    });
    // FIX 删除了无用的 backgroundTimer 字段

    public Downloader() {

        mainFrame = this;

        taskListener.start();


        this.getRootPane().putClientProperty( "JRootPane.fullWindowContent", true );
        //this.getRootPane().setBackground( new Color( 0, 0, 0, 0 ) );

        this.setTitle(StringFormat.translate("common", "app_name"));
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

        // 使用JLayeredPane包装主界面
        initLayeredPane();

        // 初始化背景相关
        initBackgroundSettings();



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

        // FIX 确保初始显示时子组件边界正确
        SwingUtilities.invokeLater(this::updateChildBounds);

        backgroundupdateTimer.start();
    }

    private void initLayeredPane() {
        // 将UIPanel添加到默认层
        layeredPane.add(UIPanel, JLayeredPane.DEFAULT_LAYER);
        // FIX 添加组件监听器，在尺寸变化时更新子组件边界
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateChildBounds();
            }
        });
        this.setContentPane(layeredPane);
    }

    // FIX 新增方法：更新UIPanel和背景面板的边界
    private void updateChildBounds() {
        int w = layeredPane.getWidth();
        int h = layeredPane.getHeight();
        if (w > 0 && h > 0) {
            UIPanel.setBounds(0, 0, w, h);
            if (backgroundPanel != null && backgroundPanel.isVisible()) {
                backgroundPanel.setBounds(0, 0, w, h);
            }
            layeredPane.revalidate();
            layeredPane.repaint();
        }
    }

    private void initBackgroundSettings() {
        // 创建背景面板（如果不存在）
        if (backgroundPanel == null) {
            String backgroundPath = DataControl.get("background", null);
            if (backgroundPath != null && !backgroundPath.isEmpty()) {
                try {
                    ImageIcon backgroundIcon = new ImageIcon(backgroundPath);
                    backgroundPanel = new BackgroundPanel(backgroundIcon.getImage());
                } catch (Exception e) {
                    logger.warn("背景图片加载失败: " + backgroundPath, e);
                }
            }
        }

        // 添加背景面板到最底层
        if (backgroundPanel != null) {
            layeredPane.add(backgroundPanel, JLayeredPane.FRAME_CONTENT_LAYER);
            // FIX 使用统一的边界更新方法
            updateChildBounds();
        }
    }

    // FIX 删除原来的 updateBackgroundBounds，合并到 updateChildBounds 中

    private void updateBackground() {
        String backgroundPath = DataControl.get("background", null);
        String mode = DataControl.get("background_mode", "None");

        if ("Image".equals(mode) && backgroundPath != null && !backgroundPath.isEmpty()) {
            try {
                ImageIcon backgroundIcon = new ImageIcon(backgroundPath);
                Image backgroundImage = backgroundIcon.getImage();

                if (backgroundPanel == null) {
                    backgroundPanel = new BackgroundPanel(backgroundImage);
                    layeredPane.add(backgroundPanel, JLayeredPane.FRAME_CONTENT_LAYER);
                } else {
                    backgroundPanel.updateBackgroundImage(backgroundImage);
                }
                backgroundPanel.setVisible(true);
                layeredPane.setLayer(UIPanel, JLayeredPane.DEFAULT_LAYER);
                layeredPane.setLayer(backgroundPanel, JLayeredPane.FRAME_CONTENT_LAYER);
                // FIX 更新边界并强制重绘
                updateChildBounds();
                backgroundPanel.repaint();
            } catch (Exception e) {
                logger.warn("背景图片加载失败: " + backgroundPath, e);
                resetBackground();
            }
        } else {
            resetBackground();
        }
    }

    private void resetBackground() {
        if (backgroundPanel != null) {
            backgroundPanel.setVisible(false);
            // FIX 刷新界面
            layeredPane.repaint();
        }
    }

    private void initSpecialSettingsComponents() {
        BasicSpecialSettings[] basicSpecialSettings = new BasicSpecialSettings[]{
                new BiliSettings(), new FFmpegSettings(), new GithubAccelerateSettings()
        };

        for (var specialSettings : basicSpecialSettings) {
            var jScrollPane1 = new JScrollPane(specialSettings.getSettings());
            jScrollPane1.setOpaque(false);
            jScrollPane1.getViewport().setOpaque(false);
            jScrollPane1.setBorder(null);
            jScrollPane1.getViewport().setBorder(null);
            SpecialSettingsTabbedPane.addTab(specialSettings.getSettingsName(), jScrollPane1);
        }
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

        //窗口
        var windowMenu = new JMenu(StringFormat.translate("download_menu_bar", "frame"));

        var alwaysOnTopCheckBox = new JCheckBoxMenuItem(StringFormat.translate("download_menu_bar", "frame.is_always_top"));
        alwaysOnTopCheckBox.addActionListener(e -> this.setAlwaysOnTop(alwaysOnTopCheckBox.isSelected()));

        windowMenu.add(alwaysOnTopCheckBox);

        windowMenu.addSeparator();

        var refreshMenuItem = new JMenuItem(StringFormat.translate("download_menu_bar", "frame.refresh"));
        refreshMenuItem.setToolTipText(StringFormat.translate("download_menu_bar", "frame.refresh.tooltip"));
        refreshMenuItem.addActionListener(e -> {
            DataControl.load();
            updateBackground();
            updateChildBounds(); // FIX 使用统一方法
            ThemeChanger.easyChanger();
        });
        windowMenu.add(refreshMenuItem);

        var updateFrameMenuItem = new JMenuItem(StringFormat.translate("download_menu_bar", "frame.update_frame"));
        updateFrameMenuItem.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, StringFormat.translate("download_menu_bar", "frame.update_frame.tip"), StringFormat.translate("common", "warn"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                if (clipboardTimer != null) {
                    clipboardTimer.stop();
                }
                this.dispose();
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

        //软件
        var AppMenu = new JMenu(StringFormat.translate("download_menu_bar", "app"));

        var DisclaimerMenuItem = new JMenuItem(StringFormat.translate("download_menu_bar", "app.disclaimer"));
        DisclaimerMenuItem.addActionListener(e -> {
            var panel = new JPanel(new BorderLayout());
            var textArea = new JTextArea("""
                    本工具（以下简称“本软件”）仅用于 个人学习、技术研究和学术交流 之目的，旨在帮助用户了解视频平台的数据传输机制与文件格式。
                    用户在使用本软件下载任何视频内容前， 必须 仔细阅读并同意以下条款：
                    1.  版权归属  \s
                       所有通过本软件下载的视频、音频、封面图等内容的版权均归原始权利人（包括但不限于抖音/字节跳动、哔哩哔哩/上海宽娱及相应创作者）所有。本软件不占有、不修改、不转授任何下载内容的版权。
                    2.  合法使用承诺  \s
                       用户承诺仅下载 自己拥有合法授权 或 已获权利人明确许可 的内容，或下载用于 合理引用、解说、学术研究 等符合《中华人民共和国著作权法》第二十四条规定的“合理使用”情形。
                    3.  禁止行为  \s
                        严禁 将下载内容用于以下用途：
                       - 商业盈利、广告投放、付费分发或任何形式的变现；
                       - 篡改水印、冒名发布、侵犯原作者署名权；
                       - 批量抓取、数据爬取或破坏平台正常运营秩序；
                       - 传播违法信息、低俗内容或侵犯他人肖像权、隐私权；
                       - 其他违反国家法律法规、平台服务协议及公序良俗的行为。
                    4.  责任承担  \s
                       用户因违反上述条款而产生的 全部法律责任（包括但不限于民事赔偿、行政处罚、平台追责等）均由用户自行承担 ，与本软件开发者、运营者及贡献者无关。本软件不提供任何内容上的担保，亦不对下载后内容的完整性、合法性做任何明示或默示的保证。
                    5.  终止与删除  \s
                       若本软件收到相关权利人的有效侵权通知，开发者有权随时终止服务或屏蔽特定功能。用户下载的内容应在 学习完成后24小时内删除 ，不得长期留存。
                    特别提醒：请尊重每一位创作者的劳动成果。若您希望长期欣赏或使用某作品，请前往官方平台进行正版观看或购买授权。
                    >使用本软件即视为您已阅读、理解并同意本免责声明全文。若不同意，请立即停止使用并卸载本软件。
                    （本声明最终解释权归本软件开发者所有，并保留根据法律法规变化适时修订的权利。）
                    """);
            textArea.setLineWrap(true);
            textArea.setColumns(30);
            panel.add(textArea);

            FunctionDialog.showDialog(this, "免责声明", panel,
                    _ -> {},
                    FunctionDialog.DEFAULT_BUTTONS, 0,
                    null, FunctionDialog.NORTH_DIRECTION_RIGHT, false, true);
        });
        AppMenu.add(DisclaimerMenuItem);

        var aboutMenuItem = new JMenuItem(StringFormat.translate("download_menu_bar", "app.about"));
        aboutMenuItem.addActionListener(e -> tabbedPane1.setSelectedIndex(tabbedPane1.getTabCount() - 1));
        AppMenu.add(aboutMenuItem);

        menuBar.add(AppMenu);

        this.setJMenuBar(menuBar);
    }

    private void createUIComponents() {

        settingsScrollPane = new JScrollPane(settingsPanel);
        settingsScrollPane.setBorder(null);
        settingsScrollPane.getViewport().setBorder(null);
        settingsScrollPane.getViewport().setOpaque(false);
        settingsScrollPane.setOpaque(false);


        backgroundSelectionPanel = new PathSelectionPanel(StringFormat.translate("settings", "settings.personalized.background_path"), new File(DataControl.get("background", "")), SystemFileChooser.FILES_ONLY);
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

        aboutScrollPane.getViewport().setOpaque(false);
    }

    private void initTaskComponents() {
        ThemeChanger.addInDynamicConverter(
                this::updateDefaultButton
        );

        createTaskButton.putClientProperty("FlatLaf.style", "font: $h2.font");
        allStartButton.putClientProperty("FlatLaf.style", "font: $h2.font");
        allPauseButton.putClientProperty("FlatLaf.style", "font: $h2.font");

        IconControl.addInDynamicConverter(
                () -> createTaskButton.setIcon(IconControl.getIcon("new", createTaskButton.getFont().getSize())),
                () -> allStartButton.setIcon(IconControl.getIcon("start", allStartButton.getFont().getSize())),
                () -> allPauseButton.setIcon(IconControl.getIcon("pause", allPauseButton.getFont().getSize()))
        );

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
                    null, FunctionDialog.NORTH_DIRECTION_RIGHT,
                    true, true);
        });

        var SupportButton = new JButton(StringFormat.translate("common", "support"));
        SupportButton.addActionListener(_ -> {
            var panel = new JPanel();
            var textArea = new JTextArea(StringFormat.translate("common", "support_text_area"));
            panel.add(textArea);

            FunctionDialog.showDialog(this, StringFormat.translate("common", "support"), panel,
                    _ -> {},
                    FunctionDialog.DEFAULT_BUTTONS, 0,
                    null, FunctionDialog.NORTH_DIRECTION_RIGHT,
                    true, true);
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
        ThemeChanger.addInDynamicConverter(
                this::updateDefaultButton
        );

        settingsScrollPane.getVerticalScrollBar().setUnitIncrement(10);

        isUseSSLCheckBox.setSelected(DataControl.get("isUseSSL", false));
        isUseClipBoardListenerCheckBox.setSelected(DataControl.get("isUseClipBoardListener", false));
        ThreadNumSlider.setValue(DataControl.get("ThreadNum", 64));
        ThreadNumLabel.setText(String.valueOf(ThreadNumSlider.getValue()));
        alphaSlider.setValue((int) (DataControl.get("background_alpha", new BigDecimal("0.3")).floatValue()*100));

        BackgroundModeComboBox.addItem("None");
        BackgroundModeComboBox.addItem("Image");

        BackgroundModeComboBox.setSelectedItem(DataControl.get("background_mode", "None"));

        backgroundSelectionPanel.setPath(DataControl.get("background", ""));
        if(Objects.equals(BackgroundModeComboBox.getSelectedItem(), "Image")){
            backgroundSelectionPanel.setVisible(true);
        } else backgroundSelectionPanel.setVisible(false);

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

        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String font : fonts) {
            FontListComboBox.addItem(font);
        }
        FontListComboBox.setSelectedItem(DataControl.get("Font", "Microsoft YaHei"));

        IconControl.addInDynamicConverter(
                () -> dataPathButton.setIcon(IconControl.getIcon("folder", dataPathButton.getFont().getSize())),
                () -> deleteTempFolderDataButton.setIcon(IconControl.getIcon("trash", deleteTempFolderDataButton.getFont().getSize()))
        );
        IconControl.addInDynamicConverter(
                () -> refreshButton.setIcon(IconControl.getIcon("refresh", refreshButton.getFont().getSize())),
                () -> saveButton.setIcon(IconControl.getIcon("save", saveButton.getFont().getSize()))
        );

        tabbedPane1.addChangeListener(e -> updateDefaultButton());
        ThreadNumSlider.addChangeListener(e -> {
            ThreadNumLabel.setText(String.valueOf(ThreadNumSlider.getValue()));
            ThreadNumLabel.setSize(ThreadNumLabel.getPreferredSize());
        });

        BackgroundModeComboBox.addItemListener(e ->{
            DataControl.putAndSave("background_mode", e.getItem().toString());
            if (e.getItem().equals("Image")) {
                backgroundSelectionPanel.setVisible(true);
            } else{
                backgroundSelectionPanel.setVisible(false);
            }
        });
        backgroundSelectionPanel.setPathChangeListener(path -> {
            DataControl.putAndSave("background", path);
        });
        alphaSlider.addChangeListener(e -> {
            DataControl.putAndSave("background_alpha", (float) alphaSlider.getValue()/100.0f);
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

            updateBackground();
            updateChildBounds(); // FIX 使用统一方法

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

            updateBackground();
            updateChildBounds(); // FIX 使用统一方法

            ToastMessage.show(this, StringFormat.translate("settings", "settings.save.tip"), ToastMessage.SUCCESS);
            ThemeChanger.easyChanger();
        });
    }

    private void startClipboardListener() {
        clipboardTimer = new Timer(500, e -> {
            if (!DataControl.get("isUseClipBoardListener", false)) {
                return;
            }
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            try {
                if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                    String content = (String) clipboard.getData(DataFlavor.stringFlavor);
                    if (!lastClipboardContent.strip().isEmpty() && content != null && !content.equals(lastClipboardContent)) {
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
        // FIX 移除了 backgroundTimer 的停止（已删除该字段）
        trayIcon.displayMessage("WDownLoader", "已最小化到系统托盘", TrayIcon.MessageType.INFO);
    }

    @Override
    public void windowClosed(WindowEvent e) {
        backgroundupdateTimer.stop();
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
        backgroundupdateTimer.start();
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        backgroundupdateTimer.stop();
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        // FIX 窗口尺寸变化时更新子组件
        updateChildBounds();
    }

    private static class BackgroundPanel extends JPanel {
        private Image backgroundImage;
        private Color backgroundColor;

        public BackgroundPanel(Image backgroundImage) {
            this.backgroundImage = backgroundImage;
            setOpaque(false);
            setLayout(new BorderLayout());
        }

        public void updateBackgroundImage(Image newImage) {
            this.backgroundImage = newImage;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(AlphaComposite.SrcOver.derive(DataControl.get("background_alpha", new BigDecimal("0.3")).floatValue()));

                int panelWidth = getWidth();
                int panelHeight = getHeight();
                int imgWidth = backgroundImage.getWidth(this);
                int imgHeight = backgroundImage.getHeight(this);

                if (imgWidth > 0 && imgHeight > 0) {
                    double scale = Math.max((double) panelWidth / imgWidth,
                            (double) panelHeight / imgHeight);
                    int scaledWidth = (int) (imgWidth * scale);
                    int scaledHeight = (int) (imgHeight * scale);

                    int x = (panelWidth - scaledWidth) / 2;
                    int y = (panelHeight - scaledHeight) / 2;

                    g2d.drawImage(backgroundImage, x, y, scaledWidth, scaledHeight, this);
                }
                g2d.dispose();
            }
        }
    }
}
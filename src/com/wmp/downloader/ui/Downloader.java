package com.wmp.downloader.ui;

import com.wmp.downloader.newArchitecture.ParserTaskInfo;
import com.wmp.downloader.newArchitecture.abstractTask.*;
import com.wmp.downloader.newArchitecture.ui.createTask.CreateTaskPanel;
import com.wmp.downloader.newArchitecture.ui.mainFrame.statusPanel.StatusPanel;
import com.wmp.downloader.newArchitecture.ui.mainFrame.mainPanels.AboutPanel;
import com.wmp.downloader.newArchitecture.ui.mainFrame.mainPanels.PluginParserPanel;
import com.wmp.downloader.newArchitecture.ui.mainFrame.mainPanels.SettingsPanel;
import com.wmp.downloader.newArchitecture.ui.mainFrame.mainPanels.SpecialSettingsPanel;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.tools.ui.UITools;
import com.wmp.downloader.tools.update.GetUpdateInfo;
import com.wmp.downloader.ui.common.LazyTabbedPane;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXBusyLabel;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class Downloader extends JFrame implements WindowListener{

    private static final Logger logger = Logger.getLogger(Downloader.class);
    public static Downloader mainFrame;
    public static TrayIcon trayIcon;
    public final List<AbstractTask> taskList = new ArrayList<>();
    public final List<AbstractTask> taskFinalyTipList = new ArrayList<>();
    public final GridBagConstraints gbc = new GridBagConstraints();

    public JPanel UIPanel;
    public JPanel settingsPanel;
    public JTabbedPane mainTabbedPane;
    public JPanel downloaderPanel;
    public JButton createTaskButton;
    public JPanel TaskButtonPanel;
    public JPanel TasksPanel;
    public JButton allStartButton;
    public JButton allPauseButton;
    public JScrollPane TasksScrollPane;
    public JPanel SpecialSettingsPanel;
    public JPanel pluginParserControlPanel;
    public JPanel aboutPanel;
    public StatusPanel StatusPanel = new StatusPanel(this);

    public SettingsPanel settingsPanelInstance;
    public SpecialSettingsPanel specialSettingsPanelInstance;
    public PluginParserPanel pluginParserPanelInstance;
    public AboutPanel aboutPanelInstance;

    public String lastClipboardContent = "";

    public Timer clipboardTimer;

    public CreateTaskPanel createTaskPanel = null;

    public final ActionListener actionListener = e -> {
        this.setVisible(true);
        this.setState(JFrame.NORMAL);

    };

    public JLayeredPane layeredPane = new JLayeredPane();
    public BackgroundPanel backgroundPanel;
    public JPanel backgroundPreviewPanel;
    public Timer backgroundupdateTimer = new Timer(100, e -> {
        updateBackground();
        updateChildBounds(); // FIX 使用统一方法
    });
    public final Timer taskListener = new Timer(100, e -> {
        taskList.removeIf(DownloadTask ->
        {
            if (DownloadTask.isCanExit()) {
                TasksPanel.remove(DownloadTask);
                taskFinalyTipList.remove(DownloadTask);
                return true;
            } else return false;
        });
        taskList.forEach(urlDownloadTask -> {
            if (urlDownloadTask.isFinally() && !taskFinalyTipList.contains(urlDownloadTask)) {
                taskFinalyTipList.add(urlDownloadTask);
                Thread.ofVirtual().start(urlDownloadTask::runWhenFinally);
                ToastMessage.show(this,
                    String.format(StringFormat.translate("task", "task.download_task.success.confirm"), urlDownloadTask.getFileName()),
                    ToastMessage.SUCCESS);
                /*if (SystemTray.isSupported()) {
                    trayIcon.displayMessage(null,
                                String.format(StringFormat.translate("task", "task.download_task.success.confirm"), urlDownloadTask.getFileName()),
                                TrayIcon.MessageType.INFO);
                    }*/
            }
        });
    });


    public Downloader() {

        mainFrame = this;

        taskListener.start();


        //this.getRootPane().putClientProperty("JRootPane.fullWindowContent", true);
        //this.getRootPane().setBackground( new Color( 0, 0, 0, 0 ) );

        this.setTitle(StringFormat.translate("common", "app_name"));
        this.setContentPane(UIPanel);
        this.setMinimumSize(new Dimension(900, 650));

        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        IconControl.addInDynamicConverter(
                () -> this.setIconImage(IconControl.getImage("icon", 256))
        );
        this.addWindowListener(this);

        initTrayIcon();
        initMenuBar();

        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;                 // 关键：不分配额外的垂直空间
        gbc.anchor = GridBagConstraints.NORTH; // 关键：顶部对齐
        gbc.insets = new Insets(5, 0, 5, 0);    // 上下各 5px 间距


        IconControl.addInDynamicConverter(
                () -> {
                    var size = mainTabbedPane.getFont().getSize();
                    mainTabbedPane.setIconAt(mainTabbedPane.indexOfComponent(downloaderPanel), IconControl.getIcon("task", size));
                    mainTabbedPane.setIconAt(mainTabbedPane.indexOfComponent(settingsPanel), IconControl.getIcon("settings", size));
                    mainTabbedPane.setIconAt(mainTabbedPane.indexOfComponent(SpecialSettingsPanel), IconControl.getIcon("settings", size));
                    mainTabbedPane.setIconAt(mainTabbedPane.indexOfComponent(pluginParserControlPanel), IconControl.getIcon("plugin", size));
                    mainTabbedPane.setIconAt(mainTabbedPane.indexOfComponent(aboutPanel), IconControl.getIcon("about", size));
                }
        );

        // 使用JLayeredPane包装主界面
        initLayeredPane();

        // 初始化背景相关
        initBackgroundSettings();

        //拓展管理
        createLazyLoadPanelInMainFrame(pluginParserControlPanel, () -> initPluginParserComponents());

        //任务
        initTaskComponents();

        //设置
        createLazyLoadPanelInMainFrame(settingsPanel, () -> initSettingsComponents());

        //专项设置
        createLazyLoadPanelInMainFrame(SpecialSettingsPanel, () -> initSpecialSettingsComponents());

        //关于
        createLazyLoadPanelInMainFrame(aboutPanel, () -> initAboutComponents());

        UIPanel.add(StatusPanel, BorderLayout.SOUTH);

        startClipboardListener();

        pack();
        this.setLocationRelativeTo(null);

        // FIX 确保初始显示时子组件边界正确
        SwingUtilities.invokeLater(this::updateChildBounds);

        backgroundupdateTimer.start();

    }

    private void createLazyLoadPanelInMainFrame(JPanel panel, Runnable run){
        var ref = new Object() {
            ChangeListener l = null;
        };
        ref.l = e -> {
            if (panel == mainTabbedPane.getSelectedComponent()) {
                //在空白页面的正中央显示 JXBusyLabel 并启动等待动画
                final JXBusyLabel busyLabel = new JXBusyLabel();
                busyLabel.setPreferredSize(new Dimension(64, 64));
                busyLabel.setHorizontalAlignment(SwingConstants.CENTER);
                // 确保 BusyPainter 被初始化（内部 Timer 依赖它）
                final org.jdesktop.swingx.painter.BusyPainter busyPainter = busyLabel.getBusyPainter();
                if (busyPainter != null) {
                    busyPainter.setPaintCentered(true);
                }
                // 使用容器将 busyLabel 严格居中于空白页面中央（不拉伸铺满）
                final JPanel busyHost = new JPanel(new GridBagLayout());
                busyHost.setOpaque(false);
                busyHost.add(busyLabel);
                panel.setLayout(new BorderLayout());
                panel.removeAll();
                panel.add(busyHost, BorderLayout.CENTER);
                panel.revalidate();
                panel.repaint();
                // JXBusyLabel 自身的动画 Timer 在 FlatLaf 下可能不会可靠地推进重绘，
                // 因此这里用一个独立的 Swing Timer 手动推进 frame 并强制 repaint，
                // 确保转圈动画一定可见。
                final Timer animTimer = new Timer(80, ev -> {
                    if (busyPainter != null && busyPainter.getPoints() > 0) {
                        busyPainter.setFrame((busyPainter.getFrame() + 1) % busyPainter.getPoints());
                    }
                    busyLabel.repaint();
                });
                animTimer.setRepeats(true);
                animTimer.start();
                SwingUtilities.invokeLater(() -> {
                    logger.info("正在加载：" + run);
                    run.run();
                    animTimer.stop();
                    busyLabel.setBusy(false);
                    panel.remove(busyHost);
                    panel.revalidate();
                    panel.repaint();
                    mainTabbedPane.removeChangeListener(ref.l);
                });
            }

        };
        mainTabbedPane.addChangeListener(ref.l);
    }

    private void initPluginParserComponents() {
        pluginParserPanelInstance = new PluginParserPanel(this);
        pluginParserPanelInstance.initPluginParserComponents();
        pluginParserControlPanel.removeAll();
        pluginParserControlPanel.add(pluginParserPanelInstance.pluginParserControlPanel, BorderLayout.CENTER);
        pluginParserControlPanel.revalidate();
        pluginParserControlPanel.repaint();
    }

    private void initSettingsComponents() {
        settingsPanelInstance = new SettingsPanel(this);
        settingsPanelInstance.initSettingsComponents();
        settingsPanel.removeAll();
        settingsPanel.add(settingsPanelInstance.settingsPanel, BorderLayout.CENTER);
        settingsPanel.revalidate();
        settingsPanel.repaint();
    }

    private void initSpecialSettingsComponents() {
        specialSettingsPanelInstance = new SpecialSettingsPanel(this);
        specialSettingsPanelInstance.initSpecialSettingsComponents();
        SpecialSettingsPanel.removeAll();
        SpecialSettingsPanel.add(specialSettingsPanelInstance.specialSettingsPanel, BorderLayout.CENTER);
        SpecialSettingsPanel.revalidate();
        SpecialSettingsPanel.repaint();
    }

    private void initAboutComponents() {
        aboutPanelInstance = new AboutPanel(this);
        aboutPanelInstance.initAboutComponents();
        aboutPanel.removeAll();
        aboutPanel.add(aboutPanelInstance.aboutPanel, BorderLayout.CENTER);
        aboutPanel.revalidate();
        aboutPanel.repaint();
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
    public void updateChildBounds() {
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

    public void updateBackground() {
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

    private void initTrayIcon() {
        if (SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
        } else return;

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
            this.requestFocus();
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
        JPopupMenu popupMenu = new JPopupMenu();

        //窗口
        var windowMenu = new JMenu(StringFormat.translate("download_menu_bar", "frame"));

        var alwaysOnTopCheckBox = new JCheckBoxMenuItem(StringFormat.translate("download_menu_bar", "frame.is_always_top"));
        alwaysOnTopCheckBox.addActionListener(e -> this.setAlwaysOnTop(alwaysOnTopCheckBox.isSelected()));

        windowMenu.add(alwaysOnTopCheckBox);

        windowMenu.addSeparator();

        var refreshMenuItem = new JMenuItem(StringFormat.translate("download_menu_bar", "refresh"));
        refreshMenuItem.setToolTipText(StringFormat.translate("download_menu_bar", "frame.refresh.tooltip"));
        refreshMenuItem.addActionListener(e -> {
            DataControl.load();
            updateBackground();
            updateChildBounds(); // FIX 使用统一方法
            ThemeChanger.easyChanger();
        });
        windowMenu.add(refreshMenuItem);

        var updateFrameMenuItem = new JMenuItem(StringFormat.translate("frame.update_frame"));
        updateFrameMenuItem.addActionListener(e -> {
            try {
                if (JOptionPane.showConfirmDialog(this, StringFormat.translate("frame.update_frame.tip"), StringFormat.translate("common", "warn"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                    if (clipboardTimer != null) {
                        clipboardTimer.stop();
                    }
                    this.dispose();
                    DataControl.load();
                    SwingUtilities.invokeLater(()->{
                        ThemeChanger.easyChanger();

                        new Downloader().setVisible(true);
                    });
                }
            } catch (HeadlessException ex) {
                ToastMessage.show(this, StringFormat.translate("refresh_failed"), ToastMessage.ERROR);
                logger.error("刷新失败！", ex);
            }
        });
        windowMenu.add(updateFrameMenuItem);

        windowMenu.addSeparator();

        var exitMenuItem = new JMenuItem(StringFormat.translate("download_menu_bar", "frame.exit"));
        exitMenuItem.addActionListener(e -> System.exit(0));
        windowMenu.add(exitMenuItem);

        popupMenu.add(windowMenu);

        //软件
        var AppMenu = new JMenu(StringFormat.translate("download_menu_bar", "app"));

        var checkUpdateMenuItem = new JMenuItem(StringFormat.translate("check_update"));
        checkUpdateMenuItem.addActionListener(e -> checkUpdate());
        AppMenu.add(checkUpdateMenuItem);

        AppMenu.addSeparator();

        var DisclaimerMenuItem = new JMenuItem(StringFormat.translate("app.disclaimer"));
        DisclaimerMenuItem.addActionListener(e -> {
            var panel = new JPanel(new BorderLayout());
            var textArea = new JTextArea("""
                    本工具（以下简称“本软件”）仅用于 个人学习、技术研究和学术交流 之目的，旨在帮助用户了解视频平台的数据传输机制与文件格式。
                    用户在使用本软件下载任何视频内容前， 必须 仔细阅读并同意以下条款：
                    1.  版权归属  \s
                       所有通过本软件下载的视频、音频、封面图等内容的版权均归原始权利人所有。本软件不占有、不修改、不转授任何下载内容的版权。
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
                    _ -> {
                    },
                    FunctionDialog.DEFAULT_BUTTONS, 0,
                    null, FunctionDialog.NORTH_DIRECTION_RIGHT, false, true);
        });
        AppMenu.add(DisclaimerMenuItem);

        var aboutMenuItem = new JMenuItem(StringFormat.translate("app.about"));
        aboutMenuItem.addActionListener(e -> mainTabbedPane.setSelectedIndex(mainTabbedPane.getTabCount() - 1));
        AppMenu.add(aboutMenuItem);

        popupMenu.add(AppMenu);

        StatusPanel.setPopupMenu(popupMenu);
        //this.setJMenuBar(menuBar);
    }

    private void createUIComponents() {
        mainTabbedPane = new LazyTabbedPane();

        TasksPanel = new JPanel(new GridBagLayout());
        TasksPanel.setOpaque(false);

        TasksScrollPane = new JScrollPane(TasksPanel);
        TasksScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动
        TasksScrollPane.getViewport().setLayout(new ViewportLayout()); // 默认布局，会拉伸组件
        UITools.setScrollPaneUnOpaque(TasksScrollPane);
    }

    public void checkUpdate() {
        try {
            var update = GetUpdateInfo.getUpdateInfo();
            if (update == null || update.url() == null) {
                //没有新版本
                ToastMessage.show(StringFormat.translate("check_update.no_update"), ToastMessage.INFO);
            } else {
                ToastMessage.showConfirm(
                        String.format(StringFormat.translate("check_update.new_update"),
                                update.version()),
                        new FunctionDialog.CustomButtons[]{
                                new FunctionDialog.CustomButtons(StringFormat.translate("learn"), 100),
                                new FunctionDialog.CustomButtons(StringFormat.translate("download"), 200)
                        },
                        (allCount, count, result) -> {
                            if (result == 100) {
                                var panel = new JPanel();

                                panel.add(UITools.createMarkdownPane(update.body()));

                                FunctionDialog.showDialog(this, StringFormat.translate("common", "learn"), panel,
                                        _ -> {
                                        },
                                        FunctionDialog.DEFAULT_BUTTONS, 0,
                                        null, FunctionDialog.NORTH_DIRECTION_RIGHT);
                            } else if (count == 1 && result == 200) {
                                //创建更新任务

                                mainTabbedPane.setSelectedIndex(0);
                                createDownloadTask(update.url(), false);
                            }
                        }
                );
            }
        } catch (Exception ex) {
            ToastMessage.show(StringFormat.translate("check_update.failed"), ToastMessage.ERROR);
            logger.error("网络数据获取失败");
        }
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
        createDownloadTask(url, true);
    }

    private void createDownloadTask(String url, boolean showDialog) {
        var createTaskPanel = new CreateTaskPanel();

        if (url != null) {
            createTaskPanel.setLink(url);
        }


        if (!showDialog) {
            Thread.ofVirtual().start(() -> {
                while (createTaskPanel.getDownloadTasks().size() != 1) {

                }
                if (createTaskPanel.getDownloadTasks().size() == 1) {
                    addDownloadTask(createTaskPanel);
                }
            });
            return;
        }
        //附属UI
        if (this.createTaskPanel == null)
            this.createTaskPanel = createTaskPanel;
        var mainPanel = createTaskPanel.MainPanel;

        var learnMoreButton = new JButton(StringFormat.translate("learn"));
        learnMoreButton.addActionListener(_ -> {
            var panel = new JPanel();
            var textArea = new JTextArea("");
            panel.add(textArea);

            FunctionDialog.showDialog(this, StringFormat.translate("learn"), panel,
                    _ -> {
                    },
                    FunctionDialog.DEFAULT_BUTTONS, 0,
                    null, FunctionDialog.NORTH_DIRECTION_RIGHT,
                    true, true);
        });

        var SupportButton = new JButton(StringFormat.translate("support"));
        SupportButton.addActionListener(_ -> {
            var panel = new JPanel();
            StringBuilder sb = new StringBuilder();
            ParserTaskInfo.getEnablePluginParserList().stream()
                    .map(AbstractParser::getSupportTip)
                    .forEach(tip ->{
                        if (tip.isBlank()) return;
                        sb.append(tip).append(", ");
                    });
            sb.deleteCharAt(sb.length() - 1)
                    .deleteCharAt(sb.length() - 1);
            var textArea = new JTextArea(String.format(
                    StringFormat.translate("common", "support_text_area"),
                    sb));
            panel.add(textArea);

            FunctionDialog.showDialog(this, StringFormat.translate("common", "support"), panel,
                    _ -> {
                    },
                    FunctionDialog.DEFAULT_BUTTONS, 0,
                    null, FunctionDialog.NORTH_DIRECTION_RIGHT,
                    true, true);
        });

        FunctionDialog.showDialog(this, StringFormat.translate("task", "task.creat_task"), mainPanel,
                result -> {
                    if (result == FunctionDialog.RESULT_OK) {
                        addDownloadTask(this.createTaskPanel);
                    }
                    this.createTaskPanel = null;
                }
                , FunctionDialog.OK_CANCEL_BUTTONS, 0,
                new JButton[]{learnMoreButton, SupportButton}, FunctionDialog.NORTH_DIRECTION_RIGHT);
    }

    public void addDownloadTask(AbstractTask... tasks) {
        if (tasks == null) {
            return;
        }
        Thread.ofVirtual().start(() -> {
            for (var task : tasks) {
                task.setAlignmentX(Component.LEFT_ALIGNMENT);

                taskList.add(task);

                TasksPanel.add(task, gbc);
                TasksPanel.revalidate();
                TasksPanel.repaint();

                task.start();
            }
        });
    }

    public void addDownloadTask(CreateTaskPanel createTaskPanel) {
        Thread.ofVirtual().start(()->{
            createTaskPanel.getDownloadTasks().forEach(taskPanel -> {
                taskPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

                taskList.add(taskPanel);

                TasksPanel.add(taskPanel, gbc);
                TasksPanel.revalidate();
                TasksPanel.repaint();

                taskPanel.start();
            });
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
                    if (!lastClipboardContent.isBlank() && content != null && !content.equals(lastClipboardContent)) {
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

    public void showLinkDetectedDialog(String url) {
        this.setVisible(true);
        mainTabbedPane.setSelectedIndex(0);
        this.toFront();

        if (createTaskPanel == null) {
            createDownloadTask(url);
        } else {
            createTaskPanel.setLink(url);
        }
    }

    private boolean isValidUrl(String url) {
        try {
            if (url.startsWith("BV") || url.contains("bilibili.com")) {
                return isValidBiliUrl(url);
            } else if (url.startsWith("http")) return isHttpReachable(url);
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

    public void updateDefaultButton() {
        int selectedIndex = mainTabbedPane.getSelectedIndex();
        if (selectedIndex == mainTabbedPane.indexOfComponent(downloaderPanel)) {
            getRootPane().setDefaultButton(createTaskButton);
        } else if (selectedIndex == mainTabbedPane.indexOfComponent(settingsPanel)) {
            getRootPane().setDefaultButton(settingsPanelInstance == null ? null : settingsPanelInstance.getSaveButton());
        } else if (selectedIndex == mainTabbedPane.indexOfComponent(SpecialSettingsPanel)) {
            if (specialSettingsPanelInstance != null && specialSettingsPanelInstance.getSpecialSettingsTabbedPane().getSelectedComponent() instanceof AbstractSpecialSettingsPage specialSettingsPanel) {
                specialSettingsPanel.setDefaultButton();
            }
        } else if (selectedIndex == mainTabbedPane.indexOfComponent(aboutPanel)) {
            getRootPane().setDefaultButton(aboutPanelInstance == null ? null : aboutPanelInstance.getCheckUpdateButton());
        } else getRootPane().setDefaultButton(null);
    }

    @Override
    public void windowOpened(WindowEvent e) {
        logger.info("窗口已显示");
        if (DataControl.get("is_start_check_update", true)) {
            checkUpdate();
        }
    }

    @Override
    public void windowClosing(WindowEvent e) {
        // FIX 移除了 backgroundTimer 的停止（已删除该字段）
        trayIcon.displayMessage("SpeedBump", "已最小化到系统托盘", TrayIcon.MessageType.INFO);
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

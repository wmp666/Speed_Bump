package com.wmp.downloader.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.wmp.downloader.newArchitecture.ParserTaskInfo;
import com.wmp.downloader.newArchitecture.abstractTask.*;
import com.wmp.downloader.newArchitecture.ui.createTask.CreateTaskPanel;
import com.wmp.downloader.newArchitecture.ui.mainFrame.statusPanel.StatusPanel;
import com.wmp.downloader.newArchitecture.ui.mainFrame.mainPanels.AboutPanel;
import com.wmp.downloader.newArchitecture.ui.mainFrame.mainPanels.PluginParserPanel;
import com.wmp.downloader.newArchitecture.ui.mainFrame.mainPanels.SettingsPanel;
import com.wmp.downloader.newArchitecture.ui.mainFrame.mainPanels.SpecialSettingsPanel;
import com.wmp.downloader.newArchitecture.ui.mainFrame.testFrame.TestControlDialog;
import com.wmp.downloader.tools.MicrosoftTranslator;
import com.wmp.downloader.tools.MicrosoftTranslator.Language;
import com.wmp.speed_bump.common.background.tool.StringFormat;
import com.wmp.downloader.tools.TestFunctionControl;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.tools.ui.UITools;
import com.wmp.downloader.tools.update.GetUpdateInfo;
import com.wmp.downloader.ui.common.LazyTabbedPane;
import com.wmp.speed_bump.platform.ui.swing.components.SearchTextField;
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

    /**
     * 显示背景图时标题栏的不透明度（0 全透明 ~ 255 全不透明）。
     * 数值越小背景图透出越明显，但标题文字与窗口按钮会越难辨认。
     */
    private static final int TITLE_BAR_ALPHA = 160;

    public static Downloader mainFrame;
    public static TrayIcon trayIcon;
    public static TrayMenu trayMenu;
    public final List<AbstractTask> taskList = new ArrayList<>();
    public final List<AbstractTask> taskFinalyTipList = new ArrayList<>();
    public final GridBagConstraints gbc = new GridBagConstraints();

    public JPanel UIPanel;

    /** 内容区顶部的搜索框。搜索行为尚未实现，这里只负责把它显示出来 */
    private SearchTextField searchTextField;

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

        // 搜索框：优先放进标题栏，标题栏里腾不出位置时降级到状态栏
        initSearchField();

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

    /**
     * 创建搜索框，并放进切换标签页的换页栏右侧。
     *
     * <p>搜索行为尚未实现，这里只负责让它显示出来。</p>
     */
    private void initSearchField() {
        searchTextField = new SearchTextField(16);
        searchTextField.setPlaceholderText("搜索");
        // 宽度是「最小宽度」，高度由标签区自动决定；见 tryAttachSearchToTabBar()
        //searchTextField.setPreferredSize(new Dimension(150, 26));

        if (!tryAttachSearchToTabBar()) {
            // 换页栏不可用（拿不到 mainTabbedPane）→ 兜底到状态栏
            StatusPanel.attachSearchComponent(searchTextField);
        }
    }

    /**
     * 把搜索框放到换页栏（标签区域）的右侧。
     *
     * <p>用的是 FlatLaf 官方支持的扩展点 {@code TABBED_PANE_TRAILING_COMPONENT}：
     * 组件会被放在标签区域的后缘，高度自动取标签区高度，宽度取可用水平空间
     * （下限是组件的 preferred width）。不依赖任何 FlatLaf 内部结构，也不会被标签页遮挡。</p>
     *
     * @return 是否成功挂到换页栏
     */
    private boolean tryAttachSearchToTabBar() {
        if (mainTabbedPane == null) {
            return false;
        }

        // 搜索框保留自身外观（含边框），只把它摆到换页栏右侧
        searchTextField.setTransparentBackground(false);

        // 不能直接把 SearchTextField 交给 trailing component：FlatLaf 会让它占满可用水平空间
        // （实测 preferredSize 设 150 会被拉伸到 600+，preferredSize 只是下限）。
        // 包一层 FlowLayout 容器后，容器被拉伸而内部组件保持自己的尺寸。
        JPanel holder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        holder.setOpaque(false);
        holder.add(searchTextField);

        mainTabbedPane.putClientProperty(
                FlatClientProperties.TABBED_PANE_TRAILING_COMPONENT, holder);
        mainTabbedPane.revalidate();
        mainTabbedPane.repaint();
        return true;
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
                JRootPane rootPane = getRootPane();
                if (rootPane != null && backgroundPanel.getParent() == rootPane.getLayeredPane()) {
                    // 背景层挂在 rootPane 上，要覆盖整个窗口（含标题栏区域）
                    backgroundPanel.setBounds(0, 0, rootPane.getWidth(), rootPane.getHeight());
                } else {
                    backgroundPanel.setBounds(0, 0, w, h);
                }
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

        if (backgroundPanel != null) {
            // 回退：背景层放回 contentPane 内（不再挂到 rootPane、不再改标题栏透明度）
            if (backgroundPanel.getParent() != layeredPane) {
                layeredPane.add(backgroundPanel, JLayeredPane.FRAME_CONTENT_LAYER);
            }
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

    /**
     * <b>当前未启用。</b>
     *
     * <p>这条路径把背景图挂到 rootPane 铺满整窗、再让标题栏半透明，虽然能让背景图透到标题栏，
     * 但 {@code BackgroundPanel} 本身是按 {@code background_alpha}（默认 0.3）绘制的，
     * 两者叠加会让整个界面明显发淡，因此暂时搁置。保留实现供将来参考。</p>
     *
     * <p>把背景层挂到 rootPane 的 layeredPane，并放在比 contentPane 与标题栏都低的层。</p>
     *
     * <p>背景图原本挂在 contentPane 内，而 contentPane 只占标题栏下方的区域，
     * 所以背景图永远到不了标题栏。改挂到 rootPane 后，它的可见范围就是整个窗口，
     * 配合半透明标题栏即可「铺到标题栏、同时保留标题栏」。</p>
     *
     * <p>{@code updateBackground()} 会被 {@code backgroundupdateTimer} 每 100ms 调用一次，
     * 因此这里必须保持幂等。</p>
     */
    private void attachBackgroundToRootPane() {
        if (backgroundPanel == null) {
            return;
        }
        JRootPane rootPane = getRootPane();
        if (rootPane == null) {
            return;
        }
        JLayeredPane rootLayered = rootPane.getLayeredPane();
        if (rootLayered == null || backgroundPanel.getParent() == rootLayered) {
            return;
        }
        // 比 FRAME_CONTENT_LAYER（contentPane 所在层）更低，确保位于内容与标题栏之下
        rootLayered.add(backgroundPanel, JLayeredPane.FRAME_CONTENT_LAYER - 10);
    }

    /**
     * 切换标题栏的半透明状态。
     *
     * <p>{@code FlatClientProperties.TITLE_BAR_BACKGROUND} 是 <b>per-window</b> 的
     * （存在 rootPane 的 client property 上），所以只影响主窗口，其他对话框不受牵连。</p>
     *
     * <p>实测：该属性在原生窗口装饰下同样生效，且带 alpha 的颜色会与标题栏下方的
     * 绘制内容正确混合——这正是背景图能透出来的原因。</p>
     *
     * @param translucent true 显示背景图时用半透明；false 恢复主题默认
     */
    private void applyTranslucentTitleBar(boolean translucent) {
        JRootPane rootPane = getRootPane();
        if (rootPane == null) {
            return;
        }
        if (!translucent) {
            rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_BACKGROUND, null);
            return;
        }
        Color base = UIManager.getColor("TitlePane.background");
        if (base == null) {
            base = UIManager.getColor("Panel.background");
        }
        if (base == null) {
            base = new Color(43, 43, 43);
        }
        rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_BACKGROUND,
                new Color(base.getRed(), base.getGreen(), base.getBlue(),
                        Math.max(0, Math.min(255, TITLE_BAR_ALPHA))));
    }

    private void resetBackground() {
        if (backgroundPanel != null) {
            backgroundPanel.setVisible(false);
        }
        // FIX 刷新界面
        layeredPane.repaint();
    }

    /**
     * 切换「内容延伸到标题栏」模式（FlatLaf 的 fullWindowContent）。
     *
     * <p><b>当前已不再调用。</b>该模式下 FlatLaf 会强制隐藏标题文字——
     * {@code FlatTitlePane.updateVisibility()} 里写死了
     * {@code titleLabel.setVisible(... && !isFullWindowContent)}，
     * 连 {@code TITLE_BAR_SHOW_TITLE} 也覆盖不了，与「保留标题栏」的需求冲突。</p>
     *
     * <p>现在改用「背景层挂到 rootPane + 标题栏半透明」的方案，见
     * {@link #attachBackgroundToRootPane()} 与 {@link #applyTranslucentTitleBar(boolean)}：
     * 标题栏的文字和按钮全部保留，背景图从标题栏后面透出来。</p>
     *
     * <p>保留此方法仅为将来可能回退到该方案时使用。</p>
     *
     * @return 是否发生了状态变化
     */
    private boolean setFullWindowContent(boolean full) {
        JRootPane rootPane = getRootPane();
        if (rootPane == null) {
            return false;
        }
        Object current = rootPane.getClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT);
        if (Boolean.valueOf(full).equals(current)) {
            return false;
        }
        rootPane.putClientProperty(FlatClientProperties.FULL_WINDOW_CONTENT, full);
        // 内容区域高度会变化（多出/少掉标题栏高度），需要重新布局并同步子组件边界
        rootPane.revalidate();
        revalidate();
        repaint();
        SwingUtilities.invokeLater(this::updateChildBounds);
        return true;
    }

    private void initTrayIcon() {
        if (!SystemTray.isSupported()) {
            logger.info("当前系统不支持系统托盘，跳过托盘图标初始化");
            return;
        }

        SystemTray systemTray = SystemTray.getSystemTray();

        // 支持重建窗口：先清理上一轮的托盘图标和菜单（菜单里有注册到全局的监听）
        if (trayIcon != null) {
            systemTray.remove(trayIcon);
            trayIcon = null;
        }
        if (trayMenu != null) {
            trayMenu.dispose();
            trayMenu = null;
        }

        trayIcon = new TrayIcon(IconControl.getImage("download"), StringFormat.translate("common", "app_name"));

        trayIcon.setImageAutoSize(true);
        IconControl.addInDynamicConverter(
                () -> trayIcon.setImage(IconControl.getImage("icon"))
        );

        // TrayIcon#setPopupMenu 只接受 AWT 的 PopupMenu，无法跟随 FlatLaf 主题，
        // 因此这里改用 Swing 的 JPopupMenu，在托盘鼠标事件里手动弹出。
        trayMenu = new TrayMenu(this);
        final TrayMenu menu = trayMenu;
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                menu.showFor(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                menu.showFor(e);
            }
        });

        // 单击/双击托盘图标：显示主窗口
        trayIcon.addActionListener(actionListener);

        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            logger.error("Tray icon added failed", e);
        }
    }

    private static void setMenuItemIcon(JMenuItem item, String iconKey) {
        IconControl.addInDynamicConverter(
                () -> item.setIcon(IconControl.getIcon(iconKey, 16)));
    }

    private void initMenuBar() {
        JPopupMenu popupMenu = new JPopupMenu();

        //窗口
        var windowMenu = new JMenu(StringFormat.translate("download_menu_bar", "frame"));

        TestFunctionControl.run(1002, 1,
                () -> {
                    var alwaysOnTopCheckBox = new JCheckBoxMenuItem(StringFormat.translate("download_menu_bar", "frame.is_always_top"));
                    alwaysOnTopCheckBox.setSelected(this.isAlwaysOnTop());
                    alwaysOnTopCheckBox.addActionListener(e -> this.setAlwaysOnTop(alwaysOnTopCheckBox.isSelected()));
                    windowMenu.add(alwaysOnTopCheckBox);

                    windowMenu.addSeparator();
                }, ()->{});



        var refreshMenuItem = new JMenuItem(StringFormat.translate("download_menu_bar", "refresh"));
        refreshMenuItem.setToolTipText(StringFormat.translate("download_menu_bar", "frame.refresh.tooltip"));
        refreshMenuItem.addActionListener(e -> {
            DataControl.load();
            updateBackground();
            updateChildBounds(); // FIX 使用统一方法
            ThemeChanger.easyChanger();
        });
        setMenuItemIcon(refreshMenuItem, "refresh");
        windowMenu.add(refreshMenuItem);

        TestFunctionControl.run(1000, 1,
                () -> {
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
                    setMenuItemIcon(updateFrameMenuItem, "update");
                    windowMenu.add(updateFrameMenuItem);
                }, ()->{});

        windowMenu.addSeparator();

        var exitMenuItem = new JMenuItem(StringFormat.translate("download_menu_bar", "frame.exit"));
        exitMenuItem.addActionListener(e -> System.exit(0));
        setMenuItemIcon(exitMenuItem, "power_switch");
        windowMenu.add(exitMenuItem);

        popupMenu.add(windowMenu);

        //软件
        var AppMenu = new JMenu(StringFormat.translate("download_menu_bar", "app"));

        var checkUpdateMenuItem = new JMenuItem(StringFormat.translate("check_update"));
        checkUpdateMenuItem.addActionListener(e -> checkUpdate());
        setMenuItemIcon(checkUpdateMenuItem, "update");
        AppMenu.add(checkUpdateMenuItem);

        var testDialogShowMenuItem = new JMenuItem(StringFormat.translate("test"));
        testDialogShowMenuItem.addActionListener(e -> {
            var panel = TestControlDialog.getPanel();
            panel.load();
            FunctionDialog.showDialog(this, StringFormat.translate("test_function_control"), panel.contentPane,
                    result -> {
                        if (result == FunctionDialog.RESULT_SAVE) {
                            panel.onOK();
                            ToastMessage.Utils.createSaveAndApplyNextMsg();
                        }
                    },
                    FunctionDialog.SAVE_CANCEL_BUTTONS, 0,
                    null, FunctionDialog.NORTH_DIRECTION_RIGHT, false, true);

        });
        AppMenu.add(testDialogShowMenuItem);

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
        setMenuItemIcon(DisclaimerMenuItem, "question");
        AppMenu.add(DisclaimerMenuItem);

        var aboutMenuItem = new JMenuItem(StringFormat.translate("app.about"));
        aboutMenuItem.addActionListener(e -> mainTabbedPane.setSelectedIndex(mainTabbedPane.getTabCount() - 1));
        setMenuItemIcon(aboutMenuItem, "about");
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

                                ArrayList<JButton> buttonList = new ArrayList<>();
                                TestFunctionControl.run(1001, 1, ()->{
                                    var translateButton = new JButton(StringFormat.translate("translate"));

                                    TestFunctionControl.run(1001, 2, ()->{
                                        translateButton.addActionListener(_ -> translateUpdateBody(update.body()));
                                    }, () -> {});

                                    buttonList.add(translateButton);
                                }, () -> {});

                                FunctionDialog.showDialog(this, StringFormat.translate("common", "learn"), panel,
                                        _ -> {
                                        },
                                        FunctionDialog.DEFAULT_BUTTONS, 0,
                                        buttonList.toArray(JButton[]::new), FunctionDialog.NORTH_DIRECTION_RIGHT, false, true);
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

    /**
     * 翻译更新日志正文（源语言为简体中文，微软翻译）。
     * 先通过 JOptionPane 让用户选择目标语言（记住上次的选择，默认选中当前界面语言），
     * 翻译完成后用 FunctionDialog 展示结果。
     */
    private void translateUpdateBody(String body) {
        var languages = MicrosoftTranslator.languages();

        // 默认语言：优先上次的选择，否则使用当前界面语言决定的默认目标语言
        Language initial = null;
        Object saved = DataControl.get("translate_target_laug", null);
        if (saved != null) {
            String code = saved.toString();
            for (var l : languages) {
                if (l.code().equalsIgnoreCase(code)) {
                    initial = l;
                    break;
                }
            }
        }
        if (initial == null) initial = MicrosoftTranslator.defaultLanguage();

        Object selected = JOptionPane.showInputDialog(
                this,
                StringFormat.translate("translate.select_target"),
                StringFormat.translate("translate"),
                JOptionPane.QUESTION_MESSAGE, null,
                languages.toArray(), initial);
        if (!(selected instanceof Language target)) return;

        // 记住本次选择
        DataControl.putAndSave("translate_target_laug", target.code());

        // 确保已配置 Azure 翻译密钥/区域（首次使用时填写一次并保存）
        if (!MicrosoftTranslator.hasKey()) {
            if (!configureAzureKey()) return;
        }

        String source = body == null ? "" : body;
        Thread.ofVirtual().start(() -> {
            try {
                String translated = MicrosoftTranslator.translate(source, target.code());
                SwingUtilities.invokeLater(() -> {
                    var panel = new JPanel();
                    panel.add(UITools.createMarkdownPane(translated));

                    FunctionDialog.showDialog(this,
                            StringFormat.translate("translate.result_title") + "（" + target.label() + "）",
                            panel,
                            _ -> {
                            },
                            FunctionDialog.DEFAULT_BUTTONS, 0,
                            null, FunctionDialog.NORTH_DIRECTION_RIGHT, false, true);
                });
            } catch (Exception ex) {
                logger.error("翻译失败", ex);
                ToastMessage.show(this, StringFormat.translate("translate.failed"), ToastMessage.ERROR);
            }
        });
    }

    /**
     * 引导用户填写并保存 Azure Translator 密钥与区域。
     *
     * @return 是否已成功完成配置
     */
    private boolean configureAzureKey() {
        Object key = JOptionPane.showInputDialog(this,
                "尚未配置微软翻译(Azure Translator)密钥。\n请填写你的 Translator 资源的 Subscription Key：",
                StringFormat.translate("translate") + " - 配置",
                JOptionPane.QUESTION_MESSAGE);
        if (key == null) return false;
        String k = key.toString().trim();
        if (k.isEmpty()) {
            ToastMessage.show(this, "密钥不能为空", ToastMessage.WARNING);
            return false;
        }
        DataControl.putAndSave(MicrosoftTranslator.KEY_DATA, k);

        Object region = JOptionPane.showInputDialog(this,
                "请填写该密钥对应的区域（如 eastasia；global 资源可留空）：",
                StringFormat.translate("translate") + " - 配置",
                JOptionPane.QUESTION_MESSAGE, null, null, "eastasia");
        if (region == null) return false;
        DataControl.putAndSave(MicrosoftTranslator.REGION_DATA, region.toString().trim());
        return true;
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
        if (trayIcon != null) {
            trayIcon.displayMessage(StringFormat.translate("common", "app_name"),
                    StringFormat.translate("tray.minimized_to_tray"),
                    TrayIcon.MessageType.INFO);
        }
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

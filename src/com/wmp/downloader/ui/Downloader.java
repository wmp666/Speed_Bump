package com.wmp.downloader.ui;

import com.formdev.flatlaf.util.ColorFunctions;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.wmp.downloader.Run;
import com.wmp.downloader.newArchitecture.ParserTaskInfo;
import com.wmp.downloader.newArchitecture.abstractTask.*;
import com.wmp.downloader.newArchitecture.ui.task.PluginParserGithubDownloadTask;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.EasterEggData;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.file.FileOperation;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.tools.ui.UITools;
import com.wmp.downloader.tools.update.GetUpdateInfo;
import com.wmp.downloader.ui.common.PathSelectionPanel;
import com.wmp.downloader.newArchitecture.ui.task.FFmpegSettings;
import com.wmp.downloader.newArchitecture.ui.createTask.CreateTaskPanel;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXBusyLabel;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
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

    private static final Logger logger = Logger.getLogger(Downloader.class);
    public static Downloader mainFrame;
    public static TrayIcon trayIcon;
    private final List<AbstractTask> taskList = new ArrayList<>();
    private final List<AbstractTask> taskFinalyTipList = new ArrayList<>();
    private final GridBagConstraints gbc = new GridBagConstraints();

    private JPanel UIPanel;
    private JPanel settingsPanel;
    private JTabbedPane mainTabbedPane;
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
    private JPanel TasksPanel;
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
    private JScrollPane downloadSetsScrollPane;
    private JSlider alphaSlider;
    private JCheckBox alibabaFastjsonCheckBox;
    private JCheckBox log4jLog4jCheckBox;
    private JScrollPane aboutScrollPane;
    private JCheckBox isUseHeavyWeightToastCheckBox;
    private JCheckBox isUseHeavyWeightFunctionDialogCheckBox;
    private JScrollPane TasksScrollPane;
    private JTabbedPane tabbedPane2;
    private JButton checkUpdateButton;
    private JButton ProjectLinkButton;
    private JScrollPane personalizedSetsScrollPane;
    private JScrollPane DataControlSetsScrollPane;
    private JCheckBox isStartCheckUpdateCheckBox;
    private JList<PluginParserInfo> PluginParserList;
    private JPanel PluginInfoPanel;
    private JLabel pluginParserIDLabel;
    private JLabel pluginParserAuthorLabel;
    private JLabel pluginParserVersionLabel;
    private JLabel pluginParserStartVersionLabel;
    private JLabel pluginParserLastVersionLabel;
    private JButton pluginParserStatusControlButton;
    private JButton PluginParserUninstallButton;
    private JScrollPane PluginInfoScrollPane;
    private JPanel PluginInfoIntroductionPanel;
    private JScrollPane installPluginInfoScrollPane;
    private JPanel installPluginInfoPanel;
    private JToolBar PluginControlToolBar;
    private JTabbedPane tabbedPane1;
    private JPanel installPluginsPanel;
    private JList<InstallPluginParserInfo> installPluginParserList;
    private JLabel installPluginParserIDLabel;
    private JLabel installPluginParserAuthorLabel;
    private JLabel installPluginParserVersionLabel;
    private JLabel installPluginParserStartVersionLabel;
    private JLabel installPluginParserLastVersionLabel;
    private JPanel installPluginInfoIntroductionPanel;
    private JButton PluginParserInstallButton;
    private JPanel installedPluginsPanel;
    private JButton installPluginParserListRefreshButton;
    private JPanel pluginParserControlPanel;
    private JScrollPane installPluginParserScrollPane;
    private JScrollPane PluginParserScrollPane;
    private JTextField accentColorTextField;
    private JButton accentColorChooseButton;
    private JProgressBar waitProgressBar;
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
    private Timer backgroundupdateTimer = new Timer(100, e -> {
        updateBackground();
        updateChildBounds(); // FIX 使用统一方法
    });
    private final Timer taskListener = new Timer(100, e -> {
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
                if (SystemTray.isSupported()) {
                    trayIcon.displayMessage(null,
                                String.format(StringFormat.translate("task", "task.download_task.success.confirm"), urlDownloadTask.getFileName()),
                                TrayIcon.MessageType.INFO);
                    }
                }
        });
    });
    // FIX 删除了无用的 backgroundTimer 字段

    public Downloader() {

        mainFrame = this;

        taskListener.start();


        this.getRootPane().putClientProperty("JRootPane.fullWindowContent", true);
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

        UITools.setScrollPaneUnOpaque(downloadSetsScrollPane);
        UITools.setScrollPaneUnOpaque(personalizedSetsScrollPane);
        UITools.setScrollPaneUnOpaque(DataControlSetsScrollPane);

        // 使用JLayeredPane包装主界面
        initLayeredPane();

        // 初始化背景相关
        initBackgroundSettings();

        //拓展管理
        {
            var ref = new Object() {
                ChangeListener l = null;
            };
            ref.l = e -> {
                if (pluginParserControlPanel == mainTabbedPane.getSelectedComponent()) {
                    waitProgressBar.setVisible(true);
                    waitProgressBar.setIndeterminate(true);
                    SwingUtilities.invokeLater(() -> {
                        initPluginParserComponents();
                        waitProgressBar.setVisible(false);
                        mainTabbedPane.removeChangeListener(ref.l);
                    });


                }

            };
            mainTabbedPane.addChangeListener(ref.l);
        }

        //任务
        initTaskComponents();

        //设置
        {
            var ref = new Object() {
                ChangeListener l = null;
            };
            ref.l = e -> {
                if (settingsPanel == mainTabbedPane.getSelectedComponent()) {
                    waitProgressBar.setVisible(true);
                    waitProgressBar.setIndeterminate(true);
                    SwingUtilities.invokeLater(() -> {
                        initSettingsComponents();
                        waitProgressBar.setVisible(false);
                        mainTabbedPane.removeChangeListener(ref.l);
                    });
                }

            };
            mainTabbedPane.addChangeListener(ref.l);
        }

        //专项设置
        {
            var ref = new Object() {
                ChangeListener l = null;
            };
            ref.l = e -> {
                if (SpecialSettingsPanel == mainTabbedPane.getSelectedComponent()) {
                    waitProgressBar.setVisible(true);
                    waitProgressBar.setIndeterminate(true);
                    SwingUtilities.invokeLater(() -> {
                        initSpecialSettingsComponents();
                        waitProgressBar.setVisible(false);
                        mainTabbedPane.removeChangeListener(ref.l);
                    });
                }

            };
            mainTabbedPane.addChangeListener(ref.l);
        }

        //关于
        initAboutComponents();
        startClipboardListener();

        pack();
        this.setLocationRelativeTo(null);

        // FIX 确保初始显示时子组件边界正确
        SwingUtilities.invokeLater(this::updateChildBounds);

        backgroundupdateTimer.start();
    }

    private void initPluginParserComponents() {

        UITools.setScrollPaneUnOpaque(installPluginParserScrollPane);
        UITools.setScrollPaneUnOpaque(PluginParserScrollPane);

        initToolBar();

        initInstallPluginParserComponents();

        initInstalledPluginParserComponents();


    }

    private void initInstallPluginParserComponents() {
        installPluginInfoPanel.setVisible(false);

        installPluginParserIDLabel.putClientProperty("FlatLaf.style", "font: $h2.font");
        installPluginParserAuthorLabel.putClientProperty("FlatLaf.style", "font: $h4.font");
        installPluginParserVersionLabel.putClientProperty("FlatLaf.style", "font: $Large.font");
        installPluginParserStartVersionLabel.putClientProperty("FlatLaf.style", "font: $Large.font");
        installPluginParserLastVersionLabel.putClientProperty("FlatLaf.style", "font: $Large.font");

        installPluginParserList.putClientProperty("FlatLaf.style", "font: $h3.font");

        updateInstallPluginParserList();

        ThemeChanger.addInDynamicConverter(() -> installPluginParserList.repaint());

        installPluginParserList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {

            final JPanel panel = new JPanel(new BorderLayout(5, 5)) {
                @Override
                protected void paintComponent(Graphics g) {
                    //super.paintComponent(g);
                    // 根据成员变量绘制背景
                    if (isSelected) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setColor(UIManager.getColor("Component.accentColor"));
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        g2.dispose();
                    } else {
                        Graphics2D g2 = (Graphics2D) g.create();
                        Color base = UIManager.getColor("Panel.background");

                        Color adjusted = DataControl.get("theme_type", "light").equals("dark")
                                ? ColorFunctions.lighten(base, 0.1f)
                                : ColorFunctions.darken(base, 0.1f);
                        Color translucent = new Color(adjusted.getRed(), adjusted.getGreen(), adjusted.getBlue(), 150);


                        g2.setColor(translucent);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        g2.dispose();
                    }

                    // 子组件由 paintChildren 绘制
                }
            };

            final JLabel nameLabel = new JLabel();
            final JLabel otherInfoLabel = new JLabel();

            {
                panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                //panel.setOpaque(false);
                nameLabel.putClientProperty("FlatLaf.style", "font: $h3.font");
                panel.add(nameLabel, BorderLayout.CENTER);
                panel.add(otherInfoLabel, BorderLayout.SOUTH);
            }

            // 更新数据
            nameLabel.setText(value.pluginParserInfo().parser().getID());
            otherInfoLabel.setText(value.pluginParserInfo().version() + " " + value.pluginParserInfo().author());

            //nameLabel.setForeground(UIManager.getColor("Label.foreground"));
            //otherInfoLabel.setForeground(UIManager.getColor("Label.foreground"));

            // 强制重绘面板（因为选中状态变化，需要刷新背景）
            panel.repaint();
            installPluginInfoPanel.repaint();

            return panel;
        });
        installPluginParserList.addListSelectionListener(e -> {
            var installPluginParserInfo = installPluginParserList.getSelectedValue();
            if (installPluginParserInfo == null) return;
            try {
                PluginParserInstallButton.setEnabled(true);
                PluginParserInstallButton.setText(
                        StringFormat.translate("install")
                );

                installPluginInfoPanel.setVisible(true);
                var id = installPluginParserInfo.pluginParserInfo().parser().getID();
                installPluginParserIDLabel.setText(id);
                installPluginParserAuthorLabel.setText(installPluginParserInfo.pluginParserInfo().author());
                installPluginParserVersionLabel.setText(installPluginParserInfo.pluginParserInfo().version());

                //设置开发版本
                var lastVersion = installPluginParserInfo.pluginParserInfo().lastVersion();
                var startVersion = installPluginParserInfo.pluginParserInfo().startVersion();
                installPluginParserStartVersionLabel.setText(startVersion);
                installPluginParserLastVersionLabel.setText(lastVersion);
                //判断是否适合当前程序
                if (!GetUpdateInfo.isVersionInRange(Run.PLUGIN_SUPPORT_VERSION,
                        startVersion, lastVersion)) {
                    installPluginParserStartVersionLabel.setForeground(Color.RED);
                    installPluginParserLastVersionLabel.setForeground(Color.RED);
                }else {
                    installPluginParserStartVersionLabel.setForeground(null);
                    installPluginParserLastVersionLabel.setForeground(null);
                }

                installPluginInfoIntroductionPanel.removeAll();
                installPluginInfoIntroductionPanel.add(UITools.createMarkdownPane(installPluginParserInfo.pluginParserInfo().introduction()), BorderLayout.CENTER);


                var pluginParserList = ParserTaskInfo.getAllPluginParserList();
                var idList = pluginParserList.stream().map(pluginParserInfo -> pluginParserInfo.parser().getID()).toList();
                if (idList.contains(id)) {
                    if (GetUpdateInfo.versionGreaterThan(installPluginParserInfo.pluginParserInfo().version(), pluginParserList.get(idList.indexOf(id)).version())) {
                        PluginParserInstallButton.setText(
                                StringFormat.translate("update")
                        );
                    }else{
                        PluginParserInstallButton.setText(
                                StringFormat.translate("installed")
                        );
                        PluginParserInstallButton.setEnabled(false);
                    }
                }
            } catch (Exception ex) {
                logger.error("安装信息加载过程抛出错误", ex);
            }

        });
        installPluginParserListRefreshButton.addActionListener(e -> updateInstallPluginParserList());
        PluginParserInstallButton.addActionListener(e -> {
            logger.info(installPluginParserList.getSelectedValue().url());
            //创建下载任务


            var info = new PluginParserGithubDownloadTask(() -> {
                ParserTaskInfo.loadParsers();
                updateInstallPluginParserList();
                updateInstalledPluginParserList();
            })
                    .getParserInfo(installPluginParserList.getSelectedValue().url());
            var jsonInfo = info.getLinkedInfoPanel().getJsonInfo();
            jsonInfo.put("savePath", DataControl.getPATPath().getAbsolutePath());
            jsonInfo.put("threadMode", 0);
            jsonInfo.put("threadNum", DataControl.get("ThreadNum", 64));
            jsonInfo.put("linkStyle", 0);
            var task = info.getTask(jsonInfo);
            addDownloadTask(task);
        });
    }

    private void initToolBar() {

        PluginControlToolBar.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JButton importLocalButton = new JButton();
        importLocalButton.setToolTipText(
                StringFormat.translate("plugins.control_tool_bar.import_local")
        );
        IconControl.addInDynamicConverter(() ->
                importLocalButton.setIcon(IconControl.getIcon("import",
                        importLocalButton.getFont().getSize())));
        importLocalButton.addActionListener(e -> {
            var path = DataControl.getPath(Downloader.this, SystemFileChooser.OPEN_DIALOG, SystemFileChooser.FILES_ONLY);
            if (path == null) return;
            if (!path.getName().endsWith(".jar")) {
                ToastMessage.show(StringFormat.translate("plugins.control_tool_bar.import_local.is_not_jar"), ToastMessage.WARNING);
            }else{
                if (FileOperation.copy(path, DataControl.getPATPath())){
                    ParserTaskInfo.loadParsers();
                    updateInstalledPluginParserList();
                    updateInstallPluginParserList();
                }
            }

        });
        PluginControlToolBar.add(importLocalButton);

        JButton refreshButton = new JButton();
        refreshButton.setToolTipText(StringFormat.translate("refresh"));
        IconControl.addInDynamicConverter(() ->
                refreshButton.setIcon(IconControl.getIcon("refresh",
                        refreshButton.getFont().getSize())));
        refreshButton.addActionListener(e -> {
            ParserTaskInfo.loadParsers();
            updateInstallPluginParserList();
            updateInstalledPluginParserList();
        });
        PluginControlToolBar.add(refreshButton);
    }

    private void initInstalledPluginParserComponents() {
        IconControl.addInDynamicConverter(
                () -> installPluginParserListRefreshButton.setIcon(IconControl.getIcon("refresh", installPluginParserListRefreshButton.getFont().getSize()))
        );

        PluginInfoPanel.setVisible(false);

        pluginParserIDLabel.putClientProperty("FlatLaf.style", "font: $h2.font");
        pluginParserAuthorLabel.putClientProperty("FlatLaf.style", "font: $h4.font");
        pluginParserVersionLabel.putClientProperty("FlatLaf.style", "font: $Large.font");
        pluginParserStartVersionLabel.putClientProperty("FlatLaf.style", "font: $Large.font");
        pluginParserLastVersionLabel.putClientProperty("FlatLaf.style", "font: $Large.font");

        PluginParserList.putClientProperty("FlatLaf.style", "font: $h3.font");

        updateInstalledPluginParserList();

        ThemeChanger.addInDynamicConverter(() -> PluginParserList.repaint());

        PluginParserList.setCellRenderer(new ListCellRenderer<>() {


            @Override
            public Component getListCellRendererComponent(JList<? extends PluginParserInfo> list,
                                                          PluginParserInfo value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {

                final JPanel panel = new JPanel(new BorderLayout(5, 5)) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        //super.paintComponent(g);
                        // 根据成员变量绘制背景
                        if (isSelected) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setColor(UIManager.getColor("Component.accentColor"));
                            g2.fillRect(0, 0, getWidth(), getHeight());
                            g2.dispose();
                        } else {
                            Graphics2D g2 = (Graphics2D) g.create();
                            Color base = UIManager.getColor("Panel.background");

                            Color adjusted = DataControl.get("theme_type", "light").equals("dark")
                                    ? ColorFunctions.lighten(base, 0.1f)
                                    : ColorFunctions.darken(base, 0.1f);
                            Color translucent = new Color(adjusted.getRed(), adjusted.getGreen(), adjusted.getBlue(), 150);


                            g2.setColor(translucent);
                            g2.fillRect(0, 0, getWidth(), getHeight());
                            g2.dispose();
                        }

                        // 子组件由 paintChildren 绘制
                    }
                };

                final JLabel nameLabel = new JLabel();
                final JLabel otherInfoLabel = new JLabel();

                {
                    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                    //panel.setOpaque(false);
                    nameLabel.putClientProperty("FlatLaf.style", "font: $h3.font");
                    panel.add(nameLabel, BorderLayout.CENTER);
                    panel.add(otherInfoLabel, BorderLayout.SOUTH);
                }

                // 更新数据
                nameLabel.setText(value.parser().getID());
                otherInfoLabel.setText(value.version() + " " + value.author());

                //nameLabel.setForeground(UIManager.getColor("Label.foreground"));
                //otherInfoLabel.setForeground(UIManager.getColor("Label.foreground"));

                // 强制重绘面板（因为选中状态变化，需要刷新背景）
                panel.repaint();
                PluginInfoPanel.repaint();

                return panel;
            }
        });
        PluginParserList.addListSelectionListener(e -> {
            try {
                var pluginParserInfo = PluginParserList.getSelectedValue();
                PluginInfoPanel.setVisible(true);
                var id = pluginParserInfo.parser().getID();
                pluginParserIDLabel.setText(id);
                pluginParserAuthorLabel.setText(pluginParserInfo.author());
                pluginParserVersionLabel.setText(pluginParserInfo.version());
                pluginParserStartVersionLabel.setText(pluginParserInfo.startVersion());
                pluginParserLastVersionLabel.setText(pluginParserInfo.lastVersion());
                PluginInfoIntroductionPanel.removeAll();
                PluginInfoIntroductionPanel.add(UITools.createMarkdownPane(pluginParserInfo.introduction()), BorderLayout.CENTER);

                if (pluginParserInfo.isAppPlugin()) {
                    pluginParserStatusControlButton.setEnabled(false);
                    PluginParserUninstallButton.setEnabled(false);
                }else{
                    pluginParserStatusControlButton.setEnabled(true);
                    PluginParserUninstallButton.setEnabled(true);
                    //处理管理按钮
                    pluginParserStatusControlButton.setText(
                            StringFormat.translate(ParserTaskInfo.isEnable(id)?"disable":"enable")
                    );
                }


            } catch (Exception ex) {
                logger.error("已安装的解析器加载失败", ex);
            }
        });
        PluginParserUninstallButton.addActionListener(e -> {
            var oldIndex = PluginParserList.getSelectedIndex();
            var id = pluginParserIDLabel.getText();
            ParserTaskInfo.setDeleteParser(id);

            ToastMessage.show(StringFormat.translate("plugins.delete_plugin.tip"), ToastMessage.INFO);

            ParserTaskInfo.loadParsers();
            updateInstalledPluginParserList();

            PluginParserList.setSelectedIndex(oldIndex != 0?oldIndex - 1:0);
        });
        pluginParserStatusControlButton.addActionListener(e -> {
            var id = pluginParserIDLabel.getText();
            if (ParserTaskInfo.isEnable(id)) ParserTaskInfo.setDisableParser(id);
            else ParserTaskInfo.removeDisableParser(id);
            pluginParserStatusControlButton.setText(StringFormat.translate(ParserTaskInfo.isEnable(id)?"disable":"enable"));
        });
    }

    private void updateInstalledPluginParserList() {
        var pluginParserArrayList = ParserTaskInfo.getAllPluginParserList();
        PluginParserList.setListData(pluginParserArrayList.toArray(PluginParserInfo[]::new));
    }

    private void updateInstallPluginParserList() {
        var installPluginParserArrayList = getInstallPluginParserInfoList();
        installPluginParserList.setListData(installPluginParserArrayList.toArray(InstallPluginParserInfo[]::new));
    }

    private List<InstallPluginParserInfo> getInstallPluginParserInfoList(){
        return ParserTaskInfo.getInstallPluginParserInfoList();
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
        /*AbstractSpecialSettingsPage[] basicSpecialSettingsr = new AbstractSpecialSettingsPage[]{
                new BiliSettings(), new FFmpegSettings(), new GithubAccelerateSettings(), new GopeedSettings()
        };*/

        var parserList = ParserTaskInfo.getEnablePluginParserList();
        ArrayList<AbstractSpecialSettingsPage> basicSpecialSettings =
                null;
        try {
            var list = parserList.stream()
                    .map(parser -> {
                        try {
                            return parser.getSettingsPage();
                        } catch (Exception e) {
                            ToastMessage.show(e.getMessage(), ToastMessage.ERROR);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .toList();
            basicSpecialSettings = new ArrayList<>(list);
        } catch (Exception e) {
            logger.error("发生错误", e);
            ToastMessage.show(e.getMessage(), ToastMessage.ERROR);
        }
        if (basicSpecialSettings != null) {
            basicSpecialSettings.add(new FFmpegSettings());
        }
        for (var specialSettings : basicSpecialSettings) {
            var jScrollPane1 = new JScrollPane(specialSettings);
            UITools.setScrollPaneUnOpaque(jScrollPane1);
            SpecialSettingsTabbedPane.addTab(specialSettings.getSettingsName(), jScrollPane1);
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
        JMenuBar menuBar = new JMenuBar();

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

        var updateFrameMenuItem = new JMenuItem(StringFormat.translate("download_menu_bar", "frame.update_frame"));
        updateFrameMenuItem.addActionListener(e -> {
            try {
                if (JOptionPane.showConfirmDialog(this, StringFormat.translate("download_menu_bar", "frame.update_frame.tip"), StringFormat.translate("common", "warn"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                    if (clipboardTimer != null) {
                        clipboardTimer.stop();
                    }
                    this.dispose();
                    DataControl.load();
                    ThemeChanger.easyChanger();

                    new Downloader().setVisible(true);
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

        menuBar.add(windowMenu);

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

        menuBar.add(AppMenu);

        this.setJMenuBar(menuBar);
    }

    private void createUIComponents() {


        TasksPanel = new JPanel(new GridBagLayout());
        TasksPanel.setOpaque(false);

        TasksScrollPane = new JScrollPane(TasksPanel);
        TasksScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动
        TasksScrollPane.getViewport().setLayout(new ViewportLayout()); // 默认布局，会拉伸组件
        UITools.setScrollPaneUnOpaque(TasksScrollPane);

        installPluginInfoScrollPane = UITools.setScrollPaneUnOpaque(new JScrollPane(installPluginInfoPanel));
        PluginInfoScrollPane = UITools.setScrollPaneUnOpaque(new JScrollPane(PluginInfoPanel));

        backgroundSelectionPanel = new PathSelectionPanel(StringFormat.translate("settings", "settings.personalized.background_path"), new File(DataControl.get("background", "")), SystemFileChooser.FILES_ONLY);
        pathSelectionPanel = new PathSelectionPanel(StringFormat.translate("common", "save_path"), DataControl.getDownloadFilePath());
        tempPathSelectionPanel = new PathSelectionPanel(StringFormat.translate("common", "temp_path"), new File(DataControl.get("TempFilePath", DataControl.getDefaultTempPath().getAbsolutePath())));

        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(DataControl.get("FontSize", 12).intValue(), 1, Integer.MAX_VALUE, 1));
    }

    private void initAboutComponents() {
        nameLabel.setText(StringFormat.translate("common", "app_name") + " V" + DataControl.get("version", "0.0.0"));
        nameLabel.putClientProperty("FlatLaf.style", "font: bold $h0.font");
        checkUpdateButton.putClientProperty("FlatLaf.style", "font: bold $h3.font");
        ProjectLinkButton.putClientProperty("FlatLaf.style", "font: bold $h3.font");
        IconControl.addInDynamicConverter(
                () -> nameLabel.setIcon(IconControl.getIcon("icon", nameLabel.getFont().getSize())),
                () -> checkUpdateButton.setIcon(IconControl.getIcon("update", checkUpdateButton.getFont().getSize())),
                () -> ProjectLinkButton.setIcon(IconControl.getIcon("link", ProjectLinkButton.getFont().getSize()))
        );

        authorCheckBox.addActionListener(_ -> {
            if (!authorCheckBox.isSelected()) {
                var panel = new JPanel(new BorderLayout());
                var textArea = new JTextArea("你真的要这么做吗!\n这样做真的很危险!\n不要继续呀!");
                panel.add(textArea);

            }
        });
        FlatLafCheckBox.addActionListener(_ -> {
            EasterEggData.canUseFlatLaf = FlatLafCheckBox.isSelected();
            ThemeChanger.easyChanger();
        });
        IconPackCheckBox.addActionListener(_ -> {
            EasterEggData.canUseIcon = IconPackCheckBox.isSelected();
            IconControl.runDynamicConverters();
        });

        checkUpdateButton.addActionListener(_ -> checkUpdate());

        ProjectLinkButton.addActionListener(_ -> {
            try {
                Desktop.getDesktop().browse(URI.create("https://github.com/wmp666/Speed_Bump"));
            } catch (Exception ex) {
                ToastMessage.show(StringFormat.translate("open_link.error"), ToastMessage.ERROR);
                logger.error("网站打开失败", ex);
            }
        });

        aboutScrollPane.getViewport().setOpaque(false);
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

    private void addDownloadTask(AbstractTask... tasks) {
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

    private void addDownloadTask(CreateTaskPanel createTaskPanel) {
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

    private void initSettingsComponents() {
        ThemeChanger.addInDynamicConverter(
                this::updateDefaultButton
        );

        downloadSetsScrollPane.getVerticalScrollBar().setUnitIncrement(10);

        isUseSSLCheckBox.setSelected(DataControl.get("isUseSSL", false));
        isUseClipBoardListenerCheckBox.setSelected(DataControl.get("isUseClipBoardListener", false));
        ThreadNumSlider.setValue(DataControl.get("ThreadNum", 64));
        ThreadNumLabel.setText(String.valueOf(ThreadNumSlider.getValue()));
        alphaSlider.setValue((int) (DataControl.get("background_alpha", new BigDecimal("0.3")).floatValue() * 100));
        isStartCheckUpdateCheckBox.setSelected(DataControl.get("is_start_check_update", true));
        accentColorTextField.setText(DataControl.get("accent_color", "29a5e3"));

        BackgroundModeComboBox.addItem("None");
        BackgroundModeComboBox.addItem("Image");

        BackgroundModeComboBox.setSelectedItem(DataControl.get("background_mode", "None"));

        backgroundSelectionPanel.setPath(DataControl.get("background", ""));
        if (Objects.equals(BackgroundModeComboBox.getSelectedItem(), "Image")) {
            backgroundSelectionPanel.setVisible(true);
        } else backgroundSelectionPanel.setVisible(false);

        {
            String[] laugs = new String[]{
                    "简体中文(zh_cn)", "English(en_us)", "日本語(ja_JP)", "Русский язык(ru_RU)",
                    "繁體中文|臺灣(zh_TW)", "繁體中文|香港地區(zh_HK)"
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

        themeComboBox.addItem("System Theme Style");
        themeComboBox.addItem("Mac Dark");
        themeComboBox.addItem("Mac Light");
        themeComboBox.addItem("Dark");
        themeComboBox.addItem("Light");
        themeComboBox.addItem("Darcula");
        themeComboBox.addItem("IntelliJ");
        themeComboBox.addItem("System");
        themeComboBox.addItem("Windows Classic");
        themeComboBox.addItem("Metal");

        themeComboBox.setSelectedItem(DataControl.get("theme", "System Theme Style"));

        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String font : fonts) {
            FontListComboBox.addItem(font);
        }
        FontListComboBox.setSelectedItem(DataControl.get("Font", "Microsoft YaHei"));

        isUseHeavyWeightToastCheckBox.setSelected(DataControl.get("is_use_heavy_weight.toast", false));
        isUseHeavyWeightFunctionDialogCheckBox.setSelected(DataControl.get("is_use_heavy_weight.function_dialog", false));

        //添加图标
        IconControl.addInDynamicConverter(
                () -> dataPathButton.setIcon(IconControl.getIcon("folder", dataPathButton.getFont().getSize())),
                () -> deleteTempFolderDataButton.setIcon(IconControl.getIcon("trash", deleteTempFolderDataButton.getFont().getSize())),
                () -> accentColorChooseButton.setIcon(IconControl.getIcon("eyedropper", accentColorChooseButton.getFont().getSize()))
        );
        IconControl.addInDynamicConverter(
                () -> refreshButton.setIcon(IconControl.getIcon("refresh", refreshButton.getFont().getSize())),
                () -> saveButton.setIcon(IconControl.getIcon("save", saveButton.getFont().getSize()))
        );

        //添加监听
        mainTabbedPane.addChangeListener(e -> updateDefaultButton());
        ThreadNumSlider.addChangeListener(e -> {
            ThreadNumLabel.setText(String.valueOf(ThreadNumSlider.getValue()));
            ThreadNumLabel.setSize(ThreadNumLabel.getPreferredSize());
        });
        //动态保存
        BackgroundModeComboBox.addItemListener(e -> {
            DataControl.putAndSave("background_mode", e.getItem().toString());
            if (e.getItem().equals("Image")) {
                backgroundSelectionPanel.setVisible(true);
            } else {
                backgroundSelectionPanel.setVisible(false);
            }
        });
        backgroundSelectionPanel.setPathChangeListener(path -> {
            DataControl.putAndSave("background", path);
        });
        alphaSlider.addChangeListener(e -> {
            DataControl.putAndSave("background_alpha", (float) alphaSlider.getValue() / 100.0f);
        });


        refreshButton.addActionListener(e -> {
            DataControl.load();
            isUseSSLCheckBox.setSelected(DataControl.get("isUseSSL", false));
            isUseClipBoardListenerCheckBox.setSelected(DataControl.get("isUseClipBoardListener", false));
            isUseHeavyWeightToastCheckBox.setSelected(DataControl.get("is_use_heavy_weight.toast", false));
            isUseHeavyWeightFunctionDialogCheckBox.setSelected(DataControl.get("is_use_heavy_weight.function_dialog", false));
            isStartCheckUpdateCheckBox.setSelected(DataControl.get("is_start_check_update", true));

            ThreadNumSlider.setValue(DataControl.get("ThreadNum", 64));
            ThreadNumLabel.setText(String.valueOf(ThreadNumSlider.getValue()));
            pathSelectionPanel.setPath(DataControl.getDownloadFilePath().getAbsolutePath());
            tempPathSelectionPanel.setPath(DataControl.get("TempFilePath", DataControl.getDataPath().getAbsolutePath()));
            FontListComboBox.setSelectedItem(DataControl.get("Font", "Microsoft YaHei"));
            fontSizeSpinner.setValue(DataControl.get("FontSize", 12));
            themeComboBox.setSelectedItem(DataControl.get("theme", "System Theme Style"));

            accentColorTextField.setText(DataControl.get("accent_color", "29a5e3"));

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
        laugComboBox.addItemListener(e -> {
            var lauguage = e.getItem().toString();
            Matcher matcher = Pattern.compile("\\((.+_.+)\\)").matcher(lauguage);
            if (matcher.find()) {
                lauguage = matcher.group(1);
            }
            DataControl.putAndSave("laug", lauguage);
        });

        accentColorChooseButton.addActionListener(e -> {
            Color color = null;
            try {
                color = Color.decode("#" + accentColorTextField.getText());
            } catch (NumberFormatException ex) {
                color = new Color(0x29a5e3);
            }
            var result = JColorChooser.showDialog(this, StringFormat.translate("settings.personalized.accent_color"), color);
/*
            int rgb = 0xFF0000; // 注意：如果 int 包含 Alpha，需先屏蔽高位
            int rgbOnly = rgb & 0x00FFFFFF;
            String hex = String.format("#%06X", rgbOnly); // 输出 "#FF0000"*/
            accentColorTextField.setText(String.format("%06X", result.getRGB() & 0x00FFFFFF));
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
            DataControl.put("is_use_heavy_weight.toast", isUseHeavyWeightToastCheckBox.isSelected());
            DataControl.put("is_use_heavy_weight.function_dialog", isUseHeavyWeightFunctionDialogCheckBox.isSelected());
            DataControl.put("is_start_check_update", isStartCheckUpdateCheckBox.isSelected());
            DataControl.put("accent_color", accentColorTextField.getText());

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

    private void showLinkDetectedDialog(String url) {
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

    private void updateDefaultButton() {
        int selectedIndex = mainTabbedPane.getSelectedIndex();
        if (selectedIndex == mainTabbedPane.indexOfComponent(downloaderPanel)) {
            getRootPane().setDefaultButton(createTaskButton);
        } else if (selectedIndex == mainTabbedPane.indexOfComponent(settingsPanel)) {
            getRootPane().setDefaultButton(saveButton);
        } else if (selectedIndex == mainTabbedPane.indexOfComponent(SpecialSettingsPanel)) {
            if (SpecialSettingsTabbedPane.getSelectedComponent() instanceof AbstractSpecialSettingsPage specialSettingsPanel) {
                specialSettingsPanel.setDefaultButton();
            }
        } else if (selectedIndex == mainTabbedPane.indexOfComponent(aboutPanel)) {
            getRootPane().setDefaultButton(checkUpdateButton);
        } else getRootPane().setDefaultButton(null);
    }

    @Override
    public void windowOpened(WindowEvent e) {
        if (DataControl.get("is_start_check_update", true)) {
            checkUpdate();
        }
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
package com.wmp.downloader.ui;

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
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Downloader extends JFrame implements WindowListener {

    public static final TrayIcon trayIcon = new TrayIcon(IconControl.getImage("download", 256), "Speed Bump");
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
                    JOptionPane.showMessageDialog(this, "下载[" + urlDownloadTask.getName() + "]完成", "下载完成", JOptionPane.INFORMATION_MESSAGE);
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

    public Downloader() {
        taskListener.start();

        this.setTitle("减速带 V" + DataControl.get("version", "0.0.1"));
        this.setContentPane(UIPanel);
        this.setMinimumSize(new Dimension(800, 550));

        this.pack();
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.setLocationRelativeTo(null);
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
    }

    private void initSpecialSettingsComponents() {
        var biliSettings = new BiliSettings();
        SpecialSettingsTabbedPane.addTab(biliSettings.getSettingsName(), biliSettings.getSettings());
    }

    private void initTrayIcon() {
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
        trayIconMenu.add(showMenuItem);

        var exitMenuItem = new MenuItem("exit");
        exitMenuItem.addActionListener(e -> System.exit(0));
        trayIconMenu.add(exitMenuItem);

        trayIcon.setPopupMenu(trayIconMenu);

        trayIcon.addActionListener(e -> {
            this.setVisible(true);
            this.setState(JFrame.NORMAL);
        });

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

        var windowMenu = new JMenu("窗口");

        var alwaysOnTopCheckBox = new JCheckBoxMenuItem("置顶状态");
        alwaysOnTopCheckBox.addActionListener(e -> this.setAlwaysOnTop(alwaysOnTopCheckBox.isSelected()));

        windowMenu.add(alwaysOnTopCheckBox);

        windowMenu.addSeparator();

        var exitMenuItem = new JMenuItem("退出程序");
        exitMenuItem.addActionListener(e -> System.exit(0));
        windowMenu.add(exitMenuItem);

        menuBar.add(windowMenu);
        this.setJMenuBar(menuBar);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here


        pathSelectionPanel = new PathSelectionPanel("保存位置：", DataControl.getDownloadFilePath());
        tempPathSelectionPanel = new PathSelectionPanel("缓存数据位置：", new File(DataControl.get("TempFilePath", DataControl.getDefaultTempPath().getAbsolutePath())));

        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(DataControl.get("FontSize", 12).intValue(), 1, Integer.MAX_VALUE, 1));

    }

    private void initAboutComponents() {
        nameLabel.setText("减速带 V" + DataControl.get("version", "0.0.0"));
        nameLabel.putClientProperty("FlatLaf.style", "font: bold $h0.font");
        IconControl.addInDynamicConverter(
                () -> nameLabel.setIcon(IconControl.getIcon("icon", nameLabel.getFont().getSize()))
        );

        authorCheckBox.addActionListener(e -> {
            if (!authorCheckBox.isSelected()) {
                var panel = new JPanel(new BorderLayout());
                var textArea = new JTextArea("你真的要这么做吗!\n这样做真的很危险!\n不要继续呀!");
                panel.add(textArea);


                var learnMoreButton = new JButton("了解更多");
                learnMoreButton.addActionListener(_ -> {

                    JOptionPane.showMessageDialog(this, "作者/程序是软件的核心部分,去除会导致异常", "了解更多", JOptionPane.WARNING_MESSAGE);
                });

                FunctionDialog.showDialog(this, "警告", panel,
                        _ -> {
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                            JOptionPane.showMessageDialog(this, "错误", "发生错误!软件开始修复修复问题...", JOptionPane.ERROR_MESSAGE);
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
            //创建下载任务

            var createTaskPanel = new CreateTaskPanel();
            var mainPanel = createTaskPanel.MainPanel;
            var learnMoreButton = new JButton("了解更多");
            learnMoreButton.addActionListener(_ -> {
                var panel = new JPanel();
                var textArea = new JTextArea("""
                        支持：哔哩哔哩，HTTP
                        由于还处于开发阶段，哔哩哔哩链接解析出的视频在使用单线程下载时，似乎不会自行暂停，同时不能显示速度
                        
                        """);
                panel.add(textArea);


                FunctionDialog.showDialog(this, "了解更多", panel,
                        _ -> {},
                        FunctionDialog.DEFAULT_BUTTONS, 0,
                        null, FunctionDialog.NORTH_DIRECTION_RIGHT);
            });

            FunctionDialog.showDialog(this, "创建下载任务", mainPanel,
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
                                    name = "未设置名称的文件";
                                }
                                TasksPanel.addTab(name, taskPanel);
                                TasksPanel.revalidate();
                                TasksPanel.repaint();
                            });
                        }
                        this.pack();
                    }
                    , FunctionDialog.OK_CANCEL_BUTTONS, 0,
                    new JButton[]{learnMoreButton}, FunctionDialog.NORTH_DIRECTION_RIGHT);
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

    private void initSettingsComponents() {


        //初始化组件数据
        ThemeChanger.addInDynamicConverter(
                this::updateDefaultButton
        );


        isUseSSLCheckBox.setSelected(DataControl.get("isUseSSL", false));
        ThreadNumSlider.setValue(DataControl.get("ThreadNum", 64));
        ThreadNumLabel.setText(String.valueOf(ThreadNumSlider.getValue()));

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
            DataControl.put("theme", themeStr);
            DataControl.save();
            ThemeChanger.easyChanger();
        });
        FontListComboBox.addActionListener(e -> {
            var fontName = FontListComboBox.getSelectedItem().toString();
            DataControl.put("Font", fontName);
            DataControl.save();
            ThemeChanger.easyChanger();
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
            try {
                //删除整个文件夹里的内容
                Files.walk(Paths.get(tempPath.toURI()))
                        .sorted((o1, o2) -> -o1.compareTo(o2))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ex) {
                                logger.error("删除失败", ex);
                            }
                        });
                JOptionPane.showMessageDialog(this, "删除成功");
            } catch (Exception ex) {
                logger.error("删除失败", ex);
            }
        });

        saveButton.addActionListener(e -> {
            DataControl.put("isUseSSL", isUseSSLCheckBox.isSelected());
            DataControl.put("ThreadNum", ThreadNumSlider.getValue());
            DataControl.put("DownloadFilePath", pathSelectionPanel.getPath());
            DataControl.put("TempFilePath", tempPathSelectionPanel.getPath());
            DataControl.put("FontSize", fontSizeSpinner.getValue());

            DataControl.save();
            DataControl.load();
            JOptionPane.showMessageDialog(this, "保存成功并刷新");
            ThemeChanger.easyChanger();
        });


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

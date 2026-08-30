package com.wmp.downloader.newArchitecture.ui.mainFrame.mainPanels;

import com.formdev.flatlaf.util.ColorFunctions;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.wmp.downloader.Run;
import com.wmp.downloader.newArchitecture.ParserTaskInfo;
import com.wmp.downloader.newArchitecture.abstractTask.InstallPluginParserInfo;
import com.wmp.downloader.newArchitecture.abstractTask.PluginParserInfo;
import com.wmp.downloader.newArchitecture.ui.task.PluginParserGithubDownloadTask;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.file.FileOperation;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.tools.ui.UITools;
import com.wmp.downloader.tools.update.GetUpdateInfo;
import com.wmp.downloader.ui.Downloader;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PluginParserPanel {

    private static final Logger logger = Logger.getLogger(PluginParserPanel.class);

    public JPanel pluginParserControlPanel;
    private JTabbedPane tabbedPane1;
    private JPanel installedPluginsPanel;
    private JScrollPane PluginInfoScrollPane;
    private JPanel PluginInfoPanel;
    private JLabel pluginParserIDLabel;
    private JLabel pluginParserAuthorLabel;
    private JLabel pluginParserVersionLabel;
    private JLabel pluginParserStartVersionLabel;
    private JLabel pluginParserLastVersionLabel;
    private JButton pluginParserStatusControlButton;
    private JButton PluginParserUninstallButton;
    private JPanel PluginInfoIntroductionPanel;
    private JScrollPane PluginParserScrollPane;
    private JList<PluginParserInfo> PluginParserList;
    private JPanel installPluginsPanel;
    private JScrollPane installPluginInfoScrollPane;
    private JPanel installPluginInfoPanel;
    private JLabel installPluginParserIDLabel;
    private JLabel installPluginParserAuthorLabel;
    private JLabel installPluginParserVersionLabel;
    private JLabel installPluginParserStartVersionLabel;
    private JLabel installPluginParserLastVersionLabel;
    private JPanel installPluginInfoIntroductionPanel;
    private JButton PluginParserInstallButton;
    private JButton installPluginParserListRefreshButton;
    private JScrollPane installPluginParserScrollPane;
    private JList<InstallPluginParserInfo> installPluginParserList;
    private JToolBar PluginControlToolBar;
    private JPanel installPluginListPanel;
    private JProgressBar installPluginParserListProgressBar;

    private final Downloader downloader;

    public PluginParserPanel(Downloader downloader) {
        this.downloader = downloader;
    }

    private void createUIComponents() {
        installPluginInfoScrollPane = UITools.setScrollPaneUnOpaque(new JScrollPane(installPluginInfoPanel));
        PluginInfoScrollPane = UITools.setScrollPaneUnOpaque(new JScrollPane(PluginInfoPanel));
    }

    public void initPluginParserComponents() {
        UITools.setScrollPaneUnOpaque(installPluginParserScrollPane);
        UITools.setScrollPaneUnOpaque(PluginParserScrollPane);

        initToolBar();

        initInstalledPluginParserComponents();

        initInstallPluginParserComponents();
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
                updateInstalledPluginParserList();
                updateInstallPluginParserList();

            })
                    .getParserInfo(installPluginParserList.getSelectedValue().url());
            var jsonInfo = info.getLinkedInfoPanel().getJsonInfo();
            jsonInfo.put("savePath", DataControl.getPATPath().getAbsolutePath());
            jsonInfo.put("threadMode", 0);
            jsonInfo.put("threadNum", DataControl.get("ThreadNum", 64));
            jsonInfo.put("linkStyle", 0);
            var task = info.getTask(jsonInfo);
            downloader.addDownloadTask(task);
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
            var path = DataControl.getPath(downloader, SystemFileChooser.OPEN_DIALOG, SystemFileChooser.FILES_ONLY);
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
            updateInstalledPluginParserList();
            updateInstallPluginParserList();

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
        //受网络影响，将加载安装列表数据的过程放入独立的虚拟线程
        installPluginParserListProgressBar.setVisible(true);
        installPluginParserListProgressBar.setIndeterminate(true);
        Thread.ofVirtual().start(() -> {
            try {
                List<InstallPluginParserInfo> installPluginParserArrayList = getInstallPluginParserInfoList();
                SwingUtilities.invokeLater(() -> {
                    installPluginParserList.setListData(installPluginParserArrayList.toArray(InstallPluginParserInfo[]::new));
                    installPluginParserListProgressBar.setVisible(false);
                });
            } catch (Exception ex) {
                logger.error("安装插件列表加载失败", ex);
                SwingUtilities.invokeLater(() -> installPluginParserListProgressBar.setVisible(false));
            }
        });
    }

    private List<InstallPluginParserInfo> getInstallPluginParserInfoList(){
        return ParserTaskInfo.getInstallPluginParserInfoList();
    }
}

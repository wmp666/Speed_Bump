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
import com.wmp.downloader.tools.ui.DropOverlayPanel;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.tools.ui.UITools;
import com.wmp.downloader.tools.update.GetUpdateInfo;
import com.wmp.downloader.ui.Downloader;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.io.File;
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

    /**
     * 叠层容器，把页面内容与拖拽遮罩叠放在一起
     */
    private JPanel pluginStackPanel;
    /**
     * 拖入文件时显示的半透明遮罩
     */
    private DropOverlayPanel dragOverlayPanel;

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

        //重组面板结构，使其支持拖入拓展并显示遮罩（需在工具栏初始化之后）
        initDragAndDropComponents();

        initInstalledPluginParserComponents();

        initInstallPluginParserComponents();
    }

    /**
     * 组装叠层结构并为整个拓展页注册拖放支持。
     *
     * <p>结构为：pluginParserControlPanel(BorderLayout) → pluginStackPanel(叠层) →
     * [页面内容, 半透明遮罩]，遮罩的 z-order 为 0，绘制在内容之上。</p>
     */
    private void initDragAndDropComponents() {
        dragOverlayPanel = new DropOverlayPanel(
                StringFormat.translate("plugins.drag_drop.title"),
                StringFormat.translate("plugins.drag_drop.tip"),
                "import"
        );
        dragOverlayPanel.setVisible(false);

        //页面内容：工具栏在上，标签页填满剩余区域
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.add(PluginControlToolBar, BorderLayout.NORTH);
        contentPanel.add(tabbedPane1, BorderLayout.CENTER);

        //所有子组件都铺满整个容器，从而形成叠层效果
        pluginStackPanel = new JPanel() {
            @Override
            public void doLayout() {
                for (Component component : getComponents()) {
                    component.setBounds(0, 0, getWidth(), getHeight());
                }
            }

            @Override
            public Dimension getPreferredSize() {
                //尺寸只由页面内容决定，遮罩不参与计算
                return getComponentCount() > 0
                        ? getComponent(0).getPreferredSize()
                        : super.getPreferredSize();
            }
        };
        pluginStackPanel.setOpaque(false);
        pluginStackPanel.add(contentPanel);
        pluginStackPanel.add(dragOverlayPanel);
        //z-order 为 0 表示绘制在最上层
        pluginStackPanel.setComponentZOrder(dragOverlayPanel, 0);

        pluginParserControlPanel.removeAll();
        pluginParserControlPanel.add(pluginStackPanel, BorderLayout.CENTER);

        ThemeChanger.addInDynamicConverter(pluginStackPanel::repaint);

        new DropTarget(pluginParserControlPanel, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                updateDragState(dtde);
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                updateDragState(dtde);
            }

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {
                updateDragState(dtde);
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                hideDragOverlay();
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                handleDrop(dtde);
            }
        }, true);
    }

    /**
     * 根据当前拖入的内容刷新遮罩显示状态
     */
    private void updateDragState(DropTargetDragEvent dtde) {
        if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            dtde.rejectDrag();
            hideDragOverlay();
            return;
        }

        dtde.acceptDrag(DnDConstants.ACTION_COPY);

        Boolean hasJar = containsJarFile(dtde.getTransferable());
        if (hasJar != null && !hasJar) {
            //拖入的都不是 jar，给出警示提示，但仍允许放下以便统一提示用户
            dragOverlayPanel.setState(false,
                    StringFormat.translate("plugins.drag_drop.title"),
                    StringFormat.translate("plugins.control_tool_bar.import_local.is_not_jar"));
        } else {
            dragOverlayPanel.setState(true,
                    StringFormat.translate("plugins.drag_drop.title"),
                    StringFormat.translate("plugins.drag_drop.tip"));
        }

        showDragOverlay();
    }

    private void showDragOverlay() {
        if (dragOverlayPanel == null || dragOverlayPanel.isVisible()) return;
        dragOverlayPanel.setVisible(true);
        pluginStackPanel.repaint();
    }

    private void hideDragOverlay() {
        if (dragOverlayPanel == null || !dragOverlayPanel.isVisible()) return;
        dragOverlayPanel.setVisible(false);
        pluginStackPanel.repaint();
    }

    private void handleDrop(DropTargetDropEvent dtde) {
        hideDragOverlay();

        if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            dtde.rejectDrop();
            return;
        }

        dtde.acceptDrop(DnDConstants.ACTION_COPY);
        try {
            @SuppressWarnings("unchecked")
            List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            int installCount = importLocalParserFiles(files);
            dtde.dropComplete(installCount > 0);

            if (installCount > 0) {
                ParserTaskInfo.loadParsers();
                updateInstalledPluginParserList();
                updateInstallPluginParserList();
                ToastMessage.show(
                        String.format(StringFormat.translate("plugins.drag_drop.install_success"), installCount),
                        ToastMessage.SUCCESS
                );
            }
        } catch (Exception ex) {
            logger.error("拖入安装拓展失败", ex);
            dtde.dropComplete(false);
        }
    }

    /**
     * 把拖入的 jar 复制到拓展目录
     *
     * @return 成功导入的数量
     */
    private int importLocalParserFiles(List<File> files) {
        if (files == null || files.isEmpty()) return 0;

        var parserPath = DataControl.getPATPath();
        int installCount = 0;
        boolean hasNotJar = false;

        for (File file : files) {
            if (file == null) continue;
            if (!file.isFile() || !file.getName().toLowerCase().endsWith(".jar")) {
                hasNotJar = true;
                continue;
            }
            if (FileOperation.copy(file, parserPath)) installCount++;
        }

        if (hasNotJar) {
            ToastMessage.show(StringFormat.translate("plugins.control_tool_bar.import_local.is_not_jar"), ToastMessage.WARNING);
        }

        return installCount;
    }

    /**
     * 判断拖入的文件中是否包含 jar
     *
     * @return 无法读取拖入内容时返回 null
     */
    private Boolean containsJarFile(Transferable transferable) {
        try {
            Object data = transferable.getTransferData(DataFlavor.javaFileListFlavor);
            if (data instanceof List<?> list) {
                for (Object element : list) {
                    if (element instanceof File file && file.getName().toLowerCase().endsWith(".jar")) {
                        return true;
                    }
                }
                return false;
            }
        } catch (Exception ex) {
            logger.debug("读取拖入的文件列表失败", ex);
        }
        return null;
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

        PluginParserList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {

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

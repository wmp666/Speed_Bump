package com.wmp.downloader.ui.task;

import com.formdev.flatlaf.util.ColorFunctions;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.ui.*;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;

public abstract class DownloadTask extends JPanel {
    private static final Logger logger = Logger.getLogger(DownloadTask.class);
    protected JPanel mainPanel;
    protected JLabel iconPanel;
    protected JLabel nameLabel;
    protected JButton exitButton;
    protected JButton openButton;
    protected JButton downloadControlButton;
    protected JButton openInFolderButton;
    protected JPanel infoPanel;
    protected JLabel infoLabel;
    protected JScrollPane ProgressBarsScrollPane;
    protected JPanel ProgressBarsPanel;
    private String originalFileName;
    protected String fileName;
    protected File savePath;
    protected boolean isStart = false;
    protected boolean isFinally = false;
    protected boolean isCanExit = false;
    private DynamicConverterTask[] IconDynamicConverterTasks = new DynamicConverterTask[0];
    private DynamicConverterTask[] ThemeDynamicConverterTasks = new DynamicConverterTask[0];

    protected int startCount = 0;

    public DownloadTask(String fileName, File savePath) {
        this.fileName = StringFormat.sanitizeName(fileName);

        this.savePath = StringFormat.sanitizeFile(savePath);
        setName(fileName);

        this.setLayout(new BorderLayout());
        this.setOpaque(false);

        this.add(mainPanel);

        downloadControlButton.setToolTipText(StringFormat.translate("task", "task.download_task.download_control.start"));
        ProgressBarsScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        ProgressBarsScrollPane.setBorder(null);
        ProgressBarsScrollPane.getViewport().setOpaque(false);
        ProgressBarsScrollPane.setOpaque(false);

        JLabel iconSize = new JLabel();
        iconSize.putClientProperty("FlatLaf.style", "font: $h1.font");

        ThemeDynamicConverterTasks = ThemeChanger.addInDynamicConverter(
                () -> {
                    Color base = UIManager.getColor("Panel.background");
                    Color adjusted;
                    if (DataControl.get("theme_type", "light").equals("dark")) {
                        adjusted = ColorFunctions.lighten(base, 0.1f);
                    } else {
                        adjusted = ColorFunctions.darken(base, 0.1f);
                    }
                    // 重新添加 alpha 通道（例如 80）
                    Color translucent = new Color(adjusted.getRed(), adjusted.getGreen(), adjusted.getBlue(), 100);
                    mainPanel.setBackground(translucent);

                    mainPanel.setOpaque(true);

                    SwingUtilities.invokeLater(this::updateNameLabel); // 字体变化时重新计算
                }
        );

        originalFileName = (fileName != null) ? fileName : StringFormat.translate("task", "task.download_task.no_name_file");
        nameLabel.setText(originalFileName); // 先显示完整名称
        nameLabel.putClientProperty("FlatLaf.style", "font: $h2.font");

        //按钮设置
        IconDynamicConverterTasks = IconControl.addInDynamicConverter(
                () -> iconPanel.setIcon(IconControl.getIcon("file", iconSize.getFont().getSize())),
                () -> exitButton.setIcon(IconControl.getIcon("close", iconSize.getFont().getSize())),
                () -> openButton.setIcon(IconControl.getIcon("file", iconSize.getFont().getSize())),
                () -> downloadControlButton.setIcon(IconControl.getIcon(isFinally || isStart ? "pause" : "start", iconSize.getFont().getSize())),
                () -> openInFolderButton.setIcon(IconControl.getIcon("folder", iconSize.getFont().getSize()))
        );
        downloadControlButton.addActionListener(e -> {
            if (isStart) {
                stop();
                downloadControlButton.setToolTipText(StringFormat.translate("task", "task.download_task.download_control.resume"));
            } else if (!isFinally) {

                start();


                downloadControlButton.setToolTipText(StringFormat.translate("task", "task.download_task.download_control.pause"));
            }
            for (var dynamicConverterTask : IconDynamicConverterTasks) {
                dynamicConverterTask.task();
            }
        });
        openButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(StringFormat.sanitizeFile(new File(savePath, this.fileName)));
            } catch (Exception ex) {
                ToastMessage.show(this, StringFormat.translate("task", "task.download_task.open_file_failed") + "\n" + new File(savePath, this.fileName), ToastMessage.ERROR);
                logger.error("文件打开失败", ex);
            }
        });
        openInFolderButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(StringFormat.sanitizeFile(savePath));
            } catch (Exception ex) {
                ToastMessage.show(this, StringFormat.translate("task", "task.download_task.open_folder_failed"), ToastMessage.ERROR);
                logger.error("文件夹打开失败", ex);
            }
        });

        exitButton.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, StringFormat.translate("task", "task.download_task.close.confirm")) == JOptionPane.YES_OPTION) {
                this.setVisible(false);
                isCanExit = true;

                IconControl.removeInDynamicConverter(IconDynamicConverterTasks);
                ThemeChanger.removeDynamicConverter(ThemeDynamicConverterTasks);
                //清除已有的进度条
                ProgressBarsPanel.removeAll();

                stop();

                doWhenExit();
            }

        });

        // 在构造方法最后，添加组件监听器
        mainPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateNameLabel();
            }
        });

// 并且为了确保初始显示正确，在窗口完全展示后再调用一次
        SwingUtilities.invokeLater(this::updateNameLabel);
    }

    /**
     * 根据标签实际宽度，自动截断文本并追加省略号
     */
    private void updateNameLabel() {
        if (nameLabel == null || originalFileName == null) return;

        // 获取标签的实际可用宽度（减去左右内边距）
        int availableWidth = nameLabel.getWidth() - 10;
        if (availableWidth <= 0) {
            // 宽度尚未确定，暂时显示完整名称
            nameLabel.setText(originalFileName);
            return;
        }

        FontMetrics fm = nameLabel.getFontMetrics(nameLabel.getFont());
        String ellipsis = "...";
        int ellipsisWidth = fm.stringWidth(ellipsis);

        // 1. 如果完整文件名能放下，直接显示，不加省略号
        if (fm.stringWidth(originalFileName) <= availableWidth) {
            nameLabel.setText(originalFileName);
            return;
        }

        // 2. 分离后缀（保留最后一个点之后的部分，包含点）
        int dotIndex = originalFileName.lastIndexOf('.');
        String prefix, suffix;
        if (dotIndex > 0) {
            prefix = originalFileName.substring(0, dotIndex);
            suffix = originalFileName.substring(dotIndex); // 包含 "."
        } else {
            prefix = originalFileName;
            suffix = "";
        }

        // 计算后缀 + 省略号占用的宽度
        int suffixWidth = fm.stringWidth(suffix);
        int fixedWidth = suffixWidth + ellipsisWidth;
        int maxPrefixWidth = availableWidth - fixedWidth;

        // 如果前缀本身就能放下（但完整放不下，这种情况极少）
        if (fm.stringWidth(prefix) <= maxPrefixWidth) {
            nameLabel.setText(prefix + ellipsis + suffix);
            return;
        }

        // 3. 对前缀进行逐字符缩短（高效，也可用二分）
        String truncatedPrefix = prefix;
        for (int i = prefix.length(); i > 0; i--) {
            String sub = prefix.substring(0, i);
            if (fm.stringWidth(sub) <= maxPrefixWidth) {
                truncatedPrefix = sub;
                break;
            }
        }
        // 极端情况：若截断后前缀为空，保留第一个字符
        if (truncatedPrefix.isEmpty() && !prefix.isEmpty()) {
            truncatedPrefix = prefix.substring(0, 1);
        }

        String finalText = truncatedPrefix + ellipsis + suffix;
        nameLabel.setText(finalText);
    }

    public void start() {
        if (isStart || isFinally) return;
        isStart = true;

        startCount++;

        //判断是否支持多线程
        try {


            doWhenStart();


        } catch (Exception e) {
            logger.error("下载异常", e);
            ToastMessage.show(this, StringFormat.translate("task", "task.download_task.download_error.confirm"), ToastMessage.ERROR);
        }

        downloadControlButton.setToolTipText(StringFormat.translate("task", "task.download_task.download_control.pause"));
        for (var dynamicConverterTask : IconDynamicConverterTasks) {
            dynamicConverterTask.task();
        }
    }

    public void stop() {
        if (!isStart || isFinally) return;
        isStart = false;

        doWhenStop();

        downloadControlButton.setToolTipText(StringFormat.translate("task", "task.download_task.download_control.start"));
        for (var dynamicConverterTask : IconDynamicConverterTasks) {
            dynamicConverterTask.task();
        }
    }

    public abstract void doWhenExit();

    public abstract void doWhenStart() throws Exception;

    public abstract void doWhenStop();

    public String getFileName() {
        return fileName;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here


        ProgressBarsPanel = new JPanel(new GridLayout(0, 1, 5, 5));

        ProgressBarsScrollPane = new JScrollPane(ProgressBarsPanel);
        ProgressBarsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        ProgressBarsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        UITools.setScrollPaneUnOpaque(ProgressBarsScrollPane);

    }

    public boolean isFinally() {
        return isFinally;
    }

    public boolean isCanExit() {

        return isCanExit;
    }

}

package com.wmp.downloader.newArchitecture.abstractTask;

import com.alibaba.fastjson2.JSONObject;
import com.formdev.flatlaf.util.ColorFunctions;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.ui.*;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;

public abstract class AbstractTask extends JPanel {
    private static final Logger logger = Logger.getLogger(AbstractTask.class);
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
    protected String fileName;
    protected File savePath;
    protected boolean isStart = false;
    protected boolean isFinally = false;
    protected boolean isCanExit = false;
    protected int startCount = 0;
    private String originalFileName;
    private JSONObject jsonObject;
    private DynamicConverterTask[] IconDynamicConverterTasks;
    private DynamicConverterTask[] ThemeDynamicConverterTasks;

    /**
     * 删除的文件是否正确这取决于你设置的savePath/filename
     */
    private boolean isSupportDeleteWhenExit = true;

    public AbstractTask(JSONObject jsonObject) {
        jsonObject.put("ProgressBarsPanel", ProgressBarsPanel);
        this.jsonObject = jsonObject;
        this.fileName = StringFormat.sanitizeName(jsonObject.getString("rootName"));

        this.savePath = StringFormat.sanitizeFile(new File(jsonObject.getString("savePath")));
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
                    Color adjusted = DataControl.get("theme_type", "light").equals("dark")
                            ? ColorFunctions.lighten(base, 0.1f)
                            : ColorFunctions.darken(base, 0.1f);
                    Color translucent = new Color(adjusted.getRed(), adjusted.getGreen(), adjusted.getBlue(), 150);
                    mainPanel.setBackground(translucent);
                    // 不要设置 mainPanel.setOpaque(false)，已在 createUIComponents 中设置
                    SwingUtilities.invokeLater(this::updateNameLabel);
                }
        );

        originalFileName = (fileName != null) ? fileName : StringFormat.translate("task", "task.download_task.no_name_file");
        nameLabel.setText(originalFileName); // 先显示完整名称
        nameLabel.putClientProperty("FlatLaf.style", "font: $h2.font");

        //按钮设置
        IconDynamicConverterTasks = IconControl.addInDynamicConverter(
                () -> iconPanel.setIcon(getIcon(iconSize.getFont().getSize())),
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

            JPanel panel = new JPanel(new BorderLayout(5, 5));
            panel.add(new JLabel(StringFormat.translate("task.download_task.close.confirm")), BorderLayout.CENTER);
            var isDeleteCheckBox = new JCheckBox(StringFormat.translate("task.download_task.close.is_delete"));
            isDeleteCheckBox.setSelected(DataControl.get("isDeleteWhenCloseTask", false));
            panel.add(isDeleteCheckBox, BorderLayout.SOUTH);

            var i = JOptionPane.showConfirmDialog(
                            this,
                            panel,
                            StringFormat.translate("close"),
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null
                            );
            if (i == JOptionPane.YES_OPTION) {
                //暂停成功
                if (stop()) {

                    try {
                        doWhenExit();
                    } catch (Exception ex) {
                        logger.error("关闭任务执行失败");
                        ToastMessage.show(this, StringFormat.translate("task", "task.stop_failed"), ToastMessage.ERROR);

                    }

                    //删除文件
                    if (isDeleteCheckBox.isSelected()) {
                        DataControl.delete(new File(savePath, fileName), true);
                    }
                    DataControl.putAndSave("isDeleteWhenCloseTask", isDeleteCheckBox.isSelected());

                    this.setVisible(false);
                    isCanExit = true;

                    IconControl.removeInDynamicConverter(IconDynamicConverterTasks);
                    ThemeChanger.removeDynamicConverter(ThemeDynamicConverterTasks);
                    //清除已有的进度条
                    ProgressBarsPanel.removeAll();


                }
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

    public final void start() {
        if (isStart || isFinally) return;
        isStart = true;

        startCount++;

        try {
            if (startCount == 1) {
                doWhenStart();
            } else doWhenRestart();
        } catch (Exception e) {
            startCount--;
            isStart = false;
            logger.error("任务异常", e);
            ToastMessage.show(this, StringFormat.translate("task", "task.download_task.download_error.confirm"), ToastMessage.ERROR);
        }

        downloadControlButton.setToolTipText(StringFormat.translate("task", "task.download_task.download_control.pause"));
        for (var dynamicConverterTask : IconDynamicConverterTasks) {
            dynamicConverterTask.task();
        }
    }

    public final boolean stop() {
        if (!isStart || isFinally) return true;
        isStart = false;

        try {
            doWhenStop();
        } catch (Exception e) {
            isStart = true;
            logger.error("任务异常", e);
            ToastMessage.show(this, StringFormat.translate("task", "task.pause_failed"), ToastMessage.ERROR);
            return false;
        }

        downloadControlButton.setToolTipText(StringFormat.translate("task", "task.download_task.download_control.start"));
        for (var dynamicConverterTask : IconDynamicConverterTasks) {
            dynamicConverterTask.task();
        }
        return true;
    }

    /**
     * 在成功执行关闭后才会调用
     */
    public void doWhenExit() {
    }

    public abstract void doWhenStart() throws Exception;

    public abstract void doWhenRestart() throws Exception;

    public abstract void doWhenStop();

    public String getFileName() {
        return fileName;
    }

    private void createUIComponents() {
        // 自定义 mainPanel 以绘制半透明背景
        mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                // 应用透明度
                float alpha = 0.75f;
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2d.setColor(getBackground());
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
                // 绘制子组件
                super.paintComponent(g);
            }
        };
        mainPanel.setOpaque(false); // 必须为 false，避免 Swing 自动填充

        // 初始化其他组件
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

    protected ImageIcon getIcon(int size) {
        return IconControl.getIcon("file", size);
    }

    public boolean isSupportDeleteWhenExit() {
        return isSupportDeleteWhenExit;
    }

    public void setSupportDeleteWhenExit(boolean supportDeleteWhenExit) {
        isSupportDeleteWhenExit = supportDeleteWhenExit;
    }

    public void runWhenFinally(){}
}

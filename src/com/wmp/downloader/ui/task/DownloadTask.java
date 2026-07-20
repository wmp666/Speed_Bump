package com.wmp.downloader.ui.task;

import com.formdev.flatlaf.util.ColorFunctions;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.ui.DynamicConverterTask;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
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
    protected String fileName;
    protected File savePath;
    protected boolean isStart = false;
    protected boolean isFinally = false;
    protected boolean isCanExit = false;
    private DynamicConverterTask[] IconDynamicConverterTasks = new DynamicConverterTask[0];
    private DynamicConverterTask[] ThemeDynamicConverterTasks = new DynamicConverterTask[0];

    public DownloadTask(String fileName, File savePath) {
        this.fileName = fileName;

        this.savePath = savePath;
        setName(fileName);

        this.setLayout(new BorderLayout());
        this.add(mainPanel);

        ProgressBarsScrollPane.getVerticalScrollBar().setUnitIncrement(10);


        JLabel iconSize = new JLabel();
        iconSize.putClientProperty("FlatLaf.style", "font: $h1.font");

        ThemeDynamicConverterTasks =
                ThemeChanger.addInDynamicConverter(
                        () -> {
                            if (DataControl.get("theme_type", "light").equals("dark"))
                                mainPanel.setBackground(ColorFunctions.lighten(UIManager.getColor("Panel.background"), 0.1f));
                            else
                                mainPanel.setBackground(ColorFunctions.darken(UIManager.getColor("Panel.background"), 0.1f));
                        }
                );

        if (fileName != null) {
            if (fileName.length() > 8) {
                fileName = fileName.substring(0, 5) + "...";
            }
        } else {
            fileName = "无名称的文件";
        }
        nameLabel.setText(fileName);
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
                downloadControlButton.setToolTipText("继续");
            } else if (!isFinally) {

                start();


                downloadControlButton.setToolTipText("暂停");
            }
            for (var dynamicConverterTask : IconDynamicConverterTasks) {
                dynamicConverterTask.task();
            }
        });
        openButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File(savePath, this.fileName));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to open file", "Error", JOptionPane.ERROR_MESSAGE);
                logger.error("Failed to open file", ex);
            }
        });
        openInFolderButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(savePath);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to open folder", "Error", JOptionPane.ERROR_MESSAGE);
                logger.error("Failed to open folder", ex);
            }
        });

        exitButton.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "你确认要关闭？") == JOptionPane.YES_OPTION) {
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
    }

    public void start() {
        if (isStart || isFinally) return;
        isStart = true;

        //判断是否支持多线程
        try {


            doWhenStart();


        } catch (Exception e) {
            logger.error("下载异常", e);
            JOptionPane.showMessageDialog(this, "下载出现异常\ncatch a exception", "错误", JOptionPane.ERROR_MESSAGE);
        }

        downloadControlButton.setToolTipText("暂停");
        for (var dynamicConverterTask : IconDynamicConverterTasks) {
            dynamicConverterTask.task();
        }
    }

    public void stop() {
        if (!isStart || isFinally) return;
        isStart = false;

        doWhenStop();

        downloadControlButton.setToolTipText("开始");
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
    }

    public boolean isFinally() {
        return isFinally;
    }

    public boolean isCanExit() {

        return isCanExit;
    }

}

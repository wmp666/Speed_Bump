package com.wmp.downloader.ui.task;

import com.formdev.flatlaf.util.ColorFunctions;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.ui.DynamicConverterTask;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.tools.ui.ToastMessage;
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

    protected int startCount = 0;

    public DownloadTask(String fileName, File savePath) {
        this.fileName = StringFormat.sanitizeName(fileName);

        this.savePath = StringFormat.sanitizeFile(savePath);
        setName(fileName);

        this.setLayout(new BorderLayout());
        this.add(mainPanel);

        downloadControlButton.setToolTipText(StringFormat.translate("task", "task.download_task.download_control.start"));
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
            fileName = StringFormat.translate("task", "task.download_task.no_name_file");
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
    }

    public boolean isFinally() {
        return isFinally;
    }

    public boolean isCanExit() {

        return isCanExit;
    }

}

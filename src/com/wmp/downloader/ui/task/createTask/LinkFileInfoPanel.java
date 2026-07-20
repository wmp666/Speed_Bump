package com.wmp.downloader.ui.task.createTask;

import com.formdev.flatlaf.util.ColorFunctions;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.ui.DynamicConverterTask;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.ui.FunctionDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 链接文件信息面板
 */
public abstract class LinkFileInfoPanel extends JPanel {

    protected long fileSizeNum;
    protected String url;

    protected JLabel nameLabel;
    protected JLabel sizeLabel;
    protected JPanel mainPanel;
    protected final DynamicConverterTask task = () -> {
        if (DataControl.get("theme_type", "light").equals("dark"))
            mainPanel.setBackground(ColorFunctions.lighten(UIManager.getColor("Panel.background"), 0.1f));
        else
            mainPanel.setBackground(ColorFunctions.darken(UIManager.getColor("Panel.background"), 0.1f));
    };
    protected JButton editButton;
    protected JLabel modeLabel;

    public LinkFileInfoPanel(String name, long size, String mode, String url) {

        this.fileSizeNum = size;
        this.url = url;

        nameLabel.putClientProperty("FlatLaf.style", "font: $h2.font");
        sizeLabel.putClientProperty("FlatLaf.style", "font: $h3.font");
        modeLabel.putClientProperty("FlatLaf.style", "font: $h3.font");


        ThemeChanger.addInDynamicConverter(
                task
        );

        IconControl.addInDynamicConverter(
                () -> nameLabel.setIcon(IconControl.getIcon("file", nameLabel.getFont().getSize())),
                () -> editButton.setIcon(IconControl.getIcon("edit", nameLabel.getFont().getSize()))
        );
        nameLabel.setText(name);
        modeLabel.setText(mode);
        sizeLabel.setText(formatFileSize(size));

        editButton.addActionListener(this::editButtonAction);

        this.setLayout(new BorderLayout());
        this.add(mainPanel);
    }

    /**
     * 将字节数转为人类可读格式（B, KB, MB, GB, TB, PB）
     *
     * @param bytes 文件大小（字节）
     * @return 格式化字符串，如 "2.34 GB"
     */
    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        double value = bytes / Math.pow(1024, digitGroups);
        // 保留两位小数，如果单位是 B 则不留小数
        return String.format("%.2f %s", value, units[digitGroups]);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        if (!aFlag)
            ThemeChanger.removeDynamicConverter(task);
    }

    public String getFileName() {
        return nameLabel.getText();
    }

    public String getMode() {
        return modeLabel.getText();
    }

    public long getFileSizeNum() {
        return fileSizeNum;
    }

    public String getUrl() {
        return url;
    }

    public abstract void editButtonAction(ActionEvent e);

    public static LinkFileInfoPanel createBasicLinkFileInfoPanel(String name, long size, String mode, String url) {
        return new LinkFileInfoPanel(name, size, mode, url) {
            @Override
            public void editButtonAction(ActionEvent e) {
                var taskFileEditPanel = new TaskFileEditPanel(nameLabel.getText());
                FunctionDialog.showDialog(this, "编辑任务信息", taskFileEditPanel,
                        result -> {
                            if (result == FunctionDialog.RESULT_SAVE)
                                nameLabel.setText(taskFileEditPanel.getFileName());
                        },
                        FunctionDialog.SAVE_CANCEL_BUTTONS, 0, null, 0);
            }
        };
    }
}

package com.wmp.downloader.ui.task.createTask;

import com.formdev.flatlaf.util.ColorFunctions;
import com.wmp.downloader.laug.StringFormat;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.ui.DynamicConverterTask;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.ui.FunctionDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 链接文件组信息面板
 */
public abstract class LinkFolderInfoPanel extends JPanel {

    private long selectedFileSize;
    protected long[] allFileSizes;
    protected final String[] allFileNames;
    protected String[] fileTypes;
    protected String[] AllUrls;
    private final ArrayList<String> selectedUrls = new ArrayList<>();

    protected boolean[] fileSelectionStatus;

    protected JLabel folderNameLabel;
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
    private JLabel IconLabel;
    private JButton fileChooseButton;

    /**
     * 创建链接文件组信息面板
     * @param folderFame 文件夹名称
     * @param sizes 所有文件大小数组
     * @param mode 下载的文件协议/种类（如：HTTP,bilibili）
     * @param urls 所有文件链接数组
     * @param fileNames 所有文件名称数组
     */
    public LinkFolderInfoPanel(String folderFame, long[] sizes, String mode, String[] urls, String[] fileNames, String[] fileTypes) {

        for (var size : sizes) {
            selectedFileSize += size;
        }
        this.selectedUrls.addAll(List.of(urls));
        this.fileTypes = fileTypes;
        this.allFileSizes = sizes;
        this.allFileNames = fileNames;
        this.AllUrls = urls;
        this.fileSelectionStatus = new boolean[fileNames.length];
        for (int i = 0; i < fileNames.length; i++) {
            fileSelectionStatus[i] = true;
        }

        folderNameLabel.putClientProperty("FlatLaf.style", "font: $h2.font");
        sizeLabel.putClientProperty("FlatLaf.style", "font: $h3.font");
        modeLabel.putClientProperty("FlatLaf.style", "font: $h3.font");


        ThemeChanger.addInDynamicConverter(task);

        IconControl.addInDynamicConverter(
                () -> IconLabel.setIcon(IconControl.getIcon("folder", folderNameLabel.getFont().getSize())),
                () -> fileChooseButton.setIcon(IconControl.getIcon("choose", folderNameLabel.getFont().getSize())),
                () -> editButton.setIcon(IconControl.getIcon("edit", folderNameLabel.getFont().getSize()))
        );
        folderNameLabel.setText(folderFame);
        modeLabel.setText(mode);
        sizeLabel.setText(formatFileSize(this.selectedFileSize));

        editButton.addActionListener(this::editButtonAction);

        fileChooseButton.addActionListener(e -> {
            var linkFileChoosePanel = new LinkFileChoosePanel(fileNames, sizes, fileTypes, fileSelectionStatus);
            FunctionDialog.showDialog(this, StringFormat.translate("task", "task.create_task.download_settings.choose_file"), linkFileChoosePanel,
                    result -> {
                        if (result == FunctionDialog.RESULT_OK) {
                            sizeLabel.setText(formatFileSize(linkFileChoosePanel.getSelectedFilesSize()));
                            selectedUrls.clear();
                            fileSelectionStatus = linkFileChoosePanel.getFileSelectionStatus();
                            for (var selectedFilesIndex : linkFileChoosePanel.getSelectedFilesIndex()) {
                                selectedUrls.add(urls[selectedFilesIndex]);
                            }
                            selectedFileSize = linkFileChoosePanel.getSelectedFilesSize();

                            //调用文件选择内容更改方法
                            selectionFileListChangeAction();
                        }
                    },
                    FunctionDialog.OK_CANCEL_BUTTONS, 0, null, 0);
        });

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

    public String getFolderName() {
        return folderNameLabel.getText();
    }

    public String getMode() {
        return modeLabel.getText();
    }

    /**
     * 获取文件名数组（选中的），带后缀
     * @return 选中的文件名数组
     */
    public String[] getSelectionFileNames(){
        ArrayList<String> selectionFileNames = new ArrayList<>();
        for (var i = 0; i < fileSelectionStatus.length; i++) {
            if (fileSelectionStatus[i]){
                if (allFileNames[i].endsWith("." + fileTypes[i]))
                    selectionFileNames.add(allFileNames[i]);
                else
                    selectionFileNames.add(allFileNames[i] + "." + fileTypes[i]);
            }
        }
        return selectionFileNames.toArray(String[]::new);
    }

    public long[] getFileSizes() {
        return allFileSizes;
    }
    public String[] getFileNames() {
        return allFileNames;
    }
    public String[] getUrls() {
        return AllUrls;
    }

    public long getSelectedFileSize() {
        return selectedFileSize;
    }

    public String[] getSelectedUrls() {
        return this.selectedUrls.toArray(String[]::new);
    }

    public long[] getSelectedFileSizes() {
        ArrayList<Long> sizes = new ArrayList<>();
        for (var i = 0; i < fileSelectionStatus.length; i++) {
            if (fileSelectionStatus[i]) {
                sizes.add(allFileSizes[i]);
            }
        }
        return sizes.stream().mapToLong(Long::longValue).toArray();
    }

    public abstract void editButtonAction(ActionEvent e);

    public abstract void selectionFileListChangeAction();

    public static LinkFolderInfoPanel createBasicLinkFileInfoPanel(String folderName, long size, String mode, String url, String[] fileTypes) {
        return new LinkFolderInfoPanel(folderName, new long[]{size}, mode, new String[]{url}, new String[]{folderName}, fileTypes) {
            @Override
            public void editButtonAction(ActionEvent e) {
                var taskFileEditPanel = new TaskFileEditPanel(folderNameLabel.getText());
                FunctionDialog.showDialog(this, StringFormat.translate("task", "task.create_task.download_settings.task_edit"), taskFileEditPanel,
                        result -> {
                            if (result == FunctionDialog.RESULT_SAVE)
                                folderNameLabel.setText(taskFileEditPanel.getFileName());
                        },
                        FunctionDialog.SAVE_CANCEL_BUTTONS, 0, null, 0);
            }

            @Override
            public void selectionFileListChangeAction() {
                long newSelectedFileSize = 0;
                for (int i = 0; i < fileSelectionStatus.length; i++) {
                    if (fileSelectionStatus[i]) {
                        newSelectedFileSize += allFileSizes[i];
                    }
                }
                sizeLabel.setText(formatFileSize(newSelectedFileSize));
            }
        };
    }
}

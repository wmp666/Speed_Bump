package com.wmp.downloader.newArchitecture.abstractTask.linkInfoPanel;

import com.alibaba.fastjson2.JSONObject;
import com.formdev.flatlaf.util.ColorFunctions;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractParser;
import com.wmp.downloader.newArchitecture.ui.createTask.TaskFileEditPanel;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.StringFormat;
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
public abstract class LinkFolderInfoPanel extends AbstractLinkInfoPanel {

    protected final String[] allFileNames;
    private final ArrayList<String> selectedUrls = new ArrayList<>();
    protected long[] allFileSizes;
    protected String[] fileTypes;
    protected String[] AllUrls;
    protected boolean[] fileSelectionStatus;

    protected String folderName;

    protected JLabel folderNameLabel;
    protected JLabel sizeLabel;
    protected JPanel mainPanel;
    protected JButton editButton;
    protected JLabel modeLabel;
    private long selectedFileSize;
    private JLabel IconLabel;
    private JButton fileChooseButton;

    protected final DynamicConverterTask task = () -> {
        if (DataControl.get("theme_type", "light").equals("dark"))
            mainPanel.setBackground(ColorFunctions.lighten(UIManager.getColor("Panel.background"), 0.1f));
        else
            mainPanel.setBackground(ColorFunctions.darken(UIManager.getColor("Panel.background"), 0.1f));
    };

    /**
     * 创建链接文件组信息面板
     *
     * @param folderName 文件夹名称
     * @param sizes      所有文件大小数组
     * @param mode       下载的文件协议/种类（如：HTTP,bilibili）
     * @param urls       所有文件链接数组
     * @param fileNames  所有文件名称数组
     */
    public LinkFolderInfoPanel(String folderName, long[] sizes, String mode, String[] urls, String[] fileNames, String[] fileTypes, AbstractParser.Info info) {
        super(info);

        for (var size : sizes) {
            selectedFileSize += size;
        }
        this.selectedUrls.addAll(List.of(urls));
        this.fileTypes = fileTypes;
        this.allFileSizes = sizes;
        this.allFileNames = fileNames;
        this.AllUrls = urls;
        this.folderName = folderName;
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
        folderNameLabel.setText(folderName);
        modeLabel.setText(mode);
        sizeLabel.setText(StringFormat.formatSize(this.selectedFileSize));

        editButton.addActionListener(this::editButtonAction);

        fileChooseButton.addActionListener(e -> {
            var linkFileChoosePanel = new LinkFileChoosePanel(fileNames, sizes, fileTypes, fileSelectionStatus);
            FunctionDialog.showDialog(SwingUtilities.getWindowAncestor(this), StringFormat.translate("task", "task.create_task.download_settings.choose_file"), linkFileChoosePanel,
                    result -> {
                        if (result == FunctionDialog.RESULT_OK) {
                            sizeLabel.setText(StringFormat.formatSize(linkFileChoosePanel.getSelectedFilesSize()));
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

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        if (!aFlag)
            ThemeChanger.removeDynamicConverter(task);
    }

    private long[] getSelectedFileSizes() {
        ArrayList<Long> sizes = new ArrayList<>();
        for (var i = 0; i < fileSelectionStatus.length; i++) {
            if (fileSelectionStatus[i]) {
                sizes.add(allFileSizes[i]);
            }
        }
        return sizes.stream().mapToLong(Long::longValue).toArray();
    }

    /**
     * 获取文件名数组（选中的），带后缀
     *
     * @return 选中的文件名数组
     */
    private String[] getSelectedFileNames() {
        ArrayList<String> selectionFileNames = new ArrayList<>();
        for (var i = 0; i < fileSelectionStatus.length; i++) {
            if (fileSelectionStatus[i]) {
                if (allFileNames[i].endsWith("." + fileTypes[i]))
                    selectionFileNames.add(allFileNames[i]);
                else
                    selectionFileNames.add(allFileNames[i] + "." + fileTypes[i]);
            }
        }
        return selectionFileNames.toArray(String[]::new);
    }

    public abstract void editButtonAction(ActionEvent e);

    public abstract void selectionFileListChangeAction();

    public boolean[] getFileSelectionStatus() {
        return fileSelectionStatus;
    }

    @Override
    public JSONObject getJsonInfo() {
        var jsonObject = new JSONObject();
        jsonObject.put("rootName", this.folderName);
        jsonObject.put("selectedUrls", this.selectedUrls.toArray(String[]::new));
        var selectedFileSizes = getSelectedFileSizes();
        jsonObject.put("sizes", selectedFileSizes);
        jsonObject.put("selectedFileNames", getSelectedFileNames());
        jsonObject.put("selectedStatus", fileSelectionStatus);
        jsonObject.put("mode", modeLabel.getText());
        var size = 0L;
        for (var fileSize : selectedFileSizes) {
            size += fileSize;
        }
        jsonObject.put("size", size);

        return jsonObject;
    }

    public static LinkFolderInfoPanel createPanel(
            String folderName, long[] size, String mode,
            String[] url, String[] fileNames, String[] fileTypes, AbstractParser.Info info) {
        return new LinkFolderInfoPanel(folderName, size, mode, url, fileNames, fileTypes, info) {
            @Override
            public void editButtonAction(ActionEvent e) {
                var taskFileEditPanel = new TaskFileEditPanel(this.folderName);
                FunctionDialog.showDialog(SwingUtilities.getWindowAncestor(this), StringFormat.translate("task", "task.create_task.download_settings.task_edit"), taskFileEditPanel,
                        result -> {
                            if (result == FunctionDialog.RESULT_SAVE) {
                                this.folderName = taskFileEditPanel.getFileName();
                                folderNameLabel.setText(taskFileEditPanel.getFileName());
                            }
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
                sizeLabel.setText(StringFormat.formatSize(newSelectedFileSize));
            }
        };
    }
}

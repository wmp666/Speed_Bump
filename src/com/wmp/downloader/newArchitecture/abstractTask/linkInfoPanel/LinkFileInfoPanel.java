package com.wmp.downloader.newArchitecture.abstractTask.linkInfoPanel;

import com.alibaba.fastjson2.JSONObject;
import com.formdev.flatlaf.util.ColorFunctions;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractParser;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.ui.DynamicConverterTask;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.ui.FunctionDialog;
import com.wmp.downloader.newArchitecture.ui.createTask.TaskFileEditPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * 链接文件信息面板
 */
public abstract class LinkFileInfoPanel extends AbstractLinkInfoPanel {

    protected String fileName;

    protected long fileSizeNum;
    protected String url;
    protected JLabel nameLabel;
    protected JLabel sizeLabel;
    protected JPanel mainPanel;
    protected JButton editButton;
    protected JLabel modeLabel;

    protected final DynamicConverterTask task = () -> {
        if (DataControl.get("theme_type", "light").equals("dark"))
            mainPanel.setBackground(ColorFunctions.lighten(UIManager.getColor("Panel.background"), 0.1f));
        else
            mainPanel.setBackground(ColorFunctions.darken(UIManager.getColor("Panel.background"), 0.1f));
    };

    public LinkFileInfoPanel(String name, long size, String mode, String url, AbstractParser.Info info) {
        super(info);

        this.fileName = name;
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
        sizeLabel.setText(StringFormat.formatSize(size));

        editButton.addActionListener(this::editButtonAction);

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

    public abstract void editButtonAction(ActionEvent e);

    @Override
    public JSONObject getJsonInfo() {

        jsonInfo.put("size", fileSizeNum);
        jsonInfo.put("url", url);
        jsonInfo.put("mode", modeLabel.getText());
        jsonInfo.put("rootName", fileName);
        return jsonInfo;
    }

    public static LinkFileInfoPanel createPanel(String name,
                                                long size,
                                                String mode,
                                                String url, AbstractParser.Info info) {
        return new LinkFileInfoPanel(name, size, mode, url, info) {
            @Override
            public void editButtonAction(ActionEvent e) {
                var taskFileEditPanel = new TaskFileEditPanel(name);
                FunctionDialog.showDialog(SwingUtilities.getWindowAncestor(this), StringFormat.translate("task", "task.create_task.download_settings.task_edit"), taskFileEditPanel,
                        result -> {
                            if (result == FunctionDialog.RESULT_SAVE) {
                                nameLabel.setText(taskFileEditPanel.getFileName());
                                fileName = taskFileEditPanel.getFileName();
                            }

                        },
                        FunctionDialog.SAVE_CANCEL_BUTTONS, 0, null, 0);
            }
        };
    }
}

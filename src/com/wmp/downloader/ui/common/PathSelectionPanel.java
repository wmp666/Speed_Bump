package com.wmp.downloader.ui.common;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.wmp.downloader.tools.ui.IconControl;

import javax.swing.*;
import java.io.File;

public class PathSelectionPanel extends JPanel {
    private JLabel DownloadFileLocationLabel;
    private JTextField FileTextField;
    private JButton LocationChooseButton;
    private JPanel PathSelectionPanel;

    public PathSelectionPanel(String prompt, File defaultFile) {
        LocationChooseButton.addActionListener(e -> {
            var systemFileChooser = new SystemFileChooser();
            systemFileChooser.setDialogType(SystemFileChooser.SAVE_DIALOG);
            systemFileChooser.setFileHidingEnabled(true);
            systemFileChooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
            if (systemFileChooser.showDialog(this, "选择") == SystemFileChooser.APPROVE_OPTION) {
                var path = systemFileChooser.getSelectedFile().getAbsolutePath();
                FileTextField.setText(path);
            }
        });
        IconControl.addInDynamicConverter(
                () -> LocationChooseButton.setIcon(IconControl.getIcon("folder", DownloadFileLocationLabel.getFont().getSize()))
        );


        DownloadFileLocationLabel.putClientProperty("FlatLaf.style", "font: $Large.font");
        FileTextField.putClientProperty("FlatLaf.style", "font: $Large.font");

        DownloadFileLocationLabel.setText(prompt);
        FileTextField.setText(defaultFile.getAbsolutePath());

        this.add(PathSelectionPanel);
    }

    public String getPath() {
        return FileTextField.getText();
    }

    public void setPath(String path) {
        FileTextField.setText(path);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        LocationChooseButton = new JButton();

    }
}

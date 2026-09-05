package com.wmp.downloader.ui.common;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.ui.IconControl;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;

public class PathSelectionPanel extends JPanel {
    private JLabel DownloadFileLocationLabel;
    private JTextField FileTextField;
    private JButton LocationChooseButton;
    private JPanel PathSelectionPanel;

    private PathChangeListener pathChangeListener = path -> {
    };

    public PathSelectionPanel(String prompt, File defaultFile) {
        this(prompt, defaultFile, SystemFileChooser.DIRECTORIES_ONLY);
    }

    public PathSelectionPanel(String prompt, File defaultFile, int FileSelectionMode) {
        LocationChooseButton.addActionListener(e -> {
            var path = DataControl.getPath(this, SystemFileChooser.OPEN_DIALOG, FileSelectionMode);
            if (path == null) return;
            var str = path.getAbsolutePath();
            FileTextField.setText(str);
            pathChangeListener.pathChanged(str);
        });
        FileTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                pathChangeListener.pathChanged(FileTextField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                pathChangeListener.pathChanged(FileTextField.getText());

            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                pathChangeListener.pathChanged(FileTextField.getText());

            }
        });
        IconControl.addInDynamicConverter(
                () -> LocationChooseButton.setIcon(IconControl.getIcon("folder", DownloadFileLocationLabel.getFont().getSize()))
        );


        DownloadFileLocationLabel.putClientProperty("FlatLaf.style", "font: $Large.font");
        FileTextField.putClientProperty("FlatLaf.style", "font: $Large.font");

        DownloadFileLocationLabel.setText(prompt);
        if (defaultFile != null)
            FileTextField.setText(defaultFile.getAbsolutePath());

        this.setLayout(new BorderLayout());
        this.add(PathSelectionPanel);
    }

    public String getPath() {
        return FileTextField.getText();
    }

    public void setPath(String path) {
        FileTextField.setText(path);
        pathChangeListener.pathChanged(path);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        LocationChooseButton = new JButton();

    }

    public void setPathChangeListener(PathChangeListener e) {
        this.pathChangeListener = e;
    }

    public interface PathChangeListener {
        void pathChanged(String path);
    }

    @Override
    public void setVisible(boolean aFlag) {
        PathSelectionPanel.setVisible(aFlag);
        super.setVisible(aFlag);
    }
}

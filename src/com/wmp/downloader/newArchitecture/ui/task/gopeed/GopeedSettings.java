package com.wmp.downloader.newArchitecture.ui.task.gopeed;

import com.wmp.downloader.newArchitecture.abstractTask.AbstractSpecialSettingsPage;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.ui.common.PathSelectionPanel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class GopeedSettings extends AbstractSpecialSettingsPage {
    private JPanel mainPanel;
    private JTextField gopeedPortTextField;
    private PathSelectionPanel gopeedPathselectionPanel;

    public GopeedSettings() {
        gopeedPathselectionPanel.setPath(DataControl.get("gopeed_path", ""));
        gopeedPortTextField.setText(DataControl.get("gopeed_port", "9999"));
        gopeedPathselectionPanel.setPathChangeListener(path -> {
            DataControl.putAndSave("gopeed_path", path);
        });
        gopeedPortTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                DataControl.putAndSave("gopeed_port", gopeedPortTextField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                DataControl.putAndSave("gopeed_port", gopeedPortTextField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                DataControl.putAndSave("gopeed_port", gopeedPortTextField.getText());
            }
        });

        this.setLayout(new BorderLayout());
        this.add(mainPanel, BorderLayout.CENTER);
    }

    @Override
    public void setDefaultButton() {

    }

    @Override
    public String getSettingsName() {
        return StringFormat.translate("special_settings", "gopeed_special_settings");
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        gopeedPathselectionPanel = new PathSelectionPanel(StringFormat.translate("special_settings", "gopeed_special_settings.selection_path"), null, JFileChooser.FILES_ONLY);
    }
}

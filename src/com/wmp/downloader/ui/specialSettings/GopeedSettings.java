package com.wmp.downloader.ui.specialSettings;

import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.ui.common.PathSelectionPanel;
import com.wmp.downloader.ui.settings.BasicSpecialSettings;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class GopeedSettings extends BasicSpecialSettings {
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
    }

    @Override
    public String getSettingsName() {
        return StringFormat.translate("special_settings", "gopeed_special_settings");
    }

    @Override
    public SpecialSettingsPanel getSettings() {
        return new GopeedSettingsPanel(mainPanel);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        gopeedPathselectionPanel = new PathSelectionPanel(StringFormat.translate("special_settings", "gopeed_special_settings.selection_path"), null, JFileChooser.FILES_ONLY);
    }


    class GopeedSettingsPanel extends SpecialSettingsPanel {
        public GopeedSettingsPanel(JPanel mainPanel) {
            this.setLayout(new BorderLayout());
            this.add(mainPanel, BorderLayout.CENTER);
        }

        @Override
        public void setDefaultButton() {

        }
    }
}

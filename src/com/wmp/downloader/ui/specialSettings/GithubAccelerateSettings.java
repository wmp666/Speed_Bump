package com.wmp.downloader.ui.specialSettings;

import com.wmp.downloader.laug.StringFormat;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.ui.settings.BasicSpecialSettings;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class GithubAccelerateSettings extends BasicSpecialSettings {
    private JPanel mainPanel;
    private JComboBox<String> accelerateSelectionComboBox;
    private JCheckBox GithubAccelerateCheckBox;
    private JTextField linkTextField;

    public GithubAccelerateSettings() {
        accelerateSelectionComboBox.addItem("gh-proxy.com");
        accelerateSelectionComboBox.addItem("gh-proxy.org");
        accelerateSelectionComboBox.addItem(StringFormat.translate("special_settings", "github_accelerate_special_settings.set_accelerate_station.custom"));
        accelerateSelectionComboBox.addItemListener(e -> {
            DataControl.putAndSave("github_accelerate_selection",accelerateSelectionComboBox.getSelectedIndex());
            if (e.getItem().toString().equals(StringFormat.translate("special_settings", "github_accelerate_special_settings.set_accelerate_station.custom"))) {
                linkTextField.setVisible(true);
                linkTextField.setText("");
            }else{
                linkTextField.setVisible(false);
                linkTextField.setText(accelerateSelectionComboBox.getSelectedItem().toString());
            }
        });
        accelerateSelectionComboBox.setSelectedIndex(DataControl.get("github_accelerate_selection",0));
        linkTextField.setVisible(accelerateSelectionComboBox.getSelectedItem().toString().equals(StringFormat.translate("special_settings", "github_accelerate_special_settings.set_accelerate_station.custom")));
        linkTextField.setText(DataControl.get("github_accelerate_link",""));

        GithubAccelerateCheckBox.addItemListener(e -> {
            DataControl.putAndSave("is_use_github_accelerate",GithubAccelerateCheckBox.isSelected());
        });
        GithubAccelerateCheckBox.setSelected(DataControl.get("is_use_github_accelerate",false));

        linkTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                DataControl.putAndSave("github_accelerate_link",linkTextField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                DataControl.putAndSave("github_accelerate_link",linkTextField.getText());

            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                DataControl.putAndSave("github_accelerate_link",linkTextField.getText());
            }
        });
    }

    @Override
    public String getSettingsName() {
        return StringFormat.translate("special_settings", "github_accelerate_special_settings");
    }

    @Override
    public SpecialSettingsPanel getSettings() {
        return new GithubAccelerateSettingsPanel(mainPanel);
    }

    class GithubAccelerateSettingsPanel extends SpecialSettingsPanel {

        public GithubAccelerateSettingsPanel(JPanel panel) {
            this.setOpaque(false);
            this.setLayout(new BorderLayout());
            this.add(panel, BorderLayout.CENTER);
        }

        @Override
        public void setDefaultButton() {

        }
    }
}

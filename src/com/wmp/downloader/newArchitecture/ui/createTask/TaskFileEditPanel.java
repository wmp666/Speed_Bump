package com.wmp.downloader.newArchitecture.ui.createTask;

import javax.swing.*;
import java.awt.*;

public class TaskFileEditPanel extends JPanel {
    private JTextField NameTextField;
    private JPanel mainPanel;


    public TaskFileEditPanel(String name) {
        this.setLayout(new BorderLayout());
        this.add(mainPanel);

        NameTextField.setText(name);
    }

    public String getFileName() {
        return NameTextField.getText();
    }

    public void setFileName(String name) {
        NameTextField.setText(name);
    }
}

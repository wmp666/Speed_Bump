package com.wmp.downloader.ui;

import javax.swing.*;

public class PreloadDialog extends JDialog {
    private JPanel contentPane;

    public PreloadDialog() {
        setTitle("Speed Bump preloading...");
        setContentPane(contentPane);
        setResizable(false);
        setUndecorated(true);
        pack();
        setLocationRelativeTo(null);

    }
}

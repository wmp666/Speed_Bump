package com.wmp.downloader.ui;

import javax.swing.*;

public class PreloadDialog extends JDialog {
    private JPanel contentPane;
    private JProgressBar progressBar1;

    public PreloadDialog() {
        setTitle("Speed Bump preloading...");
        setContentPane(contentPane);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);

    }
}

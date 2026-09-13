package com.wmp.downloader.test;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.SystemFileChooser;

import javax.swing.*;
import java.awt.*;

public class FileDialogTest {
    static void main() {
        FlatLightLaf.setup();

        var jFrame = new JFrame();
        jFrame.setVisible(true);

        var fileChooser = new SystemFileChooser();
        fileChooser.setDialogType(SystemFileChooser.SAVE_DIALOG);
        SwingUtilities.invokeLater(()->{
            fileChooser.showSaveDialog(null);
        });
    }
}

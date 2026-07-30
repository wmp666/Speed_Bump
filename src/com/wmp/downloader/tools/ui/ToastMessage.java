package com.wmp.downloader.tools.ui;

import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.ui.Downloader;
import raven.modal.Toast;
import raven.modal.toast.ToastPromise;
import raven.modal.toast.option.ToastLayoutOption;
import raven.modal.toast.option.ToastLocation;
import raven.modal.toast.option.ToastOption;

import javax.swing.*;
import java.awt.*;

public class ToastMessage {

    public static final Toast.Type DEFAULT = Toast.Type.DEFAULT;
    public static final Toast.Type INFO = Toast.Type.INFO;
    public static final Toast.Type SUCCESS = Toast.Type.SUCCESS;
    public static final Toast.Type WARNING = Toast.Type.WARNING;
    public static final Toast.Type ERROR = Toast.Type.ERROR;

    public static void show(Component c, String message, Toast.Type type, boolean isUseHeavyWeight) {
        if (c == null) c = Downloader.mainFrame;
        Toast.show(c, type, message, ToastLocation.BOTTOM_TRAILING, new ToastOption()
                .setDelay(2000)
                .setHeavyWeight(DataControl.get("is_use_heavy_weight.toast", false) || isUseHeavyWeight)
                .setLayoutOption(new ToastLayoutOption())
                .setHtmlEnabled(true));

        //JOptionPane.showMessageDialog(null, "This is a toast message", "Toast", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void show(Component c, String message, Toast.Type type) {
        show(c, message, type, false);
    }

    public static void show(String message, Toast.Type type){
        show(null, message, type);
    }

    public static void show(String message){
        show(message, Toast.Type.DEFAULT);
    }

    static void main() {
        FlatMacLightLaf.setup();

        JFrame frame = new JFrame();
        frame.setBackground(Color.PINK);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setVisible(true);

        JButton button = new JButton("Show Toast Heavy");
        button.addActionListener(e -> {
            ToastMessage.show(frame, "超长！！！------------------------------------------------------------------------------------------------------", ToastMessage.INFO, true);
            ToastMessage.show(frame, "This is a toast message2", ToastMessage.ERROR, true);
        });

        frame.add(button, BorderLayout.SOUTH);

        JButton button2 = new JButton("Show Toast");
        button2.addActionListener(e -> {
            ToastMessage.show(frame, "超长！！！------------------------------------------------------------------------------------------------------", ToastMessage.INFO);
            ToastMessage.show(frame, "This is a toast message1", ToastMessage.ERROR);
        });

        frame.add(button2, BorderLayout.NORTH);
    }
}

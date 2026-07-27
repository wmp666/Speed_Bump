package com.wmp.downloader.tools.ui;

import com.wmp.downloader.ui.Downloader;
import raven.modal.Toast;
import raven.modal.toast.option.ToastLocation;
import raven.modal.toast.option.ToastOption;
import java.awt.*;

public class ToastMessage {

    public static final Toast.Type DEFAULT = Toast.Type.DEFAULT;
    public static final Toast.Type INFO = Toast.Type.INFO;
    public static final Toast.Type SUCCESS = Toast.Type.SUCCESS;
    public static final Toast.Type WARNING = Toast.Type.WARNING;
    public static final Toast.Type ERROR = Toast.Type.ERROR;
    public static void show(Component c, String message, Toast.Type type){

        if (c == null) c = Downloader.mainFrame;
        Toast.show(c, type, message, ToastLocation.BOTTOM_TRAILING, new ToastOption().setDelay(2000));

        //JOptionPane.showMessageDialog(null, "This is a toast message", "Toast", JOptionPane.INFORMATION_MESSAGE);
    }
}

package com.wmp.downloader;

import com.formdev.flatlaf.FlatLightLaf;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.WebSetter;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.ui.Downloader;
import com.wmp.downloader.ui.FunctionDialog;
import com.wmp.downloader.ui.PreloadDialog;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.util.List;

public class Run {
    private static final Logger logger = Logger.getLogger(Run.class);

    public static String VERSION = "0.3.4";

    public static String PLUGIN_SUPPORT_VERSION = "1.0.0";

    static void main(String[] args) {
        var argList = List.of(args);
        {
            var versionIndex = argList.indexOf("-set:version") + 1;
            VERSION = versionIndex == 0 ? VERSION : argList.get(versionIndex);
        }

        FlatLightLaf.setup();

        var preloadDialog = new PreloadDialog();
        preloadDialog.setVisible(true);

        Downloader downloader = null;
        try {
            DataControl.load();

            WebSetter.SSLControl(DataControl.get("isUseSSL", false));
            WebSetter.proxies(true);


            ThemeChanger.easyChanger();

            downloader = new Downloader();

            ThemeChanger.easyChanger();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "启动发生错误\n"+e);
            logger.error("启动发生错误", e);
            System.exit(0);
            throw new RuntimeException(e);

        }

        downloader.setVisible(true);

        preloadDialog.setVisible(false);




    }
}

package com.wmp.downloader;

import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.WebSetter;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.ui.Downloader;
import org.apache.log4j.Logger;

import java.util.List;

public class Run {
    private static final Logger logger = Logger.getLogger(Run.class);

    public static String VERSION = "0.2.0";

    public static String PLUGIN_SUPPORT_VERSION = "1.0.0";

    static void main(String[] args) {
        var argList = List.of(args);
        {
            var versionIndex = argList.indexOf("-set:version") + 1;
            VERSION = versionIndex == 0 ? VERSION : argList.get(versionIndex);
        }


        DataControl.load();

        WebSetter.SSLControl(DataControl.get("isUseSSL", false));
        WebSetter.proxies(true);


        ThemeChanger.easyChanger();

        var downloader = new Downloader();

        ThemeChanger.easyChanger();

        downloader.setVisible(true);

        if (DataControl.get("is_start_check_update", true)) {
            downloader.checkUpdate();
        }


    }
}

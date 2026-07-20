package com.wmp.downloader;

import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.ui.Downloader;
import org.apache.log4j.Logger;

public class Run {
    private static final Logger logger = Logger.getLogger(Run.class);

    static void main(String[] args) {
        DataControl.load();

        WebSetter.SSLControl(DataControl.get("isUseSSL", false));

        ThemeChanger.easyChanger();

        var downloader = new Downloader();

        ThemeChanger.easyChanger();

        downloader.setVisible(true);


    }
}

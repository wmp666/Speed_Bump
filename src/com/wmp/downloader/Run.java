package com.wmp.downloader;

import com.formdev.flatlaf.FlatLightLaf;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.WebSetter;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.tools.web.TCPClient;
import com.wmp.downloader.tools.web.TCPControl;
import com.wmp.downloader.ui.Downloader;
import com.wmp.downloader.ui.FunctionDialog;
import com.wmp.downloader.ui.PreloadDialog;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.util.List;

public class Run {
    private static final Logger logger = Logger.getLogger(Run.class);

    public static String VERSION = "0.4.2";

    public static String PLUGIN_SUPPORT_VERSION = "1.0.0";

    static void main(String[] args) {
        var argList = List.of(args);
        String linkPath = null;
        {

            if (!argList.isEmpty()) {

                var versionIndex = argList.indexOf("-set:version") + 1;
                VERSION = versionIndex == 0 ? VERSION : argList.get(versionIndex);

                {
                    var first = argList.getFirst();

                    if (!first.startsWith("-")){
                        linkPath = first;
                        try {
                            var code = TCPControl.sendToServer("createTask:" + linkPath);
                            if (code == 1) {
                                throw new Exception("消息发送失败: " + linkPath);
                            } else if (code == 0) {
                                System.exit(0);
                            } else if (code == -1) {
                                logger.warn("没有服务端,将以自己作为服务端");
                            }
                        } catch (Exception e) {
                            logger.error("消息发送失败", e);
                            JOptionPane.showMessageDialog(null, "无法将消息传递至下载器!", StringFormat.translate("error"), JOptionPane.ERROR_MESSAGE);
                            System.exit(-1);
                        }
                    }
                }
            }
            try {
                if (TCPControl.isHasServer()) {
                    TCPControl.sendToServer("show");
                    System.exit(0);
                }
            } catch (Exception e) {
                System.exit(-1);
            }
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

            try {
                TCPControl.startServer();
            } catch (Exception ex) {
                logger.error("服务端启动失败!");
                JOptionPane.showMessageDialog(null, "服务端启动失败");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "启动发生错误\n"+e);
            logger.error("启动发生错误", e);
            System.exit(0);
            throw new RuntimeException(e);

        }

        if (!argList.contains("-background")){
            downloader.setVisible(true);
        }

        if (linkPath != null) downloader.showLinkDetectedDialog(linkPath);

        preloadDialog.setVisible(false);




    }
}

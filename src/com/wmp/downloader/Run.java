package com.wmp.downloader;

import com.wmp.downloader.newArchitecture.ParserTaskInfo;
import com.wmp.speed_bump.common.background.tool.StringFormat;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.WebSetter;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.tools.web.TCPControl;
import com.wmp.downloader.ui.Downloader;
import com.wmp.speed_bump.common.ui.PreLoadDialog;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.util.List;

public class Run {
    private static final Logger logger = Logger.getLogger(Run.class);

    public static String VERSION = "0.5.0";

    public static String PLUGIN_SUPPORT_VERSION = "2.0.0";

    public static List<String> argList;

    static void main(String[] args) {
        //加载窗口（以及启动阶段可能出现的弹窗）在此之前就要用上系统外观，
        //否则它们会使用 Swing 默认的 Metal 外观，和主界面差别很大
        applySystemLookAndFeel();

        argList = List.of(args);
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

        //FlatLightLaf.setup();

        var preloadDialog = PreLoadDialog.INSTANCE_CREATOR.create();
        preloadDialog.showDialog();

        logger.info("开始加载");
        Downloader downloader = null;
        try {
            DataControl.load();

            ParserTaskInfo.loadParsers();

            WebSetter.SSLControl(DataControl.get("isUseSSL", false));
            WebSetter.proxies(true);


            ThemeChanger.easyChanger();

            downloader = new Downloader();

            ThemeChanger.easyChanger();

            Thread.ofVirtual().start(() -> {
                try {
                    TCPControl.startServer();
                } catch (Exception ex) {
                    logger.error("服务端启动失败!");
                    JOptionPane.showMessageDialog(null, "服务端启动失败");
                }
            });
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

        preloadDialog.disposeDialog();




    }

    /**
     * 先把外观切到系统默认（Windows / macOS 原生外观）。
     *
     * <p>用户配置的主题要等 {@link DataControl#load()} 之后才知道，所以启动窗口只能用系统外观；
     * 这比 Swing 默认的 Metal 更接近最终界面，也避免了加载窗口与主界面观感割裂。
     * 读取配置后 {@link ThemeChanger#easyChanger()} 会再换成 FlatLaf 主题。</p>
     */
    private static void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            //拿不到系统外观不是什么大问题，继续用默认外观即可
            logger.error("设置系统外观失败，将继续使用默认外观", e);
        }
    }
}

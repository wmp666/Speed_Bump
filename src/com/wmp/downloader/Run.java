package com.wmp.downloader;

import com.wmp.speed_bump.common.UIStart;
import com.wmp.speed_bump.common.background.tools.StringFormat;
import com.wmp.downloader.tools.web.TCPControl;
import com.wmp.speed_bump.common.ui.PreLoadDialog;
import com.wmp.speed_bump.common.background.tools.SBLogger;

import javax.swing.*;
import java.util.List;

public class Run {
    private static final SBLogger logger = SBLogger.getLogger(Run.class);

    public static String VERSION = "0.5.3";

    public static String PLUGIN_SUPPORT_VERSION = "2.0.0";

    public static List<String> argList;

    static void main(String[] args) {

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

        //界面准备必须在加载窗之前：加载窗的 PreloadDialog.form 里有一个 JXBusyLabel，
        //而 Swing 组件的 UI 是构造时就取定的。晚一步会同时踩两个坑——
        //加载动画还是 SwingX 默认的旋转圆点，窗口也按 Metal 外观 pack() 过一次。
        //走 common 层的 UIStart 接口（平台实现在 platform.ui.swing），不直接引用 Swing 实现。
        UIStart.INSTANCE.prepareUi();

        var preloadDialog = PreLoadDialog.INSTANCE_CREATOR.create();
        preloadDialog.showDialog();



        UIStart.INSTANCE.show(argList, linkPath);

        preloadDialog.disposeDialog();


    }

}

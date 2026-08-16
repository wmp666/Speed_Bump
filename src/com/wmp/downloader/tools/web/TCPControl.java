package com.wmp.downloader.tools.web;

import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.ui.Downloader;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.IOException;
import java.net.ServerSocket;

public class TCPControl {

    private static final Logger logger = Logger.getLogger(TCPControl.class);

    private static int port = Integer.parseInt(DataControl.get("port", "5465"));

    private static Thread serverThread;

    public static boolean isHasServer() throws Exception{
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // 能成功绑定，说明端口空闲
            return false;
        } catch (IOException e) {
            // 绑定失败，说明端口被占用
            return true;
        }
    }

    public static void startServer() throws Exception{
        if (isHasServer()) {
            logger.warn("已存在服务端");
            return;
        }
        serverThread = Thread.ofVirtual().name("TCP Listener Thread").start(()-> {
            try {
                TCPServer.create("127.0.0.1", port,
                        (message) ->{
                            var strip = message.strip();
                            if (strip.equalsIgnoreCase("show")){
                                if (Downloader.mainFrame != null) {
                                    Downloader.mainFrame.setVisible(true);
                                }
                            }
                            else if (strip.startsWith("createTask:")) {
                                var TaskInfo = strip.substring(11);
                                if (Downloader.mainFrame != null) {
                                    Downloader.mainFrame.showLinkDetectedDialog(TaskInfo);
                                }else{
                                    JOptionPane.showMessageDialog(null, "服务端未启动,请稍后重试!", StringFormat.translate("warn"), JOptionPane.WARNING_MESSAGE);
                                }

                            }
                });
            } catch (Exception e) {
                logger.error("服务端创建失败");
                JOptionPane.showMessageDialog(null, "服务端创建失败，建议重启程序", StringFormat.translate("error"), JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * 发送数据
     * @param message 信息
     * @return 状态码：-1 - 没有服务端, 0 - 成功, 1 - 发送失败
     */
    public static int sendToServer(String message) throws Exception{
        if (!isHasServer()) {
            logger.warn("未启动服务，无法发送信息");
            return -1;
        }
        try {
            TCPClient.send("127.0.0.1", port, message);
            return 0;
        } catch (Exception e) {
            logger.error("信息发送出错", e);
            return 1;
        }
    }

    static void main() throws Exception {
        var msg = IO.readln("message: ");
        System.out.println(TCPControl.sendToServer(msg));
    }
}

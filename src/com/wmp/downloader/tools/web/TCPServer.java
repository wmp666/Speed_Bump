package com.wmp.downloader.tools.web;

import org.apache.log4j.Logger;

import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.Handler;

public class TCPServer {

    private static final Logger logger = Logger.getLogger(TCPServer.class);

    public static void create(String host, int port, Handler handler) throws Exception {
        InetSocketAddress address = new InetSocketAddress(host, port);

        try (ServerSocket server = new ServerSocket()) {
            server.bind(address);
            logger.info("TCP服务端启动，监听 " + host + ":" + port + " (跨平台)");

            while (true) {
                // 等待客户端连接（阻塞）
                try (var client = server.accept();
                     var reader = new BufferedReader(
                         new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {

                    logger.info("新客户端连接");
                    var ref = new Object() {
                        String line;
                    };
                    // 循环读取，直到客户端关闭连接（readLine() 返回 null）
                    while ((ref.line = reader.readLine()) != null) {
                        logger.info("收到: " + ref.line);
                        Thread.ofVirtual().start(()->handler.handle(ref.line));
                        // 可选回复
                        //writer.println("服务端已收到: " + line);
                    }
                    logger.info("客户端断开");
                } catch (Exception e) {
                    logger.error("处理客户端异常", e);
                }
            }
        }
    }

    public interface Handler{
        void handle(String message);
    }
}
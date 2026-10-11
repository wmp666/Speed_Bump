package com.wmp.speed_bump.platform.background.pc.tools.web;

import java.net.Socket;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class TCPClient {
    public static void send(String host, int port, String message) throws Exception {
        try (Socket client = new Socket(host, port);
             PrintWriter writer = new PrintWriter(
                 client.getOutputStream(), true, StandardCharsets.UTF_8)) {

            writer.println(message);
            System.out.println("已发送: " + message);

        }
    }
}
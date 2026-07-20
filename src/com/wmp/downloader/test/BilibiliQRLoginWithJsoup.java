package com.wmp.downloader.test;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import com.alibaba.fastjson2.JSONObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class BilibiliQRLoginWithJsoup {

    // 保存登录成功后获取的 Cookie（包含 SESSDATA）
    private static String cookies = "";

    public static void main(String[] args) throws Exception {
        getCookies();
    }

    public static String getCookies() throws Exception {
        String qrcodeKey = fetchQRCode();
        if (qrcodeKey == null) {
            System.out.println("获取二维码失败");
            return null;
        }

        String sessData = pollLoginStatus(qrcodeKey);
        if (sessData != null) {
            System.out.println("登录成功！SESSDATA: " + sessData);
            // 现在可以使用 cookies 调用其他 API
        } else {
            System.out.println("登录失败或超时");
        }
        return sessData;
    }

    // 步骤1：申请二维码
    private static String fetchQRCode() throws Exception {
        String url = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate";

        Connection.Response response = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .ignoreContentType(true)   // 返回 JSON，非 HTML
                .followRedirects(false)    // 禁用重定向（可选）
                .method(Connection.Method.GET)
                .execute();

        String json = response.body();
        JSONObject jsonObject = JSONObject.parseObject(json);
        if (jsonObject.getInteger("code") == 0) {
            JSONObject data = jsonObject.getJSONObject("data");
            String qrcodeKey = data.getString("qrcode_key");
            String qrcodeUrl = data.getString("url");

            // 生成二维码图片
            var bufferedImage = generateQRCodeImage(qrcodeUrl, 300, 300);
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(new JLabel(new ImageIcon(bufferedImage)));
            frame.pack();
            frame.setVisible(true);
            System.out.println("二维码已生成，请使用 B站手机App 扫描");
            return qrcodeKey;
        }
        return null;
    }

    // 生成二维码图片（不变）
    private static BufferedImage generateQRCodeImage(String text, int width, int height) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    // 步骤2：轮询登录状态
    private static String pollLoginStatus(String qrcodeKey) throws InterruptedException {
        String pollUrl = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=" + qrcodeKey;
        System.out.println("轮询登录状态URL: " + pollUrl);

        // 轮询最多 60 次，每次间隔 3 秒（共 180 秒）
        for (int i = 0; i < 60; i++) {
            try {
                Connection.Response response = Jsoup.connect(pollUrl)
                        .userAgent("Mozilla/5.0")
                        .ignoreContentType(true)
                        .followRedirects(false)
                        .method(Connection.Method.GET)
                        .execute();

                // 提取 Set-Cookie（可能包含多个，这里取全部）
                String setCookie = response.header("Set-Cookie");
                if (setCookie != null) {
                    cookies = setCookie;
                }

                String json = response.body();
                JSONObject jsonObject = JSONObject.parseObject(json);
                int code = jsonObject.getJSONObject("data").getInteger("code");
                switch (code) {
                    case 0:
                        // 登录成功
                        return extractSESSDATA(cookies);
                    case 86038:
                        System.out.println("二维码已过期，请重新获取");
                        return null;
                    case 86090:
                        System.out.println("已扫码，请在手机上确认...");
                        break;
                    case 86101:
                        System.out.println("等待扫码...");
                        break;
                    default:
                        System.out.println("未知状态码: " + code);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            Thread.sleep(3000);
        }
        return null;
    }

    // 从 Cookie 字符串中提取 SESSDATA
    private static String extractSESSDATA(String cookieHeader) {
        if (cookieHeader == null) return null;
        for (String part : cookieHeader.split(";")) {
            part = part.trim();
            if (part.startsWith("SESSDATA=")) {
                // SESSDATA 的值可能包含 path 等，只取第一个分号前
                String value = part.substring("SESSDATA=".length());
                return value.split(";")[0];
            }
        }
        return null;
    }
}
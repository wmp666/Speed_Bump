package com.wmp.downloader.ui.settings.bilibili;

import com.alibaba.fastjson2.JSONObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.wmp.downloader.tools.DataControl;
import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;

public class BiliScanCodePanel extends JPanel {

    private static final Logger logger = Logger.getLogger(BiliScanCodePanel.class);

    private JLabel QRLabel;
    private JButton QRRefreshButton;
    private JButton linkButton;
    private JPanel mainPanel;
    private JLabel QRStatusLabel;

    private String loginURL;

    public BiliScanCodePanel() {
        this.setLayout(new BorderLayout());
        this.add(mainPanel, BorderLayout.CENTER);

        QRRefreshButton.putClientProperty("FlatLaf.style", "font: $h2.font");
        linkButton.putClientProperty("FlatLaf.style", "font: $h2.font");

        refreshQR();

        QRRefreshButton.addActionListener(e -> {
            refreshQR();
        });
        linkButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(URI.create(loginURL));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "无法打开浏览器", "错误", JOptionPane.ERROR_MESSAGE);
                logger.error("无法打开浏览器", ex);
            }
        });
    }



    // 保存登录成功后获取的 Cookie（包含 SESSDATA）
    private static String cookies = "";

    private void refreshQR(){
        Thread.ofVirtual().start(()->{
            try {
                String qrcodeKey = fetchQRCode();
                if (qrcodeKey == null) {
                    logger.error("获取二维码失败");
                    JOptionPane.showMessageDialog(null, "获取二维码失败", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String sessData = pollLoginStatus(qrcodeKey);
                if (sessData != null) {
                    logger.info("登录成功！SESSDATA: " + sessData);
                    //保存
                    DataControl.put("bili_sessdata", sessData);
                    DataControl.save();
                    // 现在可以使用 cookies 调用其他 API
                } else {
                    logger.error("登录失败或超时");
                }
            } catch (Exception e) {
                logger.error("登录过程中发生错误", e);
                JOptionPane.showMessageDialog(null, "登录过程中发生错误", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // 步骤1：申请二维码
    private String fetchQRCode() throws Exception {
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
            this.loginURL = data.getString("url");

            // 生成二维码图片
            QRLabel.setIcon(new ImageIcon(generateQRCodeImage(this.loginURL, (int) (QRRefreshButton.getPreferredSize().getWidth() * 1.2))));
            QRStatusLabel.setText("二维码已生成");
            logger.info("二维码已生成");
            return qrcodeKey;
        }
        return null;
    }

    /** 生成二维码图片
     * @param text 要编码的文本（https）
     * @param size 二维码图片的大小
     * @return 二维码图片
     * @throws Exception 如果生成二维码图片时发生错误
     */
    private BufferedImage generateQRCodeImage(String text, int size) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    // 步骤2：轮询登录状态
    private String pollLoginStatus(String qrcodeKey) throws InterruptedException {
        String pollUrl = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=" + qrcodeKey;
        System.out.println("轮询登录状态URL: " + pollUrl);

        // 轮询最多 90 次，每次间隔 2 秒（共 180 秒）
        for (int i = 0; i < 90; i++) {
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
                    case 0 -> {
                        // 登录成功
                        QRStatusLabel.setText("登录成功");
                        return extractSESSDATA(cookies);
                    }
                    case 86038 -> {
                        QRStatusLabel.setText("二维码已过期，请重新获取");
                        logger.warn("二维码已过期，请重新获取");
                        return null;
                    }
                    case 86090 -> {
                        QRStatusLabel.setText("已扫码，请在手机上确认...");
                        logger.info("已扫码，请在手机上确认...");
                    }
                    case 86101 -> {
                        QRStatusLabel.setText("等待扫码...");
                        logger.info("等待扫码...");
                    }

                    default -> {
                        QRStatusLabel.setText("未知状态码: " + code);
                        logger.error("未知状态码: " + code);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            Thread.sleep(2000);
        }
        return null;
    }

    // 从 Cookie 字符串中提取 SESSDATA
    private String extractSESSDATA(String cookieHeader) {
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

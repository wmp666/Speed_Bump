package com.wmp.downloader.test;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document; // 注意：虽然返回JSON，但Jsoup可处理

public class BilibiliUserInfo {
    // 你的SESSDATA
    private static final String SESSDATA;

    static {
        try {
            SESSDATA = BilibiliQRLoginWithJsoup.getCookies();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        String url = "https://api.bilibili.com/x/web-interface/nav";

        Connection.Response response = Jsoup.connect(url)
                .header("Cookie", "SESSDATA=" + SESSDATA) // 关键：在Header中设置Cookie
                .header("User-Agent", "Mozilla/5.0")      // 设置UA，模拟浏览器
                .ignoreContentType(true)                  // 忽略内容类型，处理JSON
                .method(Connection.Method.GET)
                .execute();

        // 打印响应内容，实际开发中可用JSON库（如Fastjson）解析
        System.out.println(response.body());
    }
}

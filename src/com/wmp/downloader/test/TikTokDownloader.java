package com.wmp.downloader.test;

import com.alibaba.fastjson2.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Scanner;

public class TikTokDownloader {

    static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);
        System.out.print("请输入视频分享URL：");
        String videoPageUrl = scanner.nextLine();
        // 2. 访问视频页面 https://api.yujn.cn/api/dy_jx.php?msg=
        HttpURLConnection conn = (HttpURLConnection) URI.create("https://api.yujn.cn/api/dy_jx.php?msg=" + videoPageUrl).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream in = conn.getInputStream();
            var temp = new String(in.readAllBytes());
            JSONObject jsonObject = JSONObject.parseObject(temp);
            System.out.println(jsonObject.getString("title"));
            System.out.println(jsonObject.getString("play_video"));
        } else {
            System.out.println("无法访问视频页面，响应码：" + conn.getResponseCode());
        }

    }

    // 下载视频的方法
    public static void downloadVideo(String videoUrl, String destinationFile) {

    }
}

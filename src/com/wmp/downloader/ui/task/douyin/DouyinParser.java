package com.wmp.downloader.ui.task.douyin;

import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.tools.download.URLDownloadTool;
import com.wmp.downloader.ui.task.Parser;
import com.wmp.downloader.ui.task.createTask.LinkFileInfoPanel;
import com.wmp.downloader.ui.task.createTask.LinkFolderInfoPanel;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class DouyinParser extends Parser {

    Logger logger = Logger.getLogger(DouyinParser.class);

    @Override
    public JPanel parse(String content) {
        try {
            //请求链接（图集，视频）：https://api.yujn.cn/api/dy_jx.php?msg=
            var conn = (HttpURLConnection) URI.create("https://api.yujn.cn/api/dy_jx.php?msg=" + content).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9.2.15)");
            conn.connect();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String jsonText = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                var jsonObject = JSONObject.parseObject(jsonText);
                System.out.println(jsonObject);


                if (jsonObject.getString("type").equals("视频")) {
                    //获取视频大小

                    var url = jsonObject.getString("play_video");
                    return LinkFileInfoPanel.createBasicLinkFileInfoPanel(
                            jsonObject.getString("title") + ".mp4",
                            URLDownloadTool.getFileSize(url),
                            "douyin",
                            url
                    );
                } else if (jsonObject.getString("type").equals("图集")) {
                    long[] sizes = new long[jsonObject.getJSONArray("images").size()];
                    for (int i = 0; i < sizes.length; i++) {
                        sizes[i] = URLDownloadTool.getFileSize(jsonObject.getJSONArray("images").getString(i));
                    }
                    String[] images = new String[jsonObject.getJSONArray("images").size()];
                    for (int i = 0; i < images.length; i++) {
                        images[i] = jsonObject.getJSONArray("images").getString(i);
                    }
                    String[] names = new String[jsonObject.getJSONArray("images").size()];
                    String[] types = new String[names.length];
                    for (int i = 0; i < names.length; i++) {
                        String imageName = URLDownloadTool.extractFileName(images[i]);

                        var tempStringList = imageName.split("\\.");
                        types[i] = tempStringList.length <= 1 ? "None" : tempStringList[tempStringList.length - 1];
                        names[i] = i + "." + types[i];
                    }
                    return LinkFolderInfoPanel.createBasicLinkFolderInfoPanel(
                            jsonObject.getString("title"),
                            sizes,
                            "douyin",
                            images,
                            names,
                            types
                    );
                }
            }


        } catch (Exception e) {
            logger.error("抖音链接解析失败", e);
        }


        return null;
    }
}

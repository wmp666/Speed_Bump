package com.wmp.downloader.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wmp.downloader.tools.DataControl;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BiliVideoInfo {

    private static Logger logger = Logger.getLogger(BiliVideoInfo.class);
    static void main() {
        var bv = IO.readln("输入BV：");


        try {
            String sessdata = DataControl.get("bili_sessdata", "");

            URL url = URI.create("https://api.bilibili.com/x/web-interface/view?bvid=" + bv).toURL();
            HttpURLConnection conn1 = (HttpURLConnection) url.openConnection();
            conn1.setRequestMethod("GET");
            conn1.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9.2.15)");
            if (!sessdata.isEmpty()) {
                conn1.setRequestProperty("Cookie", "SESSDATA=" + sessdata);
            }
            conn1.setConnectTimeout(5000);

            String jsonText = new String(conn1.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JSONObject json = JSON.parseObject(jsonText);
            logger.info("JSON数据: " + json.toJSONString());

            int code = json.getIntValue("code");
            if (code != 0) {
                logger.error("API返回错误: " + json.getString("message"));
            }

            JSONObject data = json.getJSONObject("data");
            logger.info("视频信息: " + data.toJSONString());

            var pages = data.getJSONArray("pages");
            long[] cids = new long[pages.size()];
            String[] titles = new String[pages.size()];
            for (int i = 0; i < pages.size(); i++) {
                cids[i] = pages.getJSONObject(i).getLong("cid");
                titles[i] = pages.getJSONObject(i).getString("part");
            }
            logger.info("CIDs: " + Arrays.toString(cids));
            logger.info("Titles: " + Arrays.toString(titles));

            String videoInfoUrl = "https://api.bilibili.com/x/player/playurl?otype=json&fnver=0&fnval=16&player=1&qn=64&bvid=" + bv + "&cid=" + cids[0];


            HttpURLConnection conn2 = (HttpURLConnection) URI.create(videoInfoUrl).toURL().openConnection();
            conn2.setRequestMethod("GET");
            conn2.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9.2.15)");
            if (!sessdata.isEmpty()) {
                conn2.setRequestProperty("Cookie", "SESSDATA=" + sessdata);
            }
            conn2.setConnectTimeout(5000);
            String videoInfoJsonText = new String(conn2.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JSONObject videoInfoJson = JSON.parseObject(videoInfoJsonText);
            logger.info("视频信息: " + videoInfoJson.toJSONString());

            //仅用BV号获取的
            String title = data.getString("title");
            logger.info("视频/合集标题: " + title);



            if (cids.length == 1) {
                JSONObject durlObj = videoInfoJson.getJSONObject("data").getJSONArray("durl").getJSONObject(0);
                long size = durlObj.getLongValue("size");
                String videoUrl = durlObj.getString("url");
                logger.info("文件大小: " + size + " bytes");
            }


        } catch (IOException e) {
            logger.error("获取视频信息出错: " + e.getMessage());
        }
    }
}

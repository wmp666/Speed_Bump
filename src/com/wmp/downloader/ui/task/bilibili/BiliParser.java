package com.wmp.downloader.ui.task.bilibili;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.ui.task.Parser;
import com.wmp.downloader.ui.task.createTask.LinkFileInfoPanel;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BiliParser extends Parser {

    private static final Logger logger = Logger.getLogger(BiliParser.class);

    /**
     * 解析视频信息
     *
     * @param link 视频链接（http/bv）
     * @return 视频信息 url-视频下载链接
     */
    @Override
    public JPanel parse(String link) {
        // 从数据中解析出视频的bvid
        String BVId = "";
        if (link.strip().startsWith("BV")) {
            BVId = link.strip();
        }else{
            //https://www.bilibili.com/video/BV1uUKV6zE9R/?spm_id_from=333.1007.tianma.1-2-2.click&vd_source=1ab658dba666f92347c26ce08c448bd5
            Matcher matcher = Pattern.compile("(BV[A-Za-z0-9]+)").matcher(link);
            if (matcher.find()) {
                BVId = matcher.group(1);
            }
        }
        logger.info("BVId: " + BVId);
        //解析视频信息的链接 https://api.bilibili.com/x/web-interface/view?bvid=

        try {
            String sessdata = DataControl.get("bili_sessdata", "");

            URL url = URI.create("https://api.bilibili.com/x/web-interface/view?bvid=" + BVId).toURL();
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
                return new BiliLinkFileInfoPanel("", 0, "", 0, "");
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

            String videoInfoUrl = "https://api.bilibili.com/x/player/playurl?otype=json&fnver=0&fnval=2&player=1&qn=64&bvid=" + BVId + "&cid=" + cids[0];


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



            if (cids.length == 1){
                JSONObject durlObj = videoInfoJson.getJSONObject("data").getJSONArray("durl").getJSONObject(0);
                long size = durlObj.getLongValue("size");
                String videoUrl = durlObj.getString("url");
                logger.info("文件大小: " + size + " bytes");
                return new BiliLinkFileInfoPanel(title + ".mp4", size, BVId, cids[0], videoUrl);
            }else{
                return new BiliLinkFolderInfoPanel(title, BVId, cids, titles);
            }


        } catch (IOException e) {
            logger.error("获取视频信息出错: " + e.getMessage());
        }

            return new BiliLinkFileInfoPanel("", 0, "", 0, "");
    }


}

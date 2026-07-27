package com.wmp.downloader.ui.task.bilibili;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.ui.task.Parser;
import com.wmp.downloader.ui.task.bilibili.file.BiliLinkFileInfoPanel;
import com.wmp.downloader.ui.task.bilibili.folder.BiliLinkFolderInfoPanel;
import com.wmp.downloader.ui.task.bilibili.info.BiliAudioInfo;
import com.wmp.downloader.ui.task.bilibili.info.BiliDownloadInfo;
import com.wmp.downloader.ui.task.bilibili.info.BiliVideoInfo;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
                return null;
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



            //仅用BV号获取的
            String title = data.getString("title");
            logger.info("视频/合集标题: " + title);



            if (cids.length == 1){
                var downloadInfo = getDownloadInfo(BVId, cids[0], sessdata);
                logger.info("下载信息: " + downloadInfo);
                return new BiliLinkFileInfoPanel(title + ".mp4", downloadInfo);
            }else{
                BiliDownloadInfo[] downloadInfos = new BiliDownloadInfo[cids.length];
                for (int i = 0; i < cids.length; i++) {
                    downloadInfos[i] = getDownloadInfo(BVId, cids[i], sessdata);
                }
                return new BiliLinkFolderInfoPanel(title, titles, downloadInfos);
            }


        } catch (Exception e) {
            ToastMessage.show(null, "获取视频信息出错", ToastMessage.ERROR);
            logger.error("获取视频信息出错", e);
        }

            return null;
    }

    private static BiliDownloadInfo getDownloadInfo(String BVId, long cid, String sessdata){
        try {
            //获取每个视频中的数据
            String videoInfoUrl = "https://api.bilibili.com/x/player/playurl?otype=json&fnver=0&fnval=16&player=1&qn=64&bvid=" + BVId + "&cid=" + cid;


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

            JSONObject dashObj = videoInfoJson.getJSONObject("data").getJSONObject("dash");

            //获取分流的视频，音频数据
            ArrayList<BiliVideoInfo> videoInfoList = new ArrayList<>();
            ArrayList<BiliAudioInfo> audioInfoList = new ArrayList<>();

            double duration = dashObj.getDouble("duration");

            var videoArray = dashObj.getJSONArray("video");
            for (int i = 0; i < videoArray.size(); i++) {
                JSONObject jsonObject = videoArray.getJSONObject(i);
                var baseUrl = jsonObject.getString("base_url");
                BiliVideoInfo videoInfo = new BiliVideoInfo(jsonObject.getIntValue("codecid"),
                        jsonObject.getIntValue("id"),
                        baseUrl, getFileSize(duration, jsonObject.getIntValue("bandwidth")));
                videoInfoList.add(videoInfo);
            }

            var audioArray = dashObj.getJSONArray("audio");
            for (int i = 0; i < audioArray.size(); i++) {
                JSONObject jsonObject = audioArray.getJSONObject(i);
                var baseUrl = jsonObject.getString("baseUrl");
                BiliAudioInfo audioInfo = new BiliAudioInfo(jsonObject.getIntValue("codecid"),
                        jsonObject.getIntValue("id"),
                        baseUrl, getFileSize(duration, jsonObject.getIntValue("bandwidth")));
                audioInfoList.add(audioInfo);
            }

            return new BiliDownloadInfo(videoInfoList.toArray(BiliVideoInfo[]::new), audioInfoList.toArray(BiliAudioInfo[]::new));
        } catch (Exception e) {
            throw new RuntimeException("获取视频数据失败！BV=" + BVId + " CID=" + cid, e);
        }
    }

    private static long getFileSize(double duration, int bandwidth) {
        return (long) (duration * bandwidth / 8.0);
    }

}

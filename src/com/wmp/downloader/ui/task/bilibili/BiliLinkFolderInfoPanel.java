package com.wmp.downloader.ui.task.bilibili;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.ui.FunctionDialog;
import com.wmp.downloader.ui.task.createTask.LinkFolderInfoPanel;
import org.apache.log4j.Logger;

import java.awt.event.ActionEvent;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class BiliLinkFolderInfoPanel extends LinkFolderInfoPanel {

    private static Logger logger = Logger.getLogger(BiliLinkFolderInfoPanel.class);

    private int[] qualities;
    private String[] qualityStrList;
    private int selectionQuality = 64;

    private final String BVID;
    private final long[] cids;


    public BiliLinkFolderInfoPanel(String folderName, String BVID, long[] cids, String[] pagesTitles) {
        //根据传入的数据生成各个文件的大小和URL（默认720P）

        var videoInfo = initAllVideoInfo(64, BVID, cids);

        var fileTypes = new String[cids.length];
        Arrays.fill(fileTypes, "mp4");
        super(folderName, videoInfo.sizes, "bilibili", videoInfo.urls, pagesTitles, fileTypes);

        this.BVID = BVID;
        this.cids = cids;
        this.qualities = videoInfo.qualities;
        this.qualityStrList = videoInfo.qualityStrList;
    }

    private static videoInfo initAllVideoInfo(int quality, String BVId, long[] cids){

        ArrayList<Long> sizes = new ArrayList<>();
        ArrayList<String> urls = new ArrayList<>();
//        long[] sizes = new long[0];
//        String[] urls = new String[0];
        ArrayList<Integer> qualities = new ArrayList<>(){
            @Override
            public boolean add(Integer integer) {
                if (!contains(integer))
                    return super.add(integer);
                return false;
            }
        };
        ArrayList<String> qualityStrList = new ArrayList<>();
        try {


            String sessdata = DataControl.get("bili_sessdata", "");
            for (var i = 0; i < cids.length; i++) {
                String videoInfoUrl = "https://api.bilibili.com/x/player/playurl?otype=json&fnver=0&fnval=2&player=1&qn=" + quality + "&bvid=" + BVId + "&cid=" + cids[i];
                logger.info( i + "个视频信息链接: " + videoInfoUrl);


                HttpURLConnection conn2 = (HttpURLConnection) URI.create(videoInfoUrl).toURL().openConnection();
                conn2.setRequestMethod("GET");
                conn2.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9.2.15)");
                if (!sessdata.isEmpty()) {
                    conn2.setRequestProperty("Cookie", "SESSDATA=" + sessdata);
                }
                conn2.setConnectTimeout(5000);
                String videoInfoJsonText = new String(conn2.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                //真正需要的视频数据在data中
                JSONObject videoInfoJson = JSON.parseObject(videoInfoJsonText);
                JSONObject data = videoInfoJson.getJSONObject("data");
                logger.info(i + "个视频信息: " + data.toJSONString());

                long size = data.getJSONArray("durl").getJSONObject(0).getLong("size");
                String url = data.getJSONArray("durl").getJSONObject(0).getString("url");

                data.getJSONArray("support_formats").forEach(item -> {
                    if (item instanceof JSONObject jsonObject) {
                        if (qualities.add(jsonObject.getIntValue("quality"))) {
                            qualityStrList.add(jsonObject.getString("display_desc"));
                        }

                    }
                });

                sizes.add(size);
                urls.add(url);

            }
        } catch (Exception e) {
            logger.error("获取视频信息失败", e);
        }
        logger.info("视频大小：" + sizes);
        logger.info("视频URL：" + urls);
        return new videoInfo(sizes.stream().mapToLong(Long::longValue).toArray(), urls.toArray(new String[0]),
                qualities.stream().mapToInt(Integer::intValue).toArray(), qualityStrList.toArray(new String[0]));
    }

    record videoInfo(long[] sizes, String[] urls, int[] qualities, String[] qualityStrList){

    }

    @Override
    public void editButtonAction(ActionEvent e) {
        var taskFileEditPanel = new BiliTaskFolderEditPanel(folderNameLabel.getText(), this.selectionQuality, this.qualities, this.qualityStrList);
        FunctionDialog.showDialog(this, "编辑任务信息", taskFileEditPanel,
                result -> {
                    if (result == FunctionDialog.RESULT_SAVE) {
                        folderNameLabel.setText(taskFileEditPanel.getFileName());
                        this.selectionQuality = taskFileEditPanel.getQuality();

                        var videoInfo = initAllVideoInfo(this.selectionQuality, this.BVID, this.cids);
                        this.allFileSizes = videoInfo.sizes;
                        this.AllUrls = videoInfo.urls;

                        long tempSize = 0;
                        for (int i = 0; i < this.allFileSizes.length; i++) {
                            if (this.fileSelectionStatus[i])
                                tempSize += this.allFileSizes[i];
                        }
                        folderNameLabel.setText(taskFileEditPanel.getFileName());
                        sizeLabel.setText(formatFileSize(tempSize));
                    }
                },
                FunctionDialog.SAVE_CANCEL_BUTTONS, 0, null, 0);
    }

    @Override
    public void fileChangeAction() {
        var videoInfo = initAllVideoInfo(64, BVID, cids);
    }


}

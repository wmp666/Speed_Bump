package com.wmp.downloader.ui.task.bilibili;

import com.wmp.downloader.tools.BiliInfoFormat;
import com.wmp.downloader.ui.task.bilibili.info.BiliDownloadInfo;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import static com.wmp.downloader.ui.task.createTask.LinkFileInfoPanel.formatFileSize;

public class BiliTaskFileEditPanel extends JPanel{

    private static final Logger logger = Logger.getLogger(BiliTaskFileEditPanel.class);

    private JTextField NameTextField;
    private JPanel mainPanel;
    private JLabel sizeLabel;
    private JComboBox<String> VideoQualityComboBox;
    private JComboBox<String> SoundQualityComboBox;
    private JComboBox<String> VideoCodecsComboBox;
    //
//    private String BVID = ""    ;
//    private long cid;
//    private int[] quality_int = new int[0];

    private long size = 0;


    public BiliTaskFileEditPanel(String name, BiliDownloadInfo downloadInfo, int videoInfoIndex, int audioInfoIndex) {
        this.setLayout(new BorderLayout());
        this.add(mainPanel);
//
//        this.BVID = BVID;
//        this.cid = cid;

        NameTextField.setText(name);

        size = downloadInfo.videoInfos()[videoInfoIndex].size() +
                downloadInfo.audioInfos()[audioInfoIndex].size();
        sizeLabel.setText(formatFileSize(size));

        //添加值
        for (var i = 0; i < downloadInfo.videoInfos().length; i++) {
            VideoQualityComboBox.addItem(
                    BiliInfoFormat.VideoFormat(downloadInfo.videoInfos()[i].quality()) + " "
            + BiliInfoFormat.getVideoCode(downloadInfo.videoInfos()[i].codecid())) ;

        }
        VideoQualityComboBox.setSelectedIndex(videoInfoIndex);
        for (var i = 0; i < downloadInfo.audioInfos().length; i++) {
            SoundQualityComboBox.addItem(
                    BiliInfoFormat.AudioFormat(downloadInfo.audioInfos()[i].bitrate())) ;

        }
        SoundQualityComboBox.setSelectedIndex(audioInfoIndex);
        //添加监听
        VideoQualityComboBox.addItemListener(e ->{
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                int qualityIndex = VideoQualityComboBox.getSelectedIndex();
                if (qualityIndex >= 0 && qualityIndex < downloadInfo.videoInfos().length) {
                    size = downloadInfo.videoInfos()[qualityIndex].size() +
                            downloadInfo.audioInfos()[SoundQualityComboBox.getSelectedIndex()].size();
                    sizeLabel.setText(formatFileSize(size));
                }
            }
        });

        SoundQualityComboBox.addItemListener(e ->{
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                int audioIndex = SoundQualityComboBox.getSelectedIndex();
                if (audioIndex >= 0 && audioIndex < downloadInfo.audioInfos().length) {
                    size = downloadInfo.videoInfos()[VideoQualityComboBox.getSelectedIndex()].size() +
                            downloadInfo.audioInfos()[audioIndex].size();
                    sizeLabel.setText(formatFileSize(size));
                }
            }
        });


        /*initSelectedVideoInfo(cid);
        initSelectedQualityInfo(cid, quality_int[0]);

        VideoQualityComboBox.addItemListener(e -> {
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                int qualityIndex = VideoQualityComboBox.getSelectedIndex();
                if (qualityIndex >= 0 && qualityIndex < quality_int.length) {
                    initSelectedQualityInfo(cid, quality_int[qualityIndex]);
                }
            }
        });*/
    }

    /*public void initSelectedVideoInfo(long cid){
        try {

            String sessdata = DataControl.get("bili_sessdata", "");
            String videoInfoUrl = "https://api.bilibili.com/x/player/playurl?otype=json&fnver=0&fnval=2&player=1&qn=64&bvid=" + this.BVID + "&cid=" + cid;
            logger.info("视频信息链接: " + videoInfoUrl);


            HttpURLConnection conn2 = (HttpURLConnection) URI.create(videoInfoUrl).toURL().openConnection();
            conn2.setRequestMethod("GET");
            conn2.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9.2.15)");
            if (!sessdata.isEmpty()) {
                conn2.setRequestProperty("Cookie", "SESSDATA=" + sessdata);
            }
            conn2.setConnectTimeout(5000);
            String videoInfoJsonText = new String(conn2.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JSONObject videoInfoJson = JSON.parseObject(videoInfoJsonText).getJSONObject("data");
            logger.info("视频信息: " + videoInfoJson.toJSONString());

            var supportQuality = videoInfoJson.getJSONArray("support_formats");
            VideoQualityComboBox.removeAllItems();
            quality_int = new int[supportQuality.size()];
            for (int i = 0; i < supportQuality.size(); i++) {
                VideoQualityComboBox.addItem(supportQuality.getJSONObject(i).getString("display_desc"));
                quality_int[i] = supportQuality.getJSONObject(i).getIntValue("quality");
            }

            JSONObject durlObj = videoInfoJson.getJSONArray("durl").getJSONObject(0);
            this.size = durlObj.getLongValue("size");
            this.videoUrl = durlObj.getString("url");
            logger.info("文件大小: " + formatFileSize(this.size));
        } catch (IOException e) {
            logger.error("获取视频信息失败", e);
        }
    }

    public void initSelectedQualityInfo(long cid, int quality){
        try {
            String sessdata = DataControl.get("bili_sessdata", "");
            String videoInfoUrl = "https://api.bilibili.com/x/player/playurl?otype=json&fnver=0&fnval=2&player=1&qn=" + quality + "&bvid=" + this.BVID + "&cid=" + cid;
            logger.info("视频信息链接: " + videoInfoUrl);

            HttpURLConnection conn2 = (HttpURLConnection) URI.create(videoInfoUrl).toURL().openConnection();
            conn2.setRequestMethod("GET");
            conn2.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9.2.15)");
            if (!sessdata.isEmpty()) {
                conn2.setRequestProperty("Cookie", "SESSDATA=" + sessdata);
            }
            conn2.setConnectTimeout(5000);
            String videoInfoJsonText = new String(conn2.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JSONObject videoInfoJson = JSON.parseObject(videoInfoJsonText).getJSONObject("data");
            logger.info("视频信息: " + videoInfoJson.toJSONString());

            JSONObject durlObj = videoInfoJson.getJSONArray("durl").getJSONObject(0);
            this.size = durlObj.getLongValue("size");
            this.videoUrl = durlObj.getString("url");
            logger.info("文件大小: " + formatFileSize(this.size));
            sizeLabel.setText(formatFileSize(this.size));
        } catch (Exception e) {
            logger.error("获取视频信息失败", e);
        }
    }*/

    public String getFileName() {
        return NameTextField.getText();
    }

    public void setFileName(String name) {
        NameTextField.setText(name);
    }

    public long getFileSizeNum() {
        return this.size;
    }
    public int getVideoInfoIndex(){
        return VideoQualityComboBox.getSelectedIndex();
    }
    public int getAudioInfoIndex(){
        return SoundQualityComboBox.getSelectedIndex();
    }

}

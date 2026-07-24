package com.wmp.downloader.ui.task.bilibili.folder;

import com.wmp.downloader.exception.BiliDownloadTaskException;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.BiliInfoFormat;
import com.wmp.downloader.ui.FunctionDialog;
import com.wmp.downloader.ui.task.bilibili.info.BiliDownloadInfo;
import com.wmp.downloader.ui.task.createTask.LinkFolderInfoPanel;
import org.apache.log4j.Logger;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BiliLinkFolderInfoPanel extends LinkFolderInfoPanel {

    private static Logger logger = Logger.getLogger(BiliLinkFolderInfoPanel.class);

    private BiliDownloadInfo[] downloadInfos;

    private int videoQualityInfoIndex = 0;
    private int audioQualityInfoIndex = 0;

    private ArrayList<String> allVideoUrlList = new ArrayList<>();
    private ArrayList<String> allAudioUrlList = new ArrayList<>();

    private final long[][] sizes;

    //private int[] qualities;
    //private String[] qualityStrList;
    //private int selectionQuality = 64;

    //private final String BVID;
    //private final long[] cids;

    public BiliLinkFolderInfoPanel(String folderName, String[] partNames, BiliDownloadInfo[] downloadInfos) {
        if (downloadInfos == null) throw new BiliDownloadTaskException("下载信息不能为空");
        long[] tempSizes = new long[downloadInfos.length];
        for (var i = 0; i < downloadInfos.length; i++) {
            tempSizes[i] = downloadInfos[i].videoInfos()[0].size() + downloadInfos[i].audioInfos()[0].size();
        }
        String[] tempUrls = new String[downloadInfos.length];
        for (var i = 0; i < downloadInfos.length; i++) {
            tempUrls[i] = downloadInfos[i].videoInfos()[0].url();
        }
        var fileTypes = new String[downloadInfos.length];
        Arrays.fill(fileTypes, "mp4");
        super(folderName, tempSizes, "bilibili", tempUrls, partNames, fileTypes);

        this.sizes = new long[downloadInfos.length][2];
        this.downloadInfos = downloadInfos;
    }


    @Override
    public void editButtonAction(ActionEvent e) {
        //更新所有数据
        var taskFileEditPanel = new BiliTaskFolderEditPanel(folderNameLabel.getText(), this.downloadInfos, this.videoQualityInfoIndex, this.audioQualityInfoIndex);
        FunctionDialog.showDialog(this, StringFormat.translate("task", "task.create_task.download_settings.task_edit"), taskFileEditPanel,
                result -> {
                    if (result == FunctionDialog.RESULT_SAVE) {
                        folderNameLabel.setText(taskFileEditPanel.getFileName());

                        this.videoQualityInfoIndex = taskFileEditPanel.getVideoInfoIndex();
                        this.audioQualityInfoIndex = taskFileEditPanel.getAudioInfoIndex();

                        //清理
                        allVideoUrlList.clear();
                        allAudioUrlList.clear();

                        //添加
                        allVideoUrlList.addAll(List.of(taskFileEditPanel.getVideoUrl()));
                        allAudioUrlList.addAll(List.of(taskFileEditPanel.getAudioUrl()));

                        //修改显示的文件大小
                        resetSize();
                    }
                },
                FunctionDialog.SAVE_CANCEL_BUTTONS, 0, null, 0);
    }

    @Override
    public void selectionFileListChangeAction() {
        //选择的文件发生改变
        resetSize();
    }

    /**
     * 刷新每个下载信息中的视频，音频大小（已选的质量信息对应的）
     */
    private void resetSizes(){
        ArrayList<String> uniqueVideoQualities = new ArrayList<>();
        for (var info : downloadInfos) {
            for (var vi : info.videoInfos()) {
                String qStr = BiliInfoFormat.VideoFormat(vi.quality()) + " "
                        + BiliInfoFormat.getVideoCode(vi.codecid());
                if (!uniqueVideoQualities.contains(qStr)) {
                    uniqueVideoQualities.add(qStr);
                }
            }
        }

        ArrayList<String> uniqueAudioQualities = new ArrayList<>();
        for (var info : downloadInfos) {
            for (var ai : info.audioInfos()) {
                String qStr = BiliInfoFormat.AudioFormat(ai.bitrate());
                if (!uniqueAudioQualities.contains(qStr)) {
                    uniqueAudioQualities.add(qStr);
                }
            }
        }

        String selectedVideoQ = (videoQualityInfoIndex < uniqueVideoQualities.size())
                ? uniqueVideoQualities.get(videoQualityInfoIndex) : null;
        String selectedAudioQ = (audioQualityInfoIndex < uniqueAudioQualities.size())
                ? uniqueAudioQualities.get(audioQualityInfoIndex) : null;

        for (int i = 0; i < downloadInfos.length; i++) {
            int vIdx = 0;
            if (selectedVideoQ != null) {
                for (int j = 0; j < downloadInfos[i].videoInfos().length; j++) {
                    String qStr = BiliInfoFormat.VideoFormat(downloadInfos[i].videoInfos()[j].quality()) + " "
                            + BiliInfoFormat.getVideoCode(downloadInfos[i].videoInfos()[j].codecid());
                    if (qStr.equals(selectedVideoQ)) {
                        vIdx = j;
                        break;
                    }
                }
            }

            int aIdx = 0;
            if (selectedAudioQ != null) {
                for (int j = 0; j < downloadInfos[i].audioInfos().length; j++) {
                    String qStr = BiliInfoFormat.AudioFormat(downloadInfos[i].audioInfos()[j].bitrate());
                    if (qStr.equals(selectedAudioQ)) {
                        aIdx = j;
                        break;
                    }
                }
            }

            this.allFileSizes[i] = downloadInfos[i].videoInfos()[vIdx].size() + downloadInfos[i].audioInfos()[aIdx].size();
            sizes[i][0] = downloadInfos[i].videoInfos()[vIdx].size();
            sizes[i][1] = downloadInfos[i].audioInfos()[aIdx].size();
        }
    }

    private void resetSize(){
        resetSizes();

        //获取真正被选中的文件大小
        long tempSize = 0;
        for (var i = 0; i < this.fileSelectionStatus.length; i++) {
            if (this.fileSelectionStatus[i]){//选中
                tempSize += this.sizes[i][0] + this.sizes[i][1];
            }
        }
        sizeLabel.setText(formatFileSize(tempSize));
    }


    /**
     * 获取下载链接
     * @return 下载链接 [选中的文件][选中文件中的各个下载链接]
     */
    public String[][] getBiliDownloadUrls(){
        ArrayList<String> uniqueVideoQualities = new ArrayList<>();
        for (var info : downloadInfos) {
            for (var vi : info.videoInfos()) {
                String qStr = BiliInfoFormat.VideoFormat(vi.quality()) + " "
                        + BiliInfoFormat.getVideoCode(vi.codecid());
                if (!uniqueVideoQualities.contains(qStr)) {
                    uniqueVideoQualities.add(qStr);
                }
            }
        }

        ArrayList<String> uniqueAudioQualities = new ArrayList<>();
        for (var info : downloadInfos) {
            for (var ai : info.audioInfos()) {
                String qStr = BiliInfoFormat.AudioFormat(ai.bitrate());
                if (!uniqueAudioQualities.contains(qStr)) {
                    uniqueAudioQualities.add(qStr);
                }
            }
        }

        String selectedVideoQ = (videoQualityInfoIndex < uniqueVideoQualities.size())
                ? uniqueVideoQualities.get(videoQualityInfoIndex) : null;
        String selectedAudioQ = (audioQualityInfoIndex < uniqueAudioQualities.size())
                ? uniqueAudioQualities.get(audioQualityInfoIndex) : null;

        ArrayList<String[]> result = new ArrayList<>();
        for (int i = 0; i < downloadInfos.length; i++) {
            if (!fileSelectionStatus[i]) continue;

            String videoUrl = null;
            if (selectedVideoQ != null) {
                for (var vi : downloadInfos[i].videoInfos()) {
                    String qStr = BiliInfoFormat.VideoFormat(vi.quality()) + " "
                            + BiliInfoFormat.getVideoCode(vi.codecid());
                    if (qStr.equals(selectedVideoQ)) {
                        videoUrl = vi.url();
                        break;
                    }
                }
            }

            String audioUrl = null;
            if (selectedAudioQ != null) {
                for (var ai : downloadInfos[i].audioInfos()) {
                    String qStr = BiliInfoFormat.AudioFormat(ai.bitrate());
                    if (qStr.equals(selectedAudioQ)) {
                        audioUrl = ai.url();
                        break;
                    }
                }
            }

            ArrayList<String> urls = new ArrayList<>();
            if (videoUrl != null) urls.add(videoUrl);
            if (audioUrl != null) urls.add(audioUrl);
            result.add(urls.toArray(String[]::new));
        }
        return result.toArray(String[][]::new);
    }

    public long[] getSelectedBiliFileSizes() {
        ArrayList<Long> result = new ArrayList<>();
        for (int i = 0; i < downloadInfos.length; i++) {
            if (fileSelectionStatus[i]) {
                result.add(sizes[i][0] + sizes[i][1]);
            }
        }
        return result.stream().mapToLong(Long::longValue).toArray();
    }

    public long getSelectedBiliTotalSize() {
        long total = 0;
        for (int i = 0; i < downloadInfos.length; i++) {
            if (fileSelectionStatus[i]) {
                total += sizes[i][0] + sizes[i][1];
            }
        }
        return total;
    }

    public long[] getSelectedVideoSizes() {
        ArrayList<Long> result = new ArrayList<>();
        for (int i = 0; i < downloadInfos.length; i++) {
            if (fileSelectionStatus[i]) {
                result.add(sizes[i][0]);
            }
        }
        return result.stream().mapToLong(Long::longValue).toArray();
    }

    public long[] getSelectedAudioSizes() {
        ArrayList<Long> result = new ArrayList<>();
        for (int i = 0; i < downloadInfos.length; i++) {
            if (fileSelectionStatus[i]) {
                result.add(sizes[i][1]);
            }
        }
        return result.stream().mapToLong(Long::longValue).toArray();
    }

    @Override
    @Deprecated
    public String[] getUrls() {
        return new String[0];
    }

    @Override
    @Deprecated
    public String[] getSelectedUrls() {
        return new String[0];
    }

    @Override
    @Deprecated
    public long getSelectedFileSize() {
        return 0;
    }

    @Override
    @Deprecated
    public long[] getSelectedFileSizes() {
        return new long[0];
    }

    @Override
    @Deprecated
    public long[] getFileSizes() {
        return new long[0];
    }
}

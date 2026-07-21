package com.wmp.downloader.ui.task.bilibili;

import com.wmp.downloader.exception.BiliDownloadTaskException;
import com.wmp.downloader.ui.FunctionDialog;
import com.wmp.downloader.ui.task.bilibili.info.BiliDownloadInfo;
import com.wmp.downloader.ui.task.createTask.LinkFileInfoPanel;

import java.awt.event.ActionEvent;

public class BiliLinkFileInfoPanel extends LinkFileInfoPanel {

    private final BiliDownloadInfo downloadInfo;
    private int videoInfoIndex = 0;
    private int audioInfoIndex = 0;

    public BiliLinkFileInfoPanel(String name, BiliDownloadInfo downloadInfo) {
        if (downloadInfo == null) throw new BiliDownloadTaskException("下载信息不能为空");
        super(name, downloadInfo.videoInfos()[0].size() + downloadInfo.audioInfos()[0].size(), "bilibili", downloadInfo.videoInfos()[0].url());

        this.downloadInfo = downloadInfo;
    }

    @Override
    public void editButtonAction(ActionEvent e) {
        var taskFileEditPanel = new BiliTaskFileEditPanel(nameLabel.getText(), downloadInfo, videoInfoIndex, audioInfoIndex);
        FunctionDialog.showDialog(this, "编辑任务信息", taskFileEditPanel,
                result -> {
                    if (result == FunctionDialog.RESULT_SAVE) {
                        nameLabel.setText(taskFileEditPanel.getFileName());
                        sizeLabel.setText(formatFileSize(taskFileEditPanel.getFileSizeNum()));
                        videoInfoIndex = taskFileEditPanel.getVideoInfoIndex();
                        audioInfoIndex = taskFileEditPanel.getAudioInfoIndex();
                    }
                },
                FunctionDialog.SAVE_CANCEL_BUTTONS, 0, null, 0);
    }

    /**
     * @deprecated 该方法已弃用，请使用 {@link getBiliDownloadUrl} 进行任务信息编辑
     */
    @Deprecated
    @Override
    public String getUrl() {
        return "";
    }

    /**
     * @deprecated 该方法已弃用，请使用 {@link getFileSize} 进行任务信息编辑
     */
    @Deprecated
    @Override
    public long getFileSizeNum() {
        return 0L;
    }

    public long[] getFileSize() {
        return new long[]{downloadInfo.videoInfos()[videoInfoIndex].size(), downloadInfo.audioInfos()[audioInfoIndex].size()};
    }

    /**
     * 获取B站下载地址
     * @return 0-视频地址 1-音频地址
     */
    public String[] getBiliDownloadUrl(){
        return new String[]{downloadInfo.videoInfos()[videoInfoIndex].url(), downloadInfo.audioInfos()[audioInfoIndex].url()};
    }
}

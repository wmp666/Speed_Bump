package com.wmp.downloader.ui.task.bilibili;

import com.wmp.downloader.ui.FunctionDialog;
import com.wmp.downloader.ui.task.createTask.LinkFileInfoPanel;
import com.wmp.downloader.ui.task.createTask.TaskFileEditPanel;

import java.awt.event.ActionEvent;

public class BiliLinkFileInfoPanel extends LinkFileInfoPanel {

    private String BVID;
    private long cid;

    public BiliLinkFileInfoPanel(String name, long size, String BVID, long cid, String defaultUrl) {
        super(name, size, "bilibili", defaultUrl);

        this.BVID = BVID;
        this.cid = cid;
    }

    @Override
    public void editButtonAction(ActionEvent e) {
        var taskFileEditPanel = new BiliTaskFileEditPanel(nameLabel.getText(), BVID, cid);
        FunctionDialog.showDialog(this, "编辑任务信息", taskFileEditPanel,
                result -> {
                    if (result == FunctionDialog.RESULT_SAVE) {
                        nameLabel.setText(taskFileEditPanel.getFileName());
                        sizeLabel.setText(formatFileSize(taskFileEditPanel.getFileSizeNum()));
                        this.url = taskFileEditPanel.getVideoUrl();
                    }
                },
                FunctionDialog.SAVE_CANCEL_BUTTONS, 0, null, 0);
    }


}

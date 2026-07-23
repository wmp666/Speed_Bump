package com.wmp.downloader.ui.task.createTask;

import com.wmp.downloader.laug.StringFormat;
import com.wmp.downloader.ui.FunctionDialog;
import com.wmp.downloader.ui.task.DownloadTask;
import com.wmp.downloader.ui.task.createTask.videohandle.CreateMergeTaskFuncPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class CreateVideoHandleTaskPanel extends JPanel {
    private JPanel mainPanel;
    private JButton createMergeTaskButton;

    private DownloadTaskAddListener downloadTaskAddListener = e -> {

    };

    public CreateVideoHandleTaskPanel() {
        this.setLayout(new BorderLayout());
        this.add(this.mainPanel);
    }

    public void setDownloadTaskAddListener(DownloadTaskAddListener listener) {
        this.downloadTaskAddListener = listener;

        this.createMergeTaskButton.addActionListener(e -> {
            var createMergeTaskFuncPanel = new CreateMergeTaskFuncPanel();
            FunctionDialog.showDialog(this, StringFormat.translate("video_handle", "video_handle.create_merge_task"),
                    createMergeTaskFuncPanel, result -> {
                        if (result == FunctionDialog.RESULT_OK) {
                            downloadTaskAddListener.AddDownloadTask(createMergeTaskFuncPanel.createDownloadTask());
                        }
                    }, FunctionDialog.OK_CANCEL_BUTTONS, 0, null, 0);

        });
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        //mainPanel = this;
    }

    public interface DownloadTaskAddListener {
        void AddDownloadTask(DownloadTask downloadTask);
    }
}

package com.wmp.downloader.newArchitecture.ui.createTask.videohandle;

import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractTask;
import com.wmp.downloader.newArchitecture.abstractTask.downloadTask.StatusTipPanel;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.speed_bump.common.background.tools.StringFormat;
import com.wmp.speed_bump.common.ui.components.SBProgressBar;
import com.wmp.downloader.tools.download.ConvergenceTool;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.tools.ui.UITools;
import com.wmp.speed_bump.platform.ui.swing.components.PathSelectionPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class CreateMergeTaskFuncPanel extends JPanel {


    private JPanel mainPanel;
    private PathSelectionPanel videoPathSelectionPanel;
    private PathSelectionPanel audioPathSelectionPanel;
    private PathSelectionPanel savePathSelectionPanel;
    private JTextField FileNameTextField;

    public CreateMergeTaskFuncPanel() {
        this.setLayout(new BorderLayout());
        this.add(this.mainPanel);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        videoPathSelectionPanel = new PathSelectionPanel(StringFormat.translate("video_handle", "video_handle.create_merge_task.video_path"), null);
        audioPathSelectionPanel = new PathSelectionPanel(StringFormat.translate("video_handle", "video_handle.create_merge_task.audio_path"), null);
        savePathSelectionPanel = new PathSelectionPanel(StringFormat.translate("common", "save_path"), DataControl.getDownloadFilePath());
    }

    /**
     * 返回路径
     *
     * @return {视频位置，音频位置，保存位置}
     */
    public File[] getPath() {
        return new File[]{new File(videoPathSelectionPanel.getPath()), new File(audioPathSelectionPanel.getPath()), new File(savePathSelectionPanel.getPath())};
    }

    public String getFileName() {
        return FileNameTextField.getText();
    }

    public AbstractTask createDownloadTask() {
        return new MergeTaskDownloadTask(getFileName(), getPath());
    }

    public static class MergeTaskDownloadTask extends AbstractTask {

        private File[] paths;
        private final StatusTipPanel MERGE_TIP_PANEL = StatusTipPanel.FILE_MERGE_CREATOR.create();

        public MergeTaskDownloadTask(String fileName, File[] paths) {
            var jsonObject = new JSONObject();
            jsonObject.put("rootName", fileName);
            jsonObject.put("savePath", paths[0].getAbsolutePath());
            jsonObject.put("paths", paths);
            super(jsonObject);
            this.paths = paths;
        }

        @Override
        public void doWhenExit() {

            super.doWhenExit();
        }

        @Override
        public void doWhenStart() throws Exception {
            var progressBar = SBProgressBar.INSTANCE_CREATOR.create();
            progressBar.setProgressStringPainted(false);
            removeAllStatusTip();
            addStatusTip(MERGE_TIP_PANEL);
            MERGE_TIP_PANEL.setText(StringFormat.translate("video_handle", "video_handle.create_merge_task.run_tip"));
            ProgressBarsPanel.add(UITools.createProgressBarPanel(progressBar));
            exitButton.setEnabled(false);
            downloadControlButton.setEnabled(false);
            var converged = ConvergenceTool.converge(paths[0], paths[1], new File(paths[2], fileName), progressBar);
            ProgressBarsPanel.removeAll();
            if (converged) {
                exitButton.setEnabled(true);
                downloadControlButton.setEnabled(false);
            } else {
                exitButton.setEnabled(true);
                downloadControlButton.setEnabled(true);
                ToastMessage.show(null, StringFormat.translate("video_handle", "video_handle.create_merge_task.merge_fail"), ToastMessage.ERROR);
            }
        }

        @Override
        public void doWhenRestart() throws Exception {

        }

        @Override
        public void doWhenStop() {

        }
    }

}


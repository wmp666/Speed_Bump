package com.wmp.downloader.newArchitecture.ui.createTask.videohandle;

import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractTask;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.download.ConvergenceTool;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.tools.ui.UITools;
import com.wmp.downloader.ui.common.PathSelectionPanel;

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
            var jProgressBar = new JProgressBar(0, 100);
            jProgressBar.setStringPainted(false);
            infoLabel.setText(StringFormat.translate("video_handle", "video_handle.create_merge_task.run_tip"));
            ProgressBarsPanel.add(UITools.createProgressBarPanel(jProgressBar));
            exitButton.setEnabled(false);
            downloadControlButton.setEnabled(false);
            var converged = ConvergenceTool.converge(paths[0], paths[1], new File(paths[2], fileName), jProgressBar);
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


package com.wmp.downloader.ui.task.createTask.videohandle;

import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.download.ConvergenceTool;
import com.wmp.downloader.ui.common.PathSelectionPanel;
import com.wmp.downloader.ui.task.DownloadTask;

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
     * @return {视频位置，音频位置，保存位置}
     */
    public File[] getPath(){
        return new File[]{new File(videoPathSelectionPanel.getPath()), new File(audioPathSelectionPanel.getPath()), new File(savePathSelectionPanel.getPath())};
    }

    public String getFileName(){
        return FileNameTextField.getText();
    }

    public DownloadTask createDownloadTask() {
        return new MergeTaskDownloadTask(getFileName(), getPath());
    }

    public static class MergeTaskDownloadTask extends DownloadTask {

        private File[] paths;

        public MergeTaskDownloadTask(String fileName, File[] paths) {
            super(fileName, paths[2]);
            this.paths = paths;
        }

        @Override
        public void doWhenExit() {

        }

        @Override
        public void doWhenStart() throws Exception {
            var jProgressBar = new JProgressBar(0, 100);
            jProgressBar.setStringPainted(true);
            infoLabel.setText(StringFormat.translate("video_handle", "video_handle.create_merge_task.run_tip"));
            ProgressBarsPanel.add(jProgressBar);
            var converged = ConvergenceTool.converge(paths[0], paths[1], new File(paths[2], fileName), jProgressBar);
            ProgressBarsPanel.removeAll();
            if (converged){
                downloadControlButton.setEnabled(false);
            }else{
                JOptionPane.showMessageDialog(null, StringFormat.translate("video_handle", "video_handle.create_merge_task.merge_fail"));
            }
        }

        @Override
        public void doWhenStop() {

        }
    }

}


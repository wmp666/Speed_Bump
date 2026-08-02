package com.wmp.downloader.ui.task.createTask.videohandle;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.download.ConvergenceTool;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.tools.ui.UITools;
import com.wmp.downloader.ui.common.PathSelectionPanel;
import com.wmp.downloader.ui.task.DownloadTask;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class ResetVideoInfoPanel extends JPanel {

    private static final Logger logger = Logger.getLogger(ResetVideoInfoPanel.class);

    private static final String[] videoFormats =
            {"mp4", "mkv", "avi", "mov", "flv", "webm"};
    private static final String[] videoCodecs =
            {"h264 (AVC)", "h265 (HEVC)"};
    private static final String[] audioCodecs =
            {"aac", "mp3", "flac", "wav"};

    private JPanel mainPanel;
    private JTextField fileNameTextField;
    private PathSelectionPanel FilePathSelectionPanel;
    private JComboBox<String> videoFormatComboBox;
    private JComboBox<String> videoCodecsComboBox;
    private JComboBox<String> audioCodecsComboBox;
    private PathSelectionPanel outPathSelectionPanel;

    public ResetVideoInfoPanel() {
        this.setLayout(new BorderLayout());
        this.add(this.mainPanel);


        fileNameTextField.setText("");
        FilePathSelectionPanel.setPath("");
        for (var videoFormat : videoFormats) {
            videoFormatComboBox.addItem(videoFormat);
        }
        for (var videoCodec : videoCodecs) {
            videoCodecsComboBox.addItem(videoCodec);
        }
        for (var audioCodec : audioCodecs) {
            audioCodecsComboBox.addItem(audioCodec);
        }
        FilePathSelectionPanel.setPathChangeListener(path -> {
            try {
                var file = new File(path);
                fileNameTextField.setText(file.getName());
            } catch (Exception e) {
                fileNameTextField.setText("");
                logger.error("文件选择错误");
                ToastMessage.show(this, StringFormat.translate("video_handle", "video_handle.reset_video_info.file_selection_error"), ToastMessage.ERROR);
            }
        });
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        FilePathSelectionPanel = new PathSelectionPanel(StringFormat.translate("video_handle", "video_handle.reset_video_info.file_selection"), null, SystemFileChooser.FILES_ONLY);
        outPathSelectionPanel = new PathSelectionPanel(StringFormat.translate("common", "save_path"), DataControl.getDownloadFilePath(), SystemFileChooser.DIRECTORIES_ONLY);
    }

    public DownloadTask createDownloadTask() {
        // 输入文件：用户选择的文件
        File inputFile = new File(FilePathSelectionPanel.getPath());
        if (!inputFile.exists()) {
            logger.error("输入文件不存在");
            ToastMessage.show(this, "请选择有效的输入文件", ToastMessage.ERROR);
            return null;
        }

        // 容器格式（扩展名）
        String containerExt = (String) videoFormatComboBox.getSelectedItem();
        // 输出文件放在输入文件同目录
        File outputFile = new File(outPathSelectionPanel.getPath(), fileNameTextField.getText());

        // 将 ComboBox 显示文本映射为 FFmpeg 编码器名
        String videoEncoder = mapVideoCodec((String) videoCodecsComboBox.getSelectedItem());
        String audioEncoder = mapAudioCodec((String) audioCodecsComboBox.getSelectedItem());

        return new ResetVideoInfoDownloadTask(
                inputFile, outputFile,
                containerExt, videoEncoder, audioEncoder
        );
    }

    // 映射视频编码显示名 -> FFmpeg 编码器
    private String mapVideoCodec(String display) {
        if (display.equals("h264 (AVC)")) return "libx264";
        if (display.equals("h265 (HEVC)")) return "libx265";
        return "libx264"; // fallback
    }

    // 映射音频编码显示名 -> FFmpeg 编码器
    private String mapAudioCodec(String display) {
        switch (display) {
            case "aac":
                return "aac";
            case "mp3":
                return "libmp3lame";
            case "flac":
                return "flac";
            case "wav":
                return "pcm_s16le"; // 无损 PCM
            default:
                return "aac";
        }
    }

    static class ResetVideoInfoDownloadTask extends DownloadTask {
        private final File inputFile;
        private final File outputFile;
        private final String containerFormat;
        private final String videoCodec;
        private final String audioCodec;

        public ResetVideoInfoDownloadTask(File inputFile, File outputFile,
                                          String containerFormat, String videoCodec, String audioCodec) {
            super(outputFile.getName(), outputFile.getParentFile()); // 适配父类构造
            this.inputFile = inputFile;
            this.outputFile = outputFile;
            this.containerFormat = containerFormat;
            this.videoCodec = videoCodec;
            this.audioCodec = audioCodec;


        }

        @Override
        public void doWhenExit() {

        }

        @Override
        public void doWhenStart() throws Exception {
            Thread.ofVirtual().start(() -> {
                JProgressBar progressBar = new JProgressBar();
                progressBar.setStringPainted(false);
                progressBar.setMinimum(0);
                progressBar.setMaximum(100);
                progressBar.setValue(0);
                ProgressBarsPanel.add(UITools.createProgressBarPanel(progressBar));
                boolean success = ConvergenceTool.transcodeVideo(
                        inputFile, outputFile,
                        containerFormat, videoCodec, audioCodec,
                        progressBar // 传入进度条组件
                );
                ProgressBarsPanel.removeAll();
                if (!success) {
                    logger.error("转码失败");
                } else {
                    downloadControlButton.setEnabled(false);
                    isFinally = true;
                }
            });
        }

        @Override
        public void doWhenStop() {

        }
    }
}

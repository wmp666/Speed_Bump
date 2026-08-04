package com.wmp.downloader.newArchitecture.ui.task;

import com.wmp.downloader.newArchitecture.abstractTask.AbstractSpecialSettingsPage;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.ui.common.PathSelectionPanel;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;

public class FFmpegSettings extends AbstractSpecialSettingsPage {

    private static final Logger logger = Logger.getLogger(FFmpegSettings.class);

    private JPanel mainPanel;
    private PathSelectionPanel localFFmpegPathPanel;
    private JButton downloadButton;
    private JCheckBox isUseHardwareAccelerationCheckBox;

    @Override
    public void setDefaultButton() {

    }

    @Override
    public String getSettingsName() {
        return StringFormat.translate("special_settings", "ffmpeg_special_settings");
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        localFFmpegPathPanel = new PathSelectionPanel(StringFormat.translate("special_settings", "ffmpeg_special_settings.setLocalPath"), new File(DataControl.get("ffmpeg_appPath", "")));
    }

    public FFmpegSettings() {
        super();
        this.setLayout(new BorderLayout());
        this.add(mainPanel, BorderLayout.CENTER);

        //初始化图标
        IconControl.addInDynamicConverter(() -> {
            downloadButton.setIcon(IconControl.getIcon("link", downloadButton.getFont().getSize()));
        });

        isUseHardwareAccelerationCheckBox.setSelected(DataControl.get("is_use_hardware_acceleration", true));
        localFFmpegPathPanel.setPath(DataControl.get("ffmpeg_appPath", ""));

        isUseHardwareAccelerationCheckBox.addActionListener(e -> {
            DataControl.putAndSave("is_use_hardware_acceleration", isUseHardwareAccelerationCheckBox.isSelected());
        });
        localFFmpegPathPanel.setPathChangeListener(path -> DataControl.putAndSave("ffmpeg_appPath", path));
        downloadButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(URI.create("https://ffmpeg.org/download.html"));
            } catch (IOException ex) {
                logger.error("Error opening browser", ex);
                ToastMessage.show(mainPanel, "Error opening browser", ToastMessage.ERROR);
            }
        });
    }
}

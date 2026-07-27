package com.wmp.downloader.ui.specialSettings;

import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.ui.common.PathSelectionPanel;
import com.wmp.downloader.ui.settings.BasicSpecialSettings;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;

public class FFmpegSettings extends BasicSpecialSettings {

    private static final Logger logger = Logger.getLogger(FFmpegSettings.class);

    private JPanel mainPanel;
    private JCheckBox isUseLocalFFmpegCheckBox;
    private PathSelectionPanel localFFmpegPathPanel;
    private JButton downloadButton;
    private JCheckBox isUseHardwareAccelerationCheckBox;

    @Override
    public String getSettingsName() {
        return StringFormat.translate("special_settings", "ffmpeg_special_settings");
    }

    @Override
    public SpecialSettingsPanel getSettings() {
        return new FFmpegSpecialSettingsPanel(mainPanel);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        localFFmpegPathPanel = new PathSelectionPanel(StringFormat.translate("special_settings", "ffmpeg_special_settings.setLocalPath"), new File(DataControl.get("ffmpeg_appPath", "")));
    }

    class FFmpegSpecialSettingsPanel extends SpecialSettingsPanel {

        public FFmpegSpecialSettingsPanel(JPanel mainPanel) {
            super();
            this.setLayout(new BorderLayout());
            this.add(mainPanel, BorderLayout.CENTER);

            //初始化图标
            IconControl.addInDynamicConverter(() -> {
                downloadButton.setIcon(IconControl.getIcon("link", downloadButton.getFont().getSize()));
            });

            isUseLocalFFmpegCheckBox.setSelected(DataControl.get("ffmpeg_isUseLocal", false));
            isUseHardwareAccelerationCheckBox.setSelected(DataControl.get("is_use_hardware_acceleration", true));
            localFFmpegPathPanel.setPath(DataControl.get("ffmpeg_appPath", ""));

            isUseLocalFFmpegCheckBox.addActionListener(e -> {
                DataControl.putAndSave("ffmpeg_isUseLocal", isUseLocalFFmpegCheckBox.isSelected());
            });
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
        @Override
        public void setDefaultButton() {

        }
    }
}

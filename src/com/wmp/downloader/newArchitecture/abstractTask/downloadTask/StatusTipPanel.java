package com.wmp.downloader.newArchitecture.abstractTask.downloadTask;

import com.wmp.speed_bump.common.background.tools.StringFormat;
import com.wmp.speed_bump.common.background.tools.DynamicConverterTask;
import com.wmp.speed_bump.common.background.tools.resource.control.IconControl;

import javax.swing.*;
import java.awt.*;

public class StatusTipPanel extends JPanel {

    public static final Creator DOWNLOAD_SIZE_CREATOR = () -> new StatusTipPanel(IconControl.INSTANCE.getIcon("download_size"), "0Byte", true);
    public static final Creator DOWNLOAD_SPEED_CREATOR = () -> new StatusTipPanel(IconControl.INSTANCE.getIcon("download_speed"), "0Byte/s", true);
    public static final Creator SHARE_SIZE_CREATOR = () -> new StatusTipPanel(IconControl.INSTANCE.getIcon("share_size"), "0Byte", true);
    public static final Creator SHARE_SPEED_CREATOR = () -> new StatusTipPanel(IconControl.INSTANCE.getIcon("share_speed"), "0Byte/s", true);
    public static final Creator FILE_MERGE_CREATOR = () -> new StatusTipPanel(IconControl.INSTANCE.getIcon("file_merge"), "0Byte", true);
    public static final Creator DOWNLOAD_FAILED_CREATOR = () -> new StatusTipPanel(IconControl.INSTANCE.getIcon("download_failed"), StringFormat.translate("task.download_task.download_failed"), false);
    public static final Creator DOWNLOAD_SUCCESS_CREATOR = () -> new StatusTipPanel(IconControl.INSTANCE.getIcon("download_success"), StringFormat.translate("task.download_task.download_success"), false);

    private DynamicConverterTask[] dynamicConverterTask;
    private JLabel textLabel = new JLabel();
    private final boolean isCanReset;

    public StatusTipPanel(ImageIcon icon, String defaultTip) {
        this(icon, defaultTip, true);
    }

    public StatusTipPanel(ImageIcon icon, String defaultTip, boolean isCanReset) {

        this.isCanReset = isCanReset;

        this.setLayout(new BorderLayout(5, 5));
        this.setOpaque(false);

        JLabel iconLabel = new JLabel(icon);
        dynamicConverterTask = new DynamicConverterTask[]{
                () -> {
                    iconLabel.setIcon(new ImageIcon(icon.getImage().getScaledInstance(textLabel.getFont().getSize(), textLabel.getFont().getSize(), Image.SCALE_SMOOTH)));
                }
        };
        IconControl.INSTANCE.addInDynamicConverter(
                dynamicConverterTask
        );
        this.add(iconLabel, BorderLayout.WEST);

        textLabel.setText(defaultTip);
        this.add(textLabel, BorderLayout.CENTER);
    }

    public void setText(String text){
        if (isCanReset) {
            textLabel.setText(text);
        }
    }

    public void clear(){
        IconControl.INSTANCE.removeInDynamicConverter(dynamicConverterTask);
    }

    public interface Creator extends com.wmp.speed_bump.common.background.tools.Creator<StatusTipPanel> {
    }
}

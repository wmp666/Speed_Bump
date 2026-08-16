package com.wmp.downloader.ui.common;

import com.formdev.flatlaf.FlatLaf;
import com.sun.nio.sctp.Association;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.file.ResourceLocalizer;
import com.wmp.downloader.tools.platform.FileAssociation;
import com.wmp.downloader.tools.platform.GetPlatform;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ToastMessage;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class FileAssociationPanel extends JPanel{

    private static final Logger logger = Logger.getLogger(FileAssociationPanel.class);

    private JButton associationButton;
    private JButton unAssociationButton;
    private JLabel suffixLabel;
    private JPanel mainPanel;

    private final String suffix;
    private final String description;
    private final String localIconPath;

    /**
     *
     * @param suffix 后缀名 如：torrent
     * @param description 描述 如：种子文件
     * @param iconPath 图标路径 如：C:\icon.ico(Windows)
     */
    public FileAssociationPanel(String suffix, String description, String iconPath) {
        this.setLayout(new BorderLayout());
        this.add(mainPanel);

        this.suffix = suffix;
        this.description = description;
        String iconAssociation;
        if (GetPlatform.isWindows()) iconAssociation = ".ico";
        else if (GetPlatform.isLinux()) iconAssociation = ".png";
        else if (GetPlatform.isMac()) iconAssociation = ".icns";
        else iconAssociation = ".png";

        suffixLabel.putClientProperty("FlatLaf.style", "font: $h1.font");
        suffixLabel.setText(description);
        suffixLabel.setIcon(
                new ImageIcon(
                        new ImageIcon(FileAssociationPanel.class.getResource(iconPath + ".png")).getImage()
                        .getScaledInstance(suffixLabel.getFont().getSize(), suffixLabel.getFont().getSize(), Image.SCALE_SMOOTH)));

        localIconPath = new File(DataControl.getDataPath(), "/file_icon/" + suffix + iconAssociation).getAbsolutePath();

        associationButton.addActionListener(_-> {
            ResourceLocalizer.copyEmbeddedFile(localIconPath, iconPath + iconAssociation);
            var appPath = DataControl.getAppPath();

            try {
                if (appPath == null) {
                ToastMessage.show(null, "当前启动方式无法关联文件", ToastMessage.WARNING);
                throw new UnsupportedOperationException("当前启动方式无法关联文件");
                }
                FileAssociation.register(suffix, description, localIconPath, appPath.getAbsolutePath());
            } catch (Exception e) {
                logger.error("注册失败", e);
                ToastMessage.show(null, StringFormat.translate("file_association.failed"), ToastMessage.ERROR);
            }
        });

        unAssociationButton.addActionListener(_-> {
            ResourceLocalizer.copyEmbeddedFile(localIconPath, iconPath + iconAssociation);
            var appPath = DataControl.getAppPath();

            try {
                if (appPath == null && !GetPlatform.isWindows()) {
                    ToastMessage.show(null, "当前启动方式无法关联文件", ToastMessage.WARNING);
                    throw new UnsupportedOperationException("当前启动方式无法关联文件");
                }
                FileAssociation.unregister(suffix, appPath == null?null:appPath.getAbsolutePath());
            } catch (Exception e) {
                logger.error("注册失败", e);
                ToastMessage.show(null, StringFormat.translate("file_association.failed"), ToastMessage.ERROR);
            }
        });
    }
}

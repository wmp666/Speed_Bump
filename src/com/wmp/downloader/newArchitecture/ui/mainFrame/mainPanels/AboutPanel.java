package com.wmp.downloader.newArchitecture.ui.mainFrame.mainPanels;

import com.wmp.downloader.Run;
import com.wmp.downloader.tools.EasterEggData;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.tools.ui.UITools;
import com.wmp.downloader.ui.Downloader;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

public class AboutPanel {

    private static final Logger logger = Logger.getLogger(AboutPanel.class);

    public JPanel aboutPanel;
    private JLabel nameLabel;
    private JButton checkUpdateButton;
    private JButton ProjectLinkButton;
    private JScrollPane aboutInfoScrollPane;
    private JPanel aboutInfoPanel;
    private JScrollPane aboutScrollPane;
    private JCheckBox FlatLafCheckBox;
    private JCheckBox IconPackCheckBox;
    private JCheckBox alibabaFastjsonCheckBox;
    private JCheckBox log4jLog4jCheckBox;
    private JCheckBox authorCheckBox;
    private JLabel licenseLabel;
    private JButton issueButton;
    private JLabel runVersionLabel;
    private JLabel PluginSupportVersionLabel;
    private JLabel JavaVersionLabel;
    private JLabel JavaRuntimeLabel;

    private final Downloader downloader;

    public AboutPanel(Downloader downloader) {
        this.downloader = downloader;
    }

    public void initAboutComponents() {
        nameLabel.setText(StringFormat.translate("common", "app_name") + " V" + DataControl.get("version", "0.0.0"));
        nameLabel.putClientProperty("FlatLaf.style", "font: bold $h0.font");
        issueButton.putClientProperty("FlatLaf.style", "font: bold $h3.font");
        licenseLabel.putClientProperty("FlatLaf.style", "font: bold $h2.font");
        checkUpdateButton.putClientProperty("FlatLaf.style", "font: bold $h3.font");
        ProjectLinkButton.putClientProperty("FlatLaf.style", "font: bold $h3.font");

        runVersionLabel.setText(Run.VERSION);
        PluginSupportVersionLabel.setText(Run.PLUGIN_SUPPORT_VERSION);
        JavaVersionLabel.setText(System.getProperty("java.version"));
        JavaRuntimeLabel.setText(System.getProperty("java.runtime.name"));

        IconControl.addInDynamicConverter(
                () -> nameLabel.setIcon(IconControl.getIcon("icon", nameLabel.getFont().getSize())),
                () -> licenseLabel.setIcon(IconControl.getIcon("license", licenseLabel.getFont().getSize())),
                () -> checkUpdateButton.setIcon(IconControl.getIcon("update", checkUpdateButton.getFont().getSize())),
                () -> ProjectLinkButton.setIcon(IconControl.getIcon("link", ProjectLinkButton.getFont().getSize())),
                () -> issueButton.setIcon(IconControl.getIcon("issue", issueButton.getFont().getSize()))
        );

        authorCheckBox.addActionListener(_ -> {
            if (!authorCheckBox.isSelected()) {
                var panel = new JPanel(new BorderLayout());
                var textArea = new JTextArea("你真的要这么做吗!\n这样做真的很危险!\n不要继续呀!");
                panel.add(textArea);

            }
        });
        FlatLafCheckBox.addActionListener(_ -> {
            EasterEggData.canUseFlatLaf = FlatLafCheckBox.isSelected();
            ThemeChanger.easyChanger();
        });
        IconPackCheckBox.addActionListener(_ -> {
            EasterEggData.canUseIcon = IconPackCheckBox.isSelected();
            IconControl.runDynamicConverters();
        });

        checkUpdateButton.addActionListener(_ -> downloader.checkUpdate());

        ProjectLinkButton.addActionListener(_ -> {
            try {
                Desktop.getDesktop().browse(URI.create("https://github.com/wmp666/Speed_Bump"));
            } catch (Exception ex) {
                ToastMessage.show(StringFormat.translate("open_link.error"), ToastMessage.ERROR);
                logger.error("网站打开失败", ex);
            }
        });
        issueButton.addActionListener(_ -> {
            try {
                Desktop.getDesktop().browse(URI.create("https://github.com/wmp666/Speed_Bump/issues"));
            } catch (Exception ex) {
                ToastMessage.show(StringFormat.translate("open_link.error"), ToastMessage.ERROR);
                logger.error("网站打开失败", ex);
            }
        });

        aboutScrollPane.getViewport().setOpaque(false);

        UITools.setScrollPaneUnOpaque(aboutInfoScrollPane);
    }

    public JButton getCheckUpdateButton() {
        return checkUpdateButton;
    }
}

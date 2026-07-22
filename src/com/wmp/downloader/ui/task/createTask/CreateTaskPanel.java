package com.wmp.downloader.ui.task.createTask;

import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.ui.common.PathSelectionPanel;
import com.wmp.downloader.ui.task.DownloadTask;
import com.wmp.downloader.ui.task.Parser;
import com.wmp.downloader.ui.task.bilibili.file.BiliFileDownloadTask;
import com.wmp.downloader.ui.task.bilibili.folder.BiliFolderDownloadTask;
import com.wmp.downloader.ui.task.bilibili.file.BiliLinkFileInfoPanel;
import com.wmp.downloader.ui.task.bilibili.folder.BiliLinkFolderInfoPanel;
import com.wmp.downloader.ui.task.http.URLDownloadTask;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class CreateTaskPanel {
    private final ArrayList<JPanel> linkFileInfoPanels = new ArrayList<>();
    //private final ArrayList<LinkInfo> linkFileInfoPanels = new ArrayList<>();
    private final Logger logger = Logger.getLogger(CreateTaskPanel.class);

    public JPanel MainPanel;
    private JTextArea DownloaderURLTextArea;
    private PathSelectionPanel PathSelectionPanel;
    private JPanel linkInfoPanel;
    private JLabel tipLabel;
    private JComboBox<String> modeComboBox;
    private JSlider ThreadNumSlider;
    private JTextField ThreadNumLabel;
    private JProgressBar tipProgressBar;



    public CreateTaskPanel() {
        ThreadNumSlider.setValue(DataControl.get("ThreadNum", 64));
        ThreadNumLabel.setText(String.valueOf(ThreadNumSlider.getValue()));
        //添加链接解析功能
        DownloaderURLTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                Thread.ofVirtual().start(() -> {
                    synchronized (this) {
                        tipLabel.setText("正在解析链接...");
                        tipProgressBar.setVisible(true);
                        tipProgressBar.setIndeterminate(true);


                        linkFileInfoPanels.clear();
                        linkFileInfoPanels.clear();
                        linkInfoPanel.removeAll();
                        parseLinks(DownloaderURLTextArea.getText().split("\n"));
                        tipLabel.setText("");
                        tipProgressBar.setVisible(false);
                    }
                });
            }

            @Override
            public void removeUpdate(DocumentEvent e) {

                Thread.ofVirtual().start(() -> {
                    synchronized (this) {
                        tipLabel.setText("正在解析链接...");
                        tipProgressBar.setVisible(true);
                        tipProgressBar.setIndeterminate(true);
                        linkFileInfoPanels.clear();
                        linkFileInfoPanels.clear();
                        linkInfoPanel.removeAll();
                        parseLinks(DownloaderURLTextArea.getText().split("\n"));
                        tipLabel.setText("");
                        tipProgressBar.setVisible(false);
                    }
                });

            }
        });
        //添加右键在弹出菜单内粘贴
        DownloaderURLTextArea.addMouseListener(new MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getButton() == java.awt.event.MouseEvent.BUTTON3) {
                    JPopupMenu popupMenu = new JPopupMenu();
                    JMenuItem pasteItem = new JMenuItem("粘贴");
                    pasteItem.addActionListener(_ -> DownloaderURLTextArea.paste());
                    popupMenu.add(pasteItem);
                    popupMenu.show(DownloaderURLTextArea, e.getX(), e.getY());
                }
            }
        });
        ThreadNumSlider.addChangeListener(e -> {
            ThreadNumLabel.setText(String.valueOf(ThreadNumSlider.getValue()));
        });
    }




    private void createUIComponents() {
        // TODO: place custom component creation code here
        PathSelectionPanel = new PathSelectionPanel("保存路径", DataControl.getDownloadFilePath());

        linkInfoPanel = new JPanel(new GridLayout(0, 1, 5, 5));
    }

    private void parseLinks(String[] links) {
        for (var link : links) {
            parseLink(link);
        }
    }

    private void parseLink(String link) {
        //判断链接类型

        try{
            var linkFileInfoPanel = Parser.getParser(link).parse(link);

            if (linkFileInfoPanel != null) {
                linkFileInfoPanels.add(linkFileInfoPanel);
                linkInfoPanel.add(linkFileInfoPanel);
            }else{
                throw new LayerInstantiationException("链接解析出错");
            }
            MainPanel.revalidate();
            MainPanel.repaint();
        } catch (Exception e) {
            tipLabel.setText("存在错误链接");
            tipProgressBar.setVisible(false);
            logger.error("Error parsing link: " + link, e);
        }
    }

    public ArrayList<DownloadTask> getDownloadTasks() {
        var path = PathSelectionPanel.getPath();
        var mode = Objects.equals(modeComboBox.getSelectedItem(), "多线程") ? 0 : 1;
        var threadNum = ThreadNumSlider.getValue();

        ArrayList<DownloadTask> downloadTasks = new ArrayList<>();
        for (var panel : linkFileInfoPanels) {
            if (panel instanceof LinkFileInfoPanel linkFileInfoPanel1){
                if (linkFileInfoPanel1.getMode().equals("HTTP"))
                    downloadTasks.add(new URLDownloadTask(linkFileInfoPanel1.getFileName(), linkFileInfoPanel1.getFileSizeNum(), URI.create(linkFileInfoPanel1.getUrl()), new File(path), threadNum, mode));
                else if (linkFileInfoPanel1.getMode().equals("bilibili")) {
                    if (linkFileInfoPanel1 instanceof BiliLinkFileInfoPanel biliLinkFileInfoPanel)
                        downloadTasks.add(new BiliFileDownloadTask(
                                biliLinkFileInfoPanel.getFileName(), biliLinkFileInfoPanel.getFileSize(),
                                biliLinkFileInfoPanel.getBiliDownloadUrl(), new File(path), threadNum, mode));
                }
            } else if (panel instanceof LinkFolderInfoPanel linkFolderPanel) {
                if (linkFolderPanel instanceof BiliLinkFolderInfoPanel biliLinkFolderInfoPanel)
                    downloadTasks.add(new BiliFolderDownloadTask(
                            biliLinkFolderInfoPanel.getFolderName(),
                            biliLinkFolderInfoPanel.getSelectedBiliTotalSize(),
                            biliLinkFolderInfoPanel.getSelectedBiliFileSizes(),
                            biliLinkFolderInfoPanel.getSelectedVideoSizes(),
                            biliLinkFolderInfoPanel.getSelectedAudioSizes(),
                            biliLinkFolderInfoPanel.getBiliDownloadUrls(),
                            biliLinkFolderInfoPanel.getSelectionFileNames(),
                            new File(path), threadNum, mode));

            }


            //else if (linkFileInfoPanel.getMode().equals("ED2k"))
                //downloadTasks.add(new Ed2kDownloadTask(linkFileInfoPanel.getFileName(), linkFileInfoPanel.getFileSizeNum(), URI.create(linkFileInfoPanel.getUrl()), new File(path), threadNum, mode));
        }
        return downloadTasks;
    }

    public void setLink(String link) {
        SwingUtilities.invokeLater(() -> DownloaderURLTextArea.setText(link));
    }
}

package com.wmp.downloader.ui.task.createTask;

import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.ui.common.PathSelectionPanel;
import com.wmp.downloader.ui.task.DownloadTask;
import com.wmp.downloader.ui.task.Parser;
import com.wmp.downloader.ui.task.bilibili.file.BiliFileDownloadTask;
import com.wmp.downloader.ui.task.bilibili.folder.BiliFolderDownloadTask;
import com.wmp.downloader.ui.task.bilibili.file.BiliLinkFileInfoPanel;
import com.wmp.downloader.ui.task.bilibili.folder.BiliLinkFolderInfoPanel;
import com.wmp.downloader.ui.task.bt.TorrentFileDownloadTask;
import com.wmp.downloader.ui.task.douyin.DouyinImageDownloadTask;
import com.wmp.downloader.ui.task.douyin.DouyinVideoDownloadTask;
import com.wmp.downloader.ui.task.http.URLDownloadTask;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;

public class CreateTaskPanel {
    private final ArrayList<JPanel> linkFileInfoPanels = new ArrayList<>();
    //private final ArrayList<LinkInfo> linkFileInfoPanels = new ArrayList<>();
    private final Logger logger = Logger.getLogger(CreateTaskPanel.class);

    public JPanel MainPanel;
    private JTabbedPane tabbedPane1;
    private JTextArea DownloaderURLTextArea;
    private PathSelectionPanel PathSelectionPanel;
    private JPanel linkInfoPanel;
    private JLabel tipLabel;
    private JComboBox<String> modeComboBox;
    private JSlider ThreadNumSlider;
    private JTextField ThreadNumLabel;
    private JProgressBar tipProgressBar;
    private CreateVideoHandleTaskPanel CreateVideoHandleTaskPanel;

    private ArrayList<DownloadTask> moreDownloadTasks = new ArrayList<>();

    public CreateTaskPanel() {
        CreateVideoHandleTaskPanel.setDownloadTaskAddListener(downloadTask -> moreDownloadTasks.add(downloadTask));

        ThreadNumSlider.setValue(DataControl.get("ThreadNum", 64));
        ThreadNumLabel.setText(String.valueOf(ThreadNumSlider.getValue()));

        modeComboBox.addItem(StringFormat.translate("task", "task.create_task.choose_mode.multi_threaded"));
        modeComboBox.addItem(StringFormat.translate("task", "task.create_task.choose_mode.single_threaded"));

        modeComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String mode = e.getItem().toString();
                if (mode.equals(StringFormat.translate("task", "task.create_task.choose_mode.multi_threaded"))) {
                    ThreadNumSlider.setEnabled(true);
                }else if (mode.equals(StringFormat.translate("task", "task.create_task.choose_mode.single_threaded"))) {
                    ThreadNumSlider.setEnabled(false);
                }
            }
        });
        //添加链接解析功能
        DownloaderURLTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                Thread.ofVirtual().start(() -> {
                    synchronized (this) {
                        tipLabel.setText(StringFormat.translate("task", "task.create_task.parsing_link"));
                        tipProgressBar.setVisible(true);
                        tipProgressBar.setIndeterminate(true);


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
                        tipLabel.setText(StringFormat.translate("task", "task.create_task.parsing_link"));
                        tipProgressBar.setVisible(true);
                        tipProgressBar.setIndeterminate(true);
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
                    JMenuItem pasteItem = new JMenuItem(StringFormat.translate("task", "task.create_task.download_url.paste"));
                    pasteItem.addActionListener(_ -> DownloaderURLTextArea.paste());
                    popupMenu.add(pasteItem);
                    popupMenu.show(DownloaderURLTextArea, e.getX(), e.getY());
                }
            }
        });
        ThreadNumSlider.addChangeListener(e -> {
            ThreadNumLabel.setText(String.valueOf(ThreadNumSlider.getValue()));
        });

        // 支持拖放文件/目录到下载链接输入框
        new DropTarget(DownloaderURLTextArea, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    // 接受复制操作
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable tr = dtde.getTransferable();

                    // 检查是否包含文件列表数据
                    if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        java.util.List<File> files = (java.util.List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);

                        // 构建路径字符串（每个文件一行）
                        StringBuilder sb = new StringBuilder();
                        for (File file : files) {
                            if (!sb.isEmpty()) sb.append("\n");
                            sb.append(file.getAbsolutePath());
                        }

                        // 追加到文本区域（考虑已有内容）
                        String current = DownloaderURLTextArea.getText();
                        if (current.isBlank()) {
                            DownloaderURLTextArea.setText(sb.toString());
                        } else {
                            DownloaderURLTextArea.append("\n" + sb.toString());
                        }

                        dtde.dropComplete(true);
                    } else {
                        dtde.rejectDrop();
                    }
                } catch (Exception e) {
                    dtde.rejectDrop();
                    logger.error("处理拖放文件时出错", e);
                }
            }
        });
    }




    private void createUIComponents() {
        // TODO: place custom component creation code here
        PathSelectionPanel = new PathSelectionPanel(StringFormat.translate("common", "save_path"), DataControl.getDownloadFilePath());

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
            var parser = Parser.getParser(link);
            JPanel linkFileInfoPanel;
            if (parser != null) {
                linkFileInfoPanel = parser.parse(link);
            }else{
                return;
            }

            if (linkFileInfoPanel != null) {
                linkFileInfoPanels.add(linkFileInfoPanel);
                linkInfoPanel.add(linkFileInfoPanel);
            }else{
                throw new LayerInstantiationException("链接解析出错");
            }
            MainPanel.revalidate();
            MainPanel.repaint();
        } catch (Exception e) {
            tipLabel.setText(StringFormat.translate("task", "task.create_task.error_link"));
            tipProgressBar.setVisible(false);
            ToastMessage.show(null, StringFormat.translate("task", "task.create_task.error_link") + ": " + link, ToastMessage.ERROR);
            logger.error("Error parsing link: " + link, e);
        }
    }

    public ArrayList<DownloadTask> getDownloadTasks() {
        var path = PathSelectionPanel.getPath();
        var mode = modeComboBox.getSelectedIndex();
        var threadNum = ThreadNumSlider.getValue();

        ArrayList<DownloadTask> downloadTasks = new ArrayList<>();
        for (var panel : linkFileInfoPanels) {
            if (panel instanceof LinkFileInfoPanel linkFileInfoPanel){
                if (linkFileInfoPanel.getMode().equals("HTTP"))
                    downloadTasks.add(new URLDownloadTask(linkFileInfoPanel.getFileName(), linkFileInfoPanel.getFileSizeNum(), URI.create(linkFileInfoPanel.getUrl()), new File(path), threadNum, mode));
                else if (linkFileInfoPanel.getMode().equals("bilibili")) {
                    if (linkFileInfoPanel instanceof BiliLinkFileInfoPanel biliLinkFileInfoPanel)
                        downloadTasks.add(new BiliFileDownloadTask(
                                biliLinkFileInfoPanel.getFileName(), biliLinkFileInfoPanel.getFileSize(),
                                biliLinkFileInfoPanel.getBiliDownloadUrl(), new File(path), threadNum, mode));
                }else if (linkFileInfoPanel.getMode().equals("douyin"))
                    downloadTasks.add(new DouyinVideoDownloadTask(
                            linkFileInfoPanel.getFileName(),
                            linkFileInfoPanel.getFileSizeNum(),
                            URI.create(linkFileInfoPanel.getUrl()),
                            new File(path), threadNum, mode
                    ));

                else if (linkFileInfoPanel.getMode().equals("BT-Torrent"))
                    downloadTasks.add(new TorrentFileDownloadTask(
                            new File(path),
                            linkFileInfoPanel.getFileName(),
                            linkFileInfoPanel.getUrl()));
            } else if (panel instanceof LinkFolderInfoPanel linkFolderPanel) {
                if (linkFolderPanel.getMode().equals("douyin"))
                    downloadTasks.add(new DouyinImageDownloadTask(
                            linkFolderPanel.getFolderName(),
                            linkFolderPanel.getSelectedUrls(),
                            linkFolderPanel.getFileNames(),
                            linkFolderPanel.getFileSizes(),
                            new File(path), threadNum, mode
                    ));
                else if (linkFolderPanel instanceof BiliLinkFolderInfoPanel biliLinkFolderInfoPanel)
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
        downloadTasks.addAll(moreDownloadTasks);
        return downloadTasks;
    }

    public void setLink(String link) {
        SwingUtilities.invokeLater(() -> {
            if (DownloaderURLTextArea.getText().strip().isEmpty()) {
                DownloaderURLTextArea.setText(link);
            } else {
                DownloaderURLTextArea.append("\n" + link);
            }
        });
    }
}

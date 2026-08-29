package com.wmp.downloader.newArchitecture.ui.createTask;

import com.wmp.downloader.exception.LinkParserException;
import com.wmp.downloader.newArchitecture.ParserTaskInfo;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractParser;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractTask;
import com.wmp.downloader.newArchitecture.abstractTask.linkInfoPanel.AbstractLinkInfoPanel;
import com.wmp.downloader.newArchitecture.abstractTask.linkInfoPanel.LinkFileInfoPanel;
import com.wmp.downloader.newArchitecture.abstractTask.linkInfoPanel.LinkFolderInfoPanel;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.ui.common.PathSelectionPanel;
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
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CreateTaskPanel {
    private final ArrayList<AbstractLinkInfoPanel> linkInfoPanels = new ArrayList<>();
    private final Logger logger = Logger.getLogger(CreateTaskPanel.class);
    // ---------- 新增：增量解析状态 ----------
    private final Map<String, JPanel> linkPanelMap = new LinkedHashMap<>();      // 链接 -> 已解析的面板
    private final Set<String> parsingLinks = ConcurrentHashMap.newKeySet();      // 正在解析的链接集合
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
    private ArrayList<AbstractTask> moreDownloadTasks = new ArrayList<>();

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
                } else if (mode.equals(StringFormat.translate("task", "task.create_task.choose_mode.single_threaded"))) {
                    ThreadNumSlider.setEnabled(false);
                }
            }
        });

        // ========== 修改：DocumentListener 使用增量更新 ==========
        DownloaderURLTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateLinks();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateLinks();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateLinks();
            }
        });

        // 右键粘贴菜单
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

        // 拖放支持
        new DropTarget(DownloaderURLTextArea, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable tr = dtde.getTransferable();
                    if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        java.util.List<File> files = (java.util.List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
                        StringBuilder sb = new StringBuilder();
                        for (File file : files) {
                            if (!sb.isEmpty()) sb.append("\n");
                            sb.append(file.getAbsolutePath());
                        }
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
        PathSelectionPanel = new PathSelectionPanel(StringFormat.translate("save_path"), DataControl.getDownloadFilePath());
        linkInfoPanel = new JPanel(new GridLayout(0, 1, 5, 5));
    }

    // ========== 核心：增量更新逻辑 ==========
    private void updateLinks() {
        // 获取当前所有非空行
        String text = DownloaderURLTextArea.getText();
        String[] lines = text.split("\n");
        Set<String> currentLinks = new LinkedHashSet<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                currentLinks.add(trimmed);
            }
        }

        // 1. 移除已删除的链接面板
        List<String> toRemove = new ArrayList<>();
        for (String link : linkPanelMap.keySet()) {
            if (!currentLinks.contains(link)) {
                toRemove.add(link);
            }
        }
        if (!toRemove.isEmpty()) {
            for (String link : toRemove) {
                JPanel panel = linkPanelMap.remove(link);
                if (panel != null) {
                    linkInfoPanel.remove(panel);
                    linkInfoPanels.remove(panel);
                }
            }
            linkInfoPanel.revalidate();
            linkInfoPanel.repaint();
            MainPanel.revalidate();
            MainPanel.repaint();
        }

        // 2. 添加新链接（启动解析）
        boolean hasNew = false;
        for (String link : currentLinks) {
            if (!linkPanelMap.containsKey(link) && !parsingLinks.contains(link)) {
                hasNew = true;
                parsingLinks.add(link);
                // 启动解析线程
                Thread.ofVirtual().start(() -> {
                    try {
                        var panel = parseLinkInternal(link);
                        if (panel != null) {
                            // 解析成功，检查该链接是否仍存在于文本中
                            SwingUtilities.invokeLater(() -> {
                                String currentText = DownloaderURLTextArea.getText();
                                String[] currentLines = currentText.split("\n");
                                boolean stillExists = false;
                                for (String l : currentLines) {
                                    if (l.trim().equals(link)) {
                                        stillExists = true;
                                        break;
                                    }
                                }
                                if (stillExists) {
                                    // 添加到界面
                                    linkPanelMap.put(link, panel);
                                    linkInfoPanels.add(panel);
                                    linkInfoPanel.add(panel);
                                    linkInfoPanel.revalidate();
                                    linkInfoPanel.repaint();
                                    MainPanel.revalidate();
                                    MainPanel.repaint();
                                }
                                // 否则丢弃面板
                            });
                        }
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(() -> {
                            tipLabel.setText(StringFormat.translate("task.create_task.error_link"));
                            ToastMessage.show(null, StringFormat.translate("task.create_task.error_link") + ": " + link, ToastMessage.ERROR);
                        });
                        logger.error("Error parsing link: " + link, e);
                    } finally {
                        parsingLinks.remove(link);
                        // 如果所有解析任务结束，隐藏进度指示
                        if (parsingLinks.isEmpty()) {
                            SwingUtilities.invokeLater(() -> {
                                tipLabel.setText("");
                                tipProgressBar.setVisible(false);
                            });
                        }
                    }
                });
            }
        }

        // 如果有新链接启动，显示解析进度
        if (hasNew) {
            tipLabel.setText(StringFormat.translate("task.create_task.parsing_link"));
            tipProgressBar.setVisible(true);
            tipProgressBar.setIndeterminate(true);
        }
    }

    // ========== 实际解析逻辑（只返回面板，不操作UI） ==========
    private AbstractLinkInfoPanel parseLinkInternal(String link) throws Exception {
        tipLabel.setText(StringFormat.translate("task.create_task.tip.finding_suitable_parser"));
        var parser = ParserTaskInfo.getParser(link);
        tipLabel.setText(StringFormat.translate("task.create_task.tip.parsering"));

        AbstractParser.Info info = null;
        if (parser != null) {
            info = parser.getParserInfo(link);
        }
        logger.debug(info);
        if (info == null) {
            return null;
        }
        tipLabel.setText(StringFormat.translate("task.create_task.tip.creating_parser_panel"));
        AbstractLinkInfoPanel panel = info.getLinkedInfoPanel();
        if (panel == null) {
            throw new LayerInstantiationException("链接解析出错");
        }
        return panel;
    }

    // ========== 获取所有下载任务（保持不变） ==========
    public ArrayList<AbstractTask> getDownloadTasks() {
        var path = PathSelectionPanel.getPath();
        var mode = modeComboBox.getSelectedIndex();
        var threadNum = ThreadNumSlider.getValue();

        ArrayList<AbstractTask> downloadTasks = new ArrayList<>();
        for (var panel : linkInfoPanels) {
            var infoJson = panel.getJsonInfo();
            infoJson.put("savePath", path);
            infoJson.put("threadMode", mode);
            infoJson.put("threadNum", threadNum);
            infoJson.put("linkStyle",
                    panel instanceof LinkFileInfoPanel?
                            0: (panel instanceof LinkFolderInfoPanel?1:-1));

            var info = panel.getInfo();
            //var info = parser.setLink(infoJson.getString("url"));
            AbstractTask task = null;
            try {
                task = info.getTask(infoJson);
            } catch (Exception e) {
                logger.error("加载失败");
            }
            if (task != null) downloadTasks.add(task);
            else {
                logger.error(String.format("解析失败，解析器：%s | 链接：%s | 数据：%s",
                        info.getParserID(), info.getLink(), panel.getJsonInfo()),
                        new LinkParserException("解析出的下载任务为空"));
                ToastMessage.show(
                        String.format(StringFormat.translate("task.create_failed"),
                                info.getLink()), ToastMessage.WARNING
                );
            }
        }
        downloadTasks.addAll(moreDownloadTasks);
        return downloadTasks;
    }

    public void setLink(String link) {
        SwingUtilities.invokeLater(() -> {
            if (DownloaderURLTextArea.getText().isBlank()) {
                DownloaderURLTextArea.setText(link);
            } else {
                DownloaderURLTextArea.append("\n" + link);
            }
        });
    }
}
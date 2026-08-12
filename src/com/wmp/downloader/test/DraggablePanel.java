package com.wmp.downloader.test;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DraggablePanel extends JPanel {
    private File tempFile;

    public DraggablePanel(String content) {
        setPreferredSize(new Dimension(150, 80));
        setBackground(Color.ORANGE);
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
        setLayout(new BorderLayout());
        add(new JLabel("拖拽我", SwingConstants.CENTER));

        DragSource dragSource = DragSource.getDefaultDragSource();

        dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY,
                dge -> {
                    try {
                        // 1. 创建临时文件
                        tempFile = File.createTempFile("panel_data_", ".txt");
                        try (FileWriter writer = new FileWriter(tempFile)) {
                            writer.write(content);
                        }

                        // 2. 构建文件列表
                        List<File> fileList = new ArrayList<>();
                        fileList.add(tempFile);

                        // 3. 创建 Transferable
                        Transferable transferable = new Transferable() {
                            @Override
                            public DataFlavor[] getTransferDataFlavors() {
                                return new DataFlavor[]{DataFlavor.javaFileListFlavor};
                            }

                            @Override
                            public boolean isDataFlavorSupported(DataFlavor flavor) {
                                return DataFlavor.javaFileListFlavor.equals(flavor);
                            }

                            @Override
                            public Object getTransferData(DataFlavor flavor)
                                    throws UnsupportedFlavorException, IOException {
                                if (isDataFlavorSupported(flavor)) {
                                    return fileList;
                                }
                                throw new UnsupportedFlavorException(flavor);
                            }
                        };

                        // 4. 抓取面板快照作为拖拽图像
                        BufferedImage snapshot = captureComponent(DraggablePanel.this);
                        // 计算偏移量：鼠标相对于面板左上角的位置
                        Point mouseLoc = dge.getDragOrigin();
                        Point panelLoc = getLocationOnScreen();
                        Point offset = new Point(mouseLoc.x - panelLoc.x, mouseLoc.y - panelLoc.y);
                        // 限制偏移量在图像范围内
                        offset.x = Math.clamp(offset.x, 0, snapshot.getWidth());
                        offset.y = Math.clamp(offset.y, 0, snapshot.getHeight());

                        // 5. 开始拖拽，使用自定义图像
                        dge.startDrag(
                                DragSource.DefaultCopyDrop,
                                snapshot,
                                offset,
                                transferable,
                                new DragSourceAdapter() {
                                    @Override
                                    public void dragDropEnd(DragSourceDropEvent dsde) {
                                        // 清理临时文件
                                        if (tempFile != null && tempFile.exists()) {
                                            tempFile.delete();
                                            tempFile = null;
                                        }
                                    }
                                }
                        );
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
    }

    /**
     * 捕获组件的图像
     */
    private BufferedImage captureComponent(Component component) {
        // 确保组件尺寸有效
        int w = component.getWidth();
        int h = component.getHeight();
        if (w == 0 || h == 0) {
            w = component.getPreferredSize().width;
            h = component.getPreferredSize().height;
            component.setSize(w, h);
            component.doLayout();
        }
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        if (component.isOpaque()) {
            g2d.setColor(component.getBackground());
            g2d.fillRect(0, 0, w, h);
        }
        component.paint(g2d);
        g2d.dispose();
        return image;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("拖拽快照演示");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            frame.add(new DraggablePanel("这是面板的数据"));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
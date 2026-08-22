package com.wmp.downloader.test;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.wmp.downloader.tools.ui.IconControl;

import javax.swing.*;
import java.awt.*;

public class CursorTest {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();

            JFrame frame = new JFrame("自定义光标示例");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(300, 200);

            Toolkit tk = Toolkit.getDefaultToolkit();
            Dimension bestSize = tk.getBestCursorSize(0, 0);
            System.out.println("最佳光标尺寸: " + bestSize);
            // 在 Windows 上通常为 32x32

            // 1. 加载图片 (请将 "my_hand.png" 替换为你的图片路径)
            Image cursorImage = IconControl.getImage("cursor_text_select", bestSize.width, bestSize.height);
            // 若图片在 JAR 包里, 可用:
            // URL imgUrl = CustomCursorDemo.class.getResource("my_hand.png");
            // Image cursorImage = Toolkit.getDefaultToolkit().getImage(imgUrl);

            // 2. 创建自定义光标 (热点在左上角)
            Point hotSpot = new Point(0, 0);
            Cursor customCursor = Toolkit.getDefaultToolkit()
                    .createCustomCursor(cursorImage, hotSpot, "My Hand Cursor");

            // 3. 应用自定义光标到整个窗口
            frame.setCursor(customCursor);


            // 添加一个按钮, 演示光标只作用于特定组件
            JButton button = new JButton("点我");
            // button.setCursor(customCursor); // 取消注释可让按钮也使用自定义光标

            frame.add(button);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

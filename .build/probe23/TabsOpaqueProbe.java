import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * 验证用组件级样式 "tabsOpaque: true" 能否让「顶栏」不透明，而内容区仍透明。
 *
 * 红色背景模拟主窗口背景图：
 *   采样顶栏区域 → 变深灰 = 顶栏不透明(成功)；仍红 = 透明
 *   采样内容区   → 仍红   = 内容区依旧透明(符合预期)
 */
public class TabsOpaqueProbe {

    static final File DIR = new File(".");
    static final int W = 700, H = 420;
    static Rectangle WIN;

    public static void main(String[] args) throws Exception {
        FlatDarkLaf.setup();
        // 模拟项目的全局设置
        UIManager.put("TabbedPane.tabsOpaque", false);
        UIManager.put("TabbedPane.contentOpaque", false);

        Robot robot = new Robot();
        Rectangle screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        WIN = new Rectangle((screen.width - W) / 2, (screen.height - H) / 2, W, H);

        final JFrame[] fh = new JFrame[1];
        final JTabbedPane[] tabsRef = new JTabbedPane[1];

        SwingUtilities.invokeAndWait(() -> {
            JFrame f = new JFrame("tabsopaque-probe");
            JPanel background = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    g.setColor(new Color(220, 30, 30));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            background.setLayout(new BorderLayout());

            JTabbedPane tabs = new JTabbedPane();
            tabs.setOpaque(false);
            tabs.addTab("任务", new JPanel());
            tabs.addTab("设置", new JPanel());
            tabs.addTab("关于", new JPanel());
            background.add(tabs, BorderLayout.CENTER);

            f.setContentPane(background);
            f.setBounds(WIN);
            f.setAlwaysOnTop(true);
            f.setVisible(true);
            fh[0] = f;
            tabsRef[0] = tabs;
        });
        Thread.sleep(1300);

        sample(robot, screen, tabsRef[0], "A 默认(tabsOpaque=false)  ");

        SwingUtilities.invokeAndWait(() -> {
            tabsRef[0].putClientProperty(FlatClientProperties.STYLE, "tabsOpaque: true");
            SwingUtilities.updateComponentTreeUI(tabsRef[0]);
        });
        Thread.sleep(1000);

        sample(robot, screen, tabsRef[0], "B style tabsOpaque: true  ");

        SwingUtilities.invokeAndWait(() -> fh[0].dispose());
        System.out.println("PROBE_DONE");
        System.exit(0);
    }

    static void sample(Robot robot, Rectangle screen, JTabbedPane tabs, String name) {
        BufferedImage shot = robot.createScreenCapture(screen);
        try {
            ImageIO.write(shot, "png", new File(DIR, "tabsopaque_" + name.trim().charAt(0) + ".png"));
        } catch (Exception ignored) {
        }
        try {
            Point p = tabs.getLocationOnScreen();
            Rectangle r = tabs.getBounds();
            int tabY = p.y + 15;                       // 顶栏内
            int contentY = p.y + r.height - 40;        // 内容区内
            StringBuilder sb = new StringBuilder(name);
            for (int x : new int[]{p.x + r.width - 150, p.x + r.width - 60}) {
                Color c = new Color(shot.getRGB(x, tabY));
                sb.append(String.format("  顶栏(x-%-4d)=(%3d,%3d,%3d)", p.x + r.width - x,
                        c.getRed(), c.getGreen(), c.getBlue()));
            }
            Color cc = new Color(shot.getRGB(p.x + r.width / 2, contentY));
            sb.append(String.format("  内容区=(%3d,%3d,%3d)", cc.getRed(), cc.getGreen(), cc.getBlue()));
            System.out.println(sb);
        } catch (Exception e) {
            System.out.println(name + " 采样失败: " + e);
        }
    }
}

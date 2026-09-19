package com.wmp.speed_bump.platform.test;

import com.wmp.downloader.tools.ui.WindowBackdrop;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * 背景材质演示：无边框 + per-pixel 透明窗口 + 原生 backdrop。
 *
 * <p>运行后可直接点击按钮切换 Mica / Acrylic / 模糊。窗口内空白处按住左键可拖动。</p>
 *
 * <p>要点：{@code setUndecorated(true)} 必须在 {@code setVisible(true)} 之前，
 * 否则会抛 IllegalComponentStateException；透明与材质都依赖这一点。</p>
 */
public class MicaWindowDemo {

    private static final int TINT_ABGR = 0x66202020;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MicaWindowDemo::createAndShow);
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("Mica 演示");
        frame.setUndecorated(true);                      // 必须在显示之前
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 70)));

        // ---- 自定义标题栏（无边框窗口必须自绘） ----
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);
        titleBar.setBorder(BorderFactory.createEmptyBorder(10, 14, 6, 10));

        JLabel title = new JLabel("Mica / 模糊窗口演示");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Microsoft YaHei", Font.BOLD, 15));
        titleBar.add(title, BorderLayout.WEST);

        JButton close = new JButton("✕");
        close.setFocusPainted(false);
        close.setContentAreaFilled(false);
        close.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        close.setForeground(Color.WHITE);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> System.exit(0));
        titleBar.add(close, BorderLayout.EAST);
        root.add(titleBar, BorderLayout.NORTH);

        // ---- 中间：半透明内容区，让材质透出来 ----
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        int build = -1;
        try {
            build = WindowBackdrop.windowsMajorVersion();
        } catch (Throwable ignored) {
        }

        JLabel info = new JLabel(" " + describe());
        info.setForeground(Color.WHITE);
        info.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        info.setAlignmentX(0.5f);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.add(materialButton(frame, "Mica (Win11)", WindowBackdrop.Material.MICA));
        buttons.add(materialButton(frame, "Acrylic", WindowBackdrop.Material.ACRYLIC));
        buttons.add(materialButton(frame, "模糊 (Win10)", WindowBackdrop.Material.BLUR));
        buttons.add(materialButton(frame, "关闭材质", WindowBackdrop.Material.NONE));

        JLabel hint = new JLabel("按住空白处可拖动窗口");
        hint.setForeground(new Color(255, 255, 255, 150));
        hint.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        hint.setAlignmentX(0.5f);

        body.add(Box.createVerticalGlue());
        body.add(info);
        body.add(Box.createVerticalStrut(12));
        body.add(buttons);
        body.add(Box.createVerticalStrut(12));
        body.add(hint);
        body.add(Box.createVerticalGlue());
        root.add(body, BorderLayout.CENTER);

        frame.setContentPane(root);
        frame.setSize(560, 340);
        frame.setLocationRelativeTo(null);

        // 关键：让它成为 per-pixel 透明窗口，否则 DWM 的材质会被不透明客户区完全遮住
        WindowBackdrop.prepareTransparent(frame);

        installDragSupport(frame, root);

        frame.setVisible(true);
        SwingUtilities.invokeLater(() -> {
            boolean ok = WindowBackdrop.apply(frame, WindowBackdrop.Material.MICA, TINT_ABGR);
            System.out.println("hwnd = 0x" + Long.toHexString(WindowBackdrop.hwndOf(frame)));
            System.out.println("apply(MICA) = " + ok + "  ->  " + describe());
        });
    }

    private static String describe() {
        if (!WindowBackdrop.isSupported()) {
            return "当前环境不支持原生材质（非 Windows 或无原生访问权限）";
        }
        int build = WindowBackdrop.windowsBuild();
        if (WindowBackdrop.windowsMajorVersion() >= 11) {
            return "Windows 11 build " + build + "：DWM Mica 可用";
        }
        return "Windows 10 build " + build + "：Mica 不可用，自动回退为窗口背后内容模糊";
    }

    private static JButton materialButton(JFrame frame, String text, WindowBackdrop.Material material) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.addActionListener(e -> {
            boolean ok = WindowBackdrop.apply(frame, material, TINT_ABGR);
            System.out.println("apply(" + material + ") = " + ok);
        });
        return b;
    }

    /** 无边框窗口需要自己实现拖动 */
    private static void installDragSupport(JFrame frame, JPanel root) {
        Point[] origin = new Point[1];
        root.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                origin[0] = e.getPoint();
            }
        });
        root.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (origin[0] == null) {
                    return;
                }
                Point p = frame.getLocation();
                frame.setLocation(p.x + e.getX() - origin[0].x, p.y + e.getY() - origin[0].y);
            }
        });
    }

    /** 未使用的占位，保留自定义绘制示例 */
    @SuppressWarnings("unused")
    private static JPanel roundedPanel(Color fill) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(200, 100);
            }
        };
    }
}

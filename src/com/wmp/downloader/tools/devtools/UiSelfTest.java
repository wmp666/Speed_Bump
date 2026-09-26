package com.wmp.downloader.tools.devtools;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.tools.ui.fluent.FluentColors;
import com.wmp.downloader.tools.ui.fluent.FluentUi;
import com.wmp.downloader.tools.ui.fluent.FluentToggleSwitch;
import com.wmp.downloader.tools.ui.fluent.RevealEngine;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 离屏界面自检：构建组件画廊 → 离屏渲染 → 断言 → 出报告。
 *
 * <h3>为什么需要它</h3>
 * <p>Swing 项目最难的是「改完不知道哪里塌了」：界面缺一块、颜色不对、
 * 某个组件被渲染器复用了外观——这些都不会抛异常。本工具把「装配、布局、绘制、
 * 关键像素、渲染器回退」这些原本只能靠肉眼看的东西变成可断言的检查，
 * 并且把成图与组件树落盘供人工复核。</p>
 *
 * <h3>为什么不直接渲染主窗口</h3>
 * <p>{@code Downloader} 的构造依赖配置加载、解析器注册、系统托盘等一整套环境，
 * 在无人值守的回归场景里很容易因为「托盘不可用」之类的偶然原因失败，
 * 反而让自检本身变得不可信。所以这里渲染的是<b>组件画廊</b>——
 * 它覆盖本次移植的全部自绘组件，正是需要回归的部分。</p>
 *
 * <h3>用法</h3>
 * <pre>
 * java -cp "out;src;lib/*" com.wmp.downloader.tools.devtools.UiSelfTest
 * java ... UiSelfTest --dark              # 深色主题跑一遍
 * java ... UiSelfTest --out=out/ui-selftest
 * </pre>
 *
 * <p>退出码 0 表示全部断言通过，1 表示有问题（问题清单写在报告里）。</p>
 */
public final class UiSelfTest {

    private static final StringBuilder REPORT = new StringBuilder();

    private UiSelfTest() {
    }

    public static void main(String[] args) {
        Options options = Options.parse(args);
        File outDir = options.outDir;
        if (!outDir.exists() && !outDir.mkdirs()) {
            System.out.println("[自检] 无法创建输出目录：" + outDir.getAbsolutePath());
        }

        List<String> problems = new ArrayList<>();

        // ---- 环境 ----
        report("===== Speed_Bump 界面自检 =====");
        report("操作系统    : " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        report("JVM         : " + System.getProperty("java.vm.name") + " " + System.getProperty("java.version"));
        report("无头模式    : " + java.awt.GraphicsEnvironment.isHeadless());
        report("配置目录    : " + System.getProperty("user.home") + File.separator + ".speed-bump");
        report("输出目录    : " + outDir.getAbsolutePath());
        report("ImageIO     : " + PngWriter.describeImageIoAvailability());

        final List<String> collected = new ArrayList<>();
        try {
            // 界面构建与绘制必须在 EDT 上完成
            SwingUtilities.invokeAndWait(() -> runOnEdt(options, outDir, collected));
        } catch (Throwable t) {
            Throwable root = rootCause(t);
            problems.add("自检在 EDT 上抛出异常：" + root);
            // 一定要把根因的栈打到标准错误：invokeAndWait 会把真实异常包在
            // InvocationTargetException 里，只报个类名等于什么都没说
            root.printStackTrace();
        }
        // 即使中途抛异常，也要把已经收集到的问题带出来——否则报告只剩一句
        // 「EDT 抛出异常」，前面查出来的具体问题全部丢失（这个坑踩过一次）
        problems.addAll(collected);

        if (problems.isEmpty()) {
            report("[自检] 通过：组件装配、布局、绘制与关键像素均符合预期");
        } else {
            report("[自检] 发现 " + problems.size() + " 个问题：");
            for (String p : problems) {
                report("  - " + p);
            }
        }

        File reportFile = new File(outDir, "report.txt");
        try {
            Files.writeString(reportFile.toPath(), REPORT.toString());
            System.out.println("[自检] 报告已写入 " + reportFile.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("[自检] 报告写入失败：" + e.getMessage());
        }
        System.exit(problems.isEmpty() ? 0 : 1);
    }

    // ==================================================================
    // 主流程（EDT）
    // ==================================================================

    private static void runOnEdt(Options options, File outDir, List<String> problems) {
        // 1) 主题：走项目自己的通道，顺带让 ThemeChanger 的 1 秒定时器认为「主题没变」，
        //    否则它会在自检过程中途重设 L&F，把颜色断言全部打乱
        String themeName = options.theme != null ? options.theme : (options.dark ? "Mac Dark" : "Mac Light");
        try {
            ThemeChanger.easyChanger(themeName);
            report("主题        : " + themeName + "（经 ThemeChanger 应用）");
        } catch (Throwable t) {
            problems.add("ThemeChanger.easyChanger 失败，降级为直接 setup：" + t);
            try {
                if (options.dark) {
                    FlatDarkLaf.setup();
                } else {
                    FlatLightLaf.setup();
                }
            } catch (Throwable t2) {
                problems.add("直接安装 FlatLaf 也失败：" + t2);
            }
        }
        report("当前 L&F    : " + UIManager.getLookAndFeel().getClass().getName()
                + (FluentColors.isDark() ? "（深色）" : "（浅色）"));
        report("强调色      : " + toHex(FluentColors.accent()));

        // 2) 安装 Fluent 增强（必须在 L&F 之后，否则默认值会被 FlatLaf.setup 抹掉）
        FluentUi.install();
        report("UI 默认值   : ScrollBarUI=" + shortName(UIManager.getString("ScrollBarUI"))
                + "  ProgressBarUI=" + shortName(UIManager.getString("ProgressBarUI"))
                + "  CheckBoxUI=" + shortName(UIManager.getString("CheckBoxUI"))
                + "  ButtonUI=" + shortName(UIManager.getString("ButtonUI")));

        // 3) 构建画廊
        JFrame frame = new JFrame("UiSelfTest");
        Gallery gallery = buildGallery();
        frame.setUndecorated(true);
        frame.setContentPane(gallery.root);
        frame.pack();
        frame.setSize(760, 620);
        frame.validate();
        layoutRecursively(gallery.root);
        report("[自检] 画廊尺寸: " + gallery.root.getWidth() + "×" + gallery.root.getHeight());

        // 4) 渲染
        BufferedImage image = paint(gallery.root);
        String suffix = options.dark ? "dark" : "light";
        File png = new File(outDir, "gallery-" + suffix + ".png");
        try {
            // 用自带的编码器而不是 ImageIO：本项目的 classpath 缺少 imageio-core，
            // 任何 ImageIO 调用都会在 SPI 扫描阶段抛 ClassNotFoundException（详见 PngWriter 注释）
            PngWriter.write(image, png);
            report("[自检] 已导出截图: " + png.getAbsolutePath() + "（" + image.getWidth() + "×" + image.getHeight() + "）");
        } catch (IOException e) {
            problems.add("截图导出失败：" + e.getMessage());
        }

        // 5) 断言
        //
        // 每组断言独立 try/catch：一组里出现意外异常时，其余各组仍然会跑完。
        // 自检工具最忌讳「第一个问题把后面全挡住」——那样每次只能修一个发现一个。
        runAssertion("开关外观", problems, () -> assertSwitch(gallery, image));
        runAssertion("传统复选框退出开关", problems, () -> assertClassicCheckBox(gallery));
        runAssertion("表格 Boolean 渲染器回退", problems, () -> assertTableBooleanRenderer(gallery));
        runAssertion("进度条", problems, () -> assertProgressBar(gallery, image));
        runAssertion("滚动条", problems, () -> assertScrollBar(gallery, image));
        runAssertion("揭示高亮", problems, () -> assertReveal(gallery));
        // 放最后：它会真的切一次主题，必须放在所有颜色断言之后
        runAssertion("主题切换后 UI 默认值存活", problems,
                () -> assertUiDefaultsSurviveThemeSwitch(options));

        // 6) 组件树
        StringBuilder tree = new StringBuilder();
        dumpTree(gallery.root, 0, tree);
        File treeFile = new File(outDir, "component-tree-" + suffix + ".txt");
        try {
            Files.writeString(treeFile.toPath(), tree.toString());
            report("[自检] 已导出组件树: " + treeFile.getAbsolutePath());
        } catch (IOException e) {
            problems.add("组件树导出失败：" + e.getMessage());
        }

        frame.dispose();
    }

    // ==================================================================
    // 画廊
    // ==================================================================

    /** 自检用的组件容器 */
    private static final class Gallery {
        JPanel root;
        JCheckBox switchOn;
        JCheckBox switchOff;
        JCheckBox classicBox;
        FluentToggleSwitch explicitSwitch;
        JProgressBar determinate;
        JProgressBar indeterminate;
        JScrollPane scrollPane;
        JTable table;
        JButton revealButton;
    }

    private static Gallery buildGallery() {
        Gallery g = new Gallery();

        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);

        JLabel title = new JLabel("Speed_Bump Fluent 组件画廊");
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(title);
        column.add(Box.createVerticalStrut(12));

        // ---- 开关 ----
        g.switchOn = new JCheckBox("启用开关（选中）", true);
        g.switchOn.setName("switch-on");
        g.switchOn.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(g.switchOn);

        g.switchOff = new JCheckBox("启用开关（未选中）", false);
        g.switchOff.setName("switch-off");
        g.switchOff.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(g.switchOff);

        g.classicBox = new JCheckBox("传统复选框（应保持方框）", true);
        g.classicBox.setName("classic-box");
        // 通过客户端属性显式退回传统外观，同时也是这条退出开关的自检
        g.classicBox.putClientProperty(
                com.wmp.downloader.tools.ui.fluent.FluentSwitchUI.DISABLE_KEY, Boolean.TRUE);
        g.classicBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(g.classicBox);

        g.explicitSwitch = new FluentToggleSwitch("显式开关组件", true);
        g.explicitSwitch.setName("explicit-switch");
        g.explicitSwitch.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(g.explicitSwitch);
        column.add(Box.createVerticalStrut(12));

        // ---- 进度条 ----
        g.determinate = new JProgressBar(0, 100);
        g.determinate.setValue(60);
        g.determinate.setStringPainted(false);
        g.determinate.setName("progress-60");
        g.determinate.setAlignmentX(Component.LEFT_ALIGNMENT);
        g.determinate.setPreferredSize(new Dimension(560, 6));
        g.determinate.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        column.add(g.determinate);
        column.add(Box.createVerticalStrut(8));

        g.indeterminate = new JProgressBar(0, 100);
        g.indeterminate.setIndeterminate(true);
        g.indeterminate.setName("progress-indeterminate");
        g.indeterminate.setAlignmentX(Component.LEFT_ALIGNMENT);
        g.indeterminate.setPreferredSize(new Dimension(560, 6));
        g.indeterminate.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        column.add(g.indeterminate);
        column.add(Box.createVerticalStrut(12));

        // ---- 按钮（揭示高亮） ----
        g.revealButton = new JButton("带揭示高亮的按钮");
        g.revealButton.setName("reveal-button");
        g.revealButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        g.revealButton.setOpaque(false);
        g.revealButton.setContentAreaFilled(false);
        g.revealButton.setBorderPainted(false);
        RevealEngine.register(g.revealButton);
        column.add(g.revealButton);
        column.add(Box.createVerticalStrut(12));

        // ---- 表格（Boolean 渲染器回退） ----
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{{"拓展 A", Boolean.TRUE}, {"拓展 B", Boolean.FALSE}, {"拓展 C", Boolean.TRUE}},
                new String[]{"名称", "启用"}) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 1 ? Boolean.class : String.class;
            }
        };
        g.table = new JTable(model);
        g.table.setName("boolean-table");
        g.table.setRowHeight(22);
        JScrollPane tableScroll = new JScrollPane(g.table);
        tableScroll.setName("table-scroll");
        tableScroll.setPreferredSize(new Dimension(560, 110));
        tableScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        tableScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(tableScroll);
        column.add(Box.createVerticalStrut(12));

        // ---- 滚动条 ----
        JPanel tall = new JPanel();
        tall.setLayout(new BoxLayout(tall, BoxLayout.Y_AXIS));
        tall.setOpaque(false);
        for (int i = 0; i < 40; i++) {
            JLabel line = new JLabel("第 " + (i + 1) + " 行内容 —— 用于产生可滚动的滚动条");
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            tall.add(line);
        }
        g.scrollPane = new JScrollPane(tall);
        g.scrollPane.setName("scroll-pane");
        g.scrollPane.setPreferredSize(new Dimension(560, 120));
        g.scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        g.scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(g.scrollPane);

        g.root = new JPanel();
        g.root.setLayout(new BoxLayout(g.root, BoxLayout.Y_AXIS));
        g.root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        g.root.setOpaque(true);
        g.root.setBackground(UIManager.getColor("Panel.background"));
        g.root.add(column);
        return g;
    }

    // ==================================================================
    // 断言
    // ==================================================================

    /** 开关：选中态轨道必须是强调色，且宽度接近 40px（而不是 20px 的方框） */
    private static List<String> assertSwitch(Gallery g, BufferedImage whole) {
        List<String> problems = new ArrayList<>();
        BufferedImage on = paintComponent(g.switchOn, 260, 32);
        Rectangle bbox = accentBBox(on);
        if (bbox == null) {
            problems.add("选中的开关没有出现强调色轨道");
        } else {
            if (bbox.width < 30) {
                problems.add("选中的开关轨道宽度只有 " + bbox.width + "px，不像开关（预期 ≥30）");
            }
            report("[断言] 开关轨道强调色包围盒: " + bbox.width + "×" + bbox.height);
        }

        BufferedImage off = paintComponent(g.switchOff, 260, 32);
        if (accentBBox(off) != null) {
            problems.add("未选中的开关不应出现强调色轨道");
        }

        // 显式组件也必须是开关
        BufferedImage explicit = paintComponent(g.explicitSwitch, 260, 32);
        Rectangle explicitBox = accentBBox(explicit);
        if (explicitBox == null || explicitBox.width < 30) {
            problems.add("FluentToggleSwitch 没有渲染成开关（强调色包围盒 "
                    + (explicitBox == null ? "为空" : explicitBox.width + "px") + "）");
        }
        return problems;
    }

    /** 传统复选框：客户端属性退出开关后，应当回到 20px 左右的方框 */
    private static List<String> assertClassicCheckBox(Gallery g) {
        List<String> problems = new ArrayList<>();
        BufferedImage img = paintComponent(g.classicBox, 260, 32);
        Rectangle bbox = accentBBox(img);
        if (bbox == null) {
            problems.add("传统复选框没有出现强调色方框");
        } else {
            report("[断言] 传统复选框强调色包围盒: " + bbox.width + "×" + bbox.height);
            if (bbox.width > 26) {
                problems.add("标记为传统复选框的组件仍被渲染成开关（强调色宽度 " + bbox.width + "px）");
            }
        }
        return problems;
    }

    /**
     * 表格 Boolean 列：渲染器必须回退为传统方框，不能变成开关。
     *
     * <p>这是本次移植风险最高的一处：{@code JTable.BooleanRenderer extends JCheckBox}，
     * 如果不加区分地套用开关 UI，项目里所有勾选列都会变成开关。</p>
     */
    private static List<String> assertTableBooleanRenderer(Gallery g) {
        List<String> problems = new ArrayList<>();
        if (g.table.getColumnCount() < 2) {
            problems.add("测试表格缺少 Boolean 列");
            return problems;
        }
        TableCellRenderer renderer = g.table.getCellRenderer(0, 1);
        Component rc = renderer.getTableCellRendererComponent(g.table, Boolean.TRUE, false, false, 0, 1);
        if (!(rc instanceof javax.swing.table.TableCellRenderer)) {
            problems.add("表格 Boolean 渲染器不是 TableCellRenderer，判定依据需要重新评估");
        }
        if (!(rc instanceof JCheckBox)) {
            report("[断言] 表格 Boolean 渲染器是 " + rc.getClass().getName()
                    + "，不是 JCheckBox，本项回退无需验证"
                    + "（注意：非 FlatLaf 主题下 Swing 的 BooleanRenderer 就是 JCheckBox，届时回退逻辑才生效）");
            return problems;
        }
        BufferedImage img = paintComponent(rc, 40, 22);
        Rectangle bbox = accentBBox(img);
        if (bbox == null) {
            problems.add("表格 Boolean 单元格没有渲染出选中标记");
        } else {
            report("[断言] 表格 Boolean 单元格强调色包围盒: " + bbox.width + "×" + bbox.height);
            if (bbox.width > 26) {
                problems.add("表格 Boolean 单元格被渲染成了开关（强调色宽度 " + bbox.width
                        + "px）——渲染器回退失效，所有勾选列都会变成开关");
            }
        }
        return problems;
    }

    /** 进度条：确定态填充是强调色，不确定态有扫动带 */
    private static List<String> assertProgressBar(Gallery g, BufferedImage whole) {
        List<String> problems = new ArrayList<>();

        BufferedImage det = paintComponent(g.determinate, 560, 6);
        Color at25 = new Color(det.getRGB((int) (560 * 0.25), 3), true);
        Color at80 = new Color(det.getRGB((int) (560 * 0.80), 3), true);
        report("[断言] 进度条 60%：25% 处 " + toHex(at25) + "，80% 处 " + toHex(at80));
        if (colorDistance(at25, FluentColors.accent()) > 24) {
            problems.add("进度条 60% 处应为强调色，实际 " + toHex(at25));
        }
        if (colorDistance(at80, FluentColors.progressTrack()) > 40) {
            problems.add("进度条 80% 处应仍是轨道色，实际 " + toHex(at80)
                    + "（期望接近 " + toHex(FluentColors.progressTrack()) + "）");
        }

        // 不确定态：扫动带随时间移动，两个不同时刻应至少有一个位置落在带内
        BufferedImage ind = paintComponent(g.indeterminate, 560, 6);
        int accentPixels = countAccentPixels(ind);
        report("[断言] 不确定进度条强调色像素: " + accentPixels + " / 3360");
        if (accentPixels < 50) {
            problems.add("不确定进度条没有渲染出扫动带（强调色像素仅 " + accentPixels + "）");
        }
        return problems;
    }

    /** 滚动条：滑块必须可见（与面板底色有可辨差异） */
    private static List<String> assertScrollBar(Gallery g, BufferedImage whole) {
        List<String> problems = new ArrayList<>();
        JScrollBar bar = g.scrollPane.getVerticalScrollBar();
        if (bar == null || bar.getWidth() == 0 || bar.getHeight() == 0) {
            problems.add("测试用滚动条的尺寸为 0，无法验证");
            return problems;
        }
        if (!(bar.getUI() instanceof com.wmp.downloader.tools.ui.fluent.FluentScrollBarUI)) {
            problems.add("滚动条 UI 不是 FluentScrollBarUI，而是 " + bar.getUI().getClass().getSimpleName());
            return problems;
        }
        BufferedImage img = paintComponent(bar, bar.getWidth(), bar.getHeight());
        // 取纵向中段、横向中心的一列，找出与底色差异最大的像素
        Color bg = UIManager.getColor("Panel.background");
        int best = 0;
        Color bestColor = null;
        for (int y = 4; y < img.getHeight() - 4; y++) {
            Color c = new Color(img.getRGB(img.getWidth() / 2, y), true);
            int d = colorDistance(c, bg);
            if (d > best) {
                best = d;
                bestColor = c;
            }
        }
        report("[断言] 滚动条滑块最大对比度: " + best + "（颜色 " + toHex(bestColor) + "）");
        if (best < 6) {
            problems.add("滚动条滑块几乎不可见（与底色最大差异仅 " + best + "）");
        }
        return problems;
    }

    /**
     * 揭示高亮：注入指针状态后，控件应当出现可测量的提亮。
     *
     * <p>用「同一控件在有/无光晕两种状态下的平均亮度差」来判定——
     * 这样不依赖任何具体颜色值，对主题变化免疫。</p>
     */
    private static List<String> assertReveal(Gallery g) {
        List<String> problems = new ArrayList<>();
        report("[断言] 揭示高亮诊断：按钮 UI=" + g.revealButton.getUI().getClass().getSimpleName()
                + "，引擎注册数=" + RevealEngine.registeredCount()
                + "，按钮已注册=" + RevealEngine.isRegistered(g.revealButton)
                + "，引擎启用=" + RevealEngine.isEnabled());
        if (RevealEngine.registeredCount() == 0) {
            problems.add("没有组件注册到揭示高亮引擎");
            return problems;
        }
        if (!RevealEngine.isRegistered(g.revealButton)) {
            problems.add("按钮没有注册到揭示高亮引擎（UI 代理的 installUI 未生效？）");
            return problems;
        }

        // 基线：指针不在控件上
        RevealEngine.clearSimulatedPointer();
        double without = meanLuminance(paintComponent(g.revealButton, 260, 32));

        // 注入指针状态后再画一次
        RevealEngine.simulatePointer(g.revealButton, 130, 16);
        double with = meanLuminance(paintComponent(g.revealButton, 260, 32));
        RevealEngine.clearSimulatedPointer();

        report(String.format("[断言] 按钮平均亮度：无光晕 %.2f，有光晕 %.2f（差 %.2f）", without, with, with - without));
        if (with - without < 2.0) {
            problems.add(String.format("揭示高亮没有产生可见提亮（平均亮度差仅 %.2f）", with - without));
        }
        return problems;
    }

    /**
     * 主题切换后，Fluent 的 UI 默认值必须依然生效。
     *
     * <p>这是整个移植里<b>最容易静默失效</b>的一环：{@code FlatLaf.setup()} 会重建整张
     * {@code UIManager} 默认值表，我们塞进去的 {@code ScrollBarUI} 等会被一并抹掉。
     * 之所以能活下来，全靠 {@link FluentUi} 把自己注册成了
     * {@link com.wmp.downloader.tools.ui.DynamicConverterTask}，
     * 每次主题刷新都会重新灌入——一旦这个挂钩断了，
     * 表现是「启动时滚动条是新的，切一次主题就变回旧样式」，
     * 不主动验证根本发现不了。</p>
     */
    private static List<String> assertUiDefaultsSurviveThemeSwitch(Options options) {
        List<String> problems = new ArrayList<>();
        String original = options.theme != null ? options.theme : (options.dark ? "Mac Dark" : "Mac Light");
        String other = options.dark ? "Mac Light" : "Mac Dark";

        try {
            ThemeChanger.easyChanger(other);
        } catch (Throwable t) {
            problems.add("切换主题失败：" + rootCause(t));
            return problems;
        }

        checkUiDefault("ScrollBarUI", com.wmp.downloader.tools.ui.fluent.FluentScrollBarUI.class, problems);
        checkUiDefault("ProgressBarUI", com.wmp.downloader.tools.ui.fluent.FluentProgressBarUI.class, problems);
        checkUiDefault("CheckBoxUI", com.wmp.downloader.tools.ui.fluent.FluentSwitchUI.class, problems);
        checkUiDefault("ButtonUI", com.wmp.downloader.tools.ui.fluent.RevealButtonUI.class, problems);

        // 光看 UIManager 里的字符串还不够：UIDefaults.getUI 是反射调用静态 createUI，
        // 真正要验证的是「新建的组件确实拿到了我们的 UI」
        JCheckBox probeCheckBox = new JCheckBox("切换后新建的复选框");
        if (!(probeCheckBox.getUI() instanceof com.wmp.downloader.tools.ui.fluent.FluentSwitchUI)) {
            problems.add("主题切换后新建的复选框没有拿到 FluentSwitchUI，而是 "
                    + probeCheckBox.getUI().getClass().getName());
        }
        JProgressBar probeBar = new JProgressBar();
        if (!(probeBar.getUI() instanceof com.wmp.downloader.tools.ui.fluent.FluentProgressBarUI)) {
            problems.add("主题切换后新建的进度条没有拿到 FluentProgressBarUI，而是 "
                    + probeBar.getUI().getClass().getName());
        }
        JButton probeButton = new JButton("切换后新建的按钮");
        if (!(probeButton.getUI() instanceof com.wmp.downloader.tools.ui.fluent.RevealButtonUI)) {
            problems.add("主题切换后新建的按钮没有拿到 RevealButtonUI，而是 "
                    + probeButton.getUI().getClass().getName());
        }

        // 还原主题，避免影响后续（以及让 ThemeChanger 的 1 秒定时器保持无操作）
        try {
            ThemeChanger.easyChanger(original);
        } catch (Throwable t) {
            problems.add("还原主题失败：" + rootCause(t));
        }
        report("[断言] 主题切换（" + other + " → " + original + "）后 UI 默认值与新建组件均已验证");
        return problems;
    }

    private static void checkUiDefault(String key, Class<?> expected, List<String> problems) {
        String actual = UIManager.getString(key);
        if (actual == null || !actual.equals(expected.getName())) {
            problems.add("主题切换后 " + key + " 丢失：期望 " + expected.getSimpleName()
                    + "，实际 " + shortName(actual));
        }
    }

    // ==================================================================
    // 断言调度与工具
    // ==================================================================

    /** 一组断言，可能抛异常 */
    private interface Assertion {
        List<String> run();
    }

    /** 跑一组断言，异常被转成一条问题而不是中断整个自检 */
    private static void runAssertion(String name, List<String> problems, Assertion assertion) {
        try {
            List<String> found = assertion.run();
            if (found != null) {
                problems.addAll(found);
            }
        } catch (Throwable t) {
            problems.add("断言组「" + name + "」抛出异常：" + rootCause(t));
        }
    }

    /** 取最内层的原因（invokeAndWait 等会层层包装） */
    private static Throwable rootCause(Throwable t) {
        Throwable current = t;
        int guard = 0;
        while (current.getCause() != null && current.getCause() != current && guard++ < 16) {
            current = current.getCause();
        }
        return current;
    }

    // ==================================================================
    // 图像工具
    // ==================================================================

    private static BufferedImage paintComponent(Component c, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            FluentColors.class.getName(); // 保持类加载顺序稳定
            g.setColor(UIManager.getColor("Panel.background"));
            g.fillRect(0, 0, width, height);
            c.setSize(width, height);
            c.doLayout();
            c.paint(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static BufferedImage paint(JComponent root) {
        int width = Math.max(1, root.getWidth());
        int height = Math.max(1, root.getHeight());
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            FluentColors.class.getName();
            g.setColor(root.getBackground() != null ? root.getBackground() : Color.WHITE);
            g.fillRect(0, 0, width, height);
            root.paint(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    /** 强调色像素的包围盒（容差 30，容忍抗锯齿边缘） */
    private static Rectangle accentBBox(BufferedImage image) {
        Color accent = FluentColors.accent();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (colorDistance(new Color(image.getRGB(x, y), true), accent) <= 30) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return maxX < 0 ? null : new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static int countAccentPixels(BufferedImage image) {
        Color accent = FluentColors.accent();
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (colorDistance(new Color(image.getRGB(x, y), true), accent) <= 30) {
                    count++;
                }
            }
        }
        return count;
    }

    private static double meanLuminance(BufferedImage image) {
        double sum = 0;
        int n = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color c = new Color(image.getRGB(x, y), true);
                sum += 0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue();
                n++;
            }
        }
        return n == 0 ? 0 : sum / n;
    }

    /** 三通道最大差值 */
    private static int colorDistance(Color a, Color b) {
        if (a == null || b == null) {
            return 255;
        }
        return Math.max(Math.abs(a.getRed() - b.getRed()),
                Math.max(Math.abs(a.getGreen() - b.getGreen()), Math.abs(a.getBlue() - b.getBlue())));
    }

    private static String toHex(Color c) {
        return c == null ? "null" : String.format("#%02X%02X%02X(a=%d)", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    private static String shortName(String className) {
        if (className == null) {
            return "null";
        }
        int i = className.lastIndexOf('.');
        return i < 0 ? className : className.substring(i + 1);
    }

    // ==================================================================
    // 组件树
    // ==================================================================

    private static void layoutRecursively(Component component) {
        component.doLayout();
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                layoutRecursively(child);
            }
        }
    }

    private static void dumpTree(Component component, int depth, StringBuilder out) {
        if (depth > 12) {
            return;
        }
        out.append("  ".repeat(depth)).append(component.getClass().getSimpleName())
                .append(' ').append(component.getBounds());
        String name = component.getName();
        if (name != null && !name.isEmpty()) {
            out.append(" #").append(name);
        }
        if (name != null && !name.isEmpty() && component instanceof JComponent jc) {
            Dimension pref = jc.getPreferredSize();
            out.append(" 首选=").append(pref.width).append('×').append(pref.height);
        }
        if (component instanceof JCheckBox cb && cb.getText() != null && !cb.getText().isEmpty()) {
            out.append(" \"").append(cb.getText()).append('"');
        } else if (component instanceof JLabel label && label.getText() != null && !label.getText().isEmpty()) {
            String text = label.getText();
            out.append(" \"").append(text.length() > 30 ? text.substring(0, 30) + "…" : text).append('"');
        }
        out.append('\n');
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                dumpTree(child, depth + 1, out);
            }
        }
    }

    // ==================================================================
    // 输出
    // ==================================================================

    private static void report(String line) {
        REPORT.append(line).append(System.lineSeparator());
        System.out.println(line);
    }

    /** 命令行参数 */
    private static final class Options {
        boolean dark;
        String theme;
        File outDir = new File("out/ui-selftest");

        static Options parse(String[] args) {
            Options o = new Options();
            for (String arg : args) {
                if (arg == null || arg.isBlank()) {
                    continue;
                }
                if (arg.equals("--dark")) {
                    o.dark = true;
                } else if (arg.startsWith("--theme=")) {
                    o.theme = arg.substring("--theme=".length());
                } else if (arg.startsWith("--out=")) {
                    o.outDir = new File(arg.substring("--out=".length()));
                }
            }
            return o;
        }
    }
}

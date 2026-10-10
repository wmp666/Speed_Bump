package com.wmp.speed_bump.common.background.tools.devtools;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.speed_bump.platform.ui.swing.tools.fluent.*;
import com.wmp.speed_bump.platform.ui.swing.tools.fluent.FluentMetrics;
import com.wmp.speed_bump.platform.ui.swing.tools.fluent.FluentUi;
import com.wmp.speed_bump.common.background.tools.DynamicConverterTask;
import com.wmp.speed_bump.platform.ui.swing.tools.swingx.LoadingBusyLabelUI;
import com.wmp.speed_bump.platform.ui.swing.tools.swingx.LoadingBusyPainter;

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

        // 2) 安装 Fluent 增强与忙碌标签外观。
        //    实测结论：UIManager.put 写的是「用户默认值」，UIManager.setLookAndFeel 会保留它，
        //    所以放在 L&F 之后只是习惯，放在之前其实也不会被抹掉（见文档排查手册第 4 条）
        FluentUi.install();
        LoadingBusyLabelUI.install();
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
        runAssertion("禁用态文字灰化", problems, () -> assertDisabledText(gallery));
        runAssertion("HTML 复选框文本", problems, () -> assertHtmlCheckBox(gallery));
        runAssertion("忙碌标签加载动画", problems, () -> assertBusyLabel(gallery));
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
        JCheckBox textEnabledProbe;
        JCheckBox textDisabledProbe;
        JCheckBox htmlProbe;
        org.jdesktop.swingx.JXBusyLabel busyLabel;
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
                FluentSwitchUI.DISABLE_KEY, Boolean.TRUE);
        g.classicBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(g.classicBox);

        g.explicitSwitch = new FluentToggleSwitch("显式开关组件", true);
        g.explicitSwitch.setName("explicit-switch");
        g.explicitSwitch.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(g.explicitSwitch);

        // 禁用态文字对照：两个文案完全相同的复选框，一个启用一个禁用。
        // 「禁用后文字仍是黑的」这个 bug 就是因为原来没有覆盖禁用态而漏掉的
        g.textEnabledProbe = new JCheckBox("禁用态文字对照", true);
        g.textEnabledProbe.setName("text-enabled");
        g.textEnabledProbe.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(g.textEnabledProbe);

        g.textDisabledProbe = new JCheckBox("禁用态文字对照", true);
        g.textDisabledProbe.setName("text-disabled");
        g.textDisabledProbe.setAlignmentX(Component.LEFT_ALIGNMENT);
        g.textDisabledProbe.setEnabled(false);
        column.add(g.textDisabledProbe);

        // HTML 文本：设计器里确实有用 HTML 的复选框（ParserCompatibilityDialog），
        // 自绘 UI 必须把绘制交给 BasicHTML 的 View，否则会把标签原文画在界面上
        g.htmlProbe = new JCheckBox("<html><b>加粗</b>与<font color='#C42B1C'>着色</font></html>", true);
        g.htmlProbe.setName("html-checkbox");
        g.htmlProbe.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(g.htmlProbe);
        column.add(Box.createVerticalStrut(12));

        // ---- 忙碌标签（SwingX JXBusyLabel，用 LoadingBusyLabelUI 画滚动横杠） ----
        // Downloader 的空白页居中等待动画用的就是它；这里固定一帧来画静态截图，
        // 不调用 setBusy，避免自检期间有 Timer 在后台推帧导致截图不确定
        g.busyLabel = new org.jdesktop.swingx.JXBusyLabel(new Dimension(48, 48));
        g.busyLabel.setName("busy-label");
        g.busyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(g.busyLabel);
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
     * 禁用态文字必须「灰化」，且颜色取自主题的禁用文字色。
     *
     * <h3>为什么专门加这一组</h3>
     * <p>自绘 UI 一开始把文字交给 {@code BasicButtonUI.paintText} 绘制，
     * 而它在禁用态用的是 {@code background.brighter()} / {@code background.darker()}
     * 双描边的老式画法，<b>完全不读主题的禁用文字色</b>——
     * 在 FlatLaf 下 {@code darker()} 出来近乎纯黑，于是「禁用组件的文字反而显示为黑色」。
     * 原来的自检只看了选中/未选中，没有覆盖禁用态，所以没抓到。</p>
     *
     * <p>判定标准刻意做成与主题无关：<b>禁用态文字与背景的对比度必须小于启用态</b>。
     * 这比「断言某个具体颜色」更稳，也正好对应「灰化」这个词的语义。</p>
     */
    private static List<String> assertDisabledText(Gallery g) {
        List<String> problems = new ArrayList<>();
        int width = 260;
        int height = 32;
        // 只看文字区域，跳过左侧的开关轨道（轨道是强调色/控件色，会干扰取样）
        int textStart = FluentMetrics.SWITCH_WIDTH + FluentMetrics.SWITCH_GAP - 4;
        Color background = UIManager.getColor("Panel.background");

        BufferedImage enabled = paintComponent(g.textEnabledProbe, width, height);
        BufferedImage disabled = paintComponent(g.textDisabledProbe, width, height);

        Color enabledText = mostContrastingColor(enabled, textStart, width, background);
        Color disabledText = mostContrastingColor(disabled, textStart, width, background);

        Color expected = UIManager.getColor("CheckBox.disabledText");
        if (expected == null) {
            expected = UIManager.getColor("Button.disabledText");
        }
        int enabledContrast = colorDistance(enabledText, background);
        int disabledContrast = colorDistance(disabledText, background);
        report("[断言] 启用态文字 " + toHex(enabledText) + "（对比度 " + enabledContrast + "）"
                + "，禁用态文字 " + toHex(disabledText) + "（对比度 " + disabledContrast + "）"
                + "，主题禁用色 " + toHex(expected));

        if (enabledText == null || disabledText == null) {
            problems.add("取不到文字颜色，无法判定禁用态灰化");
            return problems;
        }
        if (disabledContrast >= enabledContrast) {
            problems.add("禁用态文字没有灰化：它与背景的对比度（" + disabledContrast
                    + "）不小于启用态（" + enabledContrast + "），字体看起来仍是正常色甚至更深");
        }
        if (expected != null && colorDistance(disabledText, expected) > 60) {
            problems.add("禁用态文字颜色与主题的禁用文字色不符：实际 " + toHex(disabledText)
                    + "，主题 " + toHex(expected));
        }
        return problems;
    }

    /**
     * HTML 文本的复选框不能被画成标签原文。
     *
     * <p>{@code BasicHTML} 会给 HTML 文本挂一个 {@code View}，由它负责绘制。
     * 自绘 UI 如果无条件调用 {@code paintText}，界面上就会出现一串 {@code <html>…}。</p>
     */
    private static List<String> assertHtmlCheckBox(Gallery g) {
        List<String> problems = new ArrayList<>();
        BufferedImage img = paintComponent(g.htmlProbe, 300, 32);
        Color background = UIManager.getColor("Panel.background");
        int textStart = FluentMetrics.SWITCH_WIDTH + FluentMetrics.SWITCH_GAP - 4;
        Object htmlView = g.htmlProbe.getClientProperty(
                javax.swing.plaf.basic.BasicHTML.propertyKey);

        // 主判据：文字的横向范围。
        // HTML 生效时画出来的是「加粗与着色」5 个字；若把标签原文当普通字符串画，
        // 画出来的是 50+ 字符的 `<html><b>…</b>…`，会被可用宽度截断，横向范围宽得多。
        // 两者相差数倍，比比对具体颜色稳得多。
        //
        // 副判据：偏红像素数。它不能写成「等于 #C42B1C」——文字是否走 LCD 次像素抗锯齿
        // 由外观与桌面设置决定（FlatLaf 显式关掉，Windows 系外观不关），一旦走 LCD，
        // 笔画会被冲淡成 #E3B8B4 这类浅红，离纯色差 150 开外（实测如此）。
        // 真正要区分的是「彩色文字」与「灰黑文字」，所以只看红色分量是否明显压过绿蓝。
        int minX = Integer.MAX_VALUE;
        int maxX = -1;
        int redPixels = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = textStart; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                if (colorDistance(new Color(argb, true), background) <= 6) {
                    continue;
                }
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                int r = (argb >> 16) & 0xFF;
                if (r - Math.max((argb >> 8) & 0xFF, argb & 0xFF) >= 25) {
                    redPixels++;
                }
            }
        }
        int textWidth = maxX < 0 ? 0 : maxX - minX + 1;
        report("[断言] HTML 复选框：BasicHTML 视图=" + (htmlView != null)
                + "，文字宽度=" + textWidth + "px，偏红像素=" + redPixels);

        if (textWidth == 0) {
            problems.add("HTML 复选框没有绘制出任何文本");
        } else if (textWidth > 180) {
            problems.add("HTML 复选框的文字横向范围过宽（" + textWidth + "px，可用宽度仅 "
                    + (300 - textStart) + "px），怀疑把 <html> 标签原文当普通字符串画了出来");
        }
        if (htmlView != null && redPixels == 0) {
            problems.add("HTML 复选框里 <font color='#C42B1C'> 的着色没有生效"
                    + "（没有任何偏红像素），怀疑绕过了 BasicHTML 的 View");
        }
        return problems;
    }

    /**
     * 忙碌标签（SwingX {@code JXBusyLabel}）：必须用 {@code LoadingBusyLabelUI} 画滚动横杠。
     *
     * <p>这里要验证的核心是<b>「画面由帧驱动」</b>：{@code JXBusyLabel} 自己有个 Timer
     * 每拍把 painter 的 {@code frame} 加一并重绘（项目里的空白页等待动画还额外用了一个
     * 80ms 的手动泵帧）。如果换成一个不读 {@code frame} 的实现，画面会静止不动，
     * 加载指示就变成了「假死」——而且它不抛异常、不报错，只能靠像素发现。</p>
     *
     * <p>另外验证两个集成点：停止态 {@code frame=-1} 不能崩，
     * 以及墨色必须跟随标签前景色（否则切换主题后会串色）。</p>
     */
    private static List<String> assertBusyLabel(Gallery g) {
        List<String> problems = new ArrayList<>();
        org.jdesktop.swingx.JXBusyLabel label = g.busyLabel;
        label.setSize(48, 48);

        String uiName = label.getUI().getClass().getSimpleName();
        org.jdesktop.swingx.painter.BusyPainter painter = label.getBusyPainter();
        String painterName = painter == null ? "null" : painter.getClass().getSimpleName();
        report("[断言] 忙碌标签 UI=" + uiName + "  painter=" + painterName
                + "  points=" + (painter == null ? "-" : painter.getPoints())
                + "  delay=" + label.getDelay() + "ms");

        if (!"LoadingBusyLabelUI".equals(uiName)) {
            problems.add("忙碌标签没有用上 LoadingBusyLabelUI，实际是 " + uiName
                    + "（SwingX 默认的 BasicBusyLabelUI 画的是旋转圆点）");
            return problems;
        }
        if (painter == null || !"LoadingBusyPainter".equals(painterName)) {
            problems.add("忙碌标签的 painter 不是 LoadingBusyPainter，实际是 " + painterName);
            return problems;
        }

        // 自然尺寸：加载窗用的就是「new JXBusyLabel() 且不设尺寸」这种写法。
        // 曾因父类把轨迹/点形状按 26px 缩放，JXBusyLabel 反推出来的尺寸只有 26×26，
        // 图标小到几乎看不清、窗口 pack() 也跟着缩窄——而且它不抛异常，只能靠尺寸发现。
        org.jdesktop.swingx.JXBusyLabel bare = new org.jdesktop.swingx.JXBusyLabel();
        Dimension bareSize = bare.getPreferredSize();
        int expected = LoadingBusyPainter.DEFAULT_ICON_SIZE;
        report("[断言] 未设尺寸的忙碌标签自然尺寸=" + bareSize.width + "×" + bareSize.height
                + "（期望 " + expected + "×" + expected + "）");
        if (Math.min(bareSize.width, bareSize.height) < 32) {
            problems.add("未设尺寸的忙碌标签自然尺寸过小（" + bareSize.width + "×" + bareSize.height
                    + "），加载窗里的图标会小到看不清");
        }

        // 帧驱动：不同 frame 必须画出不同画面
        BufferedImage f0 = paintBusyLabel(label, 0);
        BufferedImage f10 = paintBusyLabel(label, 10);
        int ink0 = countOpaquePixels(f0);
        int frameDiff = countDifferentPixels(f0, f10);
        report("[断言] 忙碌标签 48×48：frame0 墨色像素=" + ink0 + "，frame0↔frame10 差异像素=" + frameDiff);
        if (ink0 < 20) {
            problems.add("忙碌标签几乎没有画出内容（墨色像素 " + ink0 + "）");
        }
        if (frameDiff == 0) {
            problems.add("忙碌标签的画面不随 frame 变化，动画是静止的（加载指示会假死）");
        }

        // 停止态：JXBusyLabel.stopAnimation 会把 frame 设成 -1
        try {
            int inkStopped = countOpaquePixels(paintBusyLabel(label, -1));
            report("[断言] 忙碌标签 frame=-1（停止态）墨色像素=" + inkStopped);
            if (inkStopped < 20) {
                problems.add("忙碌标签在 frame=-1（停止态）没有画出内容（" + inkStopped + " 像素）");
            }
        } catch (Throwable t) {
            problems.add("忙碌标签在 frame=-1 时抛出异常：" + rootCause(t));
        }

        // 颜色跟随：墨色应取标签当前前景色，这样切换主题才会跟着变
        Color probe = new Color(0xC4, 0x2B, 0x1C);
        Color old = label.getForeground();
        try {
            label.setForeground(probe);
            int matched = countColorPixels(paintBusyLabel(label, 5), probe, 30);
            report("[断言] 忙碌标签墨色跟随前景色：匹配像素=" + matched);
            if (matched < 20) {
                problems.add("忙碌标签没有用标签的前景色作为墨色（匹配像素 " + matched
                        + "），切换主题后颜色不会跟着变");
            }
        } finally {
            label.setForeground(old);
        }
        return problems;
    }

    /** 把忙碌标签按指定帧画到透明底图上（{@code JLabel} 非不透明，所以只会有图标被画出来）。 */
    private static BufferedImage paintBusyLabel(org.jdesktop.swingx.JXBusyLabel label, int frame) {
        org.jdesktop.swingx.painter.BusyPainter painter = label.getBusyPainter();
        if (painter != null) {
            painter.setFrame(frame);
        }
        BufferedImage img = new BufferedImage(Math.max(1, label.getWidth()), Math.max(1, label.getHeight()),
                BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        try {
            label.paint(g2);
        } finally {
            g2.dispose();
        }
        return img;
    }

    /** 统计非透明像素（透明底图上「画了东西」的像素）。 */
    private static int countOpaquePixels(BufferedImage img) {
        int n = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) != 0) {
                    n++;
                }
            }
        }
        return n;
    }

    /** 统计与目标颜色接近的非透明像素。 */
    private static int countColorPixels(BufferedImage img, Color target, int tolerance) {
        int n = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                if (colorDistance(new Color(argb, true), target) <= tolerance) {
                    n++;
                }
            }
        }
        return n;
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

    /**
     * 进度条：确定态必须完全交给当前 L&amp;F，不确定态必须由本插件画出扫动带。
     *
     * <p>这里刻意用「逐像素对照」而不是「断言某个颜色」：
     * 后者的前提是「我知道 L&amp;F 该画成什么颜色」，那本身就是一种越权——
     * 而本次的需求恰恰是「确定态由 L&amp;F 决定」。所以正确的验证方式是
     * 自己拿组件装一份 L&amp;F 原生 UI 当作参照，两边画出来必须一模一样。</p>
     */
    private static List<String> assertProgressBar(Gallery g, BufferedImage whole) {
        List<String> problems = new ArrayList<>();

        // ---- 1) 确定态：与 L&F 自身绘制逐像素对照 ----
        int width = 560;
        int height = 6;
        javax.swing.JProgressBar reference = new javax.swing.JProgressBar(0, 100);
        reference.setValue(g.determinate.getValue());
        reference.setStringPainted(g.determinate.isStringPainted());

        javax.swing.plaf.ComponentUI lafUi =
                FluentProgressBarUI.createLafUi(reference);
        if (lafUi == null) {
            report("[断言] 当前 L&F 的进度条 UI 为共享实例或取不到，跳过确定态对照");
        } else if (!(lafUi instanceof javax.swing.plaf.ProgressBarUI)) {
            problems.add("LAF 的进度条 UI 类型异常：" + lafUi.getClass().getName());
        } else {
            reference.setUI((javax.swing.plaf.ProgressBarUI) lafUi);
            BufferedImage mine = paintComponent(g.determinate, width, height);
            BufferedImage theirs = paintComponent(reference, width, height);
            int diff = countDifferentPixels(mine, theirs);
            report("[断言] 确定态进度条 vs LAF 自身绘制：差异像素 " + diff + " / " + (width * height));
            if (diff > 0) {
                problems.add("确定态进度条没有完全交给 LAF 绘制（与 LAF 自身绘制有 " + diff + " 个像素不同）");
            }
        }

        // ---- 2) 不确定态：必须由本插件画出扫动带 ----
        BufferedImage ind = paintComponent(g.indeterminate, width, height);
        int accentPixels = countAccentPixels(ind);
        report("[断言] 不确定进度条强调色像素: " + accentPixels + " / " + (width * height));
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
        if (!(bar.getUI() instanceof FluentScrollBarUI)) {
            problems.add("滚动条 UI 不是 FluentScrollBarUI，而是 " + bar.getUI().getClass().getSimpleName());
            return problems;
        }
        BufferedImage img = paintComponent(bar, bar.getWidth(), bar.getHeight());
        report("[断言] 滚动条尺寸: " + bar.getWidth() + "×" + bar.getHeight()
                + "（UI=" + bar.getUI().getClass().getSimpleName() + "）");

        // 统计整张图里与底色不同的像素，而不是只取中间一列：
        // 滑块很窄（3px）且不同主题的滚动条宽度不同，固定取样点容易取空，
        // 那会变成「断言假失败」，比漏检更糟
        Color bg = UIManager.getColor("Panel.background");
        int differing = 0;
        int best = 0;
        Color bestColor = null;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                Color c = new Color(img.getRGB(x, y), true);
                int d = colorDistance(c, bg);
                if (d > 2) {
                    differing++;
                }
                if (d > best) {
                    best = d;
                    bestColor = c;
                }
            }
        }
        report("[断言] 滚动条滑块像素: " + differing + " / " + (img.getWidth() * img.getHeight())
                + "，最大对比度 " + best + "（颜色 " + toHex(bestColor) + "）");
        if (differing == 0) {
            problems.add("滚动条滑块完全没有绘制（整根滚动条与底色完全一致）");
        } else if (best < 6) {
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

        // 按钮光晕的 UI 代理继承自 FlatLaf，非 FlatLaf 外观下按设计不安装。
        // 这不是缺陷，所以跳过而不是报错——否则用 Metal 主题跑自检会得到假失败。
        if (!(g.revealButton.getUI() instanceof RevealButtonUI)) {
            report("[断言] 当前外观不是 FlatLaf，按钮光晕不适用，跳过本组");
            return problems;
        }
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
     * {@link DynamicConverterTask}，
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

        checkUiDefault("ScrollBarUI", FluentScrollBarUI.class, problems);
        checkUiDefault("ProgressBarUI", FluentProgressBarUI.class, problems);
        checkUiDefault("CheckBoxUI", FluentSwitchUI.class, problems);

        // 按钮光晕的 UI 代理继承自 FlatLaf，非 FlatLaf 外观下按设计不安装
        boolean flatLaf = FluentUi.isFlatLaf();
        if (flatLaf) {
            checkUiDefault("ButtonUI", RevealButtonUI.class, problems);
        }

        // 光看 UIManager 里的字符串还不够：UIDefaults.getUI 是反射调用静态 createUI，
        // 真正要验证的是「新建的组件确实拿到了我们的 UI」
        JCheckBox probeCheckBox = new JCheckBox("切换后新建的复选框");
        if (!(probeCheckBox.getUI() instanceof FluentSwitchUI)) {
            problems.add("主题切换后新建的复选框没有拿到 FluentSwitchUI，而是 "
                    + probeCheckBox.getUI().getClass().getName());
        }
        JProgressBar probeBar = new JProgressBar();
        if (!(probeBar.getUI() instanceof FluentProgressBarUI)) {
            problems.add("主题切换后新建的进度条没有拿到 FluentProgressBarUI，而是 "
                    + probeBar.getUI().getClass().getName());
        }
        if (flatLaf) {
            JButton probeButton = new JButton("切换后新建的按钮");
            if (!(probeButton.getUI() instanceof RevealButtonUI)) {
                problems.add("主题切换后新建的按钮没有拿到 RevealButtonUI，而是 "
                        + probeButton.getUI().getClass().getName());
            }
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

    /**
     * 在指定横向区间内找出与背景差异最大的像素颜色——用它代表「文字颜色」。
     *
     * <p>取最对比的像素而不是取平均或众数：文字是抗锯齿的，笔画核心才是真正的文字色，
     * 边缘像素是它与背景的混合色。这样得到的颜色可以直接与主题里的颜色比较。</p>
     *
     * @param image  图像
     * @param fromX  起始列（含）
     * @param toX    结束列（不含）
     * @param background 背景色
     * @return 最对比的像素颜色；区间内没有任何与背景不同的像素时返回 {@code null}
     */
    private static Color mostContrastingColor(BufferedImage image, int fromX, int toX, Color background) {
        int best = 0;
        Color bestColor = null;
        int y1 = Math.max(0, image.getHeight());
        for (int y = 0; y < y1; y++) {
            for (int x = Math.max(0, fromX); x < Math.min(toX, image.getWidth()); x++) {
                Color c = new Color(image.getRGB(x, y), true);
                int d = colorDistance(c, background);
                if (d > best) {
                    best = d;
                    bestColor = c;
                }
            }
        }
        return best > 2 ? bestColor : null;
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

    /**
     * 两张同尺寸图像的差异像素数。
     *
     * <p><b>必须把 alpha 也算进去。</b>自绘控件的「槽位」是透明像素，
     * 而透明像素的 RGB 也是 0——只比 RGB 的话，「墨色恰好是纯黑」的外观
     * （Windows Classic 就是）下槽位移动会被判成「毫无变化」，动画断言假绿。
     * Mac Light 之所以能过，只是因为它的墨色 #262626 与 0 差 38，属于运气。</p>
     *
     * <p>判据：任一 RGB 通道相差超过 2，<b>或 alpha 不同</b>，即计为不同。</p>
     */
    private static int countDifferentPixels(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            return Integer.MAX_VALUE;
        }
        int count = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                int pa = a.getRGB(x, y);
                int pb = b.getRGB(x, y);
                if ((pa >>> 24) != (pb >>> 24)
                        || colorDistance(new Color(pa, true), new Color(pb, true)) > 2) {
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

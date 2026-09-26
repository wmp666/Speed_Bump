package com.wmp.speedbump.fluent;

import com.wmp.speedbump.fluent.backdrop.BackdropMaterial;
import com.wmp.speedbump.fluent.backdrop.NativeBackdrop;
import com.wmp.speedbump.fluent.component.FluentButton;
import com.wmp.speedbump.fluent.component.FluentProgressBar;
import com.wmp.speedbump.fluent.page.CreateTaskPage;
import com.wmp.speedbump.fluent.page.SettingsPage;
import com.wmp.speedbump.fluent.page.TasksPage;
import com.wmp.speedbump.fluent.reveal.RevealEngine;
import com.wmp.speedbump.fluent.theme.FluentColors;
import com.wmp.speedbump.fluent.theme.FluentIcons;
import com.wmp.speedbump.fluent.theme.FluentMetrics;
import com.wmp.speedbump.fluent.theme.FluentPainting;
import com.wmp.speedbump.fluent.theme.FluentTheme;
import com.wmp.speedbump.fluent.theme.FluentTypography;
import com.wmp.speedbump.fluent.ui.FluentNavigationView;
import com.wmp.speedbump.fluent.ui.FluentWindow;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Speed Bump Fluent 应用入口。
 *
 * <h3>命令行参数</h3>
 * <pre>
 *   --theme=light|dark|system    起始主题（默认 light）
 *   --material=acrylic|mica|blur|none|auto   窗口材质（默认 acrylic）
 *   --accent=#RRGGBB             自定义强调色
 *   --no-backdrop                关闭原生材质，使用不透明背景（调试用）
 *   --selftest                   离屏构建整套界面、导出截图并打印诊断，然后退出
 *   --screenshot=&lt;file&gt;          与 --selftest 配合，指定输出目录或文件名
 * </pre>
 *
 * <h3>为什么入口这么薄</h3>
 * <p>{@link FluentApp} 只做三件事：解析参数、装配页面、把窗口显示出来。
 * 界面层次全部落在 {@link FluentWindow} + {@link FluentNavigationView}，
 * 页面落在 {@code page} 包。这样后续接入真实下载引擎时，
 * 需要改的只有页面里那几个「数据来源」，不影响框架。</p>
 */
public final class FluentApp {

    public static final String APP_NAME = "减速带 · Speed Bump";
    public static final String SUB_TITLE = "Speed_Bump_Fluent";
    public static final String VERSION = "0.5.0-fluent.1";

    private FluentApp() {
    }

    public static void main(String[] args) {
        Options options = Options.parse(args);
        verbose = options.verbose;
        step("解析参数");

        // 1) 先装 Look & Feel 与主题令牌，必须在创建任何 Swing 组件之前
        FluentTheme.install();
        step("安装 Look & Feel 与主题令牌");
        if (options.theme != null) {
            FluentTheme.setMode(options.theme);
            step("切换主题到 " + options.theme);
        }
        if (options.accent != null) {
            FluentColors.setAccentOverride(options.accent);
        }
        RevealEngine.enabled = !options.noReveal;
        TasksPage.demoAnimationEnabled = !options.staticUi;
        step("初始化开关与强调色");

        if (options.selfTest) {
            int exit = runSelfTest(options);
            System.exit(exit);
        }

        SwingUtilities.invokeLater(() -> {
            step("进入 EDT，准备构建窗口");
            FluentWindow window = buildWindow(options.material);
            step("构建窗口与页面");
            window.setDefaultCloseOperation(FluentWindow.DO_NOTHING_ON_CLOSE);
            window.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    RevealEngine.dispose();
                    window.dispose();
                    System.exit(0);
                }
            });
            window.setVisible(true);
            step("显示窗口");
            // 把窗口几何与材质生效情况打出来：做「同一画面不同材质」的截图对照时，
            // 需要精确知道窗口落在屏幕的哪个矩形里，否则无法界定分析区域
            System.out.println("[Speed_Bump_Fluent] 已启动：" + APP_NAME + " " + VERSION);
            System.out.println("[Speed_Bump_Fluent] 窗口几何: " + window.getBounds()
                    + "  HWND: 0x" + Long.toHexString(
                            com.wmp.speedbump.fluent.backdrop.NativeBackdrop.hwndOf(window))
                    + "  请求材质: " + window.requestedMaterial()
                    + "  实际材质: " + window.appliedMaterial()
                    + "  材质生效: " + window.isBackdropActive()
                    + "  透明退化: " + FluentColors.isSolidFallback());
            System.out.println("[Speed_Bump_Fluent] " + NativeBackdrop.describeEnvironment().replace("\n", " | "));
        });
    }

    // ==================================================================
    // 界面装配
    // ==================================================================

    /**
     * 构建主窗口（不含显示）。
     *
     * <p>拆出来是为了让 {@code --selftest} 能在不显示窗口的前提下复用同一套装配逻辑——
     * 测试如果自己另搭一棵组件树，就失去了「测的就是真实界面」的意义。</p>
     */
    public static FluentWindow buildWindow(BackdropMaterial material) {
        FluentWindow window = new FluentWindow(APP_NAME);
        if (material != null) {
            window.setBackdropMaterial(material);
        }
        window.titleBar().setAppIcon(createAppIcon(64));
        step("创建窗口骨架");

        FluentNavigationView navigation = new FluentNavigationView();
        // 只有首页立即构建；另外两页改为延迟构建（首次点开才建），
        // 这样启动时能省掉约 1.5 秒的无效构建
        navigation.addPage("任务", FluentIcons.DOWNLOAD, TasksPage::new);
        step("构建任务页");
        navigation.addPage("创建任务", FluentIcons.ADD, CreateTaskPage::new);
        navigation.addFooterPage("设置", FluentIcons.SETTINGS, () -> new SettingsPage(window));
        step("注册其余页面");
        navigation.selectFirst();
        step("选中首页");

        window.setContent(navigation);
        step("挂载到窗口");
        return window;
    }

    /**
     * 用矢量绘制应用图标，避免打包图片资源。
     *
     * <p>图形语义：主题色圆角方块 + 白色的「减速带」（中间隆起的一条弧）+ 向下的下载箭头。</p>
     */
    public static BufferedImage createAppIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            float s = size / 64f;

            g.setColor(FluentColors.accent());
            g.fill(FluentPainting.roundRect(0, 0, size, size, Math.round(14 * s)));

            g.setColor(Color.WHITE);
            // 下载箭头
            g.setStroke(new java.awt.BasicStroke(5f * s, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            g.drawLine(Math.round(32 * s), Math.round(14 * s), Math.round(32 * s), Math.round(34 * s));
            g.drawLine(Math.round(23 * s), Math.round(26 * s), Math.round(32 * s), Math.round(35 * s));
            g.drawLine(Math.round(41 * s), Math.round(26 * s), Math.round(32 * s), Math.round(35 * s));

            // 减速带：一条中间隆起的曲线
            Path2D.Float bump = new Path2D.Float();
            bump.moveTo(10 * s, 50 * s);
            bump.curveTo(22 * s, 50 * s, 22 * s, 40 * s, 32 * s, 40 * s);
            bump.curveTo(42 * s, 40 * s, 42 * s, 50 * s, 54 * s, 50 * s);
            g.setStroke(new java.awt.BasicStroke(4f * s, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            g.draw(bump);
        } finally {
            g.dispose();
        }
        return image;
    }

    // ==================================================================
    // 自检 / 离屏截图
    // ==================================================================

    /**
     * 离屏自检：构建界面 → 布局 → 绘制到图片 → 校验 → 输出报告。
     *
     * <p>存在的意义：GUI 项目最难的是「改完不知道哪里塌了」。有了这个模式，
     * 在没有人工点开窗口的情况下也能验证「组件树能装配、布局非零、关键像素正确」，
     * 并且把成图与组件树落到磁盘供人工复核。做法是把窗口
     * {@code pack()} 一次（创建不可见 peer）再 {@code validate()}，
     * 之后 {@code paint()} 到 {@link BufferedImage}。</p>
     *
     * <p>报告除了打到标准输出，还会写成 UTF-8 的 {@code selftest-report.txt}——
     * Windows 控制台的默认代码页会把中文变成乱码，落盘的文件才是可信的那一份。</p>
     */
    private static int runSelfTest(Options options) {
        File outputDir = options.screenshot == null
                ? new File("out") : options.screenshot.getAbsoluteFile();
        if (!outputDir.isDirectory()) {
            outputDir = outputDir.getParentFile() == null ? new File(".") : outputDir.getParentFile();
        }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            System.out.println("[self-test] cannot create output dir: " + outputDir);
        }

        report("===== Speed_Bump_Fluent 自检 =====");
        report("版本        : " + VERSION);
        report("操作系统    : " + System.getProperty("os.name") + " "
                + System.getProperty("os.version") + " (build " + NativeBackdrop.windowsBuild() + ")");
        report("JVM         : " + System.getProperty("java.vm.name") + " "
                + System.getProperty("java.version"));
        report("字体        : 正文=" + FluentTypography.TEXT_FAMILY
                + " / 标题=" + FluentTypography.DISPLAY_FAMILY
                + " / 图标=" + FluentTypography.ICON_FAMILY
                + "（图标字体可用=" + FluentTypography.iconFontAvailable() + "）");
        report("原生材质    : 可用=" + NativeBackdrop.isSupported()
                + "，Mica 实际解析为 " + NativeBackdrop.resolve(BackdropMaterial.MICA)
                + "，Acrylic 解析为 " + NativeBackdrop.resolve(BackdropMaterial.ACRYLIC));

        // 整个界面构建、主题切换、布局与离屏绘制都必须跑在 EDT 上。
        // 这里踩过一次坑：setMode() 触发的 FluentTheme.refresh() 内部走的是 invokeLater，
        // 若自检留在主线程继续布局与绘制，就会与 EDT 上的主题刷新竞争，
        // 表现是「深色那一轮某些主题色元素凭空消失」——一个纯粹由测试方式引入的假象。
        // 放进 invokeAndWait 后主题切换变成同步的，结果可复现。
        List<String> problems = new ArrayList<>();
        final File targetDir = outputDir;
        try {
            SwingUtilities.invokeAndWait(() -> runSelfTestOnEdt(options, targetDir, problems));
        } catch (Throwable t) {
            problems.add("自检在 EDT 上抛出异常：" + t);
        }

        if (problems.isEmpty()) {
            report("[自检] 通过：组件树装配、布局、离屏绘制与关键像素均正常");
        } else {
            report("[自检] 发现 " + problems.size() + " 个问题：");
            for (String problem : problems) {
                report("  - " + problem);
            }
        }

        File reportFile = new File(outputDir, "selftest-report.txt");
        try {
            java.nio.file.Files.writeString(reportFile.toPath(), REPORT.toString());
            System.out.println("[self-test] report written to " + reportFile.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("[self-test] cannot write report: " + e.getMessage());
        }
        return problems.isEmpty() ? 0 : 1;
    }

    /** 自检报告缓冲：同时打到标准输出与文件 */
    private static final StringBuilder REPORT = new StringBuilder();

    private static void report(String line) {
        REPORT.append(line).append(System.lineSeparator());
        System.out.println(line);
    }

    // ==================================================================
    // 启动分步日志
    // ==================================================================

    /** 是否输出启动分步耗时（--verbose） */
    private static boolean verbose;
    private static final long STEP_START = System.nanoTime();
    private static long stepLast = STEP_START;

    /**
     * 打印一步启动耗时。
     *
     * <p>存在的理由很具体：GUI 程序卡在启动阶段时，界面上什么都没有、也不抛异常，
     * 只能靠「最后打印到哪一步」来定位；而线程转储在受限环境里未必拿得到
     * （{@code jcmd} 依赖 Attach 机制，沙箱内会失败）。这个分步日志是零依赖的替代方案。</p>
     */
    private static void step(String what) {
        if (!verbose) {
            return;
        }
        long now = System.nanoTime();
        System.out.printf("[启动] %-24s 本步 %7.1f ms | 累计 %7.1f ms%n",
                what, (now - stepLast) / 1_000_000.0, (now - STEP_START) / 1_000_000.0);
        System.out.flush();
        stepLast = now;
    }

    /** 自检的界面部分（必须在 EDT 上执行） */
    private static void runSelfTestOnEdt(Options options, File outputDir, List<String> problems) {
        FluentWindow window = buildWindow(options.material);
        window.setSize(1440, 940);

        // 创建不可见 peer 并完成首次布局
        window.pack();
        window.setSize(1440, 940);
        window.validate();

        // 页面是延迟构建的，自检必须主动把每一页都构建一遍：
        // 一是保证覆盖面（否则 --selftest 只测了首页），
        // 二是能把某个页面构造失败的异常暴露在报告里，而不是等到用户点开才崩。
        FluentNavigationView navigation = find(window.getContentPane(),
                FluentNavigationView.class, n -> true);
        if (navigation != null) {
            for (var item : navigation.pages()) {
                navigation.select(item);
                window.validate();
                layoutRecursively(window.getContentPane());
            }
            navigation.selectFirst();
            window.validate();
            layoutRecursively(window.getContentPane());
            report("[自检] 已构建全部 " + navigation.pages().size() + " 个页面（含延迟构建）");
        }

        for (FluentTheme.Mode mode : new FluentTheme.Mode[]{FluentTheme.Mode.LIGHT, FluentTheme.Mode.DARK}) {
            FluentTheme.setMode(mode);
            window.validate();
            layoutRecursively(window.getContentPane());

            String label = mode == FluentTheme.Mode.DARK ? "dark" : "light";
            File png = new File(outputDir, "selftest-" + label + ".png");
            BufferedImage image = null;
            try {
                image = paint(window);
                ImageIO.write(image, "png", png);
                report("[自检] 已导出 " + label + " 主题截图：" + png.getAbsolutePath()
                        + "（" + image.getWidth() + "×" + image.getHeight() + "）");
            } catch (IOException e) {
                problems.add("导出 " + label + " 截图失败：" + e.getMessage());
            }

            problems.addAll(checkLayout(window, label));

            if (options.dumpTree) {
                StringBuilder sb = new StringBuilder();
                dumpTree(window.getContentPane(), 0, sb);
                File dump = new File(outputDir, "selftest-" + label + "-tree.txt");
                try {
                    java.nio.file.Files.writeString(dump.toPath(), sb.toString());
                    report("[自检] 已导出 " + label + " 组件树：" + dump.getAbsolutePath());
                } catch (IOException e) {
                    problems.add("导出组件树失败：" + e.getMessage());
                }
            }

            problems.addAll(checkPixels(window, image, label));
        }

        // 恢复默认主题
        FluentTheme.setMode(options.theme == null ? FluentTheme.Mode.LIGHT : options.theme);
    }

    /** 绘制窗口内容到一个 ARGB 图片 */
    private static BufferedImage paint(FluentWindow window) {
        int width = Math.max(1, window.getWidth());
        int height = Math.max(1, window.getHeight());
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            FluentPainting.antialias(g);
            // 离屏绘制没有原生材质，先铺一层不透明底，否则透明像素会成为黑洞
            g.setColor(FluentColors.solid());
            g.fillRect(0, 0, width, height);
            window.getContentPane().paint(g);
        } finally {
            g.dispose();
        }
        return image;
    }

    /** 递归布局：非显示状态下 {@code validate()} 不会下推到所有子容器 */
    private static void layoutRecursively(Component component) {
        component.doLayout();
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                layoutRecursively(child);
            }
        }
    }

    /**
     * 布局校验：找出「可见却没有尺寸」的组件。
     *
     * <p>这类问题在 Swing 里极其常见（忘了 {@code revalidate}、容器用了错的
     * {@code BoxLayout} 约束），而且表现往往是「界面看起来正常，只是少了一块」。
     * 只要有一条规则替我们盯着，就能在改动后立刻发现。</p>
     */
    private static List<String> checkLayout(FluentWindow window, String label) {
        List<String> problems = new ArrayList<>();
        Container content = window.getContentPane();

        if (window.titleBar().getHeight() != FluentMetrics.TITLE_BAR_HEIGHT) {
            problems.add("[" + label + "] 标题栏高度应为 " + FluentMetrics.TITLE_BAR_HEIGHT
                    + "，实际 " + window.titleBar().getHeight());
        }
        if (content.getWidth() < 800 || content.getHeight() < 600) {
            problems.add("[" + label + "] 内容区尺寸异常：" + content.getWidth() + "×" + content.getHeight());
        }

        List<String> empty = new ArrayList<>();
        collectEmptyVisible(content, empty, 0);
        for (String item : empty) {
            problems.add("[" + label + "] 可见但尺寸为 0 的组件：" + item);
        }
        return problems;
    }

    private static void collectEmptyVisible(Component component, List<String> out, int depth) {
        if (!component.isVisible()) {
            return;
        }
        if (depth > 0 && (component.getWidth() == 0 || component.getHeight() == 0)) {
            // 高度为 0 的间距组件是刻意为之，不算问题
            if (component instanceof JComponent jc
                    && jc.getPreferredSize() != null
                    && jc.getPreferredSize().height == 0) {
                return;
            }
            out.add(component.getClass().getSimpleName() + " @" + component.getBounds());
            return;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectEmptyVisible(child, out, depth + 1);
            }
        }
    }

    // ==================================================================
    // 像素级校验
    // ==================================================================

    /**
     * 像素级校验：把「组件树画出来之后，关键位置到底是不是预期颜色」变成可断言的检查。
     *
     * <p>为什么需要它：布局非零只能证明「组件有位置」，不能证明「画对了」。
     * 常见的坑有：绘制顺序错误导致焦点环/文字被盖住、颜色令牌返回全透明、
     * 组件被裁剪到可视区之外、z-order 反了。这些问题在组件树上看不出来，
     * 但一定会在像素上暴露。三种检查分别是：</p>
     * <ol>
     *   <li><b>实心填充</b>：强调按钮、进度条填充、导航选中指示条必须是强调色；</li>
     *   <li><b>区域复杂度</b>：标题栏/导航窗格/内容区必须有足够比例的非底色像素
     *       （否则说明文字或卡片根本没画出来）；</li>
     *   <li><b>层次差异</b>：卡片区域的平均颜色必须与材质层不同，否则界面会「糊成一片」。</li>
     * </ol>
     */
    private static List<String> checkPixels(FluentWindow window, BufferedImage image, String label) {
        List<String> problems = new ArrayList<>();
        if (image == null) {
            problems.add("[" + label + "] 没有可用于校验的离屏图像");
            return problems;
        }
        Container content = window.getContentPane();

        // ---- 1) 强调按钮底色 ----
        FluentButton accentButton = find(content,
                FluentButton.class, b -> b.getStyle() == FluentButton.Style.ACCENT);
        if (accentButton == null) {
            problems.add("[" + label + "] 未找到强调按钮");
        } else {
            assertPixel(image, toContentPoint(accentButton, 10, accentButton.getHeight() / 2, content),
                    FluentColors.accent(), 24, label + " 强调按钮底色", problems);
        }

        // ---- 2) 进度条填充 ----
        FluentProgressBar bar = find(content, FluentProgressBar.class,
                b -> !b.isIndeterminate() && b.getValue() > 20);
        if (bar == null) {
            problems.add("[" + label + "] 未找到处于下载中的进度条");
        } else {
            int x = Math.max(3, (int) (bar.getWidth() * 0.15));
            assertPixel(image, toContentPoint(bar, x, bar.getHeight() / 2, content),
                    FluentColors.accent(), 24, label + " 进度条填充色", problems);
        }

        // ---- 3) 导航选中指示条 ----
        FluentNavigationView navigation = find(content, FluentNavigationView.class, n -> true);
        if (navigation == null || navigation.selectedItem() == null) {
            problems.add("[" + label + "] 导航视图没有选中项");
        } else {
            Component item = navigation.selectedItem();
            assertPixel(image, toContentPoint(item, 1, item.getHeight() / 2, content),
                    FluentColors.accent(), 30, label + " 导航选中指示条", problems);
        }

        // ---- 4) 区域复杂度 ----
        int titleBarHeight = FluentMetrics.TITLE_BAR_HEIGHT;
        double titleRatio = regionVariation(image,
                new java.awt.Rectangle(0, 0, image.getWidth(), titleBarHeight));
        double navRatio = regionVariation(image,
                new java.awt.Rectangle(0, titleBarHeight, FluentMetrics.NAV_WIDTH_EXPANDED,
                        image.getHeight() - titleBarHeight));
        double contentRatio = regionVariation(image,
                new java.awt.Rectangle(FluentMetrics.NAV_WIDTH_EXPANDED, titleBarHeight,
                        image.getWidth() - FluentMetrics.NAV_WIDTH_EXPANDED,
                        image.getHeight() - titleBarHeight));
        report(String.format("[自检] %s 区域非底色像素占比：标题栏 %.3f%%，导航窗格 %.3f%%，内容区 %.3f%%",
                label, titleRatio * 100, navRatio * 100, contentRatio * 100));
        if (titleRatio < 0.002) {
            problems.add(String.format("[%s] 标题栏几乎没有内容（非底色像素仅 %.3f%%）", label, titleRatio * 100));
        }
        if (navRatio < 0.01) {
            problems.add(String.format("[%s] 导航窗格几乎没有内容（非底色像素仅 %.3f%%）", label, navRatio * 100));
        }
        if (contentRatio < 0.03) {
            problems.add(String.format("[%s] 内容区几乎没有内容（非底色像素仅 %.3f%%）", label, contentRatio * 100));
        }

        // ---- 5) 层次校验：页面底部留白处应当正好是「材质层」的颜色 ----
        int probeX = image.getWidth() - 40;
        int probeY = image.getHeight() - 8;
        Color layer = FluentColors.layer();
        Color base = FluentColors.solid();
        float alpha = layer.getAlpha() / 255f;
        Color expectedLayer = new Color(
                Math.round(layer.getRed() * alpha + base.getRed() * (1 - alpha)),
                Math.round(layer.getGreen() * alpha + base.getGreen() * (1 - alpha)),
                Math.round(layer.getBlue() * alpha + base.getBlue() * (1 - alpha)));
        Color actualLayer = new Color(image.getRGB(probeX, probeY), true);
        if (distance(actualLayer, expectedLayer) > 6) {
            problems.add(String.format("[%s] 材质层颜色不符：实际 %s，期望 %s（@%d,%d）",
                    label, hex(actualLayer), hex(expectedLayer), probeX, probeY));
        }
        return problems;
    }

    /** 采样一个像素并与期望颜色比较 */
    private static void assertPixel(BufferedImage image, java.awt.Point p, Color expected,
                                    int tolerance, String what, List<String> problems) {
        if (p == null || p.x < 0 || p.y < 0 || p.x >= image.getWidth() || p.y >= image.getHeight()) {
            problems.add(what + "：采样点越界 " + p);
            return;
        }
        Color actual = new Color(image.getRGB(p.x, p.y), true);
        int diff = distance(actual, expected);
        if (diff > tolerance) {
            problems.add(String.format("%s：像素 %s(@%d,%d) 与期望 %s 相差 %d（容差 %d）",
                    what, hex(actual), p.x, p.y, hex(expected), diff, tolerance));
        }
    }

    /**
     * 统计区域内「与最多数颜色不同」的像素占比。
     *
     * <p>用占比而不是绝对计数，是为了让阈值不随窗口尺寸变化——
     * 这些阈值只用来判断「有没有东西」，不追求精确的视觉回归。</p>
     */
    private static double regionVariation(BufferedImage image, java.awt.Rectangle rect) {
        java.awt.Rectangle r = rect.intersection(new java.awt.Rectangle(0, 0, image.getWidth(), image.getHeight()));
        if (r.isEmpty()) {
            return 0;
        }
        java.util.Map<Integer, Integer> histogram = new java.util.HashMap<>();
        int total = 0;
        for (int y = r.y; y < r.y + r.height; y += 2) {
            for (int x = r.x; x < r.x + r.width; x += 2) {
                int rgb = image.getRGB(x, y) & 0xFFFFFF;
                histogram.merge(rgb, 1, Integer::sum);
                total++;
            }
        }
        int modal = histogram.values().stream().max(Integer::compareTo).orElse(0);
        return total == 0 ? 0 : (total - modal) / (double) total;
    }

    private static java.awt.Point toContentPoint(Component component, int x, int y, Container content) {
        try {
            return SwingUtilities.convertPoint(component, x, y, content);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 颜色差异：取三个通道差值的最大值 */
    private static int distance(Color a, Color b) {
        return Math.max(Math.abs(a.getRed() - b.getRed()),
                Math.max(Math.abs(a.getGreen() - b.getGreen()), Math.abs(a.getBlue() - b.getBlue())));
    }

    private static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    /** 深度优先查找第一个满足条件的组件 */
    private static <T extends Component> T find(Component root, Class<T> type,
                                               java.util.function.Predicate<T> filter) {
        if (type.isInstance(root)) {
            T candidate = type.cast(root);
            if (filter.test(candidate)) {
                return candidate;
            }
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = find(child, type, filter);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    // ==================================================================
    // 组件树转储
    // ==================================================================

    /** 把组件树（含尺寸与关键属性）写成文本，便于在没有图形界面的环境下核对布局 */
    private static void dumpTree(Component component, int depth, StringBuilder out) {
        if (depth > 14) {
            return;
        }
        out.append("  ".repeat(depth))
                .append(component.getClass().getSimpleName())
                .append(' ')
                .append(component.getBounds());
        if (component instanceof javax.swing.AbstractButton button && button.getText() != null
                && !button.getText().isEmpty()) {
            out.append(" \"").append(button.getText()).append('"');
        } else if (component instanceof javax.swing.JLabel label && label.getText() != null
                && !label.getText().isEmpty()) {
            String text = label.getText();
            out.append(" \"").append(text.length() > 40 ? text.substring(0, 40) + "…" : text).append('"');
        } else if (component instanceof javax.swing.text.JTextComponent textComponent) {
            out.append(" \"").append(textComponent.getText().replace("\n", "\\n")).append('"');
        }
        out.append('\n');
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                dumpTree(child, depth + 1, out);
            }
        }
    }

    // ==================================================================
    // 参数
    // ==================================================================

    /** 命令行参数 */
    static final class Options {
        FluentTheme.Mode theme;
        BackdropMaterial material = BackdropMaterial.ACRYLIC;
        Color accent;
        boolean selfTest;
        boolean noReveal;
        boolean dumpTree;
        /** 关闭演示动画（截图对照与低性能环境用） */
        boolean staticUi;
        /** 输出启动分步耗时 */
        boolean verbose;
        File screenshot;

        static Options parse(String[] args) {
            Options options = new Options();
            for (String arg : args) {
                if (arg == null || arg.isBlank()) {
                    continue;
                }
                String value = arg.contains("=") ? arg.substring(arg.indexOf('=') + 1) : "";
                if (arg.startsWith("--theme=")) {
                    options.theme = switch (value.toLowerCase()) {
                        case "dark" -> FluentTheme.Mode.DARK;
                        case "system" -> FluentTheme.Mode.SYSTEM;
                        default -> FluentTheme.Mode.LIGHT;
                    };
                } else if (arg.startsWith("--material=")) {
                    options.material = switch (value.toLowerCase()) {
                        case "mica" -> BackdropMaterial.MICA;
                        case "mica-alt" -> BackdropMaterial.MICA_ALT;
                        case "blur" -> BackdropMaterial.BLUR;
                        case "none" -> BackdropMaterial.NONE;
                        default -> BackdropMaterial.ACRYLIC;
                    };
                } else if (arg.startsWith("--accent=")) {
                    String hex = value.startsWith("#") ? value.substring(1) : value;
                    try {
                        options.accent = new Color(Integer.parseInt(hex, 16));
                    } catch (NumberFormatException ignored) {
                        System.out.println("[警告] 无法解析强调色：" + value);
                    }
                } else if (arg.equals("--selftest")) {
                    options.selfTest = true;
                } else if (arg.equals("--dump-tree")) {
                    options.selfTest = true;
                    options.dumpTree = true;
                } else if (arg.equals("--no-reveal")) {
                    options.noReveal = true;
                } else if (arg.equals("--static")) {
                    options.staticUi = true;
                } else if (arg.equals("--verbose")) {
                    options.verbose = true;
                } else if (arg.equals("--no-backdrop")) {
                    options.material = BackdropMaterial.NONE;
                } else if (arg.startsWith("--screenshot=")) {
                    options.screenshot = new File(value);
                } else if (arg.equals("--help") || arg.equals("-h")) {
                    printUsage();
                    System.exit(0);
                }
            }
            return options;
        }

        static void printUsage() {
            System.out.println("""
                    减速带 · Speed Bump（Fluent 版）
                    用法：java -cp ... com.wmp.speedbump.fluent.FluentApp [选项]

                      --theme=light|dark|system                 起始主题
                      --material=acrylic|mica|mica-alt|blur|none 窗口材质
                      --accent=#RRGGBB                           强调色
                      --no-backdrop                              关闭原生材质
                      --no-reveal                                关闭揭示高亮光晕
                      --selftest                                 离屏自检并导出截图
                      --screenshot=<目录或文件>                  自检截图输出位置
                      --help                                     显示本帮助
                    """);
        }
    }
}

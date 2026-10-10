package com.wmp.speed_bump.platform.ui.swing.window;

import com.wmp.downloader.tools.file.DataControl;
import com.wmp.speed_bump.platform.ui.swing.tools.DialogBackdrop;
import com.wmp.speed_bump.common.background.tools.DynamicConverterTask;
import com.wmp.speed_bump.common.background.tools.resource.control.IconControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.speed_bump.platform.ui.swing.tools.fluent.FluentColors;
import com.wmp.speed_bump.common.background.tools.StringFormat;
import com.wmp.speed_bump.common.background.tools.SBLogger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;

/**
 * 初次使用欢迎页的 Swing 外壳（抽象端：{@code com.wmp.speed_bump.common.ui.WelcomePage}）。
 *
 * <h3>它做什么</h3>
 * <p>第一次启动时，在主界面出现之前先让用户把几个「一眼就知道自己喜欢哪个」的选项定下来。
 * 具体选项拆成两个设置页（{@link AppearanceSettingsPage} / {@link GeneralSettingsPage}），
 * 本类只负责外壳：页眉、{@link JTabbedPane} 标签栏、页脚按钮、以及「跳过就整体回滚」的语义。</p>
 *
 * <h3>标签栏即进度</h3>
 * <p>页面数量少，用一个 {@link JTabbedPane} 就够了：标签名就是分组名（外观 / 语言与启动），
 * 当前选中哪个标签本身就是进度指示，不需要再画一套页码圆点。
 * 默认选中的标签页宽高取「所有页里最大的那个」，所以翻页时窗口大小纹丝不动。
 * 底部仍保留「上一步 / 下一步」，最后一页的主按钮变成「开始使用」。</p>
 *
 * <h3>为什么必须赶在主界面之前</h3>
 * <p>{@code Downloader} 在构造时就会读取主题色、组件弧度、背景等配置。
 * 如果等主窗口建好再问，用户的选择就要么看不到效果、要么得重启一次才生效。
 * 所以 {@code UIStart} 把本页插在「应用主题」之后、「构建主窗口」之前。</p>
 *
 * <h3>「跳过」的语义</h3>
 * <p>因为选项是即时生效的，全程都没有「确定」这个提交动作，
 * 所以这里把配置快照在构造时留一份（{@link WelcomeSettings#capture()}）：</p>
 * <ul>
 *   <li><b>开始使用</b> —— 保留预览出来的结果；</li>
 *   <li><b>跳过 / Esc / 直接关窗</b> —— 把快照写回去，等于「这次什么都没改」
 *       （开机自启动是系统层面的改动，同样会被还原）。</li>
 * </ul>
 * <p>两种情况都会把 {@code is_show_welcome} 记为 {@code false}，所以本页默认只出现一次；
 * 想再看一次加启动参数 {@code -showWelcome}。</p>
 *
 * <h3>线程</h3>
 * <p>本类会创建并显示 Swing 窗口，<b>必须在界面线程上构造</b>。
 * 调用方 {@code UIStart} 负责把创建动作切到 EDT。</p>
 */
public class WelcomePage extends JDialog
        implements com.wmp.speed_bump.common.ui.WelcomePage, WelcomePageHost {

    private static final SBLogger logger = SBLogger.getLogger(WelcomePage.class);

    /** 页眉应用图标的边长 */
    private static final int ICON_SIZE = 56;

    /** 标签栏标题用的翻译键 */
    private static final String KEY_SECTION_APPEARANCE = "welcome.section.appearance";
    private static final String KEY_SECTION_GENERAL = "welcome.section.general";

    /** Esc 键绑定的动作名 */
    private static final String ESCAPE_ACTION = "WelcomePage.escape";

    // ==================================================================
    // 界面组件
    // ==================================================================

    private final JPanel rootPanel = new JPanel(new BorderLayout(0, 16));
    private final JLabel iconLabel = new JLabel();

    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();

    private final JTabbedPane tabbedPane = new JTabbedPane();

    private final JButton skipButton = new JButton();
    private final JButton prevButton = new JButton();
    private final JButton nextButton = new JButton();
    private final JLabel hintLabel = new JLabel();

    // ==================================================================
    // 设置页
    // ==================================================================

    private final AppearanceSettingsPage appearancePage = new AppearanceSettingsPage();
    private final GeneralSettingsPage generalPage = new GeneralSettingsPage();

    // ==================================================================
    // 状态
    // ==================================================================

    /** 打开本页之前的配置快照，「跳过」时用它回滚 */
    private final WelcomeSettings.Snapshot origin;

    /** 用户是否点了「开始使用」 */
    private boolean completed;

    /** 页脚提示当前是不是一条错误信息（决定用次要文字色还是红色） */
    private boolean hintIsError;

    /**
     * 正在重建界面（{@code updateComponentTreeUI}）时置位。
     *
     * <p>{@code updateUI} 会顺带调整下拉/开关的内部状态，某些情况下还会补发
     * {@code ItemEvent}/{@code ActionEvent}。设置页的监听器分不清那是「用户点的」
     * 还是「刷新捎带的」，一律写进配置的话，光是换一次主题就可能把控件当时的状态
     * 盖到配置上。</p>
     */
    private boolean rebuilding;

    /** 注册给全局列表的动态任务，窗口销毁时要摘掉，避免任务列表越积越多 */
    private DynamicConverterTask[] iconTasks;
    private DynamicConverterTask[] themeTasks;

    public WelcomePage() {
        //先留快照：后面所有即时预览都是直接写 DataControl / 系统设置的，跳过时要靠它还原
        origin = WelcomeSettings.capture();

        //登记设置页：顺序即标签栏顺序，标题由 key 在重刷文案时翻译
        addPage(KEY_SECTION_APPEARANCE, appearancePage);
        addPage(KEY_SECTION_GENERAL, generalPage);

        setTitle(pageTitle());
        setIconImage(IconControl.INSTANCE.getIcon("icon").getImage());
        setModal(true);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        rootPanel.setBorder(BorderFactory.createEmptyBorder(22, 26, 18, 26));
        rootPanel.add(buildHeader(), BorderLayout.NORTH);
        rootPanel.add(buildTabs(), BorderLayout.CENTER);
        rootPanel.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(rootPanel);

        getRootPane().setDefaultButton(nextButton);
        //Esc 与「跳过」等价：模态窗口没有退路时用户会觉得被困住
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ESCAPE_ACTION);
        getRootPane().getActionMap().put(ESCAPE_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close(false);
            }
        });

        // ===== [BACKDROP-START] 背景材质（Mica / 模糊）测试项 =====
        // 整块删除即可彻底移除该功能，本页恢复原来的不透明外观。
        // 开关：在「测试功能」里启用 mainId 1004 后重启应用。
        // 必须先于 pack()：窗口一旦 displayable 就无法再设为透明。
        DialogBackdrop.applyTestFunctionSwitch();
        DialogBackdrop.install(this, getTitle(), null);
        //材质要透出来，根面板就不能是不透明底色；测试项关闭时本行是空操作
        DialogBackdrop.makeTranslucent(DialogBackdrop.CONTENT_ALPHA, rootPanel);
        // ===== [BACKDROP-END] =====

        //设置页在这里才接上外壳：此时外壳已构造完毕，页面回调它是安全的
        appearancePage.bind(this);
        generalPage.bind(this);
        retranslateAll();

        //主题回调放在最后注册：此时组件树已经完全搭好
        registerThemeHooks();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                //直接关窗按「跳过」处理：用户没说要这些改动
                close(false);
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    // ==================================================================
    // 抽象端
    // ==================================================================

    @Override
    public void showPage() {
        setVisible(true);
        finish();
    }

    /**
     * 关闭之后收尾：落盘选择，并让 {@code is_show_welcome} 不再为真。
     *
     * <p>无论用户是「开始使用」还是「跳过」，都记为「已经看过欢迎页」，
     * 否则每次启动都会再弹一次，那就成打扰了。</p>
     */
    private void finish() {
        try {
            if (!completed) {
                WelcomeSettings.restore(origin);
            }
            //写入的是 saveData，save() 之前主界面读到的也是新值（put 同时更新了内存数据）
            DataControl.put(KEY_IS_SHOW_WELCOME, false);
            DataControl.save();
        } catch (Throwable t) {
            //收尾失败最多是下次再弹一次欢迎页，不该影响启动
            logger.error("保存欢迎页设置失败", t);
        }
    }

    private void close(boolean isCompleted) {
        completed = isCompleted;
        dispose();
    }

    @Override
    public void dispose() {
        if (iconTasks != null) {
            IconControl.INSTANCE.removeInDynamicConverter(iconTasks);
            iconTasks = null;
        }
        if (themeTasks != null) {
            ThemeChanger.removeDynamicConverter(themeTasks);
            themeTasks = null;
        }
        appearancePage.dispose();
        generalPage.dispose();
        super.dispose();
    }

    // ==================================================================
    // 外壳：WelcomePageHost
    // ==================================================================

    @Override
    public void reapplyTheme() {
        WelcomeSettings.reapplyTheme();
    }

    @Override
    public void retranslateAll() {
        setTitle(pageTitle());
        titleLabel.setText(pageTitle());
        subtitleLabel.setText(StringFormat.translate("welcome.subtitle"));

        //标签栏标题：顺序与 pages 一致（buildTabs 就是按它建的）
        for (int i = 0; i < pages.size() && i < tabbedPane.getTabCount(); i++) {
            tabbedPane.setTitleAt(i, StringFormat.translate(pages.get(i).translateKey()));
        }

        skipButton.setText(StringFormat.translate("welcome.skip"));
        prevButton.setText(StringFormat.translate("welcome.prev"));
        //语言一变，之前那条错误提示也该让位给常规提示
        showHint(StringFormat.translate("welcome.hint"), false);

        appearancePage.retranslate();
        generalPage.retranslate();

        updateNavigation();
        revalidate();
        repaint();

        //换语言会把标签栏标题、页脚按钮、各页标签统统换成长度不同的文字，
        //窗口尺寸得跟着走。构造阶段还没 displayable，那一次由构造器末尾的 pack() 负责。
        if (isDisplayable()) {
            pack();
            setLocationRelativeTo(null);
        }
    }

    @Override
    public void showHint(String text, boolean isError) {
        hintIsError = isError;
        hintLabel.setText(text);
        applyHintColor();
    }

    @Override
    public boolean isRebuilding() {
        return rebuilding;
    }

    // ==================================================================
    // 界面搭建
    // ==================================================================

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        iconLabel.setVerticalAlignment(SwingConstants.TOP);
        header.add(iconLabel, BorderLayout.WEST);

        titleLabel.putClientProperty("FlatLaf.style", "font: bold $h2.font");
        subtitleLabel.putClientProperty("FlatLaf.style", "foreground: $Label.disabledForeground");

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        //上下各一个 glue：文字块在图标高度内垂直居中
        text.add(Box.createVerticalGlue());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(titleLabel);
        text.add(Box.createVerticalStrut(6));
        text.add(subtitleLabel);
        text.add(Box.createVerticalGlue());

        header.add(text, BorderLayout.CENTER);
        return header;
    }

    /**
     * 把 {@link #pages} 里的设置页装进标签栏。
     *
     * <p>页面面板由 {@code MultiplePanel#getPanel()} 提供——外壳不认识具体的页面类，
     * 只认这个抽象，将来换平台（FX / Android）时这里不用改。</p>
     */
    private JComponent buildTabs() {
        tabbedPane.setOpaque(false);
        for (var page : pages) {
            //getPanel() 是 <T> T，这里明确要 JComponent，避免把裸类型带进布局
            JComponent content = page.page().getPanel();
            tabbedPane.addTab(StringFormat.translate(page.translateKey()), content);
        }
        //点标签直接翻页，所以选中项一变就要同步「上一步 / 下一步」的状态
        tabbedPane.addChangeListener(e -> updateNavigation());
        return tabbedPane;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(16, 0));
        footer.setOpaque(false);

        footer.add(skipButton, BorderLayout.WEST);
        footer.add(hintLabel, BorderLayout.CENTER);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        nav.setOpaque(false);
        nav.add(prevButton);
        nav.add(nextButton);
        footer.add(nav, BorderLayout.EAST);

        return footer;
    }

    /** 同步「上一步 / 下一步」的可用状态与文案（选中标签即当前位置） */
    private void updateNavigation() {
        int index = tabbedPane.getSelectedIndex();
        int last = tabbedPane.getTabCount() - 1;
        prevButton.setEnabled(index > 0);
        nextButton.setText(StringFormat.translate(index >= last ? "welcome.start" : "welcome.next"));
        //主按钮的文案会在「下一步」和「开始使用」之间切换，两者宽度差得不小
        //（英文下 Next / Get started 差得更远）。窗口是一次 pack 定型的，
        //不把宽度钉住的话，翻到最后一页底栏就会被挤一下、提示文字被省略号截掉。
        nextButton.setPreferredSize(nextButtonSize());
    }

    /** 按「下一步」和「开始使用」里更宽的那个算主按钮尺寸 */
    private Dimension nextButtonSize() {
        //先清掉上次设的尺寸，否则量到的还是旧值，语言一变就再也长不大了
        nextButton.setPreferredSize(null);
        String original = nextButton.getText();
        Dimension widest = new Dimension();
        try {
            for (String key : new String[]{"welcome.next", "welcome.start"}) {
                nextButton.setText(StringFormat.translate(key));
                Dimension size = nextButton.getPreferredSize();
                widest.width = Math.max(widest.width, size.width);
                widest.height = Math.max(widest.height, size.height);
            }
        } finally {
            nextButton.setText(original);
        }
        return widest;
    }

    // ==================================================================
    // 交互
    // ==================================================================

    private void registerThemeHooks() {
        iconTasks = IconControl.INSTANCE.addInDynamicConverter(
                () -> iconLabel.setIcon(IconControl.INSTANCE.getIcon("icon", ICON_SIZE)));
        themeTasks = ThemeChanger.addInDynamicConverter(this::refreshWindow);

        skipButton.addActionListener(e -> close(false));
        prevButton.addActionListener(e -> tabbedPane.setSelectedIndex(tabbedPane.getSelectedIndex() - 1));
        nextButton.addActionListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            if (index >= tabbedPane.getTabCount() - 1) {
                close(true);
            } else {
                tabbedPane.setSelectedIndex(index + 1);
            }
        });
    }

    private void applyHintColor() {
        hintLabel.setForeground(hintIsError ? WelcomeSettings.errorColor() : FluentColors.textSecondary());
    }

    /**
     * 让本窗口重新套用当前外观，并顺带刷新自绘的文字颜色。
     *
     * <p><b>为什么必须自己动手：</b>{@code ThemeChanger} 刷新的是
     * {@code JWindow.getOwnerlessWindows()}，而 {@code new JDialog()} 会被 Swing 自动挂到一个
     * 共享的隐藏 owner（{@code SwingUtilities$SharedOwnerFrame}）上，于是这扇窗根本不在
     * 「无主窗口」列表里——主题换了、{@code UIManager} 也换了，只有本页纹丝不动
     * （表现就是点了深色/强调色/方角，配置变了、界面没变）。</p>
     *
     * <p>注册成 {@code ThemeChanger} 的动态转换任务，既能补上这次刷新，
     * 又能赶在 {@code FlatAnimatedLafChange.hideSnapshotWithAnimation()} 之前完成，
     * 与主题切换的动画节奏对齐。</p>
     */
    private void refreshWindow() {
        rebuilding = true;
        try {
            SwingUtilities.updateComponentTreeUI(this);
            //updateUI 会把前景按 LAF 重置一遍，所以自绘的颜色要在它之后再压一次
            applyHintColor();
            //字号一变，整窗的首选尺寸也跟着变；窗口不可缩放，pack + 重新居中是唯一
            //能让它跟上内容的办法（尺寸没变时这两个调用都是空操作）。
            pack();
            setLocationRelativeTo(null);
        } catch (Throwable t) {
            logger.error("欢迎页外观刷新失败", t);
        } finally {
            rebuilding = false;
        }
    }

    // ==================================================================
    // 小工具
    // ==================================================================

    private static String pageTitle() {
        try {
            return MessageFormat.format(StringFormat.translate("welcome.title"), StringFormat.translate("app_name"));
        } catch (Exception e) {
            //翻译文件里漏了占位符之类的问题不该让欢迎页开不了
            return StringFormat.translate("welcome.title");
        }
    }
}

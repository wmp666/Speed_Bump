package com.wmp.downloader.tools.ui.fluent;

import com.formdev.flatlaf.ui.FlatButtonUI;
import com.formdev.flatlaf.ui.FlatUIUtils;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;

/**
 * 让普通 {@code JButton} 也有揭示高亮光晕的按钮 UI。
 *
 * <h3>为什么必须是 UI 代理，而不能只靠注册</h3>
 * <p>{@link RevealEngine} 只负责「算状态 + 画光晕」，真正的绘制时机必须由组件自己提供。
 * 而项目里 91 个按钮都是标准 {@code JButton}，它们的 {@code paint} 不认识引擎——
 * 只调 {@code RevealEngine.register(button)} 是<b>完全不会出效果</b>的
 * （自检里「按钮平均亮度差 0.00」就是这么发现的）。</p>
 *
 * <h3>为什么继承 FlatLaf 的按钮 UI，而不是 BasicButtonUI</h3>
 * <p>继承 {@code BasicButtonUI} 会把 FlatLaf 的整套按钮外观丢掉
 * （悬停底色、描边、圆角设置、焦点环、默认按钮处理），等于把项目现有界面推倒重来。
 * 继承 {@link FlatButtonUI} 则完整保留它们，只做一件事：多画一层光晕。</p>
 *
 * <h3>插入点是怎么选的</h3>
 * <p>FlatLaf 的绘制顺序是（见 {@code FlatButtonUI.update}）：
 * <pre>
 *   update(g, c):
 *       if (c.isOpaque())               FlatUIUtils.paintParentBackground(g, c);
 *       if (isContentAreaFilled(c))     paintBackground(g, c);      ← 背景
 *       paint(g, c);                                                ← 图标 + 文字
 * </pre>
 * 本类覆写 {@link #paint}，在调用 {@code super.paint} 之前先画光晕，
 * 于是光晕落在「背景之后、图标与文字之前」，与 WinUI 的层级一致。
 * 若改成在 {@code super.paint} 之后画，光晕会盖在文字上，浅色主题下会把标签洗淡。</p>
 *
 * <p><b>为什么不覆写 {@code paintBackground}</b>：那个方法只有在
 * {@code isContentAreaFilled()} 为真时才被调用，而
 * {@code setContentAreaFilled(false)} 的按钮恰恰是「没有背景但依然需要悬停反馈」的那一类。
 * 覆写 {@code paint} 对两种情况都成立。</p>
 *
 * <h3>适用条件</h3>
 * <p>本类依赖 FlatLaf。非 FlatLaf 外观（System / Metal / Windows Classic）下不应安装它，
 * 这一点由 {@link FluentUi#applyUiDefaults()} 判断。</p>
 */
public class RevealButtonUI extends FlatButtonUI {

    /**
     * 必须显式定义：{@code UIDefaults.getUI()} 反射调用的是静态 {@code createUI}，
     * 缺省会继承 {@link FlatButtonUI#createUI} 并返回普通按钮 UI。
     * 详见 {@link FluentScrollBarUI#createUI}。
     */
    public static ComponentUI createUI(JComponent c) {
        return new RevealButtonUI();
    }

    /**
     * @see FlatButtonUI#FlatButtonUI(boolean)
     */
    public RevealButtonUI() {
        super(false);
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        // 装上本 UI 即自动参与光晕，不需要调用方记得注册
        RevealEngine.register(c);
    }

    @Override
    public void uninstallUI(JComponent c) {
        RevealEngine.unregister(c);
        super.uninstallUI(c);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        if (RevealEngine.isEnabled()) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                // 用 FlatLaf 自己的圆角参数做裁剪，保证光晕不溢出按钮的圆角之外
                float arc = FlatUIUtils.getBorderArc(c);
                Shape clip = FluentPainting.roundRect(0, 0, c.getWidth(), c.getHeight(),
                        Math.round(arc / 2f));
                RevealEngine.paint(c, g2, clip);
            } catch (Throwable ignored) {
                // 光晕画不出来不应该影响按钮本身
            } finally {
                g2.dispose();
            }
        }
        // 图标与文字在光晕之上
        super.paint(g, c);
    }
}

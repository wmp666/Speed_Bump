package com.wmp.speed_bump.platform.ui.swing.window;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * 设置页的排版骨架：把「分组标题 / 标签+控件 / 独占整行」三种形式按
 * {@link GridBagLayout} 往下摆。
 *
 * <p>标签列宽由该列最宽的标签决定，所以各语言下标签都自然对齐，不需要为每种语言
 * 手工调宽度。</p>
 */
final class FormPanel extends JPanel {

    /** 各选项之间的行间距 */
    private static final int ROW_GAP = 10;

    /** 下一个可用行号 */
    private int row;

    FormPanel() {
        super(new GridBagLayout());
        setOpaque(false);
    }

    /** 分组小标题：横跨两列，与上一组之间拉开距离 */
    void section(JLabel label) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(row == 1 ? 0 : 16, 0, 8, 0);
        add(label, c);
    }

    /** 「标签 + 控件」一行 */
    void row(JLabel label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(0, 0, ROW_GAP, 14);
        add(label, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = row++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(0, 0, ROW_GAP, 0);
        add(field, c);
    }

    /** 独占整行（用来放自带说明文字的复选框之类） */
    void wide(JComponent component) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(0, 0, ROW_GAP, 0);
        add(component, c);
    }
}

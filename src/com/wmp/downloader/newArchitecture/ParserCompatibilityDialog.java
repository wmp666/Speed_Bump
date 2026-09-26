package com.wmp.downloader.newArchitecture;

import com.wmp.downloader.tools.ui.UITools;
import com.wmp.downloader.ui.FunctionDialog;
import com.wmp.speed_bump.common.background.tool.StringFormat;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 拓展兼容性确认弹窗。
 *
 * <p>以前 {@link ParserTaskInfo#loadParsers()} 每遇到一个与当前程序不兼容的拓展就会弹一次确认框，
 * 一次导入/加载多个拓展时要点很多次，也没法在同一个界面里对比它们。这里把「开发版本区间不匹配」
 * 与「推荐平台不匹配」两类警告收集起来，合并到同一个 {@link FunctionDialog} 里一次性显示：
 * 每个拓展对应一个勾选项，勾选 = 本次继续加载，取消勾选 = 本次跳过。
 * 弹窗只影响本次加载，不会记进配置。</p>
 *
 * <p>弹窗只在真的存在不兼容拓展时才出现；没有问题时不会打扰用户。</p>
 */
public final class ParserCompatibilityDialog {

    private static final Logger logger = Logger.getLogger(ParserCompatibilityDialog.class);

    /**
     * 勾选列表的宽度
     */
    private static final int LIST_WIDTH = 520;
    /**
     * 勾选列表的最大高度，超出后交给滚动条
     */
    private static final int LIST_MAX_HEIGHT = 320;
    /**
     * 单个勾选项（两行文字 + 上下留白）占用的高度
     */
    private static final int ITEM_HEIGHT = 42;

    private ParserCompatibilityDialog() {
    }

    /**
     * 一条与当前程序不兼容的拓展。
     *
     * @param jar        拓展所在的 jar
     * @param title      勾选项上显示的文字（可含 {@code \n}，渲染时会转成换行）
     * @param tooltip    悬浮提示，给出更完整的说明（可含 {@code \n}）
     */
    public record Conflict(File jar, String title, String tooltip) {
    }

    /**
     * 把全部兼容性警告合并成一个勾选弹窗显示，并算出本次需要跳过的拓展。
     *
     * <p>弹窗因异常没能显示时（例如极端的窗口环境问题）不阻断加载流程，本次不做任何跳过；
     * 而只要弹窗正常显示过，用户取消或直接关闭它就意味着「这些拓展本次全部跳过」。</p>
     *
     * @param conflicts 需要用户确认的拓展，允许为 {@code null} 或空
     * @return 用户选择本次跳过的 jar
     */
    public static Set<File> confirmSkips(List<Conflict> conflicts) {
        Set<File> skips = new LinkedHashSet<>();
        if (conflicts == null || conflicts.isEmpty()) {
            return skips;
        }

        DialogOutcome outcome = showOnEdt(conflicts);
        if (!outcome.shown()) {
            return skips;
        }

        Set<File> selected = outcome.selected();
        for (Conflict conflict : conflicts) {
            //selected 为 null 表示用户取消/关闭了弹窗，此时全部跳过
            if (selected == null || !selected.contains(conflict.jar())) {
                skips.add(conflict.jar());
            }
        }
        return skips;
    }

    /**
     * 保证弹窗在事件分发线程上创建与显示（{@code loadParsers()} 也可能在虚拟线程里被调用）。
     */
    private static DialogOutcome showOnEdt(List<Conflict> conflicts) {
        if (SwingUtilities.isEventDispatchThread()) {
            try {
                return new DialogOutcome(true, showDialog(conflicts));
            } catch (Throwable t) {
                logger.error("拓展兼容性弹窗显示失败", t);
                return DialogOutcome.NOT_SHOWN;
            }
        }

        AtomicReference<DialogOutcome> outcomeRef = new AtomicReference<>();
        try {
            //切到 EDT 后走上面的同一段逻辑，避免异常处理写两遍
            SwingUtilities.invokeAndWait(() -> outcomeRef.set(showOnEdt(conflicts)));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.error("等待拓展兼容性弹窗时被中断", ex);
            return DialogOutcome.NOT_SHOWN;
        } catch (InvocationTargetException ex) {
            logger.error("拓展兼容性弹窗显示失败", ex.getCause());
            return DialogOutcome.NOT_SHOWN;
        }

        DialogOutcome outcome = outcomeRef.get();
        return outcome != null ? outcome : DialogOutcome.NOT_SHOWN;
    }

    /**
     * 弹窗结果
     *
     * @param shown    弹窗是否成功显示过
     * @param selected 用户勾选的 jar；用户取消或直接关闭弹窗时为 {@code null}
     */
    private record DialogOutcome(boolean shown, Set<File> selected) {
        private static final DialogOutcome NOT_SHOWN = new DialogOutcome(false, null);
    }

    /**
     * 真正构造并显示弹窗（只能在 EDT 上调用）。
     *
     * @return 用户勾选的 jar；用户取消或直接关闭弹窗时返回 {@code null}
     */
    private static Set<File> showDialog(List<Conflict> conflicts) {
        List<JCheckBox> checkBoxes = new ArrayList<>(conflicts.size());

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        for (Conflict conflict : conflicts) {
            JCheckBox checkBox = new JCheckBox(toHtml(conflict.title()), true);
            checkBox.setOpaque(false);
            checkBox.setToolTipText(toHtml(conflict.tooltip()));
            checkBox.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            checkBox.setHorizontalAlignment(SwingConstants.TRAILING);
            checkBoxes.add(checkBox);
            listPanel.add(checkBox);
        }

        JScrollPane scrollPane = UITools.setScrollPaneUnOpaque(new JScrollPane(listPanel));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(LIST_WIDTH,
                Math.min(LIST_MAX_HEIGHT, conflicts.size() * ITEM_HEIGHT + 8)));

        JPanel contentPanel = new JPanel(new BorderLayout(0, 8));
        contentPanel.setOpaque(false);
        contentPanel.add(createTipLabel(), BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        JButton selectAllButton = new JButton(StringFormat.translate("selection_all"));
        selectAllButton.addActionListener(e -> setAllSelected(checkBoxes, true));

        JButton selectNoneButton = new JButton(StringFormat.translate("selection_none"));
        selectNoneButton.addActionListener(e -> setAllSelected(checkBoxes, false));

        //回调里收集勾选结果：无论项目配置的是独立弹窗还是嵌入式弹窗都能拿到用户的选择
        AtomicReference<Set<File>> selectedRef = new AtomicReference<>();
        FunctionDialog.showDialog(
                null,
                StringFormat.translate("plugins.load_warning.title"),
                contentPanel,
                result -> {
                    if (result != FunctionDialog.RESULT_OK) return;
                    Set<File> selected = new LinkedHashSet<>();
                    for (int i = 0; i < conflicts.size(); i++) {
                        if (checkBoxes.get(i).isSelected()) {
                            selected.add(conflicts.get(i).jar());
                        }
                    }
                    selectedRef.set(selected);
                },
                FunctionDialog.OK_CANCEL_BUTTONS,
                0,
                new JButton[]{selectAllButton, selectNoneButton},
                FunctionDialog.NORTH_DIRECTION_RIGHT,
                //启动阶段预加载窗口是置顶的，不置顶会让警告弹窗被它盖住
                true,
                false);

        return selectedRef.get();
    }

    private static JLabel createTipLabel() {
        JLabel tipLabel = new JLabel(toHtml(StringFormat.translate("plugins.load_warning.tip")));
        tipLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        return tipLabel;
    }

    private static void setAllSelected(List<JCheckBox> checkBoxes, boolean selected) {
        for (JCheckBox checkBox : checkBoxes) {
            checkBox.setSelected(selected);
        }
    }

    /**
     * 把带 {@code \n} 的普通文本转成 Swing 能换行显示的 HTML，并转义 &lt; &amp; &gt;
     */
    private static String toHtml(String text) {
        String escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        return escaped;
    }
}

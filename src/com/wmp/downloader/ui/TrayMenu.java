package com.wmp.downloader.ui;

import com.wmp.downloader.newArchitecture.abstractTask.AbstractTask;
import com.wmp.speed_bump.common.background.tool.StringFormat;
import com.wmp.downloader.tools.TestFunctionControl;
import com.wmp.downloader.tools.ui.DialogBackdrop;
import com.wmp.downloader.tools.ui.DynamicConverterTask;
import com.wmp.downloader.tools.ui.FlyoutMenu;
import com.wmp.downloader.tools.ui.IconControl;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * 托盘图标的右键菜单。
 *
 * <p>菜单本体是 {@link FlyoutMenu}——一个自绘的透明 {@link JWindow}，而不是
 * {@link JPopupMenu}。原因是这里有两点 JPopupMenu 做不到：</p>
 * <ul>
 *   <li>菜单项要有<b>划入/划出动画</b>（高亮色渐变过渡）；</li>
 *   <li>菜单窗口要能承载<b>原生背景材质</b>（Mica，Win10 上回退为模糊）。
 *       JPopupMenu 的弹窗由 Swing 的 {@code PopupFactory} 托管且被缓存复用，
 *       拿不到稳定可控的窗口来做 per-pixel 透明。</li>
 * </ul>
 *
 * <p>材质的开关沿用项目已有的测试项
 * （{@code TestFunctionControl} mainId {@link DialogBackdrop#TEST_FUNCTION_MAIN_ID}）：
 * 未启用时菜单就是一个普通的圆角不透明菜单，行为与改造前一致。</p>
 *
 * <p>托盘的 {@code TrayIcon#setPopupMenu} 只接受 AWT 原生菜单，因此
 * {@link Downloader} 改为监听托盘鼠标事件并调用 {@link #showFor(MouseEvent)}。</p>
 */
public class TrayMenu {

    private static final Logger logger = Logger.getLogger(TrayMenu.class);

    private final Downloader frame;
    private final FlyoutMenu menu = new FlyoutMenu();

    private final FlyoutMenu.Item showItem;
    private final FlyoutMenu.Item startAllItem;
    private final FlyoutMenu.Item pauseAllItem;
    private FlyoutMenu.Item alwaysOnTopItem;
    private final FlyoutMenu.Item checkUpdateItem;
    private final FlyoutMenu.Item settingsItem;
    private final FlyoutMenu.Item aboutItem;
    private final FlyoutMenu.Item exitItem;

    private DynamicConverterTask[] iconTasks = new DynamicConverterTask[0];

    /**
     * 本次点击是否已经在「按下」时弹出过菜单，用于避免同一击在松开时再次弹出
     * （不同平台 popupTrigger 出现的时机不同，这里按下与松开都监听）
     */
    private boolean popupShownOnPress = false;

    public TrayMenu(Downloader frame) {
        this.frame = frame;

        // 先读取测试项开关，再创建窗口——材质必须在窗口变为 displayable 之前接入
        DialogBackdrop.applyTestFunctionSwitch();

        menu.setHeaderTitle(StringFormat.translate("app_name"));
        menu.setHeaderIconKey("icon");

        showItem = menu.addItem("icon", translate("tray.show"), this::showMainFrame);

        menu.addSeparator();

        startAllItem = menu.addItem("start", translate("task.taskControl.allStart"), this::startAllTasks);
        pauseAllItem = menu.addItem("pause", translate("task.taskControl.allPause"), this::pauseAllTasks);

        menu.addSeparator();


        TestFunctionControl.run(1002, 1,
                () -> {
                    alwaysOnTopItem = menu.addCheckItem(translate("frame.is_always_top"),
                            frame::setAlwaysOnTop);
                    alwaysOnTopItem.setChecked(frame.isAlwaysOnTop());
                }, ()->{});

        checkUpdateItem = menu.addItem("update", translate("check_update"), () -> {
            showMainFrame();
            frame.checkUpdate();
        });
        settingsItem = menu.addItem("settings", translate("settings"), () -> selectTab(frame.settingsPanel));
        aboutItem = menu.addItem("about", translate("app.about"), () -> selectTab(frame.aboutPanel));

        menu.addSeparator();

        exitItem = menu.addItem("power_switch", translate("frame.exit"), () -> System.exit(0));

        // 图标随主题 / 图标包变化刷新
        iconTasks = IconControl.addInDynamicConverter(
                () -> menu.setHeaderIconKey("icon"),
                () -> showItem.setIconKey("icon"),
                () -> startAllItem.setIconKey("start"),
                () -> pauseAllItem.setIconKey("pause"),
                () -> checkUpdateItem.setIconKey("update"),
                () -> settingsItem.setIconKey("settings"),
                () -> aboutItem.setIconKey("about"),
                () -> exitItem.setIconKey("power_switch")
        );

        refreshState();
    }

    private static String translate(String key) {
        return StringFormat.translate(key);
    }

    // ------------------------------------------------------------------
    // 弹出
    // ------------------------------------------------------------------

    /**
     * 在托盘鼠标事件的位置弹出菜单。
     *
     * @param event 托盘图标上的鼠标事件
     */
    public void showFor(MouseEvent event) {
        if (event == null || (!event.isPopupTrigger() && !SwingUtilities.isRightMouseButton(event))) {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showFor(event));
            return;
        }

        boolean onPress = event.getID() == MouseEvent.MOUSE_PRESSED;
        if (!onPress && popupShownOnPress) {
            // 这一击已经在按下时弹出过菜单，松开时不再重复弹出
            popupShownOnPress = false;
            return;
        }
        popupShownOnPress = onPress;

        refreshState();
        menu.showAt(eventLocationOnScreen(event));
    }

    /**
     * 关闭菜单并释放注册到全局的监听与动态转换任务。
     */
    public void dispose() {
        menu.dispose();
        IconControl.removeInDynamicConverter(iconTasks);
        iconTasks = new DynamicConverterTask[0];
    }

    /**
     * 刷新菜单内与运行状态相关的部分（任务数量、置顶勾选、按钮可用性）。
     */
    private void refreshState() {
        List<AbstractTask> tasks = List.copyOf(frame.taskList);
        long running = tasks.stream().filter(AbstractTask::isRunning).count();
        long pending = tasks.stream().filter(task -> !task.isFinally() && !task.isRunning()).count();

        startAllItem.setEnabled(pending > 0);
        pauseAllItem.setEnabled(running > 0);


        String summary = running > 0
                ? StringFormat.translate("statusbar.running_summary")
                : StringFormat.translate("statusbar.idle_summary");
        try {
            menu.setHeaderSubtitle(String.format(summary, running, tasks.size()));
        } catch (Exception e) {
            menu.setHeaderSubtitle(summary);
        }
    }

    // ------------------------------------------------------------------
    // 动作
    // ------------------------------------------------------------------

    /**
     * 显示并激活主窗口。
     */
    public void showMainFrame() {
        frame.setVisible(true);
        frame.setState(JFrame.NORMAL);
        frame.toFront();
        frame.requestFocus();
    }

    private void startAllTasks() {
        for (AbstractTask task : List.copyOf(frame.taskList)) {
            if (!task.isFinally() && !task.isRunning()) {
                task.start();
            }
        }
    }

    private void pauseAllTasks() {
        for (AbstractTask task : List.copyOf(frame.taskList)) {
            if (task.isRunning()) {
                task.stop();
            }
        }
    }

    private void selectTab(JComponent panel) {
        if (panel == null) {
            return;
        }
        showMainFrame();
        if (frame.mainTabbedPane == null) {
            return;
        }
        int index = frame.mainTabbedPane.indexOfComponent(panel);
        if (index >= 0) {
            frame.mainTabbedPane.setSelectedIndex(index);
        }
    }

    private Point eventLocationOnScreen(MouseEvent event) {
        try {
            // 部分 Linux 桌面环境下托盘事件的坐标不可用，直接取指针屏幕位置更可靠
            Point pointer = MouseInfo.getPointerInfo() == null ? null : MouseInfo.getPointerInfo().getLocation();
            if (pointer != null) {
                return pointer;
            }
        } catch (Exception e) {
            logger.warn("获取鼠标位置失败", e);
        }
        try {
            return event.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            return new Point(0, 0);
        }
    }
}

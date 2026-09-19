package com.wmp.speed_bump.platform.ui.swing.window;

import com.wmp.downloader.tools.ui.DialogBackdrop;
import com.wmp.speed_bump.common.ui.PreLoadDialog;

import javax.swing.*;

public class PreloadDialog extends JDialog implements PreLoadDialog {
    private JPanel contentPane;

    public PreloadDialog() {
        setTitle("Speed Bump preloading...");
        setContentPane(contentPane);
        setResizable(false);
        setUndecorated(true);
        setAlwaysOnTop(true);

        // ===== [BACKDROP-START] 背景材质（Mica / 模糊）测试项 =====
        // 整块删除即可彻底移除该功能，加载窗恢复原来的不透明外观。
        // 开关：在「测试功能」里启用 mainId 1004 后重启应用；
        //       代码里也可用 DialogBackdrop.setEnabled(...) 强制开关。
        // 必须先于 pack()：窗口一旦 displayable 就无法再设为透明。
        DialogBackdrop.applyTestFunctionSwitch();
        DialogBackdrop.installBorderless(this, contentPane);
        // ===== [BACKDROP-END] =====

        pack();
        setLocationRelativeTo(null);

    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (b) {
            // ===== [BACKDROP-START] 应用原生背景材质 =====
            // 窗口可见后才能通过 EnumWindows 枚举到它的 HWND，因此放在显示之后；
            // activate 内部对「窗口尚未可见」的情况带自动重试，不会漏掉。
            DialogBackdrop.activate(this);
            // ===== [BACKDROP-END] =====
        }
    }

    @Override
    public void showDialog() {
        setVisible(true);
    }

    @Override
    public void disposeDialog() {
        setVisible(false);
    }
}

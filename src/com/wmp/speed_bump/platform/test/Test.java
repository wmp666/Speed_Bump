package com.wmp.speed_bump.platform.test;

import com.wmp.downloader.tools.ui.IconControl;

import javax.swing.*;
import java.awt.*;

interface TestLambda {
    int test(int a, int b);
}

public class Test {
    static void main() {
        var jFrame = new JFrame("");

        jFrame.setIconImage(IconControl.getImage("icon"));
        jFrame.add(new JButton("13123"));
        jFrame.pack();
        jFrame.setVisible(true);

        var taskbar = Taskbar.getTaskbar();
        taskbar.setWindowProgressState(jFrame, Taskbar.State.INDETERMINATE);
        taskbar.setWindowIconBadge(jFrame, IconControl.getImage("info"));


    }
}
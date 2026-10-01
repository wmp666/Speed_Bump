package com.wmp.speed_bump.platform.ui.swing.components;

import javax.swing.*;

/**
 * 通用进度条 {@link com.wmp.speed_bump.common.ui.components.SBProgressBar} 的 Swing 平台实现。
 */
public class SBProgressBar extends JProgressBar implements com.wmp.speed_bump.common.ui.components.SBProgressBar {

    public SBProgressBar() {
        super(0, 100);
    }

    public SBProgressBar(int min, int max) {
        super(min, max);
    }

    @Override
    public void setProgressValue(int value) {
        setValue(value);
    }

    @Override
    public void setProgressMinValue(int minValue) {
        setMinimum(minValue);
    }

    @Override
    public void setProgressMaxValue(int maxValue) {
        setMaximum(maxValue);
    }

    @Override
    public void setProgressIndeterminate(boolean indeterminate) {
        setIndeterminate(indeterminate);
    }

    @Override
    public boolean ProgressIsIndeterminate() {
        return isIndeterminate();
    }

    @Override
    public void setProgressString(String text) {
        setString(text);
    }

    @Override
    public void setProgressStringPainted(boolean painted) {
        setStringPainted(painted);
    }
}

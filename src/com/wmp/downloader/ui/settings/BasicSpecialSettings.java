package com.wmp.downloader.ui.settings;

import javax.swing.*;

public abstract class BasicSpecialSettings {


    public abstract String getSettingsName();

    public abstract SpecialSettingsPanel getSettings();

    /**
     * 需要动态保存数据
     */
    public abstract class SpecialSettingsPanel extends JPanel{
        public SpecialSettingsPanel() {
            this.setOpaque(false);
        }

        public abstract void setDefaultButton();

    }
}

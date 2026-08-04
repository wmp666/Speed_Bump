package com.wmp.downloader.newArchitecture.abstractTask;

import javax.swing.*;

public abstract class AbstractSpecialSettingsPage extends JPanel {
    public AbstractSpecialSettingsPage() {
        this.setOpaque(false);
    }

    public abstract void setDefaultButton();

    public abstract String getSettingsName();
}

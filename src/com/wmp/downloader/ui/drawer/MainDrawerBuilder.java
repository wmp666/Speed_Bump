package com.wmp.downloader.ui.drawer;

import com.wmp.downloader.tools.DataControl;
import raven.modal.drawer.menu.MenuOption;
import raven.modal.drawer.simple.SimpleDrawerBuilder;
import raven.modal.drawer.simple.footer.SimpleFooterData;
import raven.modal.drawer.simple.header.SimpleHeaderData;

public class MainDrawerBuilder extends SimpleDrawerBuilder {
    public MainDrawerBuilder(MenuOption menuOption) {
        super(menuOption);

        var drawerMenu = getDrawerMenu();
    }

    @Override
    public SimpleHeaderData getSimpleHeaderData() {
        return new SimpleHeaderData();
    }

    @Override
    public SimpleFooterData getSimpleFooterData() {
        return new SimpleFooterData().setTitle("Version " + DataControl.get("version", "0.0.1"));
    }
}

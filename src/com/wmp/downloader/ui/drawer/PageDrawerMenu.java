package com.wmp.downloader.ui.drawer;

import com.wmp.downloader.tools.ui.IconControl;
import raven.modal.drawer.item.Item;
import raven.modal.drawer.menu.DrawerMenu;
import raven.modal.drawer.menu.MenuOption;

public class PageDrawerMenu extends DrawerMenu {
    public PageDrawerMenu(MenuOption menuOption) {
        super(menuOption);
    }

    @Override
    protected ButtonItem createMenuItem(Item item, int[] index, int menuLevel, boolean isMainItem) {
        var menuItem = super.createMenuItem(item, index, menuLevel, isMainItem);
        menuItem.setIcon(IconControl.getIcon(item.getIcon(), menuItem.getFont().getSize()));
        return menuItem;
    }
}

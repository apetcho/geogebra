package org.geogebra.common.gui.menu.impl;

import org.geogebra.common.GeoGebraConstants;
import org.geogebra.common.gui.menu.Action;
import org.geogebra.common.gui.menu.DrawerMenu;
import org.geogebra.common.gui.menu.Icon;
import org.geogebra.common.gui.menu.MenuItem;
import org.geogebra.common.gui.menu.MenuItemGroup;

public class ExamDrawerMenuFactory extends AbstractDrawerMenuFactory {

	ExamDrawerMenuFactory(GeoGebraConstants.Platform platform) {
		super(platform);
	}

	@Override
	public DrawerMenu createDrawerMenu() {
		MenuItemGroup group = new MenuItemGroupImpl(clearConstruction(), showExamLog(), exitExamMode());
		String title = getMenuTitle();
		return new DrawerMenuImpl(title, group);
	}

	private static MenuItem exitExamMode() {
		return new ActionableItemImpl(Icon.HOURGLASS_EMPTY, "exam_menu_exit", Action.EXIT_EXAM_MODE);
	}

	private static MenuItem showExamLog() {
		return new ActionableItemImpl(Icon.ASSIGNMENT, "exam_log_header", Action.SHOW_EXAM_LOG);
	}
}

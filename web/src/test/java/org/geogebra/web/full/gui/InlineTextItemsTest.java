package org.geogebra.web.full.gui;

import static junit.framework.TestCase.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geogebra.common.awt.GPoint2D;
import org.geogebra.common.euclidian.draw.DrawInlineText;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoInlineText;
import org.geogebra.common.kernel.geos.GeoPolygon;
import org.geogebra.web.full.html5.GMenuBarMock;
import org.geogebra.web.full.html5.MenuFactory;
import org.geogebra.web.html5.main.AppW;
import org.geogebra.web.test.AppMocker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gwt.dom.client.TextAreaElement;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.gwtmockito.WithClassesToStub;

@RunWith(GwtMockitoTestRunner.class)
@WithClassesToStub({ TextAreaElement.class})
public class InlineTextItemsTest {

	public static final String LINK_URL = "www.foo.bar";
	private ContextMenuGeoElementW contextMenu;
	private Construction construction;
	private AppW app;
	private GPoint2D point;
	private ContextMenuFactory factory;
	private InlineTextControllerMock controllerMockWithLink;

	@Before
	public void setUp() {
		app = AppMocker.mockNotes(getClass());
		factory = new MenuFactory(app);
		construction = app.getKernel().getConstruction();
		point = new GPoint2D(0, 0);
		controllerMockWithLink = new InlineTextControllerMock(LINK_URL);
		enableSettingsItem();
	}

	private void enableSettingsItem() {
		app.setShowMenuBar(true);
		app.setRightClickEnabled(true);
	}

	@Test
	public void testSingleInlineTextContextMenu() {
		ArrayList<GeoElement> geos = new ArrayList<>();
		geos.add(createTextInline("text1", new InlineTextControllerMock()));
		contextMenu = new ContextMenuGeoElementW(app, geos, factory);
		contextMenu.addOtherItems();
		GMenuBarMock menu = (GMenuBarMock) contextMenu.getWrappedPopup().getPopupMenu();
		List<String> expected = Arrays.asList(
				"TEXTTOOLBAR", "ContextMenu.Font", "Link",
				"SEPARATOR", "Cut", "Copy", "Paste",
				"SEPARATOR", "General.Order",
				"SEPARATOR",
				"FixObject", "Settings"
		);

		assertEquals(expected, menu.getTitles());
	}

	@Test
	public void testSingleInlineTextWithLinkContextMenu() {
		ArrayList<GeoElement> geos = new ArrayList<>();
		geos.add(createTextInline("text1", controllerMockWithLink));
		contextMenu = new ContextMenuGeoElementW(app, geos, factory);
		contextMenu.addOtherItems();
		GMenuBarMock menu = (GMenuBarMock) contextMenu.getWrappedPopup().getPopupMenu();
		List<String> expected = Arrays.asList(
				"TEXTTOOLBAR", "ContextMenu.Font", "editLink", "removeLink",
				"SEPARATOR", "Cut", "Copy", "Paste",
				"SEPARATOR", "General.Order",
				"SEPARATOR",
				"FixObject", "Settings"
		);

		assertEquals(expected, menu.getTitles());
	}

	@Test
	public void testMultipleInlineTextContextMenu() {
		ArrayList<GeoElement> geos = new ArrayList<>();
		geos.add(createTextInline("text1", controllerMockWithLink));
		geos.add(createTextInline("text2", controllerMockWithLink));
		contextMenu = new ContextMenuGeoElementW(app, geos, factory);
		contextMenu.addOtherItems();
		GMenuBarMock menu = (GMenuBarMock) contextMenu.getWrappedPopup().getPopupMenu();
		List<String> expected = Arrays.asList(
				"TEXTTOOLBAR", "ContextMenu.Font",
				"SEPARATOR", "Cut", "Copy", "Paste",
				"SEPARATOR", "General.Order",
				"SEPARATOR",
				"FixObject", "Settings"
		);

		assertEquals(expected, menu.getTitles());
	}

	@Test
	public void testInlineTextAndPolygonContextMenu() {
		ArrayList<GeoElement> geos = new ArrayList<>();
		geos.add(createTextInline("text1", new InlineTextControllerMock(LINK_URL)));
		geos.add(createPolygon("poly1"));
		contextMenu = new ContextMenuGeoElementW(app, geos, factory);
		contextMenu.addOtherItems();
		GMenuBarMock menu = (GMenuBarMock) contextMenu.getWrappedPopup().getPopupMenu();
		List<String> expected = Arrays.asList(
				"Cut", "Copy", "Paste",
				"SEPARATOR", "General.Order",
				"SEPARATOR",
				"FixObject", "Settings"
		);

		assertEquals(expected, menu.getTitles());
	}

	@Test
	public void testPolygonContextMenu() {
		ArrayList<GeoElement> geos = new ArrayList<>();
		geos.add(createPolygon("Poly1"));
		contextMenu = new ContextMenuGeoElementW(app, geos, factory);
		contextMenu.addOtherItems();
		GMenuBarMock menu = (GMenuBarMock) contextMenu.getWrappedPopup().getPopupMenu();
		List<String> expected = Arrays.asList(
				"Cut", "Copy", "Paste", "SEPARATOR", "General.Order", "SEPARATOR",
				"FixObject", "ShowTrace", "Settings"
		);

		assertEquals(expected, menu.getTitles());
	}

	private GeoElement createPolygon(String label) {
		GeoPolygon poly = new GeoPolygon(construction);
		poly.setLabel(label);
		return poly;
	}

	private GeoInlineText createTextInline(String label, InlineTextControllerMock inlineTextControllerMock) {
		GeoInlineText text = new GeoInlineText(construction, point);
		text.setLabel(label);
		DrawInlineText drawInlineText = (DrawInlineText) app.getActiveEuclidianView()
				.getDrawableFor(text);
		drawInlineText.setTextController(inlineTextControllerMock);
		return text;
	}
}

package org.geogebra.web.editor;

import org.geogebra.gwtutil.JsConsumer;
import org.geogebra.keyboard.web.TabbedKeyboard;
import org.geogebra.web.html5.bridge.RenderGgbElement.RenderGgbElementFunction;
import org.geogebra.web.resources.StyleInjector;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.himamis.retex.editor.web.MathFieldW;

import elemental2.dom.DomGlobal;

public final class RenderEditor implements RenderGgbElementFunction {
	private TabbedKeyboard tabbedKeyboard = null;
	private EditorApi editorApi;
	private MathFieldW mathField;

	@Override
	public void render(Element el, JsConsumer<Object> callback) {
		EditorListener listener = new EditorListener();
		MathFieldW mf = initMathField(el, listener);
		if (tabbedKeyboard == null) {
			tabbedKeyboard = initKeyboard(mf, el);
			StyleInjector.onStylesLoaded(tabbedKeyboard::show);
			editorApi = new EditorApi(mf, tabbedKeyboard, listener);
			tabbedKeyboard.setListener((visible, field) -> {
				if (!visible) {
					editorApi.closeKeyboard();
				} else {
					editorApi.openKeyboard();
				}
				return false;
			});
		}
		if (callback != null) {
			callback.accept(editorApi);
		}
	}

	private TabbedKeyboard initKeyboard(MathFieldW mf, Element el) {
		EditorKeyboardContext editorKeyboardContext = new EditorKeyboardContext(el);
		TabbedKeyboard tabbedKeyboard = new TabbedKeyboard(editorKeyboardContext, false);
		FlowPanel keyboardWrapper = new FlowPanel();
		keyboardWrapper.setStyleName("GeoGebraFrame");
		keyboardWrapper.add(tabbedKeyboard);
		RootPanel.get().add(keyboardWrapper);
		tabbedKeyboard.setProcessing(new MathFieldProcessing(mf));
		tabbedKeyboard.clearAndUpdate();
		DomGlobal.window.addEventListener("resize", evt -> tabbedKeyboard.onResize());
		return tabbedKeyboard;
	}

	private MathFieldW initMathField(Element el, EditorListener listener) {
		Canvas canvas = Canvas.createIfSupported();
		FlowPanel wrapper = new FlowPanel();
		wrapper.setWidth("100%");
		wrapper.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
		mathField = new MathFieldW(null, wrapper, canvas, listener);
		EditorParams editorParams = new EditorParams(el, mathField);
		listener.setMathField(mathField);
		mathField.parse("");
		wrapper.add(mathField);

		if (!editorParams.isPreventFocus()) {
			mathField.requestViewFocus();
		}

		mathField.setPixelRatio(DomGlobal.window.devicePixelRatio);
		mathField.getInternal().setSyntaxAdapter(new EditorSyntaxAdapter());
		RootPanel rootPanel = newRoot(el);
		rootPanel.add(wrapper);

		rootPanel.addDomHandler(evt -> onFocus(), ClickEvent.getType());
		return mathField;
	}

	private void onFocus() {
		mathField.requestViewFocus();
		MathFieldProcessing processing = new MathFieldProcessing(mathField);
		tabbedKeyboard.setProcessing(processing);
	}

	private RootPanel newRoot(Element el) {
		Element detachedKeyboardParent = DOM.createDiv();
		detachedKeyboardParent.setClassName("GeoGebraFrame editor");
		String uid = DOM.createUniqueId();
		detachedKeyboardParent.setId(uid);
		el.appendChild(detachedKeyboardParent);
		return RootPanel.get(uid);
	}
}

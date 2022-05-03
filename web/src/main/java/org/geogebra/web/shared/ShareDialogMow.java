package org.geogebra.web.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geogebra.common.main.Localization;
import org.geogebra.common.main.SaveController.SaveListener;
import org.geogebra.common.move.ggtapi.GroupIdentifier;
import org.geogebra.common.move.ggtapi.models.Material;
import org.geogebra.common.move.ggtapi.operations.BackendAPI;
import org.geogebra.common.move.ggtapi.requests.MaterialCallbackI;
import org.geogebra.common.util.AsyncOperation;
import org.geogebra.web.html5.gui.laf.VendorSettings;
import org.geogebra.web.html5.gui.tooltip.ToolTipManagerW;
import org.geogebra.web.html5.gui.util.FastClickHandler;
import org.geogebra.web.html5.gui.util.NoDragImage;
import org.geogebra.web.html5.gui.view.button.StandardButton;
import org.geogebra.web.html5.main.AppW;
import org.geogebra.web.shared.components.ComponentLinkBox;
import org.geogebra.web.shared.components.ComponentSwitch;
import org.geogebra.web.shared.components.dialog.ComponentDialog;
import org.geogebra.web.shared.components.dialog.DialogData;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 *  Joint share dialog for mow (group + link sharing)
 */
public class ShareDialogMow extends ComponentDialog
		implements FastClickHandler, SaveListener {
	private Localization localization;
	private ScrollPanel scrollPanel;
	private Label linkShareOnOffLbl;
	private Label linkShareHelpLbl;
	private ComponentSwitch shareSwitch;
	private FlowPanel linkPanel;
	private ComponentLinkBox linkBox;
	private StandardButton copyBtn;
	private Material material;
	private MaterialCallbackI callback;
	private HashMap<GroupIdentifier, Boolean> changedGroups = new HashMap<>();
	private List<GroupIdentifier> sharedGroups = new ArrayList<>();

	/**
	 * @param app
	 *            see {@link AppW}
	 * @param data
	 *            dialog tanslation keys
	 * @param shareURL
	 *            share URL
	 * @param mat
	 *            active material
	 */
	public ShareDialogMow(AppW app, DialogData data, String shareURL, Material mat) {
		super(app, data, false, true);
		this.localization = app.getLocalization();
		this.material = mat == null ? app.getActiveMaterial() : mat;
		addStyleName("shareDialogMow");
		buildContent(shareURL);
		setAction();
	}

	private void setAction() {
		setOnPositiveAction(() -> {
			if (isShareLinkOn()) {
				// set from private -> shared
				if (material != null && "P".equals(material.getVisibility())) {
					app.getLoginOperation().getGeoGebraTubeAPI().uploadMaterial(
							material.getSharingKeyOrId(), "S",
							material.getTitle(), null, callback,
							material.getType());
					material.setVisibility("S");
				}
			} else {
				// set from shared -> private
				if (material != null && "S".equals(material.getVisibility())) {
					app.getLoginOperation().getGeoGebraTubeAPI().uploadMaterial(
							material.getSharingKeyOrId(), "P",
							material.getTitle(), null, callback,
							material.getType());
					material.setVisibility("P");
				}
			}
			shareWithGroups(this::onGroupShareChanged);
		});
	}

	/**
	 * @return text field containing share link
	 */
	public ComponentLinkBox getLinkBox() {
		return linkBox;
	}

	/**
	 * @param sharedGroupList
	 *            list of group with which the material was shared
	 */
	public void updateOnSharedGroups(List<GroupIdentifier> sharedGroupList) {
		if (sharedGroupList == null) {
			return;
		}
		this.sharedGroups = sharedGroupList;
		ArrayList<GroupIdentifier> groupNames = app.getLoginOperation().getModel()
				.getUserGroups();
		scrollPanel.clear();
		FlowPanel groups = new FlowPanel();
		// first add button for groups with which material was already shared
		for (GroupIdentifier sharedGroup : sharedGroups) {
			addGroup(groups, sharedGroup, true);
		}

		// then add other existent groups of user
		for (GroupIdentifier group : groupNames) {
			if (!sharedGroups.contains(group)) {
				addGroup(groups, group, false);
			}
		}
		scrollPanel.add(groups);
		centerAndResize(((AppW) app).getAppletFrame().getKeyboardHeight());
	}

	private void addGroup(FlowPanel groupsPanel, GroupIdentifier groupDesc, boolean shared) {
		groupsPanel.add(new GroupButtonMow(groupDesc, shared,
				this::updateChangedGroupList));
	}

	/**
	 * @return true if share by link is active
	 */
	public boolean isShareLinkOn() {
		return shareSwitch.isSwitchOn();
	}

	private void buildContent(String shareURL) {
		FlowPanel dialogContent = new FlowPanel();
		getGroupsSharedWith();
		// get list of groups of user
		ArrayList<GroupIdentifier> groupNames = app.getLoginOperation().getModel()
				.getUserGroups();
		// user has no groups
		if (groupNames.isEmpty()) {
			buildNoGroupPanel(dialogContent);
		} else { // show groups of user
			buildGroupPanel(dialogContent);
		}
		buildShareByLinkPanel(dialogContent, shareURL);
		buildSharingAvailableInfo(dialogContent);
		addDialogContent(dialogContent);
	}

	private void buildNoGroupPanel(FlowPanel dialogContent) {
		FlowPanel noGroupPanel = new FlowPanel();
		noGroupPanel.addStyleName("noGroupPanel");
		SimplePanel groupImgHolder = new SimplePanel();
		groupImgHolder.addStyleName("groupImgHolder");
		NoDragImage groupImg = new NoDragImage(
				SharedResources.INSTANCE.groups(), 48);
		groupImgHolder.add(groupImg);
		noGroupPanel.add(groupImgHolder);
		Label noGroupsLbl = new Label(localization.getMenu("NoGroups"));
		noGroupsLbl.setStyleName("noGroupsLbl");
		Label noGroupsHelpLbl = new Label(localization.getMenu("NoGroupShareTxt"));
		noGroupsHelpLbl.setStyleName("noGroupsHelpLbl");
		noGroupPanel.add(noGroupsLbl);
		noGroupPanel.add(noGroupsHelpLbl);
		dialogContent.add(noGroupPanel);
	}

	private void buildGroupPanel(FlowPanel dialogContent) {
		Label selGroupLbl = new Label(localization.getMenu("shareGroupHelpText"));
		selGroupLbl.setStyleName("selGrLbl");
		dialogContent.add(selGroupLbl);
		FlowPanel groupPanel = new FlowPanel();
		groupPanel.addStyleName("groupPanel");
		scrollPanel = new ScrollPanel();
		groupPanel.add(scrollPanel);
		dialogContent.add(groupPanel);
	}

	protected void updateChangedGroupList(GroupIdentifier groupID, Boolean shared) {
		if (changedGroups.containsKey(groupID)) {
			changedGroups.remove(groupID);
		} else {
			changedGroups.put(groupID, shared);
		}
	}

	private void buildShareByLinkPanel(FlowPanel dialogContent,
			String shareURL) {
		FlowPanel shareByLinkPanel = new FlowPanel();
		shareByLinkPanel.addStyleName("shareByLink");
		shareSwitch = new ComponentSwitch(isMatShared(material), this::onSwitch);

		FlowPanel switcherPanel = new FlowPanel();
		switcherPanel.addStyleName("switcherPanel");

		NoDragImage linkImg = new NoDragImage(
				SharedResources.INSTANCE.mow_link_black(), 24);
		linkImg.addStyleName("linkImg");
		switcherPanel.add(linkImg);

		FlowPanel textPanel = new FlowPanel();
		textPanel.addStyleName("textPanel");
		linkShareOnOffLbl = new Label(localization.getMenu(
				isShareLinkOn() ? "linkShareOn" : "linkShareOff"));
		linkShareOnOffLbl.setStyleName("linkShareOnOff");
		linkShareHelpLbl = new Label(localization.getMenu(getLinkShareHelpLabelTextKey()));
		linkShareHelpLbl.setStyleName("linkShareHelp");
		textPanel.add(linkShareOnOffLbl);
		textPanel.add(linkShareHelpLbl);
		switcherPanel.add(textPanel);
		switcherPanel.add(shareSwitch);

		shareByLinkPanel.add(switcherPanel);
		buildLinkPanel(shareByLinkPanel, shareURL);
		dialogContent.add(shareByLinkPanel);
	}

	private void buildSharingAvailableInfo(FlowPanel dialogContent) {
		Label sharingAvailableInfo = new Label(localization.getMenu("SharingAvailableMow"));
		sharingAvailableInfo.setStyleName("shareLinkAvailableInfo");
		dialogContent.add(sharingAvailableInfo);
	}

	private static boolean isMatShared(Material mat) {
		if (mat != null) {
			return "S".equals(mat.getVisibility());
		}
		return false;
	}

	private void buildLinkPanel(FlowPanel shareByLinkPanel, String shareURL) {
		linkPanel = new FlowPanel();
		linkPanel.setStyleName("linkPanel");
		linkBox = new ComponentLinkBox(true, shareURL, "linkBox");
		// build and add copy button
		copyBtn = new StandardButton(localization.getMenu("Copy"));
		copyBtn.setStyleName("copyButton");
		copyBtn.addFastClickHandler(this);
		linkPanel.add(linkBox);
		linkPanel.add(copyBtn);
		shareByLinkPanel.add(linkPanel);
		linkPanel.setVisible(isShareLinkOn());
	}

	/**
	 * update switch dependent UI
	 * 
	 * @param isSwitchOn
	 *            true if switch is on
	 */
	public void onSwitch(boolean isSwitchOn) {
		linkShareOnOffLbl
				.setText(localization.getMenu(
						isShareLinkOn() ? "linkShareOn" : "linkShareOff"));
		linkShareHelpLbl.setText(localization.getMenu(getLinkShareHelpLabelTextKey()));
		linkPanel.setVisible(isSwitchOn);
		if (isSwitchOn) {
			Scheduler.get().scheduleDeferred(() -> {
				getLinkBox().selectAll();
				getLinkBox().setFocus(true);
			});
		}
		centerAndResize(((AppW) app).getAppletFrame().getKeyboardHeight());
	}

	private String getLinkShareHelpLabelTextKey() {
		if (isShareLinkOn()) {
			VendorSettings settings = ((AppW) app).getVendorSettings();
			return settings.getMenuLocalizationKey("SharedLinkHelpTxt");
		}
		return "NotSharedLinkHelpTxt";
	}

	@Override
	public void onClick(Widget source) {
		if (source == copyBtn) {
			linkBox.setFocused(false);
			app.getCopyPaste().copyTextToSystemClipboard(linkBox.getText());
			linkBox.focus();
			ToolTipManagerW.sharedInstance()
			    .showBottomMessage(((AppW) app).getLocalization()
			    .getMenu("linkCopyClipboard"), (AppW) app);
		}
	}

	/**
	 * @param materialCallbackI
	 *            callback for visibility change
	 */
	public void setCallback(MaterialCallbackI materialCallbackI) {
		this.callback = materialCallbackI;
	}

	/**
	 * @param groupCallback
	 *            callback for share with group
	 */
	protected void shareWithGroups(AsyncOperation<Boolean> groupCallback) {
		for (Map.Entry<GroupIdentifier, Boolean> group : changedGroups.entrySet()) {
			app.getLoginOperation().getGeoGebraTubeAPI().setShared(material,
					group.getKey(), group.getValue(), groupCallback);
		}
	}

	protected void getGroupsSharedWith() {
		final AsyncOperation<List<GroupIdentifier>> partial =
				new AsyncOperation<List<GroupIdentifier>>() {
					private int counter = 2;
					private List<GroupIdentifier> all = new ArrayList<>();

					@Override
					public void callback(List<GroupIdentifier> obj) {
						counter--;
						if (obj == null) {
							all = null;
						} else if (all != null) {
							all.addAll(obj);
						}
						if (counter == 0) {
							updateOnSharedGroups(all);
						}
					}
				};
		BackendAPI api = app.getLoginOperation().getGeoGebraTubeAPI();
		api.getGroups(material.getSharingKeyOrId(), GroupIdentifier.GroupCategory.CLASS, partial);
		api.getGroups(material.getSharingKeyOrId(), GroupIdentifier.GroupCategory.COURSE, partial);
	}

	/**
	 * @param success
	 *            shared with group successful or not
	 */
	protected void onGroupShareChanged(boolean success) {
		ToolTipManagerW.sharedInstance().showBottomMessage(
				app.getLocalization()
						.getMenu(success ? "GroupShareOk"
								: "GroupShareFail"), (AppW) app);
		if (success && callback != null) {
			callback.onLoaded(Collections.singletonList(material), null);
		}
	}
}
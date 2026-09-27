package com.hmdm.launcher.ui;

import android.app.Activity;

public class FolderAppListAdapter extends BaseAppListAdapter {
    private final String folderId;

    public FolderAppListAdapter(Activity parentActivity,
                                OnAppChooseListener appChooseListener,
                                SwitchAdapterListener switchAdapterListener,
                                String folderId) {
        super(parentActivity, appChooseListener, switchAdapterListener);
        this.folderId = folderId;
        updateShortcuts(parentActivity);
    }

    @Override
    public void updateShortcuts(Activity parentActivity) {
        items = AppShortcutManager.getInstance().getInstalledAppsInFolder(parentActivity, folderId);
        initShortcuts();
    }
}

package com.hmdm.launcher.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.json.Application;
import com.hmdm.launcher.json.LauncherFolder;
import com.hmdm.launcher.json.ServerConfig;
import com.hmdm.launcher.util.AppInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppShortcutManager {

    private static AppShortcutManager instance;

    public static AppShortcutManager getInstance() {
        if (instance == null) {
            instance = new AppShortcutManager();
        }
        return instance;
    }

    public int getInstalledAppCount(Context context, boolean bottom) {
        return getInstalledApps(context, bottom).size();
    }

    public List<AppInfo> getInstalledApps(Context context, boolean bottom) {
        return getInstalledAppsInternal(context, bottom, null, true);
    }

    public List<AppInfo> getInstalledAppsInFolder(Context context, String folderId) {
        String normalizedFolderId = LauncherFolderRules.normalizeId(folderId);
        if (normalizedFolderId == null) {
            return new ArrayList<>();
        }
        return getInstalledAppsInternal(context, null, normalizedFolderId, false);
    }

    public Map<Integer, AppInfo> getShortcuts(Context context, boolean bottom) {
        Map<Integer, AppInfo> result = new HashMap<>();
        for (AppInfo item : getInstalledApps(context, bottom)) {
            addShortcut(result, item);
            if (item.type == AppInfo.TYPE_FOLDER && item.folderId != null) {
                for (AppInfo child : getInstalledAppsInFolder(context, item.folderId)) {
                    addShortcut(result, child);
                }
            }
        }
        return result;
    }

    private void addShortcut(Map<Integer, AppInfo> result, AppInfo item) {
        if (item.keyCode != null) {
            result.put(item.keyCode, item);
        }
    }

    private List<AppInfo> getInstalledAppsInternal(Context context, Boolean bottom, String folderId, boolean includeFolders) {
        Map<String, Application> requiredPackages = new HashMap<>();
        Map<String, Application> requiredLinks = new HashMap<>();
        getConfiguredApps(context, bottom, folderId, requiredPackages, requiredLinks);

        List<AppInfo> appInfos = new ArrayList<>();
        List<ApplicationInfo> packs = context.getPackageManager().getInstalledApplications(0);
        if (packs == null) {
            packs = new ArrayList<>();
        }

        PackageManager pm = context.getPackageManager();
        for (ApplicationInfo p : packs) {
            if (pm.getLaunchIntentForPackage(p.packageName) != null &&
                    requiredPackages.containsKey(p.packageName)) {
                Application app = requiredPackages.get(p.packageName);
                AppInfo newInfo = new AppInfo();
                newInfo.type = AppInfo.TYPE_APP;
                newInfo.keyCode = app.getKeyCode();
                newInfo.name = app.getIconText() != null ? app.getIconText() : p.loadLabel(pm).toString();
                newInfo.packageName = p.packageName;
                newInfo.iconUrl = app.getIcon();
                newInfo.screenOrder = app.getScreenOrder();
                newInfo.longTap = app.isLongTap() ? 1 : 0;

                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setPackage(p.packageName);
                List<ResolveInfo> shortcuts = pm.queryIntentActivities(intent, 0);
                if (shortcuts.size() > 1) {
                    for (int j = 0; j < shortcuts.size(); j++) {
                        AppInfo shortcutInfo = new AppInfo(newInfo);
                        shortcutInfo.multiIcon = true;
                        shortcutInfo.iconIndex = j;
                        shortcutInfo.name = shortcuts.get(j).loadLabel(pm).toString();
                        appInfos.add(shortcutInfo);
                    }
                } else {
                    appInfos.add(newInfo);
                }
            }
        }

        for (Map.Entry<String, Application> entry : requiredLinks.entrySet()) {
            Application application = entry.getValue();
            AppInfo newInfo = new AppInfo();
            newInfo.type = application.getType().equals(Application.TYPE_INTENT) ? AppInfo.TYPE_INTENT : AppInfo.TYPE_WEB;
            newInfo.keyCode = application.getKeyCode();
            newInfo.name = application.getIconText();
            newInfo.url = application.getUrl();
            newInfo.iconUrl = application.getIcon();
            newInfo.screenOrder = application.getScreenOrder();
            newInfo.useKiosk = application.isUseKiosk() ? 1 : 0;
            newInfo.intent = application.getIntent();
            appInfos.add(newInfo);
        }

        if (includeFolders && folderId == null && bottom != null) {
            addFolders(context, bottom, appInfos);
        }

        Collections.sort(appInfos, new AppInfosComparator());
        return appInfos;
    }

    private void addFolders(Context context, boolean bottom, List<AppInfo> appInfos) {
        ServerConfig config = SettingsHelper.getInstance(context).getConfig();
        if (config == null || config.getLauncherFolders() == null) {
            return;
        }

        for (LauncherFolder folder : LauncherFolderRules.foldersForPosition(config.getLauncherFolders(), bottom)) {
            String folderId = LauncherFolderRules.normalizeId(folder.getId());

            if (getInstalledAppsInFolder(context, folderId).isEmpty()) {
                continue;
            }

            AppInfo folderInfo = new AppInfo();
            folderInfo.type = AppInfo.TYPE_FOLDER;
            folderInfo.folderId = folderId;
            folderInfo.name = folder.getName() != null && !folder.getName().trim().isEmpty()
                    ? folder.getName() : folderId;
            folderInfo.iconUrl = folder.getIcon();
            folderInfo.screenOrder = folder.getScreenOrder();
            appInfos.add(folderInfo);
        }
    }

    private void getConfiguredApps(Context context, Boolean bottom, String folderId,
                                   Map<String, Application> requiredPackages,
                                   Map<String, Application> requiredLinks) {
        ServerConfig config = SettingsHelper.getInstance(context).getConfig();
        if (config == null || config.getApplications() == null) {
            return;
        }

        Set<String> validFolderIds = LauncherFolderRules.validFolderIds(config.getLauncherFolders());
        for (Application application : config.getApplications()) {
            if (application == null || !application.isShowIcon() || application.isRemove()) {
                continue;
            }

            if (folderId != null) {
                if (!LauncherFolderRules.belongsToFolder(application, folderId)) {
                    continue;
                }
            } else if (!LauncherFolderRules.belongsToRoot(application, bottom, validFolderIds)) {
                continue;
            }

            if (application.getType() == null || application.getType().equals(Application.TYPE_APP)) {
                requiredPackages.put(application.getPkg(), application);
            } else if (application.getType().equals(Application.TYPE_WEB)) {
                requiredLinks.put(application.getUrl(), application);
            } else if (application.getType().equals(Application.TYPE_INTENT)) {
                requiredLinks.put(application.getIntent(), application);
            }
        }
    }

    public class AppInfosComparator implements Comparator<AppInfo> {
        @Override
        public int compare(AppInfo o1, AppInfo o2) {
            if (o1.screenOrder == null) {
                if (o2.screenOrder == null) {
                    return 0;
                }
                return 1;
            }
            if (o2.screenOrder == null) {
                return -1;
            }
            return Integer.compare(o1.screenOrder, o2.screenOrder);
        }
    }
}

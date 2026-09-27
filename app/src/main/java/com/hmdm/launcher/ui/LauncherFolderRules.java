package com.hmdm.launcher.ui;

import com.hmdm.launcher.json.Application;
import com.hmdm.launcher.json.LauncherFolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure-Java rules for mapping server launcher configuration to root/folder placement.
 * Kept Android-free so the contract can be covered by fast unit tests.
 */
final class LauncherFolderRules {

    private LauncherFolderRules() {
    }

    static String normalizeId(String folderId) {
        if (folderId == null) {
            return null;
        }
        String normalized = folderId.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    static Set<String> validFolderIds(List<LauncherFolder> folders) {
        Set<String> result = new HashSet<>();
        if (folders == null) {
            return result;
        }

        for (LauncherFolder folder : folders) {
            if (folder == null) {
                continue;
            }
            String id = normalizeId(folder.getId());
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    static boolean belongsToFolder(Application application, String folderId) {
        if (application == null) {
            return false;
        }
        String normalizedFolderId = normalizeId(folderId);
        return normalizedFolderId != null &&
                normalizedFolderId.equals(normalizeId(application.getFolderId()));
    }

    static boolean belongsToRoot(Application application, Boolean bottom, Set<String> validFolderIds) {
        if (application == null) {
            return false;
        }

        String applicationFolderId = normalizeId(application.getFolderId());
        if (applicationFolderId != null && validFolderIds != null && validFolderIds.contains(applicationFolderId)) {
            return false;
        }

        return bottom == null || bottom == application.isBottom();
    }

    /**
     * Returns folder definitions for one launcher area. Duplicate IDs are resolved globally:
     * the first valid declaration wins, even when a later duplicate targets another area.
     */
    static List<LauncherFolder> foldersForPosition(List<LauncherFolder> folders, boolean bottom) {
        List<LauncherFolder> result = new ArrayList<>();
        if (folders == null) {
            return result;
        }

        Set<String> seenIds = new HashSet<>();
        for (LauncherFolder folder : folders) {
            if (folder == null) {
                continue;
            }

            String id = normalizeId(folder.getId());
            if (id == null || !seenIds.add(id)) {
                continue;
            }
            if (folder.isBottom() == bottom) {
                result.add(folder);
            }
        }
        return result;
    }
}

package com.hmdm.launcher.ui;

import com.hmdm.launcher.json.Application;
import com.hmdm.launcher.json.LauncherFolder;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LauncherFolderRulesTest {

    @Test
    public void normalizesFolderIds() {
        assertNull(LauncherFolderRules.normalizeId(null));
        assertNull(LauncherFolderRules.normalizeId("   "));
        assertEquals("school", LauncherFolderRules.normalizeId("  school "));
    }

    @Test
    public void knownFolderMovesApplicationOffRoot() {
        LauncherFolder folder = folder("school", false);
        Application application = application("school", false);
        Set<String> validIds = LauncherFolderRules.validFolderIds(Collections.singletonList(folder));

        assertFalse(LauncherFolderRules.belongsToRoot(application, false, validIds));
        assertTrue(LauncherFolderRules.belongsToFolder(application, "school"));
    }

    @Test
    public void unknownFolderFallsBackToLegacyRootPlacement() {
        Application application = application("server-typo", false);
        Set<String> validIds = LauncherFolderRules.validFolderIds(
                Collections.singletonList(folder("school", false)));

        assertTrue(LauncherFolderRules.belongsToRoot(application, false, validIds));
        assertFalse(LauncherFolderRules.belongsToRoot(application, true, validIds));
    }

    @Test
    public void blankFolderIdBehavesLikeLegacyApplication() {
        Application application = application("   ", true);
        Set<String> validIds = LauncherFolderRules.validFolderIds(
                Collections.singletonList(folder("school", false)));

        assertTrue(LauncherFolderRules.belongsToRoot(application, true, validIds));
        assertFalse(LauncherFolderRules.belongsToRoot(application, false, validIds));
    }

    @Test
    public void duplicateFolderIdUsesFirstDeclarationGlobally() {
        LauncherFolder first = folder("shared", false);
        LauncherFolder duplicate = folder("shared", true);
        List<LauncherFolder> folders = Arrays.asList(first, duplicate);

        assertEquals(1, LauncherFolderRules.foldersForPosition(folders, false).size());
        assertTrue(LauncherFolderRules.foldersForPosition(folders, true).isEmpty());
    }

    @Test
    public void ignoresBlankAndNullFolderDefinitions() {
        LauncherFolder blank = folder(" ", false);
        List<LauncherFolder> folders = Arrays.asList(null, blank, folder("valid", false));

        Set<String> validIds = LauncherFolderRules.validFolderIds(folders);
        assertEquals(1, validIds.size());
        assertTrue(validIds.contains("valid"));
        assertEquals(1, LauncherFolderRules.foldersForPosition(folders, false).size());
    }

    private static LauncherFolder folder(String id, boolean bottom) {
        LauncherFolder folder = new LauncherFolder();
        folder.setId(id);
        folder.setBottom(bottom);
        return folder;
    }

    private static Application application(String folderId, boolean bottom) {
        Application application = new Application();
        application.setFolderId(folderId);
        application.setBottom(bottom);
        return application;
    }
}

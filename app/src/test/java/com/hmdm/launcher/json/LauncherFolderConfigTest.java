package com.hmdm.launcher.json;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LauncherFolderConfigTest {

    @Test
    public void parsesLauncherFoldersAndApplicationMembership() throws Exception {
        String json = "{"
                + "\"launcherFolders\":[{"
                + "\"id\":\"school\","
                + "\"name\":\"School\","
                + "\"icon\":\"https://example.test/folder.png\","
                + "\"screenOrder\":3,"
                + "\"bottom\":true"
                + "}],"
                + "\"applications\":[{"
                + "\"type\":\"app\","
                + "\"pkg\":\"com.example.math\","
                + "\"showIcon\":true,"
                + "\"folderId\":\"school\""
                + "}]"
                + "}";

        ServerConfig config = new ObjectMapper().readValue(json, ServerConfig.class);

        assertNotNull(config.getLauncherFolders());
        assertEquals(1, config.getLauncherFolders().size());
        LauncherFolder folder = config.getLauncherFolders().get(0);
        assertEquals("school", folder.getId());
        assertEquals("School", folder.getName());
        assertEquals(Integer.valueOf(3), folder.getScreenOrder());
        assertTrue(folder.isBottom());

        assertEquals(1, config.getApplications().size());
        assertEquals("school", config.getApplications().get(0).getFolderId());
    }
}

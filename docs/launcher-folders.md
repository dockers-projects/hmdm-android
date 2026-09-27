# Server-configured launcher folders

The Android launcher accepts optional folder metadata in the existing server configuration JSON.

## What changes in the launcher UI

Without folder configuration, the launcher keeps the existing flat application grid.

With folders configured:

1. Applications assigned to a known folder are removed from the root grid.
2. The folder itself appears as a single launcher tile.
3. The tile uses the configured `name` and optional remote `icon`.
4. If no custom icon is supplied, the built-in folder icon is used.
5. Tapping the folder opens a dialog containing a grid of the folder applications.
6. Tapping an application closes the folder dialog and launches the selected application.
7. A folder with `bottom: true` is rendered as one tile in the bottom launcher row.
8. Empty folders are not rendered.

The feature does not add local drag-and-drop or device-side editing. Folder structure is controlled by server configuration.

```text
Before:
[ Math ] [ Browser ] [ Video ] [ Mail ]

After:
[ School ] [ Mail ]
    |
    +--> [ Math ] [ Browser ] [ Video ]
```

## Server JSON contract

```json
{
  "launcherFolders": [
    {
      "id": "school",
      "name": "School",
      "icon": "https://mdm.example/icon/school.png",
      "screenOrder": 10,
      "bottom": false
    }
  ],
  "applications": [
    {
      "type": "app",
      "pkg": "com.example.math",
      "showIcon": true,
      "screenOrder": 1,
      "folderId": "school"
    }
  ]
}
```

### Folder fields

| Field | Required | Meaning |
| --- | --- | --- |
| `id` | yes | Stable server-side folder identifier. Blank IDs are ignored. |
| `name` | no | Displayed folder title. If blank, `id` is used. |
| `icon` | no | Custom icon URL. The normal signed image-loading path is reused. |
| `screenOrder` | no | Folder position among root launcher entries. |
| `bottom` | no | If `true`, render the folder in the bottom launcher row. Default is the main grid. |

### Application field

`folderId` is optional on existing application entries and works for `app`, `web`, and `intent` entries.

When an application belongs to a valid folder, its own `bottom` field no longer determines root placement because the application is not displayed directly on the root. The folder's `bottom` field controls where the folder tile is rendered.

The application's existing `screenOrder` controls its position inside the folder.

## How to configure a folder

1. Add one entry to `launcherFolders` with a unique, stable `id`.
2. Set the folder `name`, optional `icon`, `screenOrder`, and `bottom`.
3. Add the same `folderId` to each application that must appear inside the folder.
4. Deliver the updated configuration through the normal Headwind MDM configuration-sync endpoint.
5. Refresh the device configuration or wait for the normal push/configuration refresh.
6. Verify that the root contains one folder tile and that tapping it shows the expected applications.

Example with two folders:

```json
{
  "launcherFolders": [
    {
      "id": "school",
      "name": "School",
      "screenOrder": 10
    },
    {
      "id": "communication",
      "name": "Communication",
      "screenOrder": 1,
      "bottom": true
    }
  ],
  "applications": [
    {
      "type": "app",
      "pkg": "com.example.math",
      "showIcon": true,
      "screenOrder": 1,
      "folderId": "school"
    },
    {
      "type": "web",
      "url": "https://school.example/",
      "iconText": "School portal",
      "showIcon": true,
      "screenOrder": 2,
      "folderId": "school"
    },
    {
      "type": "app",
      "pkg": "com.example.chat",
      "showIcon": true,
      "screenOrder": 1,
      "folderId": "communication"
    }
  ]
}
```

## Compatibility and safety rules

- When `launcherFolders` is absent, launcher behavior is unchanged.
- An application with no `folderId` stays in its legacy root/bottom position.
- An application referencing an unknown folder ID stays in its legacy root/bottom position. A server typo therefore does not hide the application.
- Blank folder IDs are ignored.
- Empty folders are not rendered.
- Duplicate folder IDs are deterministic: the first valid declaration wins globally.
- Folder contents use the application's existing `screenOrder`.
- Application key-code shortcuts remain active even when the application is inside a folder.
- Nested folders are intentionally not supported.

## Automated validation

The repository CI runs on a GitHub self-hosted Linux runner and builds inside `ci/android/Dockerfile`.

The quality gates are:

1. `testOpensourceDebugUnitTest`
   - JSON deserialization of `launcherFolders` and `folderId`
   - ID normalization
   - known-folder placement
   - unknown-folder fallback
   - blank-folder fallback
   - duplicate-ID first-wins behavior
2. `lintOpensourceDebug`
3. `assembleOpensourceDebug`
4. Verify that `app-opensource-debug.apk` exists and is non-empty.

The workflow uploads unit-test reports, lint reports, and the debug APK as GitHub Actions artifacts.

## Stock Headwind MDM server status

This repository implements the Android/device side of the contract.

The stock `h-mdm/hmdm-server` currently does not persist or emit `launcherFolders` and `folderId`, and its web panel has no folder editor. A companion server change is therefore required before administrators can configure folders through the stock Headwind MDM web interface.

Until that server-side change exists, the Android feature can be exercised by a compatible/custom server that includes these fields in the normal configuration JSON.

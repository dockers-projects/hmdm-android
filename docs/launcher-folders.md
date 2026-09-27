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

## Migration from a launcher version without folders

Folder support is designed for a mixed-version fleet, so the launcher APK and the server configuration do not need to change at exactly the same moment.

### Compatibility matrix

| Device launcher | Server sends folder fields | Result |
| --- | --- | --- |
| Old launcher, no folder support | No | Existing flat launcher |
| Old launcher, no folder support | Yes | Existing flat launcher; folder fields are ignored |
| New folder-capable launcher | No | Existing flat launcher |
| New folder-capable launcher | Yes | Configured folders are rendered |

The pre-folder launcher already declares `@JsonIgnoreProperties(ignoreUnknown = true)` on both `ServerConfig` and `Application`. Therefore an older launcher safely ignores the new top-level `launcherFolders` field and the new per-application `folderId` field instead of rejecting the configuration.

This means a server may start emitting the new fields while older devices are still present. Older devices continue to use each application's existing `bottom` and `screenOrder` values and show a flat launcher. Folder-capable devices use the folder placement rules.

### Production release prerequisites

For an in-place update of an already installed Headwind MDM launcher:

- keep the package name `com.hmdm.launcher`;
- sign the new APK with the same signing certificate/key as the installed launcher;
- increment `versionCode` above the deployed build;
- use a new `versionName` so operators can distinguish the folder-capable build;
- build and distribute a release APK, not the CI debug APK.

For example, when upgrading a deployed `versionCode 15390 / versionName 6.39`, a first folder-enabled production build could use `versionCode 15391 / versionName 6.39.1`.

The exact version values are a release decision; the important Android requirement is that the update uses the same package/signing identity and a higher `versionCode`.

### Recommended rollout

1. Deploy the server-side schema/UI support while leaving folder assignment disabled for existing configurations.
2. Publish the folder-capable launcher APK as an update of `com.hmdm.launcher`.
3. Upgrade a small canary group first.
4. Verify that the upgraded devices still display the legacy flat launcher while no folders are configured.
5. Enable one test folder only for the canary configuration.
6. Verify folder rendering, child ordering, app launch, web/intent entries, bottom-row placement and key-code shortcuts.
7. Expand the APK rollout to the remaining devices.
8. Enable folder assignments progressively after the required devices have upgraded.

Because older launchers ignore the new fields, steps 7 and 8 may overlap when necessary. Keeping them separate makes troubleshooting and rollback easier.

### Mixed-version behavior

During migration, devices may intentionally render the same configuration differently:

- old launcher: applications remain visible in the flat root/bottom layout;
- new launcher: applications with a valid `folderId` move into the corresponding folder;
- applications with an unknown `folderId` remain visible in their legacy root/bottom position on the new launcher as a fail-safe.

When a folder's `bottom` value differs from a child's existing `bottom` value, old launchers use the application's `bottom`, while folder-capable launchers use the folder's `bottom`. This is expected during a mixed-version rollout.

### Rollback

The preferred rollback is configuration-only and does not require downgrading the APK:

1. Remove `folderId` assignments from applications.
2. Remove or stop emitting `launcherFolders`.
3. Push/refresh the configuration.

A folder-capable launcher with no folder configuration immediately returns to the legacy flat layout.

Avoid relying on APK downgrade as the primary rollback. Android normally rejects installing an APK with a lower `versionCode` over a newer installed version. If a binary rollback is ever required, build the previous behavior again with the same package/signing key and a `versionCode` higher than the currently installed build.

### Migration acceptance checks

Before declaring the migration complete, verify all of the following:

- an old launcher accepts a configuration containing `launcherFolders` and `folderId` without JSON/configuration failure;
- an old launcher still shows applications in the flat layout;
- a new launcher with no folder configuration behaves exactly like the legacy launcher;
- a new launcher with folder configuration renders the expected folder tiles and contents;
- unknown folder IDs do not hide applications;
- empty folders are hidden;
- bottom-row folders render in the correct area;
- removing folder configuration restores the flat layout without reinstalling the APK.

## Automated validation

The repository CI runs on GitHub-hosted Ubuntu (`ubuntu-latest`) and builds inside `ci/android/Dockerfile`.

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

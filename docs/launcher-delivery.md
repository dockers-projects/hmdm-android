# Launcher delivery and Docker packaging

This document describes how the custom `com.hmdm.launcher` APK is built, how it gets into Headwind MDM, and how to roll it out without replacing the existing database or configuration.

## Short answer

The stock `headwindmdm/hmdm` Docker image does **not** compile or embed the Android launcher APK.

The stock image uses `CLIENT_VERSION` during initialization, generates a URL such as:

```text
https://h-mdm.com/files/hmdm-<CLIENT_VERSION>-os.apk
```

and downloads that APK into the persistent Headwind work volume:

```text
/usr/local/tomcat/work/files/
```

This repository now provides a custom delivery image. Its production workflow:

```text
Android sources
    |
    v
assembleOpensourceRelease
    |
    v
zipalign + sign with existing production key
    |
    v
verify package / version / certificate
    |
    v
signed com.hmdm.launcher APK
    |
    v
derived Headwind server image
    |
    +--> /opt/hmdm/bundled-launcher/launcher.apk
```

The APK is therefore physically present in the resulting custom server image. The Android APK is compiled before the server-image build and then copied into the server image.

## Production launcher version

The folder-capable release is:

```text
package:     com.hmdm.launcher
versionName: 6.39.1
versionCode: 15391
```

Android requires a higher `versionCode` and the same signing certificate for an in-place update.

## Signing requirement

A modified APK cannot update an already installed `com.hmdm.launcher` unless it is signed with the same certificate as the installed APK.

The release workflow requires these GitHub repository secrets:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

`ANDROID_KEYSTORE_BASE64` is the existing release keystore encoded as base64. Do not use a new signing key for an in-place migration.

The workflow refuses to publish a production image when these secrets are missing.

To compare an old APK and the newly signed APK locally:

```bash
bash ci/verify-upgrade-apks.sh old-hmdm.apk new-hmdm.apk
```

The check verifies:

- both packages are `com.hmdm.launcher`;
- both APKs use the same signing certificate;
- the new APK has a higher `versionCode`.

## Build and publish the bundled server image

Open GitHub Actions and run:

```text
Package launcher server image
```

The workflow asks for:

- `base_image` — the **exact Headwind server image currently deployed**;
- `image_tag` — optional; defaults to launcher `versionName`.

Example:

```text
base_image = headwindmdm/hmdm:0.1.9
image_tag  = 6.39.1
```

Do not use the example base image blindly. For an existing installation, use the exact server image/tag that is currently running so the launcher delivery change does not also upgrade or downgrade the Headwind server.

A successful workflow publishes:

```text
ghcr.io/dockers-projects/hmdm-server-launcher:<tag>
```

and uploads the signed APK as a GitHub Actions artifact.

The resulting server image contains:

```text
/opt/hmdm/bundled-launcher/launcher.apk
```

At container startup, the wrapper copies it to:

```text
/usr/local/tomcat/work/files/hmdm-6.39.1-os.apk
```

before executing the original Headwind `/docker-entrypoint.sh`.

On a fresh installation, the wrapper sets the stock `CLIENT_VERSION` to the bundled APK version so bootstrap metadata and the embedded APK stay aligned. On an existing installation (detected by the persistent `work/init.sql`), it preserves the existing `CLIENT_VERSION` and only seeds the new APK file; database/configuration selection remains unchanged.

## Replacing the Docker image on an existing installation

Replacing only the server image is intentionally non-destructive.

Keep the existing PostgreSQL database and all existing mounts, especially:

```text
/usr/local/tomcat/work
/usr/local/tomcat/webapps
/usr/local/tomcat/conf/Catalina/localhost
```

Change only the image reference, for example:

```yaml
services:
  hmdm:
    image: ghcr.io/dockers-projects/hmdm-server-launcher:6.39.1
    # keep the existing ports, environment and volumes unchanged
```

Then:

```bash
docker compose pull hmdm
docker compose up -d hmdm
docker compose logs -f hmdm
```

Do **not** enable `FORCE_RECONFIGURE=true` just to deploy the launcher. The upstream Docker documentation warns that forced reconfiguration can reset generated configuration/application settings.

### What image replacement does on an existing installation

It does:

- start the same Headwind server base image;
- preserve the existing database and volumes;
- seed the new APK file into `work/files`;
- leave the old APK and old application-version rows untouched.

It does **not**:

- automatically change the launcher version selected in existing configurations;
- automatically install the new APK on devices;
- add folder configuration to the Headwind web panel;
- modify the Headwind database.

This behavior makes replacing the image safe and reversible.

## Register the new launcher version in the existing Headwind UI

After the new Docker image is running, the APK is available on the server as:

```text
https://<mdm-domain>/files/hmdm-6.39.1-os.apk
```

There are two practical ways to register it.

### Option A — upload the signed APK

In the Headwind web panel:

```text
Applications
  → Headwind MDM (com.hmdm.launcher)
  → Versions
  → Add
  → upload hmdm-6.39.1-os.apk
  → Save
```

This is the simplest option because Headwind reads package/version metadata directly from the APK.

### Option B — add the version by URL

The same Add Version dialog also has a URL field. Point it to:

```text
https://<mdm-domain>/files/hmdm-6.39.1-os.apk
```

Use version `6.39.1` and verify the version is shown as the latest application version.

## Roll out the launcher to devices

Do not upgrade every device first.

Recommended flow:

1. Create or copy a canary configuration.
2. Assign one test device to it.
3. In the configuration, find `Headwind MDM / com.hmdm.launcher`.
4. Select/upgrade it to version `6.39.1`.
5. Save the configuration and trigger/await configuration synchronization.
6. Verify the device reports launcher version `6.39.1`.
7. Verify the launcher still works with no folder configuration.
8. Only then expand the launcher update to more devices.

If the signing certificate is wrong, Android rejects the update. Do not roll out until the certificate check passes.

## Folder configuration: what can be done today

There are two separate features:

1. **Delivering the folder-capable launcher APK** — supported by the stock Headwind application/version UI.
2. **Defining folders** — not yet supported by the stock Headwind server UI.

The Android launcher understands:

```json
{
  "launcherFolders": [
    {
      "id": "school",
      "name": "School",
      "screenOrder": 10,
      "bottom": false
    }
  ],
  "applications": [
    {
      "type": "app",
      "pkg": "com.example.math",
      "showIcon": true,
      "folderId": "school"
    }
  ]
}
```

However, the current stock `h-mdm/hmdm-server` has no persistence fields or web editor for `launcherFolders` / `folderId`.

Therefore you cannot currently create these folders by clicking in the standard Headwind UI.

For development/testing, the sync response must be produced by a custom server implementation (or a controlled test/mock sync endpoint) that emits `launcherFolders` and `folderId`.

A companion server implementation is required before folder creation becomes a normal web-panel operation.

## Compatibility / rollback

The new launcher remains compatible with configurations that do not contain folder fields: it renders the existing flat launcher.

Older launchers already ignore unknown JSON properties, so they ignore `launcherFolders` and `folderId` and continue to render the flat launcher.

For server-image rollback:

```yaml
image: <previous exact Headwind server image>
```

Keep the same volumes/database and run `docker compose up -d hmdm`.

For launcher rollback, prefer configuration rollback: keep or restore the previous launcher application version in the configuration. Android cannot normally install a lower `versionCode` over a newer version, so binary downgrade is not the preferred rollback mechanism.

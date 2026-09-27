# Headwind MDM: free and open-source MDM launcher

A Powerful Open Source Platform to Manage your Enterprise Android Devices

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.hmdm.launcher/)

## Starting work

Open the project directory in Android Studio (use default settings).

## Debugging on the device

1. Connect the device by USB
2. Click "Run 'App'" icon in Android Studio
3. After successful run, add device owner rights to the app (optional).

    Run in the console
   
    `adb shell`

    Run the command in the adb console
   
    `dpm set-device-owner com.hmdm.launcher/.AdminReceiver`

## Building the APK

Build the APK after you successfully build the app.

1. Setup the keys for signing the app
2. Select Build - Generate signed Bundle / APK
3. Select the place you'd like to save APK

## Building the library

1. Select the 'lib' item in the project tree
2. Select Build - Make Module 'lib'
3. Find the library in the 'lib/build/outputs/aar' directory

## Building the project in the command line

1. Install the Gradle plugin v5.1.1 (Linux only)
2. Install Android Studio or download the standalone Android SDK
3. Create the file local.properties and store the SDK location in this file:

sdk.dir=/path/to/sdk

4. Run the command

gradlew build

5. Find the resulting APK in the app/build/outputs/apk/release/ directory.



## Continuous integration

Pull requests to `master`, pushes to `master`, and manual workflow dispatches run the Android quality gates on a GitHub runner labeled:

```yaml
runs-on: ubuntu-latest
```

CI uses GitHub-hosted Ubuntu (`ubuntu-latest`). Docker is available on the hosted runner, while Java and Android SDK dependencies remain isolated in the reproducible image defined by `ci/android/Dockerfile`.

The workflow runs:

```text
unit tests -> Android lint -> debug APK build -> APK existence check
```

To reproduce the same checks on a Linux host with Docker:

```bash
docker build -f ci/android/Dockerfile -t hmdm-android-ci ci/android

docker run --rm \
  --user "$(id -u):$(id -g)" \
  -e CI=true \
  -e HOME=/tmp/home \
  -e GRADLE_USER_HOME=/tmp/gradle-home \
  -v "$PWD:/workspace" \
  -w /workspace \
  hmdm-android-ci \
  bash ci/run-checks.sh
```

GitHub Actions uploads the unit-test reports, lint reports, and `app-opensource-debug.apk`.


## Launcher delivery image

The stock Headwind MDM server Docker image does **not** build or embed the Android launcher APK. It normally downloads the launcher APK at runtime using `CLIENT_VERSION`.

This fork provides a separate production workflow, **Package launcher server image**, which:

1. builds `com.hmdm.launcher`;
2. signs it with the existing production signing key;
3. verifies package/version/signature;
4. embeds the signed APK into a derived Headwind server image;
5. publishes the image to `ghcr.io/dockers-projects/hmdm-server-launcher:<tag>`.

The bundled APK is stored in the image at:

```text
/opt/hmdm/bundled-launcher/launcher.apk
```

and is seeded at container startup into the normal Headwind persistent files directory.

For an existing installation, replacing the Docker image **does not automatically switch devices to the new launcher version** and does not modify the database. Register the new APK version in **Applications → Headwind MDM → Versions → Add**, then upgrade/select it in the target configuration.

For the exact build, signing, Docker replacement, canary rollout and rollback procedure, see [docs/launcher-delivery.md](docs/launcher-delivery.md).

Folder creation itself is not yet available in the stock Headwind web UI; the Android client supports the folder JSON contract, but a server-side companion implementation is still required.

For the temporary server-side storage convention, use `examples/launcher-folders.json` and mount it as `/opt/hmdm/custom/launcher-folders.json:ro`. The exact expected server merge behavior is documented in [docs/launcher-delivery.md](docs/launcher-delivery.md). No code in this repository currently reads that file; it is the agreed contract for the future `SyncResponseHook` implementation.

## Launcher folders

The launcher supports server-configured application folders. See [docs/launcher-folders.md](docs/launcher-folders.md) for:

- the UI behavior;
- the server JSON contract;
- root vs bottom-row placement;
- compatibility/fallback rules;
- configuration examples;
- migration from a pre-folder launcher and rollback;
- current stock-server limitations.

## Dummy


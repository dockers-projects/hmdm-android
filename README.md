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
runs-on: [self-hosted, Linux, X64]
```

The host needs a GitHub Runner with the standard `self-hosted`, `Linux`, and `X64` labels plus Docker. The workflow also verifies `uname -s == Linux` and `uname -m == x86_64` before building. Java and Android SDK dependencies are isolated in the reproducible image defined by `ci/android/Dockerfile`.

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

## Launcher folders

The launcher supports server-configured application folders. See [docs/launcher-folders.md](docs/launcher-folders.md) for:

- the UI behavior;
- the server JSON contract;
- root vs bottom-row placement;
- compatibility/fallback rules;
- configuration examples;
- current stock-server limitations.

## Dummy


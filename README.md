# SP25 Android App

This repository contains the source code for the SP25 Android application. It is configured for MVVM architecture using Jetpack Compose and Room.

## Pushing to GitHub

The repository in this environment does not have any remotes configured. To upload your latest commits to GitHub from your local machine, run:

```bash
git remote add origin <your-github-repo-url>
# Or if the remote already exists but points elsewhere
# git remote set-url origin <your-github-repo-url>

git push -u origin work  # replace `work` with your branch name
```

After configuring the remote, you can continue using `git push` to sync subsequent commits.

## Building Locally

Due to the lack of internet access in this environment, Gradle cannot download dependencies here. Open the project in Android Studio on your machine to resolve dependencies, run unit tests, and install the app on a device or emulator.

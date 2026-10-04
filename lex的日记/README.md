# 今日手记

一款使用 Kotlin 和 Jetpack Compose 编写的安卓日记应用。日记、图片、计划和个人资料保存在当前设备，不提供云同步或导出。

## 自动构建 APK（无需在电脑安装安卓开发工具）

### 第一次上传

1. 在 GitHub 注册或登录账号。
2. 在 GitHub 网站创建一个新的空仓库，例如 `today-journal`。不要勾选自动创建 README、`.gitignore` 或 License。
3. 安装并打开 GitHub Desktop，登录 GitHub。
4. 选择 **File > Add local repository...**，选中本项目文件夹 `lex的日记`。
5. 如果 GitHub Desktop 提示该文件夹还不是仓库，先按提示创建仓库；创建后点击 **Publish repository** 发布到 GitHub。若选择已有远端仓库，请确保仓库是空的。
6. 回到 GitHub 网页，在仓库的 **Actions** 标签页确认工作流已启用。每次推送代码后，都会自动构建 APK。

### 下载并安装

1. 打开 GitHub 仓库的 **Actions** 标签页，进入最新的 **Build Android APK** 运行记录。
2. 等待运行结果显示绿色勾号。在页面下方 **Artifacts** 区域点击 `today-journal-apk` 下载 ZIP。
3. 在电脑上解压 ZIP，得到 `app-debug.apk`，再传到安卓手机并打开安装。
4. 如果手机询问是否允许从该来源安装应用，请按系统提示允许本次安装。

也可以在 **Actions > Build Android APK > Run workflow** 手动触发一次构建。每个 APK 产物保留 90 天。

## 常见情况

- 第一次构建会在 GitHub 云端下载 Gradle、Java 和 Android SDK，通常比后续构建更慢；无需在自己的电脑安装这些工具。
- 工作流会缓存 APK 的调试签名，通常可让后续版本覆盖安装并保留日记。GitHub 可能清理长期未使用的缓存；如果手机提示签名不一致，先不要卸载旧应用，因为卸载会删除本地日记。
- 如果 Actions 页面提示工作流需要授权，在该仓库的 **Actions** 设置中启用工作流，然后重新推送或手动运行。
- 这是供个人安装的调试 APK，不是已签名的商店发布版本。
- 日记和个人资料只保存在手机本地。卸载应用或清除应用数据会删除内容，应用不提供云同步和导出备份。
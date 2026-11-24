<!--
SPDX-FileCopyrightText: 2026 judebun
SPDX-License-Identifier: Apache-2.0
-->

# Aperture Lafa

基于 LineageOS Aperture 的真我 GT8 Pro（`lafa`）相机适配，支持普通后置拍照模式下的连续镜头切换与最高 **120× 数字变焦**。

上游项目：<https://github.com/LineageOS/android_packages_apps_Aperture>，本次适配基于 `5c52de5d27f1bc2e6cebb07d6352a299d22cb27d`（`lineage-24.0`）。保留上游源码、历史及许可证。

## 已实现

- 通过独立主摄、超广角与长焦路由普通后置拍照，改善逻辑相机切换到长焦时的预览问题。
- 保持各镜头间的全局倍率换算，支持放大、缩回、捏合缩放及缩放滑条。
- 在设备主动启用且厂商声明支持时，为最长焦镜头开放扩展倍率，最高全局倍率可设为 120×。
- 使用实际 Camera2 `CONTROL_ZOOM_RATIO` 请求和厂商扩展标志，不只修改界面显示。
- 对镜头切换中的过期绑定和取消请求进行处理；未配置或不支持扩展倍率的设备保留原有范围。

120× 是数字变焦。此项目没有移植原厂相机的超分辨率等专有算法，不承诺与原厂相机相同的画质。本分支不包含尚未完成的 50MP 实验。

## lafa 设备配置

通用资源默认不启用独立镜头组或扩展倍率。lafa 的已验证配置位于 [`device-config/lafa/config.xml`](device-config/lafa/config.xml)：

| 项目 | 配置 |
| --- | --- |
| 独立主摄 | Camera2 ID `2` |
| 超广角 | Camera2 ID `3` |
| 长焦 | Camera2 ID `4` |
| 全局倍率上限 | `120` |

这些 ID 仅对应本次测试设备及其厂商相机实现，不能直接套用到其他机型。代码还会检查实际镜头范围和厂商能力，设备配置无法凭空增加硬件能力。

### 集成到 ROM

将示例 XML 放入设备树已经启用的 Aperture 资源覆盖目录，例如：

```text
device/realme/lafa/overlay-lineage/packages/apps/Aperture/app/src/main/res/values/config.xml
```

保留设备树原有的 overlay 注册方式，再按所用 ROM 的流程编译 Aperture。示例配置需要与本仓库源码一起使用。

### 单独调试 APK

也可以将示例配置作为 Gradle debug 变体的资源覆盖：

```bash
mkdir -p app/src/debug/res/values
cp device-config/lafa/config.xml app/src/debug/res/values/config.xml
./gradlew :app:assembleDebug
```

构建需要与本项目 Gradle 配置匹配的 Android SDK 和 JDK。debug 包名为 `org.lineageos.aperture.dev`，可以与系统相机并存；正式系统包使用 ROM 自己的签名流程。

## 验证记录与限制

已在 lafa 的 uwu Android 17 系统上验证：

| 全局倍率 | 实际相机 ID | 返回的原生倍率 | 返回裁切区域：左、上、宽、高 |
| --- | --- | --- | --- |
| 1× | 2 | 1.0× | 0, 0, 4096, 3072 |
| 2× | 2 | 2.0× | 1024, 768, 2048, 1536 |
| 4× | 4 | 1.39860141× | 583, 437, 2928, 2196 |
| 32× | 4 | 11.18881130× | 1865, 1398, 366, 274 |
| 120× | 4 | 41.95804214× | 1999, 1499, 97, 73 |
| 返回 1× | 2 | 1.0× | 0, 0, 4096, 3072 |

上述值来自实际返回的相机元数据，已验证 1× → 120× → 1× 往返。用户另行确认过 120× 保存照片与取景的放大范围一致；普通 JPEG 拍照也已由用户确认恢复，并核验了新照片成功保存。

验证范围是普通后置拍照。录像、厂商扩展效果及全部输出格式未做完整回归。本次设备测试中，启用 ZSL 与 JPEG Ultra HDR 的组合出现过拍照卡住及相机服务重启，两个选项各自的影响尚未单独验证；遇到此现象时可先关闭它们，使用普通 JPEG。

## 运行缩放计算测试

测试不连接手机、不拍摄照片，也不需要完整 ROM 编译。安装 Kotlin 命令行编译器和 Java 后运行：

```bash
bash tests/test-physical-camera-zoom.sh
```

也可以通过 `KOTLINC_BIN` 指定 Kotlin 编译器路径。测试覆盖镜头边界、正反向切换、视场倍率换算、120× 上限、厂商能力限制和缺少能力时的回退。

## 许可证

沿用上游各文件的 SPDX 标记和 [`LICENSES`](LICENSES) 中的许可证。本仓库的适配代码使用 Apache-2.0。

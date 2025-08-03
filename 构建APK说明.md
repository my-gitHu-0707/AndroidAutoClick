# Android 自动连点器 - APK 构建说明

## 方法一：使用 Android Studio（推荐）

### 1. 安装 Android Studio
- 下载并安装最新版本的 Android Studio
- 确保安装了 Android SDK API 34
- 配置好 Java/Kotlin 开发环境

### 2. 导入项目
1. 打开 Android Studio
2. 选择 "Open an existing project"
3. 选择项目根目录 `AndroidAutoClick`
4. 等待 Gradle 同步完成

### 3. 构建 APK
1. 在菜单栏选择 `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
2. 等待构建完成
3. 构建成功后会显示 "APK(s) generated successfully"
4. 点击 "locate" 找到生成的 APK 文件

### 4. APK 位置
生成的 APK 文件位于：
```
app/build/outputs/apk/debug/app-debug.apk
```

## 方法二：使用命令行

### 前提条件
- 安装 Java 8 或更高版本
- 安装 Android SDK
- 设置环境变量 ANDROID_HOME

### 1. 检查环境
```bash
java -version
echo $ANDROID_HOME
```

### 2. 构建 APK
```bash
# 进入项目目录
cd AndroidAutoClick

# 给 gradlew 执行权限
chmod +x gradlew

# 构建 debug APK
./gradlew assembleDebug

# 构建 release APK（需要签名）
./gradlew assembleRelease
```

### 3. 查找生成的 APK
```bash
find . -name "*.apk" -type f
```

## 方法三：在线构建服务

如果本地环境配置困难，可以使用在线构建服务：

### GitHub Actions（推荐）
1. 将项目上传到 GitHub
2. 配置 GitHub Actions 工作流
3. 自动构建并下载 APK

### 其他在线服务
- Bitrise
- CircleCI
- Travis CI

## 签名 APK（发布版本）

### 1. 生成密钥库
```bash
keytool -genkey -v -keystore my-release-key.keystore -alias alias_name -keyalg RSA -keysize 2048 -validity 10000
```

### 2. 配置签名
在 `app/build.gradle` 中添加：
```gradle
android {
    signingConfigs {
        release {
            storeFile file('my-release-key.keystore')
            storePassword 'your_store_password'
            keyAlias 'alias_name'
            keyPassword 'your_key_password'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### 3. 构建签名 APK
```bash
./gradlew assembleRelease
```

## 故障排除

### 常见问题

#### 1. Gradle 同步失败
- 检查网络连接
- 更新 Gradle 版本
- 清理项目：`./gradlew clean`

#### 2. SDK 版本问题
- 确保安装了 Android SDK API 34
- 检查 `local.properties` 文件中的 SDK 路径

#### 3. 内存不足
在 `gradle.properties` 中增加内存：
```
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m
```

#### 4. 权限问题
```bash
chmod +x gradlew
```

### 构建优化

#### 1. 加速构建
```gradle
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
```

#### 2. 减小 APK 大小
```gradle
android {
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
        }
    }
}
```

## 安装和测试

### 1. 安装 APK
```bash
# 使用 ADB 安装
adb install app-debug.apk

# 或者直接在设备上安装
# 需要开启"未知来源"选项
```

### 2. 测试功能
1. 授予无障碍服务权限
2. 授予悬浮窗权限
3. 设置点击坐标和间隔
4. 测试自动点击功能

## 发布准备

### 1. 版本管理
在 `app/build.gradle` 中更新：
```gradle
defaultConfig {
    versionCode 1
    versionName "1.0"
}
```

### 2. 图标和资源
- 确保所有图标资源完整
- 检查字符串资源
- 测试不同屏幕尺寸

### 3. 性能优化
- 启用代码混淆
- 压缩资源文件
- 移除未使用的代码

## 注意事项

1. **调试版本**: 仅用于开发测试，包含调试信息
2. **发布版本**: 用于正式发布，经过优化和签名
3. **权限说明**: 确保用户了解所需权限的用途
4. **兼容性**: 测试不同 Android 版本的兼容性

---

如果遇到构建问题，请检查：
- Java 版本是否正确
- Android SDK 是否完整安装
- 网络连接是否正常
- 项目文件是否完整

#!/bin/bash

echo "=== Android 自动连点器项目检查 ==="
echo ""

# 检查必要文件
echo "检查项目文件结构..."

files=(
    "build.gradle"
    "settings.gradle"
    "app/build.gradle"
    "app/src/main/AndroidManifest.xml"
    "app/src/main/java/com/autoclick/app/MainActivity.kt"
    "app/src/main/java/com/autoclick/app/service/AutoClickService.kt"
    "app/src/main/java/com/autoclick/app/service/FloatingWindowService.kt"
    "app/src/main/java/com/autoclick/app/utils/ClickSettings.kt"
    "app/src/main/java/com/autoclick/app/utils/PermissionUtils.kt"
    "app/src/main/res/layout/activity_main.xml"
    "app/src/main/res/layout/floating_window.xml"
    "app/src/main/res/values/strings.xml"
    "app/src/main/res/values/colors.xml"
    "app/src/main/res/values/themes.xml"
    "app/src/main/res/xml/accessibility_service_config.xml"
)

missing_files=0

for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file"
    else
        echo "❌ $file (缺失)"
        missing_files=$((missing_files + 1))
    fi
done

echo ""
echo "=== 检查结果 ==="
if [ $missing_files -eq 0 ]; then
    echo "✅ 所有必要文件都存在！"
    echo "项目结构完整，可以尝试构建。"
else
    echo "❌ 发现 $missing_files 个缺失文件"
    echo "请检查并补充缺失的文件。"
fi

echo ""
echo "=== 构建建议 ==="
echo "1. 使用 Android Studio 打开项目"
echo "2. 等待 Gradle 同步完成"
echo "3. 检查是否有编译错误"
echo "4. 连接 Android 设备或启动模拟器"
echo "5. 运行应用进行测试"

echo ""
echo "=== 权限说明 ==="
echo "应用需要以下权限："
echo "- 无障碍服务权限 (BIND_ACCESSIBILITY_SERVICE)"
echo "- 悬浮窗权限 (SYSTEM_ALERT_WINDOW)"
echo "- 前台服务权限 (FOREGROUND_SERVICE)"

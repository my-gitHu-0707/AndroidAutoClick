#!/usr/bin/env python3
"""
创建Android应用图标的脚本
"""

from PIL import Image, ImageDraw
import os

def create_icon(size, output_path):
    """创建指定尺寸的应用图标"""
    # 创建图像
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # 绘制圆形背景
    margin = size // 10
    draw.ellipse([margin, margin, size-margin, size-margin], 
                fill=(33, 150, 243, 255))  # Material Blue
    
    # 绘制内部图标（简单的点击符号）
    center = size // 2
    inner_size = size // 3
    
    # 绘制中心圆点
    dot_size = size // 8
    draw.ellipse([center-dot_size//2, center-dot_size//2, 
                 center+dot_size//2, center+dot_size//2], 
                fill=(255, 255, 255, 255))
    
    # 绘制外围圆环
    ring_width = size // 20
    draw.ellipse([center-inner_size//2, center-inner_size//2,
                 center+inner_size//2, center+inner_size//2],
                outline=(255, 255, 255, 255), width=ring_width)
    
    # 保存图像
    img.save(output_path, 'PNG')
    print(f"Created icon: {output_path} ({size}x{size})")

def main():
    """主函数"""
    # 定义各种尺寸的图标
    icon_sizes = {
        'mipmap-mdpi': 48,
        'mipmap-hdpi': 72,
        'mipmap-xhdpi': 96,
        'mipmap-xxhdpi': 144,
        'mipmap-xxxhdpi': 192
    }
    
    base_path = 'app/src/main/res'
    
    for folder, size in icon_sizes.items():
        folder_path = os.path.join(base_path, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # 创建普通图标
        icon_path = os.path.join(folder_path, 'ic_launcher.png')
        create_icon(size, icon_path)
        
        # 创建圆形图标（复制相同的）
        round_icon_path = os.path.join(folder_path, 'ic_launcher_round.png')
        create_icon(size, round_icon_path)

if __name__ == '__main__':
    main()

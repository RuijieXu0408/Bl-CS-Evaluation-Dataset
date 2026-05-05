import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
from pathlib import Path

# 配置中文字体支持
plt.rcParams['font.sans-serif'] = ['SimHei', 'DejaVu Sans']  # Windows中文字体
plt.rcParams['axes.unicode_minus'] = False

def plot_distance_data(csv_file):
    """
    绘制测距数据图表
    
    Args:
        csv_file: CSV文件路径
    """
    # 读取CSV文件
    df = pd.read_csv(csv_file)
    
    # 将Timestamp转换为datetime格式
    df['Timestamp'] = pd.to_datetime(df['Timestamp'])
    
    # 创建图表
    fig, ax = plt.subplots(figsize=(14, 6))
    
    # 绘制距离数据
    ax.plot(df['Timestamp'], df['Distance(m)'], linewidth=1.5, color='#1f77b4', alpha=0.8)
    ax.scatter(df['Timestamp'], df['Distance(m)'], s=10, color='#1f77b4', alpha=0.5)
    
    # 设置x轴格式（显示时间）
    ax.xaxis.set_major_formatter(mdates.DateFormatter('%H:%M:%S'))
    ax.xaxis.set_major_locator(mdates.AutoDateLocator())
    plt.xticks(rotation=45, ha='right')
    
    # 设置y轴以获得最细的单位
    ax.yaxis.set_major_locator(plt.MaxNLocator(nbins=20))  # 最多20个主刻度
    ax.yaxis.set_minor_locator(plt.MaxNLocator(nbins=100))  # 更多的次刻度
    ax.grid(True, which='major', alpha=0.3, linestyle='-', linewidth=0.5)
    ax.grid(True, which='minor', alpha=0.1, linestyle=':', linewidth=0.3)
    
    # 设置标签和标题
    ax.set_xlabel('时间', fontsize=12, fontweight='bold')
    ax.set_ylabel('距离 (m)', fontsize=12, fontweight='bold')
    ax.set_title('测距数据可视化', fontsize=14, fontweight='bold')
    
    # 调整布局
    plt.tight_layout()
    
    # 显示图表
    plt.show()

if __name__ == "__main__":
    # 获取脚本所在目录
    script_dir = Path(__file__).parent
    
    # 默认使用GT文件夹下的CSV文件，可以根据需要修改
    csv_file = script_dir / "Static_5Hz_10m_Indoor_Corridor.csv"
    
    # 如果文件不存在，尝试查找其他CSV文件
    if not csv_file.exists():
        print(f"文件 {csv_file} 不存在")
        print("\n可用的CSV文件：")
        if script_dir.exists():
            csv_files = list(script_dir.glob("*.csv"))
            for i, f in enumerate(csv_files, 1):
                print(f"{i}. {f.name}")
    else:
        print(f"正在绘制: {csv_file.name}")
        plot_distance_data(str(csv_file))

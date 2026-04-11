package com.group_finity.mascot.util;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;

/**
 * DPI 缩放工具类
 * 用于正确处理高 DPI 显示器的坐标计算
 * 
 * 在 Windows 高 DPI 环境下（如 200% 缩放）：
 * - 1920x1080 的物理屏幕，逻辑坐标范围为 0-960 x 0-540
 * - Toolkit.getScreenSize() 返回的是逻辑像素（已考虑缩放）
 * - GraphicsConfiguration.getBounds() 返回的也是逻辑像素
 * 
 * Java Swing 默认使用逻辑像素系统，所以大部分情况下不需要特殊处理
 * 但如果需要获取物理像素或进行精确的坐标转换，需要使用此类
 */
public class DPIScaler {
    
    private static Double systemScaleFactor = null;
    
    /**
     * 获取系统 DPI 缩放因子
     * @return 缩放因子（例如：1.0=100%, 2.0=200%）
     */
    public static double getSystemScaleFactor() {
        if (systemScaleFactor != null) {
            return systemScaleFactor;
        }
        
        try {
            // 使用标准 Java API 获取缩放因子
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            
            // 获取仿射变换的缩放比例
            double scaleX = gc.getDefaultTransform().getScaleX();
            systemScaleFactor = scaleX;
            
        } catch (Exception e) {
            // 降级方案：通过 Toolkit 估算
            // 这种方法在某些情况下可能不准确
            systemScaleFactor = Toolkit.getDefaultToolkit().getScreenResolution() / 96.0;
        }
        
        return systemScaleFactor;
    }
    
    /**
     * 将逻辑坐标转换为物理坐标
     * @param logicalValue 逻辑坐标值
     * @return 物理坐标值
     */
    public static int logicalToPhysical(double logicalValue) {
        return (int) Math.round(logicalValue * getSystemScaleFactor());
    }
    
    /**
     * 将物理坐标转换为逻辑坐标
     * @param physicalValue 物理坐标值
     * @return 逻辑坐标值
     */
    public static int physicalToLogical(double physicalValue) {
        return (int) Math.round(physicalValue / getSystemScaleFactor());
    }
    
    /**
     * 获取主屏幕的逻辑边界（推荐用于大多数场景）
     * Java 的坐标系统使用的是逻辑像素，这个方法返回的值可以直接使用
     * @return 屏幕边界矩形（逻辑像素）
     */
    public static java.awt.Rectangle getLogicalScreenBounds() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        return gc.getBounds();
    }
    
    /**
     * 获取主屏幕的物理边界（实际像素）
     * 仅在需要知道真实物理分辨率时使用
     * @return 屏幕边界矩形（物理像素）
     */
    public static java.awt.Rectangle getPhysicalScreenBounds() {
        java.awt.Rectangle logicalBounds = getLogicalScreenBounds();
        double scale = getSystemScaleFactor();
        
        return new java.awt.Rectangle(
            logicalToPhysical(logicalBounds.x),
            logicalToPhysical(logicalBounds.y),
            logicalToPhysical(logicalBounds.width),
            logicalToPhysical(logicalBounds.height)
        );
    }
    
    /**
     * 获取工作区域边界（排除任务栏等）
     * @return 工作区域边界（逻辑像素）
     */
    public static java.awt.Rectangle getLogicalWorkAreaBounds() {
        return java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    }
    
    /**
     * 刷新缓存的缩放因子
     * 当显示器配置改变时调用
     */
    public static void refreshScaleFactor() {
        systemScaleFactor = null;
    }
}

package com.wishes.utils;

/**
 * @Author:郑龙
 * @Date:2018-10-10 17:54
 * @Description:
 */
public class FormatUtils {
    /**
     * 格式化路径输出
     *
     * @param origin
     * @return
     */
    public static String formatImagePath(String origin) {
        return origin.replaceAll("\\\\", "/");//替换路径表示法
    }
}

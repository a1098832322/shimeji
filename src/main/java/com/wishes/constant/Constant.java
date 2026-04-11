package com.wishes.constant;

import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @Author:郑龙
 * @Date:2018-11-07 16:45
 * @Description:定义一些全局常量
 */
public class Constant {
    /**********************
     *      全局设定       *
     **********************/

    /**
     * 是否为开发环境<br>
     * 在IDE中运行是，请添加VM启动参数： -Ddev=true
     */
    public static final boolean isDevEnvironment = Boolean.parseBoolean(System.getProperty("dev", "false"));
    
    /**
     * 是否自动打开 DebugWindow<br>
     * 在IDE中运行时，请添加VM启动参数： -Ddebugwindow=true
     */
    public static final boolean autoOpenDebugWindow = Boolean.parseBoolean(System.getProperty("debugwindow", "false"));
    /**
     * 当前版本号(从 pom.xml 中自动获取)
     */
    public static final String CURRENT_VERSION = loadVersion();

    /**
     * 从 version.properties 文件中加载版本号
     */
    private static String loadVersion() {
        Properties props = new Properties();
        try (InputStream is = Constant.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (is != null) {
                props.load(is);
                return props.getProperty("app.version", "unknown");
            }
        } catch (IOException e) {
            // 如果读取失败,返回默认值
            System.err.println("警告: 无法加载版本信息 - " + e.getMessage());
        }
        return "unknown";
    }

    /**
     * 更新类型
     */
    public enum UPDATE_TYPE {
        CHILDREN("children"),
        MAIN("main"),
        BOTH("both");

        UPDATE_TYPE(String type) {
            this.type = type;
        }

        @Getter
        String type;
    }

    /**********************
     *   开发时使用的地址   *
     **********************/
//    /**
//     * 更新指示JSON文件下载地址
//     */
//    public static final String UPDATE_JSON_URL = "http://192.168.0.125/update/DeskTopPetVersion.json";
//
//    /**
//     * 默认主程序包下载地址
//     */
//    public static final String DEFAULT_JAR_DOWNLOAD_URL = "http://192.168.0.125/update/files/shimeji.jar";


    /**********************
     *   发布时使用的地址   *
     **********************/

    /**
     * 更新指示JSON文件下载地址
     */
    public static final String UPDATE_JSON_URL = "http://wishes-blog.cn/update/DeskTopPetVersion.json";

    /**
     * 默认主程序包下载地址
     */
    public static final String DEFAULT_JAR_DOWNLOAD_URL = "http://wishes-blog.cn/update/files/shimeji.jar";

}

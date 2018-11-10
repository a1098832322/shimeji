package com.wishes.constant;

import lombok.Getter;

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
     * 是否为开发环境
     */
    public static final boolean isDevEnvironment = true;
    /**
     * 当前版本号
     */
    public static final String CURRENT_VERSION = "1.0.3-beta1";

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

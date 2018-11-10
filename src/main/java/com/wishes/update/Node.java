package com.wishes.update;

import lombok.Data;

/**
 * @Author:郑龙
 * @Date:2018-11-09 10:08
 * @Description:更新时子文件节点
 */
@Data
public class Node {
    /**
     * 文件名
     */
    private String name;
    
    /**
     * 下载地址
     */
    private String downLoadURL;

    /**
     * 部署地址
     */
    private String deployPath;
}

package com.wishes.update;

import lombok.Data;

import java.util.List;

/**
 * @Author:郑龙
 * @Date:2018-11-08 11:55
 * @Description:针对下载指示文件进行解析并下载文件
 */
@Data
public class DataAnalysis {
    /**
     * 版本
     */
    private String version;
    /**
     * 更新信息
     */
    private String message;
    /**
     * 下载地址
     */
    private String url;

    /**
     * 需要更新的子节点
     */
    private List<Node> nodeList;
}

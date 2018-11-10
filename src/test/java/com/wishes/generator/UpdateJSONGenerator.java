package com.wishes.generator;

import com.alibaba.fastjson.JSONObject;
import com.wishes.constant.Constant;
import com.wishes.update.DataAnalysis;
import com.wishes.update.Node;
import lombok.Getter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author:郑龙
 * @Date:2018-11-09 10:23
 * @Description:用于生成更新文件json
 */
public class UpdateJSONGenerator {
    private DataAnalysis data;


    /**
     * json
     */
    private String json;

    /**
     * 保存位置
     */
    private final String savePath = System.getProperty("user.dir") + "/DeskTopPetVersion.json";

    /**
     * 编辑更新根节点数据
     */
    @Before
    public void before() {
        data = new DataAnalysis();
        data.setVersion("1.0.3 - beta 01");
        data.setMessage("修复更新进度条窗口显示错乱的BUG\n\n注：本次更新会将所有文件下载到download文件夹中，" +
                "待程序下载完毕后将download文件夹中所有文件直接复制到程序根目录下覆盖即可！");
        data.setUrl("http://wishes-blog.cn/update/files/shimeji.jar");
        data.setUpdateType(Constant.UPDATE_TYPE.BOTH.getType());
    }

    /**
     * 编辑更新子节点数据
     */
    @Test
    public void test() {
        //更新cmd启动脚本
        Node cmdNode = new Node();
        cmdNode.setDeployPath("\\");//在根目录一级
        cmdNode.setDownLoadURL("http://wishes-blog.cn/update/files/点我运行.cmd");
        cmdNode.setName("点我运行.cmd");

        //添加说明文档
        Node readmeText = new Node();
        readmeText.setDeployPath("\\download\\");//download目录
        readmeText.setDownLoadURL("http://wishes-blog.cn/update/files/请先读我.txt");
        readmeText.setName("请先读我.txt");

        List<Node> list = new ArrayList<>();
        list.add(cmdNode);
        list.add(readmeText);

        data.setNodeList(list);

        //转换成JSON字符串
        json = JSONObject.toJSONString(data);
        //打印显示
        System.out.println(json);
    }

    /**
     * 保存JSON文件
     */
    @After
    public void after() {
        try {
            FileOutputStream fos = new FileOutputStream(savePath);
            byte[] bytes = json.getBytes();
            fos.write(bytes);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

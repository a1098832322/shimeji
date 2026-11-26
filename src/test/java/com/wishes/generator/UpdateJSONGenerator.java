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
        // 版本号由 pom.xml 通过资源过滤提供
        data.setVersion(Constant.CURRENT_VERSION);
        // 精炼后的更新内容（不超过100字）
        data.setMessage("新增桌宠克隆与多实例控制，支持调皮模式、受限繁殖、可选鼠标推动及假装吃图标效果。");
        data.setUrl("https://wishes-blog.cn/shimeji/" + Constant.CURRENT_VERSION + "/shimeji.jar");
        data.setUpdateType(Constant.UPDATE_TYPE.MAIN.getType());
    }

    /**
     * 编辑更新子节点数据
     */
    @Test
    public void test() {
        // 根据用户需求，不生成任何 Node 节点
        // 如需添加其他文件，请在生成前明确说明
        data.setNodeList(new ArrayList<>());

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
        try (FileOutputStream fos = new FileOutputStream(savePath)) {
            byte[] bytes = json.getBytes();
            fos.write(bytes);
            fos.flush();
            System.out.println("\n✅ 更新文件已生成: " + savePath);
        } catch (FileNotFoundException e) {
            logger.error("文件未找到: {}", savePath, e);
        } catch (IOException e) {
            logger.error("写入文件失败: {}", savePath, e);
        }
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UpdateJSONGenerator.class);
}

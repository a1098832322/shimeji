package com.wishes.fix;


import com.wishes.constant.Constant;
import lombok.Data;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @Author:郑龙
 * @Date:2018-10-10 15:07
 * @Description:将源代码从ant移植到maven部署时需要做许多修改，这个类提供多种修改以及精简工具
 */
@Data
public class OriginEngineFix {
    private static Logger logger = Logger.getLogger(OriginEngineFix.class);

    private String BASE_PATH = "";

    private String BASE_ENVIRONMENT_PATH = "";

    private String BASE_IMG_PATH = "";

    private static OriginEngineFix instance = new OriginEngineFix();

    /**
     * 单例
     *
     * @return
     */
    public static OriginEngineFix getInstance() {
        return instance;
    }

    /**
     * 取读配置文件
     *
     * @param isDevEnvironment (是否是开发环境：true / false)
     * @param fileName         完整的文件名。  eg: logging.properties
     * @return InputStream
     */
    public InputStream propertiesLoder(boolean isDevEnvironment, String fileName) throws FileNotFoundException {
        String basePath = System.getProperty("user.dir");//项目根目录
        BASE_PATH = basePath;
        String path = "";
        if (isDevEnvironment) {
            //如果是开发环境
            BASE_ENVIRONMENT_PATH = basePath + "\\src\\main\\resources\\conf\\";
            BASE_IMG_PATH = basePath + "\\src\\main\\resources\\img\\";
            path = BASE_ENVIRONMENT_PATH + fileName;
        } else {
            BASE_ENVIRONMENT_PATH = basePath + "\\conf\\";
            BASE_IMG_PATH = basePath + "\\img\\";
            path = BASE_ENVIRONMENT_PATH + fileName;
        }

        InputStream is = new FileInputStream(path);
        logger.info("读取文件：" + path);
        return is;
    }

    public Properties getInstanceSettingProp() {
        // load properties
        Properties properties = new Properties();
        FileInputStream input;
        try {
            input = (FileInputStream) OriginEngineFix.getInstance().propertiesLoder(Constant.isDevEnvironment, "settings.properties");
            properties.load(input);
        } catch (FileNotFoundException e) {
            logger.error("找不到配置文件!", e);
        } catch (IOException e) {
            logger.error("I/O错误！读取文件失败！", e);
        }
        return properties;
    }

}

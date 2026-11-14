package com.group_finity.mascot.glados;

import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * GLaDOS AI 行为映射加载器。
 * <p>
 * 设计文档：docs/glados-ai-integration-design.md §5.4
 * <p>
 * 读取顺序：
 * <ol>
 *   <li>{@code --glados-home/glados-behaviors.xml}（GLaDOS 覆盖文件，优先）；</li>
 *   <li>classpath 内置 {@code glados-behaviors.xml}（默认）。</li>
 * </ol>
 * 覆盖文件缺失或解析失败时回退内置默认，不影响主程序启动。
 */
public final class GladosBehaviors {

    private static final Logger log = LoggerFactory.getLogger(GladosBehaviors.class);

    /** 内置默认行为表资源路径（classpath）。 */
    public static final String DEFAULT_RESOURCE = "/glados-behaviors.xml";

    /** GLaDOS 覆盖文件相对 --glados-home 的文件名。 */
    public static final String OVERRIDE_FILE_NAME = "glados-behaviors.xml";

    private GladosBehaviors() {
    }

    /**
     * 把 AI 行为映射合并进给定配置。
     *
     * @param configuration 目标配置（已加载 actions.xml + behaviors.xml）
     * @param gladosHome    --glados-home 目录；可为 null，此时仅用内置默认
     * @return 实际使用的行为表来源描述（覆盖文件路径或内置资源名），失败返回 null
     */
    public static String loadInto(Configuration configuration, String gladosHome) {
        // 1) 优先读覆盖文件
        if (gladosHome != null && !gladosHome.trim().isEmpty()) {
            File override = new File(gladosHome, OVERRIDE_FILE_NAME);
            if (override.isFile() && override.canRead()) {
                try (InputStream in = new FileInputStream(override)) {
                    merge(configuration, in);
                    log.info("GLaDOS AI behaviors loaded from override file: {}", override.getAbsolutePath());
                    return override.getAbsolutePath();
                } catch (Exception e) {
                    log.warn("Failed to load GLaDOS AI behaviors override file {}, fallback to built-in: {}",
                            override.getAbsolutePath(), e.toString());
                }
            } else {
                log.debug("No GLaDOS AI behaviors override file at {}", override.getAbsolutePath());
            }
        }

        // 2) 内置默认
        try (InputStream in = GladosBehaviors.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                log.warn("Built-in GLaDOS AI behaviors resource not found: {}", DEFAULT_RESOURCE);
                return null;
            }
            merge(configuration, in);
            log.info("GLaDOS AI behaviors loaded from built-in resource: {}", DEFAULT_RESOURCE);
            return DEFAULT_RESOURCE;
        } catch (Exception e) {
            log.warn("Failed to load built-in GLaDOS AI behaviors: {}", e.toString());
            return null;
        }
    }

    private static void merge(Configuration configuration, InputStream in)
            throws IOException, SAXException, ParserConfigurationException, ConfigurationException {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
        configuration.loadGladosBehaviors(new Entry(doc.getDocumentElement()));
    }
}
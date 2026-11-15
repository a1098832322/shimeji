package com.group_finity.mascot.studio;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * XML 读写、规范化与 XSD 校验。
 * <p>
 * 设计文档：docs/skin-action-designer-design.md §4.3（硬约束）、§5.4 L0/L1。
 * <p>
 * 三条实现原则：
 * <ol>
 *   <li>序列化用 DOM，不做字符串拼接；未知属性原样保留（P1 / SD-NFR-8）；</li>
 *   <li>片段根节点统一为 {@code <Mascot>}，首行写 {@code generated-by} 归属标记（P2）；</li>
 *   <li>XSD 校验对「未声明命名空间的片段」在内存中补默认命名空间 —— 仓库内片段写法就是
 *       无命名空间的 {@code <Mascot>}，而 {@code Mascot.xsd} 的 elementFormDefault 是 qualified。</li>
 * </ol>
 */
public final class Xml {

    /** Mascot.xsd 的 targetNamespace。 */
    public static final String NS = "http://www.group-finity.com/Mascot";

    private static final String MARKER = "generated-by: shimeji-studio v1.0; do not hand-edit";

    private Xml() {
    }

    public static DocumentBuilder newBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(false);
        // 片段是本地可信文件，但仍关闭外部实体，避免 XXE（SD-NFR-4）
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder();
    }

    public static Document parse(byte[] bytes) throws IOException, SAXException, ParserConfigurationException {
        // 仓库内 conf/*.xml 带 UTF-8 BOM；经 StringReader 解析时 BOM 会变成 U+FEFF 触发
        // "Content is not allowed in prolog"。这里统一剥掉（不改变文件本身）。
        int offset = 0;
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            offset = 3;
        }
        String text = new String(bytes, offset, bytes.length - offset, StandardCharsets.UTF_8);
        return newBuilder().parse(new InputSource(new StringReader(text)));
    }

    public static Document parse(Path file) throws IOException, SAXException, ParserConfigurationException {
        return parse(Files.readAllBytes(file));
    }

    /** DOM → UTF-8 文本（带声明、缩进 2）。 */
    public static String serialize(Document doc) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        // 先去掉纯空白文本节点：否则缩进输出会与原有换行叠加成大量空行
        stripWhitespace(doc.getDocumentElement());
        StringWriter writer = new StringWriter();
        t.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString()
                .replace(" standalone=\"no\"", "")
                .replace(" standalone=\"yes\"", "");
    }

    private static void stripWhitespace(Node node) {
        if (node == null) {
            return;
        }
        NodeList children = node.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE && child.getTextContent().trim().isEmpty()) {
                node.removeChild(child);
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                stripWhitespace(child);
            }
        }
    }

    /**
     * 规范化片段文本：解析 → 根节点校验 → 补归属标记 → 重新序列化。
     *
     * @param xml  前端提交的完整 XML 文本
     * @param kind {@code actions} 或 {@code behaviors}
     * @throws StudioException 结构不合法时抛 422（L0）
     */
    public static String normalizeFragment(String xml, String kind) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new StudioException(422, "片段内容为空");
        }
        Document doc;
        try {
            doc = parse(xml.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new StudioException(422, "XML 无法解析：" + shortMessage(e));
        }
        Element root = doc.getDocumentElement();
        if (!"Mascot".equals(localName(root))) {
            throw new StudioException(422, "片段根节点必须是 <Mascot>，当前是 <" + root.getNodeName() + ">");
        }
        int actionLists = countTopLevel(root, "ActionList");
        int behaviorLists = countTopLevel(root, "BehaviorList");
        if (actionLists + behaviorLists == 0) {
            throw new StudioException(422, "片段内必须包含一个 <ActionList> 或 <BehaviorList>");
        }
        if ("actions".equals(kind) && behaviorLists > 0) {
            throw new StudioException(422, "动作片段只能包含 <ActionList>，不能含 <BehaviorList>");
        }
        if ("behaviors".equals(kind) && actionLists > 0) {
            throw new StudioException(422, "行为片段只能包含 <BehaviorList>，不能含 <ActionList>");
        }
        if (actionLists + behaviorLists > 1) {
            throw new StudioException(422, "一个片段只能包含一个列表节点（当前 " + (actionLists + behaviorLists) + " 个）");
        }
        ensureMarker(doc);
        try {
            return serialize(doc);
        } catch (TransformerException e) {
            throw new StudioException(500, "序列化失败：" + shortMessage(e));
        }
    }

    private static void ensureMarker(Document doc) {
        if (doc.getDocumentElement() == null) {
            return;
        }
        NodeList children = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.COMMENT_NODE) {
                String text = ((Comment) node).getData();
                if (text != null && text.contains("generated-by: shimeji-studio")) {
                    return;
                }
            }
        }
        Comment marker = doc.createComment(" " + MARKER + " ");
        Element root = doc.getDocumentElement();
        if (root.getFirstChild() != null) {
            root.insertBefore(marker, root.getFirstChild());
        } else {
            root.appendChild(marker);
        }
    }

    /** 判断文本是否已带设计器归属标记。 */
    public static boolean hasMarker(String xml) {
        return xml != null && xml.contains("generated-by: shimeji-studio");
    }

    public static String localName(Element element) {
        String local = element.getLocalName();
        return local != null ? local : element.getNodeName();
    }

    /** 顶层（root 直接子级）列表节点数量；与引擎 selectChildren 语义一致。 */
    public static int countTopLevel(Element root, String name) {
        int count = 0;
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element && name.equals(localName((Element) node))) {
                count++;
            }
        }
        return count;
    }

    /**
     * XSD 校验。片段通常写成无命名空间的 {@code <Mascot>}，这里在内存副本上补默认命名空间，
     * 不改磁盘文件、不改变引擎解析结果。
     *
     * @return 错误列表（空 = 通过）
     */
    public static java.util.List<String> validateXsd(byte[] xml, Path xsd) {
        java.util.List<String> errors = new java.util.ArrayList<>();
        if (xsd == null || !Files.isRegularFile(xsd)) {
            return errors;
        }
        try {
            Document doc = parse(withDefaultNamespace(xml));
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            sf.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            sf.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Schema schema = sf.newSchema(xsd.toFile());
            Validator validator = schema.newValidator();
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            final java.util.List<String> collected = new java.util.ArrayList<>();
            validator.setErrorHandler(new org.xml.sax.ErrorHandler() {
                @Override
                public void warning(org.xml.sax.SAXParseException e) {
                    collected.add("line " + e.getLineNumber() + ": " + e.getMessage());
                }

                @Override
                public void error(org.xml.sax.SAXParseException e) {
                    collected.add("line " + e.getLineNumber() + ": " + e.getMessage());
                }

                @Override
                public void fatalError(org.xml.sax.SAXParseException e) {
                    collected.add("line " + e.getLineNumber() + ": " + e.getMessage());
                }
            });
            validator.validate(new DOMSource(doc));
            errors.addAll(collected);
        } catch (SAXException e) {
            errors.add("XSD 校验异常：" + shortMessage(e));
        } catch (Exception e) {
            errors.add("XSD 校验异常：" + shortMessage(e));
        }
        return errors;
    }


    /**
     * 在文本层面给根节点补默认命名空间。
     * <p>
     * 不能对已解析的 DOM 直接 {@code setAttributeNS("xmlns")} —— 那样只加了一条属性，
     * 已有元素仍然处于「无命名空间」状态，XSD 会报找不到元素声明。
     */
    static byte[] withDefaultNamespace(byte[] xml) {
        int offset = 0;
        if (xml.length >= 3 && (xml[0] & 0xFF) == 0xEF && (xml[1] & 0xFF) == 0xBB && (xml[2] & 0xFF) == 0xBF) {
            offset = 3;
        }
        String text = new String(xml, offset, xml.length - offset, StandardCharsets.UTF_8);
        int idx = text.indexOf("<Mascot");
        if (idx < 0) {
            return xml;
        }
        int end = idx + "<Mascot".length();
        int gt = text.indexOf('>', end);
        if (gt < 0) {
            return xml;
        }
        String attrs = text.substring(end, gt);
        if (attrs.contains("xmlns=") || attrs.contains("xmlns:")) {
            return xml;
        }
        String patched = text.substring(0, end) + " xmlns=\"" + NS + "\"" + text.substring(end);
        return patched.getBytes(StandardCharsets.UTF_8);
    }

    public static String shortMessage(Throwable e) {
        StringBuilder sb = new StringBuilder();
        Throwable cur = e;
        int depth = 0;
        while (cur != null && depth < 6) {
            String msg = cur.getMessage();
            if (msg != null && !msg.trim().isEmpty() && sb.indexOf(msg) < 0) {
                if (sb.length() > 0) {
                    sb.append(" ← ");
                }
                sb.append(msg.trim());
            }
            cur = cur.getCause();
            depth++;
        }
        return sb.length() == 0 ? e.getClass().getSimpleName() : sb.toString();
    }
}

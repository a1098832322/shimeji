package com.group_finity.mascot.glados;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * GLaDOS 托管约束：禁止自主增殖。
 * <p>
 * 设计文档：docs/glados-managed-constraints-design.md §4.1
 * <p>
 * 复用原版 {@code Breeding} 开关（{@code Breed.isEnabled()} 读取
 * {@code Main.getProperties().getProperty("Breeding", "true")}）：
 * <ul>
 *   <li>托管：置 {@code Breeding=false} → 跳过 {@code breed()}，不产生新角色；</li>
 *   <li>非托管：不设置 → 默认 {@code "true"} → 增殖照旧，用户直跑不受影响。</li>
 * </ul>
 * 本测试只验证属性语义与托管分支，不需要 Swing/原生窗口。
 */
public class GladosManagedConstraintsTest {

    /** 模拟 Main.run() 中「加载 settings.properties 后按托管状态调整」这一段。 */
    private static Properties applyManagedPolicy(Properties loaded, boolean managed) {
        Properties properties = (Properties) loaded.clone();
        if (managed) {
            properties.setProperty("Breeding", "false");
        }
        return properties;
    }

    /** 与 Breed.Delegate#isEnabled() 等价的判定（非瞬态分支）。 */
    private static boolean isBreedingEnabled(Properties properties) {
        return Boolean.parseBoolean(properties.getProperty("Breeding", "true"));
    }

    // ------------------------------------------------------------------
    // 托管模式：禁止增殖
    // ------------------------------------------------------------------

    @Test
    public void managedDisablesBreeding() {
        Properties loaded = new Properties();
        Properties effective = applyManagedPolicy(loaded, true);

        assertEquals("false", effective.getProperty("Breeding"));
        assertFalse("托管模式下不允许自主增殖", isBreedingEnabled(effective));
    }

    @Test
    public void managedDisablesBreedingEvenIfConfSaysTrue() {
        // 用户 conf 里显式写了 Breeding=true 也不该让托管模式增殖
        Properties loaded = new Properties();
        loaded.setProperty("Breeding", "true");

        Properties effective = applyManagedPolicy(loaded, true);

        assertEquals("false", effective.getProperty("Breeding"));
        assertFalse(isBreedingEnabled(effective));
    }

    @Test
    public void managedPolicyDoesNotMutateLoadedConfInFile() {
        // 关键：只改内存副本，不能污染调用方的 Properties（对应"不回写 settings.properties"）
        Properties loaded = new Properties();
        loaded.setProperty("Language", "zh-CN");

        Properties effective = applyManagedPolicy(loaded, true);

        assertEquals("false", effective.getProperty("Breeding"));
        assertEquals("zh-CN", loaded.getProperty("Language"));
        assertEquals("原对象不应被写入 Breeding", null, loaded.getProperty("Breeding"));
    }

    // ------------------------------------------------------------------
    // 非托管模式：保持原版行为
    // ------------------------------------------------------------------

    @Test
    public void unmanagedKeepsBreedingEnabledByDefault() {
        Properties loaded = new Properties();

        Properties effective = applyManagedPolicy(loaded, false);

        // 未设置该键 → 默认 "true"
        assertEquals("true", effective.getProperty("Breeding", "true"));
        assertTrue("用户直接运行时增殖必须保持可用", isBreedingEnabled(effective));
    }

    @Test
    public void unmanagedRespectsUserConfiguredBreedingFalse() {
        // 用户自己关掉增殖（托盘菜单或手改配置）时，非托管应尊重该设置
        Properties loaded = new Properties();
        loaded.setProperty("Breeding", "false");

        Properties effective = applyManagedPolicy(loaded, false);

        assertEquals("false", effective.getProperty("Breeding"));
        assertFalse(isBreedingEnabled(effective));
    }

    @Test
    public void unmanagedDoesNotInjectBreedingKey() {
        Properties loaded = new Properties();

        Properties effective = applyManagedPolicy(loaded, false);

        assertEquals("非托管不写该键", null, effective.getProperty("Breeding"));
    }

    // ------------------------------------------------------------------
    // 与其他托管约束的一致性
    // ------------------------------------------------------------------

    @Test
    public void managedFlagParsedFromArgs() {
        assertTrue(GladosOptions.parse(new String[]{"--glados-managed"}).isManaged());
        assertFalse(GladosOptions.parse(new String[]{}).isManaged());
    }

    @Test
    public void managedPolicyIsDrivenByManagedFlag() {
        Properties loaded = new Properties();
        boolean managed = GladosOptions.parse(new String[]{"--glados-managed"}).isManaged();
        assertFalse(isBreedingEnabled(applyManagedPolicy(loaded, managed)));

        Properties loaded2 = new Properties();
        boolean managed2 = GladosOptions.parse(new String[]{}).isManaged();
        assertTrue(isBreedingEnabled(applyManagedPolicy(loaded2, managed2)));
    }

    // ------------------------------------------------------------------
    // 真实配置文件契约（§7 改动清单的回归保护）
    // 前 8 个用例验证的是策略语义本身；这里验证「实现载体」没被误删/误改。
    // 只读 classpath 资源，不触碰 Main / Swing，可在 headless 环境运行。
    // ------------------------------------------------------------------

    private static Document loadXmlResource(String resource) throws Exception {
        try (InputStream in = GladosManagedConstraintsTest.class.getResourceAsStream(resource)) {
            assertNotNull("classpath 资源缺失: " + resource, in);
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
        }
    }

    /** 按 Name 属性查找元素；兼容带前缀与不带前缀的 XML 标签。 */
    private static Element findByName(Document doc, String tag, String name) {
        NodeList nodes = doc.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            nodes = doc.getElementsByTagNameNS("*", tag);
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            if (name.equals(element.getAttribute("Name"))) {
                return element;
            }
        }
        return null;
    }

    @Test
    public void builtInBehaviorTableDisablesBothBreedingBehaviors() throws Exception {
        Document doc = loadXmlResource("/glados-behaviors.xml");

        for (String name : new String[]{"SplitIntoTwo", "PullUpShimeji"}) {
            Element behavior = findByName(doc, "Behavior", name);
            assertNotNull("内置行为表必须声明 " + name + " 的禁频覆盖", behavior);
            assertEquals(name + " 频率必须为 0", "0", behavior.getAttribute("Frequency"));
            assertEquals(name + " 必须改用非增殖动作", "Sit", behavior.getAttribute("Action"));
        }
    }

    @Test
    public void originalConfStillAllowsBreedingWhenRunDirectly() throws Exception {
        // 用户直跑用的 conf/*.xml 一个字节都不能改：这里锁定原始频率。
        for (String resource : new String[]{"/conf/behaviors.xml", "/conf/MischievousBehavior.xml"}) {
            Document doc = loadXmlResource(resource);
            for (String name : new String[]{"SplitIntoTwo", "PullUpShimeji"}) {
                Element behavior = findByName(doc, "Behavior", name);
                assertNotNull(resource + " 缺少 " + name, behavior);
                assertEquals(resource + " 中 " + name + " 应保持原版频率", "50",
                        behavior.getAttribute("Frequency"));
            }
        }

        // 记录事实：Calm 两者天然为 0；Professional 只有 SplitIntoTwo 为 0，
        // PullUpShimeji 仍是 50 —— 所以「Professional 天然禁用」的说法不成立。
        Document calm = loadXmlResource("/conf/CalmBehavior.xml");
        assertEquals("0", findByName(calm, "Behavior", "SplitIntoTwo").getAttribute("Frequency"));
        assertEquals("0", findByName(calm, "Behavior", "PullUpShimeji").getAttribute("Frequency"));

        Document professional = loadXmlResource("/conf/ProfessionalBehavior.xml");
        assertEquals("0", findByName(professional, "Behavior", "SplitIntoTwo").getAttribute("Frequency"));
        assertEquals("50", findByName(professional, "Behavior", "PullUpShimeji").getAttribute("Frequency"));
    }

    @Test
    public void settingsPropertiesIsNeverWrittenBack() throws Exception {
        // 托管只改内存 Properties；若这里出现 Breeding 键，说明有人写回了文件，
        // 用户之后直接运行会继承 Breeding=false，违背硬性要求。
        Properties settings = new Properties();
        try (InputStream in = GladosManagedConstraintsTest.class
                .getResourceAsStream("/conf/settings.properties")) {
            assertNotNull("classpath 资源缺失: /conf/settings.properties", in);
            settings.load(in);
        }
        assertEquals("settings.properties 不应含 Breeding 键", null, settings.getProperty("Breeding"));
    }

    @Test
    public void breedingActionsAreNotTransient() throws Exception {
        // Breed.Delegate.isEnabled() 对 BornTransient=true 的动作读的是 Transients
        // 而不是 Breeding。若这两个动作是瞬态，Breeding=false 就拦不住它们。
        Document doc = loadXmlResource("/conf/actions.xml");
        for (String name : new String[]{"Divide1", "PullUpShimeji1"}) {
            Element action = findByName(doc, "Action", name);
            assertNotNull("actions.xml 缺少 " + name, action);
            assertEquals(name + " 应为 Breed 动作", "com.group_finity.mascot.action.Breed",
                    action.getAttribute("Class"));
            assertFalse(name + " 不得为瞬态（否则 Breeding 开关失效）",
                    Boolean.parseBoolean(action.getAttribute("BornTransient")));
        }
    }
}
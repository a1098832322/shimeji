package com.group_finity.mascot.glados;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
}
package com.group_finity.mascot.glados;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 托管约束：增殖行为在代码层被强制禁用。
 * <p>
 * 设计文档：docs/glados-managed-constraints-design.md §4.4
 * <p>
 * 背景：{@code GladosBehaviors.loadInto()} 在存在
 * {@code --glados-home/glados-behaviors.xml} 时是「替换」而非「合并」，
 * 用户自定义覆盖文件会让内置的 {@code Frequency="0"} 失效。
 * 因此增殖约束必须在代码层施加，与配置来源无关。
 */
public class GladosManagedConstraintsTest {

    @After
    public void resetManagedFlag() {
        GladosOptions.setManagedProcessForTest(null);
    }

    // ------------------------------------------------------------------
    // 行为名判定
    // ------------------------------------------------------------------

    @Test
    public void splitIntoTwoIsBreedingBehavior() {
        assertTrue(GladosManagedConstraints.isBreedingBehavior("SplitIntoTwo"));
    }

    @Test
    public void pullUpShimejiIsBreedingBehavior() {
        assertTrue(GladosManagedConstraints.isBreedingBehavior("PullUpShimeji"));
    }

    @Test
    public void ordinaryBehaviorsAreNotBreedingBehaviors() {
        assertFalse(GladosManagedConstraints.isBreedingBehavior("StandUp"));
        assertFalse(GladosManagedConstraints.isBreedingBehavior("SitDown"));
        assertFalse(GladosManagedConstraints.isBreedingBehavior("Walk"));
        assertFalse(GladosManagedConstraints.isBreedingBehavior("Fall"));
        // Transform（变身换形象）会改 imageSet，但不新增角色，不应被误伤
        assertFalse(GladosManagedConstraints.isBreedingBehavior("Transform"));
        assertFalse(GladosManagedConstraints.isBreedingBehavior("AIWorking"));
    }

    @Test
    public void nullOrEmptyIsNotBreedingBehavior() {
        assertFalse(GladosManagedConstraints.isBreedingBehavior(null));
        assertFalse(GladosManagedConstraints.isBreedingBehavior(""));
    }

    // ------------------------------------------------------------------
    // 托管标记
    // ------------------------------------------------------------------

    @Test
    public void notManagedByDefault() {
        assertFalse(GladosOptions.isManagedProcess());
    }

    @Test
    public void parseWithoutManagedFlagKeepsUnmanaged() {
        GladosOptions.parseAndMark(new String[]{});
        assertFalse(GladosOptions.isManagedProcess());
    }

    @Test
    public void parseWithManagedFlagMarksManaged() {
        GladosOptions.parseAndMark(new String[]{"--glados-managed"});
        assertTrue(GladosOptions.isManagedProcess());
    }

    @Test
    public void parseWithManagedFlagAndOthersMarksManaged() {
        GladosOptions.parseAndMark(new String[]{"--glados-managed", "--glados-port", "0", "--glados-home", "/tmp/x"});
        assertTrue(GladosOptions.isManagedProcess());
    }

    @Test
    public void managedFlagCanBeReset() {
        GladosOptions.setManagedProcessForTest(true);
        assertTrue(GladosOptions.isManagedProcess());
        GladosOptions.setManagedProcessForTest(null);
        assertFalse(GladosOptions.isManagedProcess());
    }

    // ------------------------------------------------------------------
    // 与 Breeding 开关的配合（两条约束互不替代）
    // ------------------------------------------------------------------

    @Test
    public void managedDisablesBreedingSwitch() {
        java.util.Properties properties = new java.util.Properties();
        if (GladosOptions.parseAndMark(new String[]{"--glados-managed"}).isManaged()) {
            properties.setProperty("Breeding", "false");
        }
        assertFalse(Boolean.parseBoolean(properties.getProperty("Breeding", "true")));
    }

    @Test
    public void unmanagedKeepsBreedingSwitchDefault() {
        java.util.Properties properties = new java.util.Properties();
        if (GladosOptions.parseAndMark(new String[]{}).isManaged()) {
            properties.setProperty("Breeding", "false");
        }
        // 未设置 → 默认 true（允许增殖）
        assertTrue(Boolean.parseBoolean(properties.getProperty("Breeding", "true")));
    }
}
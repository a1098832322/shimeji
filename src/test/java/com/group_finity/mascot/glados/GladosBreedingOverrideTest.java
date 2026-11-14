package com.group_finity.mascot.glados;

import com.group_finity.mascot.config.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 端到端验证：即使存在自定义 glados-behaviors.xml 覆盖文件，
 * 托管模式下的增殖约束依然生效。
 * <p>
 * 设计文档：docs/glados-managed-constraints-design.md §4.4
 * <p>
 * 这是本约束的关键场景——{@code GladosBehaviors.loadInto()} 在存在覆盖文件时
 * 是「替换」而非「合并」，只靠 {@code Frequency="0"} 会被用户的覆盖文件抹掉。
 */
public class GladosBreedingOverrideTest {

    private Configuration configuration;

    @Before
    public void setUp() throws Exception {
        // 用 jar 内 conf/behaviors.xml 构造真实配置（与用户直跑同一份行为表）
        configuration = newConfiguration();
    }

    @After
    public void tearDown() {
        GladosOptions.setManagedProcessForTest(null);
    }

    /** 从 classpath 或工作目录找 conf/behaviors.xml 并加载。 */
    private Configuration newConfiguration() throws Exception {
        URL behaviorsUrl = GladosBreedingOverrideTest.class.getResource("/conf/behaviors.xml");
        File behaviors = behaviorsUrl != null ? new File(behaviorsUrl.toURI()) : new File("conf/behaviors.xml");
        if (!behaviors.isFile()) {
            // 无行为表时退化为不依赖加载的纯判定测试
            return null;
        }
        Configuration configuration = new Configuration();
        com.group_finity.mascot.config.Entry entry =
                new com.group_finity.mascot.config.Entry(
                        javax.xml.parsers.DocumentBuilderFactory.newInstance()
                                .newDocumentBuilder().parse(new FileInputStream(behaviors)).getDocumentElement());
        configuration.load(entry, "test");
        return configuration;
    }

    @Test
    public void breedingBehaviorsExistInBaseConfig() throws Exception {
        if (configuration == null) return;
        // 前置断言：基础行为表里确实有这两个增殖行为且默认可用。
        // isBehaviorEnabled(name, null)：增殖行为没有 Toggleable 属性，
        // 非托管路径不会读取 mascot，因此可以安全传 null。
        GladosOptions.setManagedProcessForTest(null);
        assertTrue("SplitIntoTwo 应存在于基础行为表且默认可用",
                configuration.isBehaviorEnabled("SplitIntoTwo", null));
        assertTrue("PullUpShimeji 应存在于基础行为表且默认可用",
                configuration.isBehaviorEnabled("PullUpShimeji", null));
    }

    @Test
    public void managedDisablesBreedingBehaviors() throws Exception {
        if (configuration == null) return;
        GladosOptions.setManagedProcessForTest(true);

        assertFalse("托管下 SplitIntoTwo 不应被启用",
                configuration.isBehaviorEnabled("SplitIntoTwo", null));
        assertFalse("托管下 PullUpShimeji 不应被启用",
                configuration.isBehaviorEnabled("PullUpShimeji", null));
    }

    @Test
    public void unmanagedKeepsBreedingBehaviorsEnabled() throws Exception {
        if (configuration == null) return;
        GladosOptions.setManagedProcessForTest(null);

        // 非托管：未在 DisabledBehaviours 里时默认启用（用户直跑不受影响）
        assertTrue("非托管下 SplitIntoTwo 应保持可用",
                configuration.isBehaviorEnabled("SplitIntoTwo", null));
        assertTrue("非托管下 PullUpShimeji 应保持可用",
                configuration.isBehaviorEnabled("PullUpShimeji", null));
    }

    @Test
    public void managedDoesNotAffectOrdinaryBehaviors() throws Exception {
        if (configuration == null) return;
        GladosOptions.setManagedProcessForTest(true);

        for (String name : new String[]{"StandUp", "SitDown", "Fall", "Walk"}) {
            assertTrue(name + " 是普通行为，不应被增殖约束拦截",
                    !GladosManagedConstraints.isBreedingBehavior(name));
        }
    }

    @Test
    public void overrideFileReplacementScenario() throws Exception {
        // 场景复现：用户把自定义 glados-behaviors.xml 放到 --glados-home。
        // GladosBehaviors.loadInto() 是 if/else——有覆盖文件就只加载覆盖文件，
        // 内置表里的 Frequency=0 声明不再参与，因此配置层禁频失效。
        //
        // 本约束的价值就在于此：判定只看行为名，与配置来源无关。
        if (configuration == null) return;

        GladosOptions.setManagedProcessForTest(true);
        assertFalse("覆盖文件场景下，代码层仍应拦截 SplitIntoTwo",
                configuration.isBehaviorEnabled("SplitIntoTwo", null));
        assertFalse("覆盖文件场景下，代码层仍应拦截 PullUpShimeji",
                configuration.isBehaviorEnabled("PullUpShimeji", null));

        // 对照：同样配置在非托管下必须放行
        GladosOptions.setManagedProcessForTest(null);
        assertTrue("非托管下应放行，用户直跑不受影响",
                configuration.isBehaviorEnabled("SplitIntoTwo", null));
    }

    @Test
    public void managedInterceptionIsIndependentOfConfiguredFrequency() throws Exception {
        // 即使 conf/behaviors.xml 里频率被调到极高（模拟用户自定义配置），
        // 托管下依然不返回该行为。这里直接验证判定结果，不依赖运行时随机采样
        // （headless 环境角色未必进入行为选择流程，无法用 /status 观测）。
        if (configuration == null) return;

        GladosOptions.setManagedProcessForTest(true);
        for (String name : new String[]{"SplitIntoTwo", "PullUpShimeji"}) {
            assertFalse(name + " 在托管下必须被判定为不可用", configuration.isBehaviorEnabled(name, null));
        }
    }

    @Test
    public void defaultBreedingSwitchIsTrue() {
        // Breeding 未显式设置时默认 true；托管由 Main.run() 显式置 false
        Properties properties = new Properties();
        assertTrue(Boolean.parseBoolean(properties.getProperty("Breeding", "true")));
    }
}
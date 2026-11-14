package com.group_finity.mascot.glados;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * GLaDOS 托管模式下的行为约束。
 * <p>
 * 设计文档：docs/glados-managed-constraints-design.md
 * <p>
 * 与配置层的区别：
 * <ul>
 *   <li>{@code glados-behaviors.xml} 里的 {@code Frequency="0"} 是「表现层」约束，
 *       只在没有自定义覆盖文件时生效；</li>
 *   <li>本类是「逻辑层」约束，由 {@code Configuration.isBehaviorEnabled} 调用，
 *       任何覆盖文件都绕不过去。</li>
 * </ul>
 * 两者叠加的效果：托管模式下增殖行为既不会被选中（不播动画），
 * 也不会产生新角色（{@code Breeding=false}）。
 */
public final class GladosManagedConstraints {

    /**
     * 自主增殖行为名。
     * <p>
     * 这两个行为在 {@code conf/behaviors.xml} 里引用 {@code action/Breed}：
     * <ul>
     *   <li>{@code SplitIntoTwo} → {@code Divide1}</li>
     *   <li>{@code PullUpShimeji} → {@code PullUpShimeji1}</li>
     * </ul>
     * 仅禁用这两个行为，不影响 GLaDOS 主动下发的 {@code createMascot} 命令
     * （那走 {@code Main.createMascot}，不经过行为表）。
     */
    private static final Set<String> BREEDING_BEHAVIORS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("SplitIntoTwo", "PullUpShimeji")));

    private GladosManagedConstraints() {
    }

    /**
     * 该行为是否属于「自主增殖」，需要在托管模式下禁用。
     *
     * @param behaviorName 行为名，可为 null
     */
    public static boolean isBreedingBehavior(String behaviorName) {
        return behaviorName != null && BREEDING_BEHAVIORS.contains(behaviorName);
    }
}
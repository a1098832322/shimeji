package com.group_finity.mascot.mischief;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import org.junit.After;
import org.junit.Assume;
import org.junit.Test;
import static org.junit.Assert.*;

public class VisualIconTest {
    private final Properties properties = new Properties();

    @After public void cleanup() throws Exception {
        MischiefMode.set(properties, false, false);
        SwingUtilities.invokeAndWait(() -> {});
    }

    @Test public void closingStopsVisibleAnimationAndInvalidatesQueuedWork() throws Exception {
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());
        MischiefMode.set(properties, true, false);
        long generation = VisualIcon.currentGeneration();
        Set<Window> before = new HashSet<>(Arrays.asList(Window.getWindows()));
        SwingUtilities.invokeAndWait(() -> VisualIcon.showSnack(new Rectangle(-1000, -1000, 100, 100), generation));
        Window created = Arrays.stream(Window.getWindows()).filter(w -> !before.contains(w) && w.isVisible())
                .findFirst().orElse(null);
        assertNotNull("动画窗口应显示", created);
        MischiefMode.set(properties, false, false);
        SwingUtilities.invokeAndWait(() -> {});
        assertFalse("关闭后动画应立即消失", created.isDisplayable());
        MischiefMode.set(properties, true, false);
        SwingUtilities.invokeAndWait(() -> VisualIcon.showSnack(new Rectangle(-1000, -1000, 100, 100), generation));
        assertFalse("关闭前排队的动画不能在重新开启后出现", created.isDisplayable());
        assertEquals(0, Arrays.stream(Window.getWindows()).filter(w -> !before.contains(w) && w.isVisible()).count());
    }

    @Test public void closingWaitsForStartedActionAndRejectsLaterAction() throws Exception {
        MischiefMode.set(properties, true, false);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch closed = new CountDownLatch(1);
        Thread action = new Thread(() -> MischiefMode.runIfEnabled(() -> {
            started.countDown();
            try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }));
        Thread closer = new Thread(() -> { MischiefMode.set(properties, false, false); closed.countDown(); });
        try {
            action.start();
            assertTrue(started.await(2, TimeUnit.SECONDS));
            closer.start();
            assertFalse(closed.await(100, TimeUnit.MILLISECONDS));
        } finally {
            release.countDown();
            action.join(2000);
            closer.join(2000);
        }
        assertEquals(0, closed.getCount());
        AtomicBoolean ran = new AtomicBoolean();
        assertFalse(MischiefMode.runIfEnabled(() -> ran.set(true)));
        assertFalse(ran.get());
    }
}

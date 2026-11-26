package com.group_finity.mascot.mischief;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Properties;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class CursorPushTest {
    private final Properties properties = new Properties();
    @After public void cleanup() { MischiefMode.set(properties, false, false); CursorPush.resetForTest(); }
    @Test public void cursorRequiresOptInAndChecksTheActualMove() {
        final Point[] pos = { new Point(100, 100) };
        CursorPush.Pointer pointer = new CursorPush.Pointer() {
            public Point location() { return new Point(pos[0]); }
            public void move(Point target) { pos[0] = new Point(target); }
        };
        MischiefMode.set(properties, true, false);
        CursorPush.push(pointer, new Rectangle(0, 0, 800, 600));
        assertEquals(new Point(100, 100), pos[0]);
        MischiefMode.set(properties, true, true);
        CursorPush.push(pointer, new Rectangle(0, 0, 800, 600));
        assertEquals(new Point(280, 100), pos[0]);
        assertEquals("supported", CursorPush.capability());
    }
    @Test public void unchangedPointerDisablesFutureAttempts() {
        CursorPush.Pointer pointer = new CursorPush.Pointer() {
            public Point location() { return new Point(100, 100); }
            public void move(Point target) { }
        };
        MischiefMode.set(properties, true, true);
        CursorPush.push(pointer, new Rectangle(0, 0, 800, 600));
        assertEquals("unsupported", CursorPush.capability());
    }
    @Test public void respectsInterval() {
        CursorPush.resetForTest();
        assertTrue(CursorPush.reserve(20000, 15000));
        assertFalse(CursorPush.reserve(34999, 15000));
        assertTrue(CursorPush.reserve(35000, 15000));
    }
}

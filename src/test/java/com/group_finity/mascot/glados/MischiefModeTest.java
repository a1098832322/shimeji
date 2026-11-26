package com.group_finity.mascot.glados;

import com.group_finity.mascot.mischief.MischiefMode;
import java.util.HashMap;
import java.util.Properties;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class MischiefModeTest {
    private final Properties properties = new Properties();
    @After public void reset() { MischiefMode.set(properties, false, false); }
    @Test public void togglingRestoresSettingsAndBehaviorWeights() {
        properties.setProperty("Breeding", "false");
        properties.setProperty("Throwing", "true");
        assertEquals(200, MischiefMode.frequency("SitDown", 200));
        MischiefMode.set(properties, true, false);
        assertEquals(50, MischiefMode.frequency("SitDown", 200));
        assertEquals(200, MischiefMode.frequency("SplitIntoTwo", 0));
        assertEquals("true", properties.getProperty("Breeding"));
        assertEquals("false", properties.getProperty("Throwing"));
        assertEquals("unsupported", MischiefMode.status().getString("throwWindow"));
        MischiefMode.set(properties, false, false);
        assertEquals("false", properties.getProperty("Breeding"));
        assertEquals("true", properties.getProperty("Throwing"));
        assertEquals(200, MischiefMode.frequency("SitDown", 200));
    }
    @Test public void bridgeCommandsRequireAnEnabledValue() {
        assertEquals(GladosCommand.SET_MISCHIEF, GladosCommand.fromWireName("setMischief"));
        assertNotNull(GladosCommand.SET_MISCHIEF.validate(new HashMap<>()));
        assertEquals(GladosCommand.MISCHIEF_RESTORE, GladosCommand.fromWireName("mischiefRestore"));
    }
}

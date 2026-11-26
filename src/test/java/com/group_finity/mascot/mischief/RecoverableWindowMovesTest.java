package com.group_finity.mascot.mischief;

import com.alibaba.fastjson.JSONObject;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class RecoverableWindowMovesTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void moveIsJournaledBeforeMutationAndRestoredOnDisableAndRestart() throws Exception {
        Path path = temporary.getRoot().toPath().resolve("mischief-restore.json");
        AtomicInteger position = new AtomicInteger(10);
        RecoverableWindowMoves.Backend backend = new RecoverableWindowMoves.Backend() {
            public JSONObject capture() { JSONObject o = new JSONObject(); o.put("x", position.get()); o.put("y", 20); o.put("width", 100); o.put("height", 80); return o; }
            public boolean move(JSONObject identity, int x, int y) throws Exception {
                assertEquals(1, new RestoreJournal(path).pending().size()); position.set(x); return true;
            }
            public boolean restore(JSONObject record) { position.set(record.getIntValue("x")); return true; }
        };
        RecoverableWindowMoves service = RecoverableWindowMoves.open(path, backend);
        service.setActive(true);
        service.move(true, 10, 20, 100, 80);
        assertEquals(310, position.get());
        service.setActive(false);
        assertEquals(10, position.get());
        assertTrue(new RestoreJournal(path).pending().isEmpty());
        service.close();
        JSONObject identity = new JSONObject(); identity.put("name", "existing");
        new RestoreJournal(path).record("window", identity, 10, 20);
        position.set(310);
        RecoverableWindowMoves reopened = RecoverableWindowMoves.open(path, backend);
        assertEquals(10, position.get());
        assertTrue(new RestoreJournal(path).pending().isEmpty());
        reopened.close();
    }

    @Test public void invalidJournalPreventsMoveAndRemainsUntouched() throws Exception {
        Path path = temporary.getRoot().toPath().resolve("mischief-restore.json");
        java.nio.file.Files.write(path, new byte[]{42});
        AtomicInteger moves = new AtomicInteger();
        RecoverableWindowMoves.Backend backend = new RecoverableWindowMoves.Backend() {
            public JSONObject capture() { return new JSONObject(); }
            public boolean move(JSONObject i, int x, int y) { moves.incrementAndGet(); return true; }
            public boolean restore(JSONObject r) { return true; }
        };
        try { RecoverableWindowMoves.open(path, backend); fail("invalid journal"); }
        catch (java.io.IOException expected) { assertEquals(0, moves.get()); }
        assertEquals(42, java.nio.file.Files.readAllBytes(path)[0]);
    }
}

package com.group_finity.mascot.mischief;

import com.alibaba.fastjson.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

public class RestoreJournalTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void recordsSurviveRestartAndOnlySuccessfulRestoresAreRemoved() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("mischief-restore.json");
        RestoreJournal journal = new RestoreJournal(file);
        JSONObject identity = new JSONObject();
        identity.put("process", "example");
        String first = journal.record("window", identity, 12, 34);
        String second = journal.record("icon", identity, -20, 42);
        RestoreJournal reopened = new RestoreJournal(file);
        assertEquals(2, reopened.pending().size());
        assertEquals(1, reopened.restorePending(entry -> first.equals(entry.getString("id"))));
        assertEquals(second, new RestoreJournal(file).pending().get(0).getString("id"));
        assertEquals(0, reopened.restorePending(entry -> { throw new IllegalStateException(); }));
        assertEquals(1, reopened.pending().size());
        assertEquals(1, reopened.restorePending(entry -> true));
        assertTrue(new RestoreJournal(file).pending().isEmpty());
    }

    @Test public void corruptJournalIsNeverOverwritten() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("mischief-restore.json");
        Files.write(file, "broken".getBytes(StandardCharsets.UTF_8));
        JSONObject identity = new JSONObject();
        identity.put("name", "x");
        try {
            new RestoreJournal(file).record("window", identity, 0, 0);
            fail("corrupt journal must prevent the move");
        } catch (java.io.IOException expected) {
            assertEquals("broken", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        }
    }
}

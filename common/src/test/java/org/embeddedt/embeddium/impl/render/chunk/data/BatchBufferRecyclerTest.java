package org.embeddedt.embeddium.impl.render.chunk.data;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchBufferRecyclerTest {
    private final List<String> deleted = new ArrayList<>();

    @Test
    void disabledDeletesAtOnceAndGivesNothing() {
        var recycler = new BatchBufferRecycler<String>();

        recycler.retire("a", this.deleted::add);

        assertEquals(List.of("a"), this.deleted);
        assertEquals(0, recycler.size());
        assertNull(recycler.take());
    }

    @Test
    void enabledHandsTheRetiredBufferBack() {
        var recycler = enabled();

        recycler.retire("a", this.deleted::add);

        assertSame("a", recycler.take());
        assertTrue(this.deleted.isEmpty());
        assertNull(recycler.take());
    }

    @Test
    void overflowIsDeletedNotHeld() {
        var recycler = enabled();

        for (int i = 0; i < BatchBufferRecycler.CAPACITY + 3; i++) {
            recycler.retire("b" + i, this.deleted::add);
        }

        assertEquals(BatchBufferRecycler.CAPACITY, recycler.size());
        assertEquals(3, this.deleted.size());
    }

    @Test
    void disablingDeletesEverythingHeld() {
        var recycler = enabled();
        recycler.retire("a", this.deleted::add);
        recycler.retire("b", this.deleted::add);

        recycler.setEnabled(false, this.deleted::add);

        assertEquals(Set.of("a", "b"), new HashSet<>(this.deleted));
        assertEquals(0, recycler.size());
        assertNull(recycler.take());
    }

    @Test
    void drainDeletesEverythingHeld() {
        var recycler = enabled();
        recycler.retire("a", this.deleted::add);

        recycler.drain(this.deleted::add);

        assertEquals(List.of("a"), this.deleted);
        assertEquals(0, recycler.size());
    }

    @Test
    void nullIsIgnored() {
        var recycler = enabled();

        recycler.retire(null, this.deleted::add);

        assertEquals(0, recycler.size());
        assertTrue(this.deleted.isEmpty());
    }

    /** Every retired buffer ends up handed out, deleted or held, and never in two of those at once. */
    @Test
    void everyBufferEndsUpExactlyOnce() {
        var random = new Random(20261021L);

        for (int run = 0; run < 200; run++) {
            var recycler = new BatchBufferRecycler<String>();
            Set<String> retired = new HashSet<>();
            Set<String> taken = new HashSet<>();
            Set<String> gone = new HashSet<>();
            int next = 0;

            for (int step = 0; step < 100; step++) {
                switch (random.nextInt(5)) {
                    case 0, 1 -> {
                        String buffer = "r" + run + "-" + next++;
                        retired.add(buffer);
                        recycler.retire(buffer, b -> assertTrue(gone.add(b), "deleted twice: " + b));
                    }
                    case 2 -> {
                        String buffer = recycler.take();
                        if (buffer != null) {
                            assertFalse(gone.contains(buffer), "handed out a deleted buffer: " + buffer);
                            assertTrue(taken.add(buffer), "handed out twice: " + buffer);
                        } else if (recycler.isEnabled()) {
                            assertEquals(0, recycler.size());
                        }
                    }
                    case 3 -> recycler.setEnabled(random.nextBoolean(), b -> assertTrue(gone.add(b), "deleted twice: " + b));
                    default -> {
                        if (!recycler.isEnabled()) {
                            assertEquals(0, recycler.size(), "a disabled recycler holds buffers");
                        }
                    }
                }

                assertTrue(recycler.size() <= BatchBufferRecycler.CAPACITY);
                assertEquals(retired.size(), taken.size() + gone.size() + recycler.size(),
                        "a buffer leaked or was counted twice");
            }

            recycler.drain(b -> assertTrue(gone.add(b), "deleted twice: " + b));
            assertEquals(retired.size(), taken.size() + gone.size(), "a buffer leaked");
        }
    }

    private static BatchBufferRecycler<String> enabled() {
        var recycler = new BatchBufferRecycler<String>();
        recycler.setEnabled(true, b -> {
            throw new AssertionError("nothing to delete yet");
        });
        return recycler;
    }
}

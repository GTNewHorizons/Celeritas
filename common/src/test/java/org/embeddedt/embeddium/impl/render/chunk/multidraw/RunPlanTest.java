package org.embeddedt.embeddium.impl.render.chunk.multidraw;

import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunPlanTest {
    private static final int MASK_COUNT = 1 << ModelQuadFacing.COUNT;

    private record Run(int first, int last) {
    }

    private static List<Run> referenceRuns(int mask) {
        List<Run> runs = new ArrayList<>();
        int facing = 0;

        while (facing < ModelQuadFacing.COUNT) {
            if ((mask & (1 << facing)) == 0) {
                facing++;
                continue;
            }

            int first = facing;
            while (facing < ModelQuadFacing.COUNT && (mask & (1 << facing)) != 0) {
                facing++;
            }
            runs.add(new Run(first, facing - 1));
        }

        return runs;
    }

    @Test
    void packRunsMatchesNaiveDecompositionForEveryMask() {
        for (int m = 0; m < MASK_COUNT; m++) {
            final int mask = m;
            List<Run> expected = referenceRuns(mask);
            long packed = BatchAssembler.packRuns(mask);

            assertEquals(expected.size(), BatchAssembler.runCount(packed), () -> "run count for mask " + Integer.toBinaryString(mask));

            for (int i = 0; i < expected.size(); i++) {
                assertEquals(expected.get(i).first(), BatchAssembler.runFirst(packed, i), "run " + i + " start for mask " + Integer.toBinaryString(mask));
                assertEquals(expected.get(i).last(), BatchAssembler.runLast(packed, i), "run " + i + " end for mask " + Integer.toBinaryString(mask));
            }
        }
    }

    @Test
    void everyMaskFitsThePackedPlan() {
        int worst = 0;

        for (int mask = 0; mask < MASK_COUNT; mask++) {
            worst = Math.max(worst, referenceRuns(mask).size());
        }

        assertEquals(4, worst, "a 7-bit mask should decompose into at most 4 runs");
    }

    @Test
    void runsCoverExactlyTheSetBits() {
        for (int mask = 0; mask < MASK_COUNT; mask++) {
            long packed = BatchAssembler.packRuns(mask);
            int covered = 0;

            for (int run = 0; run < BatchAssembler.runCount(packed); run++) {
                for (int facing = BatchAssembler.runFirst(packed, run); facing <= BatchAssembler.runLast(packed, run); facing++) {
                    covered |= 1 << facing;
                }
            }

            assertEquals(mask, covered, "runs for mask " + Integer.toBinaryString(mask));
        }
    }

    @Test
    void agreeingCornersImplyAgreementThroughoutTheRegion() {
        for (int cameraX = -40; cameraX <= 40; cameraX += 7) {
            for (int cameraY = -40; cameraY <= 40; cameraY += 11) {
                for (int cameraZ = -40; cameraZ <= 40; cameraZ += 7) {
                    assertCornerAgreementHolds(cameraX, cameraY, cameraZ, 0, 0, 0);
                    assertCornerAgreementHolds(cameraX, cameraY, cameraZ, -8, -4, -8);
                }
            }
        }
    }

    private static void assertCornerAgreementHolds(int cameraX, int cameraY, int cameraZ, int minX, int minY, int minZ) {
        int maxX = minX + RenderRegion.REGION_WIDTH - 1;
        int maxY = minY + RenderRegion.REGION_HEIGHT - 1;
        int maxZ = minZ + RenderRegion.REGION_LENGTH - 1;

        int atMin = BatchAssembler.getVisibleFaces(cameraX, cameraY, cameraZ, minX, minY, minZ);
        int atMax = BatchAssembler.getVisibleFaces(cameraX, cameraY, cameraZ, maxX, maxY, maxZ);

        if (atMin != atMax) {
            return;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    assertEquals(atMin, BatchAssembler.getVisibleFaces(cameraX, cameraY, cameraZ, x, y, z),
                            "corners agreed on " + Integer.toBinaryString(atMin) + " but section (" + x + ", " + y + ", " + z + ") disagreed for camera (" + cameraX + ", " + cameraY + ", " + cameraZ + ")");
                }
            }
        }
    }

    @Test
    void unassignedIsAlwaysVisibleSoPerSectionRunEmissionAlwaysTerminates() {
        for (int camera = -64; camera <= 64; camera += 3) {
            int mask = BatchAssembler.getVisibleFaces(camera, camera, camera, 0, 0, 0);

            assertTrue((mask & (1 << ModelQuadFacing.UNASSIGNED.ordinal())) != 0, "UNASSIGNED must always be set; the per-section do/while relies on a non-zero mask");
        }
    }
}

package com.yuno.yunosbosses.render;

import com.yuno.yunosbosses.util.ActiveBeam;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.util.math.Vec3d;

public class BeamManager {
    // List for active beams
    public static final List<ActiveBeam> ACTIVE_BEAMS = new ArrayList<>();

    public static void addBeam(UUID ownerUuid, Vec3d start, int range, int maxTicks) {
        ACTIVE_BEAMS.add(new ActiveBeam(ownerUuid, start, range, maxTicks, 0));
    }

    public static void tick() {
        for (int i = ACTIVE_BEAMS.size() - 1; i >= 0; i--) {
            ActiveBeam beam = ACTIVE_BEAMS.get(i);

            beam.incrementAge();

            if (beam.isExpired()) {
                ACTIVE_BEAMS.remove(i);
            }
        }
    }
}

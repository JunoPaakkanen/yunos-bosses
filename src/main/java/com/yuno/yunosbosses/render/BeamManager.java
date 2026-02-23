package com.yuno.yunosbosses.render;

import com.yuno.yunosbosses.util.ActiveBeam;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.Vec3d;

public class BeamManager {
    // List for active beams
    public static final List<ActiveBeam> ACTIVE_BEAMS = new ArrayList<>();

    public static void addBeam(Vec3d start, Vec3d end, int maxTicks) {
        ACTIVE_BEAMS.add(new ActiveBeam(start, end, maxTicks, 0));
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

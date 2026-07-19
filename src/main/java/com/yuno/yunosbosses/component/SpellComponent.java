package com.yuno.yunosbosses.component;

import com.yuno.yunosbosses.spell.Spell;
import net.minecraft.util.math.Vec3d;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.List;

public interface SpellComponent extends Component, AutoSyncedComponent, ServerTickingComponent {
    Spell getActiveSpell();

    @Override
    default void serverTick() {}

    void setActiveSpell(Spell spell);

    List<Spell> getKnownSpells();

    void learnSpell(Spell spell);

    void cycleSpell();

    void setCanChangeSpell(boolean value);

    boolean canChangeSpell();

    // Alternate Casting
    boolean hasAltCastWindow(Spell spell);
    void startAltCastWindow(Spell spell, int ticks);
    void clearAltCastWindow(Spell spell);

    // Projection Sorcery Data
    List<Vec3d> getProjectionImages();
    void setProjectionImages(List<Vec3d> images);
    int getProjectionIndex();
    void setProjectionIndex(int index);
    void addSpeedStack();
    int getSpeedStacks();

}

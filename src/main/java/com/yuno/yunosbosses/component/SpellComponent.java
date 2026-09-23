package com.yuno.yunosbosses.component;

import com.yuno.yunosbosses.spell.ModSpells;
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

    int getMaxSpellSlots();

    void setMaxSpellSlots(int slots);

    Spell getEquippedSpell(int slot);

    void setEquippedSpell(int slot, Spell spell);

    Spell[] getEquippedSpells();

    void setActiveSpell(Spell spell);

    List<Spell> getKnownSpells();

    void learnSpell(Spell spell);

    void cycleSpell();

    void setCanChangeSpell(boolean value);

    boolean canChangeSpell();

    // Unlocks
    boolean unlockedOpenDomain();
    void unlockOpenDomain();

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
    void setSpeedStacks(int stacks);

    // Spell Meters (Generic)
    int getMeter(Spell spell);
    void setMeter(Spell spell, int value);
    void addMeter(Spell spell, int value);
    void incrementMeter(Spell spell);

    int clamp(int newValue);
    void resetCombatState();

    // Shrine Data
    void setShrineCooldown(int ticks);
    int getShrineCooldown();
}

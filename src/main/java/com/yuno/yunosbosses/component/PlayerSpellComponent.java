package com.yuno.yunosbosses.component;

import com.yuno.yunosbosses.render.ManaHudRenderer;

import com.yuno.yunosbosses.effect.ModEffects;
import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.spell.Spell;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerSpellComponent implements SpellComponent, ServerTickingComponent {
    private Spell activeSpell;
    private final List<Spell> knownSpells = new ArrayList<>();
    private boolean canChangeSpell = true;

    // Store the player that owns this component
    private final LivingEntity player;

    // Spell Loadout data
    private int maxSpellSlots = 3; // 3 slots by default, can be increased later
    private Spell[] equippedSpells = new Spell[10]; // Hard cap of 10

    // Timer tracking for alt cast windows
    private final Map<Identifier, Integer> activeAltCasts = new HashMap<>();

    // Projection Sorcery memory
    private List<Vec3d> projectionImages = new ArrayList<>();
    private int projectionIndex = 0;
    private int projectionSpeedStacks = 0;
    private int speedStackDecayTimer = 0;
    private static final Identifier PROJECTION_SPEED_MODIFIER = Identifier.of("yunosbosses", "projection_speed_modifier");
    private int frameMeter = 0;

    // Contructor to grab the player
    public PlayerSpellComponent(LivingEntity player) {
        this.player = player;
    }

    // Logic methods

    @Override
    public int getMaxSpellSlots() {
        return this.maxSpellSlots;
    }

    @Override
    public void setMaxSpellSlots(int slots) {
        this.maxSpellSlots = Math.min(slots, 10);
        ModEntityComponents.SPELL_DATA.sync(this.player);
    }

    @Override
    public Spell getEquippedSpell(int slot) {
        if (slot >= 0 && slot < this.maxSpellSlots) {
            return this.equippedSpells[slot];
        }
        return null;
    }

    @Override
    public void setEquippedSpell(int slot, Spell spell) {
        if (slot >= 0 && slot < this.maxSpellSlots) {
            this.equippedSpells[slot] = spell;

            // Set as the active spell if the player has none selected
            if (this.activeSpell == null && spell != null) {
                this.activeSpell = spell;
            }
            ModEntityComponents.SPELL_DATA.sync(this.player);
        }
    }

    @Override
    public Spell[] getEquippedSpells() {
        return this.equippedSpells;
    }

    @Override
    public Spell getActiveSpell() {
        // If the active spell is null, return the first equipped spell
        if (this.activeSpell == null) {
            for (int i = 0; i < this.maxSpellSlots; i++) {
                if (this.equippedSpells[i] != null) {
                    this.activeSpell = this.equippedSpells[i];
                    break;
                }
            }
        }
        return this.activeSpell;
    }

    @Override
    public void setActiveSpell(Spell spell) {
        this.activeSpell = spell;
        ModEntityComponents.SPELL_DATA.sync(this.player);
    }

    @Override
    public List<Spell> getKnownSpells() {
        return this.knownSpells;
    }

    @Override
    public void learnSpell(Spell spell) {
        if (!knownSpells.contains(spell)) {
            knownSpells.add(spell);

            // Find the first empty slot within unlocked maxSpellSlots
            for (int i = 0; i < this.maxSpellSlots; i++) {
                if (this.equippedSpells[i] == null) {
                    this.equippedSpells[i] = spell;
                    break; // Stop immediately after equipping into the first empty slot
                }
            }

            // Set as the active spell
            this.activeSpell = spell;

            ModEntityComponents.SPELL_DATA.sync(this.player);
        }
    }

    @Override
    public void cycleSpell() {
        if (!canChangeSpell()) return;

        // Collect equipped spells into an active list
        List<Spell> activeLoadout = new ArrayList<>();
        for (int i = 0; i < this.maxSpellSlots; i++) {
            if (this.equippedSpells[i] != null) {
                activeLoadout.add(this.equippedSpells[i]);
            }
        }

        // If no spells are equipped, reset active spell
        if (activeLoadout.isEmpty()) {
            this.activeSpell = null;
            ModEntityComponents.SPELL_DATA.sync(this.player);
            return;
        }

        // Find the current position in the active loadout
        int currentIndex = activeLoadout.indexOf(this.activeSpell);

        // If the active spell is null or not equipped, select the first equipped spell
        if (currentIndex == -1) {
            this.activeSpell = activeLoadout.getFirst();
        } else {
            // Cycle to the next equipped spell in the sequence
            int nextIndex = (currentIndex + 1) % activeLoadout.size();
            this.activeSpell = activeLoadout.get(nextIndex);
        }

        ModEntityComponents.SPELL_DATA.sync(this.player);
    }

    // NBT Serialization

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        // Read learned spells
        NbtList list = tag.getList("KnownSpells", NbtElement.STRING_TYPE);
        knownSpells.clear();
        for (int i = 0; i < list.size(); i++) {
            knownSpells.add(ModSpells.getSpell(Identifier.of(list.getString(i))));
        }

        // Read active spell
        if (tag.contains("ActiveSpellId")) {
            Identifier id = Identifier.of(tag.getString("ActiveSpellId"));
            this.activeSpell = ModSpells.getSpell(id);
        }

        // Read active alt casts
        this.activeAltCasts.clear();
        if (tag.contains("ActiveAltCasts")) {
            NbtCompound altCastsTag = tag.getCompound("ActiveAltCasts");
            for (String key : altCastsTag.getKeys()) {
                this.activeAltCasts.put(Identifier.of(key), altCastsTag.getInt(key));
            }
        }

        // Read max slots
        if (tag.contains("MaxSpellSlots")) {
            this.maxSpellSlots = tag.getInt("MaxSpellSlots");
        }

        // Read equipped spells loadout
        this.equippedSpells = new Spell[10];
        if (tag.contains("EquippedSpells")) {
            NbtList equippedList = tag.getList("EquippedSpells", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < equippedList.size(); i++) {
                NbtCompound slotTag = equippedList.getCompound(i);
                int slot = slotTag.getInt("Slot");
                Identifier id = Identifier.of(slotTag.getString("SpellId"));

                if (slot >= 0 && slot < 10) {
                    this.equippedSpells[slot] = ModSpells.getSpell(id);
                }
            }
        }

        // Read Projection Sorcery data
        if (tag.contains("ProjectionSpeedStacks")) {
            this.projectionSpeedStacks = tag.getInt("ProjectionSpeedStacks");
        }
        if (tag.contains("FrameMeter")) {
            this.frameMeter = tag.getInt("FrameMeter");
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        // Persist the known spells
        NbtList list = new NbtList();
        for (Spell spell : knownSpells) {
            list.add(NbtString.of(spell.getId().toString()));
        }
        tag.put("KnownSpells", list);

        // Persist the active spell
        if (activeSpell != null) {
            tag.putString("ActiveSpellId", activeSpell.getId().toString());
        }

        // Persist the active alt casts
        NbtCompound altCastsTag = new NbtCompound();
        for (Map.Entry<Identifier, Integer> entry : this.activeAltCasts.entrySet()) {
            altCastsTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put("ActiveAltCasts", altCastsTag);

        // Persist max slots
        tag.putInt("MaxSpellSlots", this.maxSpellSlots);

        // Persist equippedSpells loadout
        NbtList equippedList = new NbtList();
        for (int i = 0; i < this.maxSpellSlots; i++) {
            Spell spell = this.equippedSpells[i];
            if (spell != null) {
                NbtCompound slotTag = new NbtCompound();
                slotTag.putInt("Slot", i);
                slotTag.putString("SpellId", spell.getId().toString());
                equippedList.add(slotTag);
            }
        }
        tag.put("EquippedSpells", equippedList);

        // Persist Projection Sorcery data
        tag.putInt("ProjectionSpeedStacks", this.projectionSpeedStacks);
        tag.putInt("FrameMeter", this.frameMeter);
    }

    @Override
    public void setCanChangeSpell(boolean value) { this.canChangeSpell = value;}

    @Override
    public boolean canChangeSpell() { return this.canChangeSpell; }

    @Override
    public boolean hasAltCastWindow(Spell spell) {
        return this.activeAltCasts.getOrDefault(spell.getId(), 0) > 0;
    }

    @Override
    public void startAltCastWindow(Spell spell, int ticks) {
        this.activeAltCasts.put(spell.getId(), ticks);
        ModEntityComponents.SPELL_DATA.sync(this.player);
    }

    @Override
    public void clearAltCastWindow(Spell spell) {
        this.activeAltCasts.remove(spell.getId());
        ModEntityComponents.SPELL_DATA.sync(this.player);
    }

    @Override
    public List<Vec3d> getProjectionImages() {
        return this.projectionImages;
    }

    @Override
    public void setProjectionImages(List<Vec3d> images) {
        this.projectionImages = images;
    }

    @Override
    public int getProjectionIndex() {
        return this.projectionIndex;
    }

    @Override
    public void setProjectionIndex(int index) {
        this.projectionIndex = index;
    }

    @Override
    public void addSpeedStack() {
        // Cap speed stacks at 15
        if (this.projectionSpeedStacks < 15) {
            this.projectionSpeedStacks++;
        }
        this.speedStackDecayTimer = 60;
        updateSpeedAttribute(); // Update the speed attribute
        ModEntityComponents.SPELL_DATA.sync(this.player);
    }

    public void updateSpeedAttribute() {
        EntityAttributeInstance speedAttribute = this.player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttribute == null) return;

        // Remove any old modifier first
        speedAttribute.removeModifier(PROJECTION_SPEED_MODIFIER);

        // If stacks exist, apply the speed modifier
        if (this.projectionSpeedStacks > 0) {
            double boostValue = this.projectionSpeedStacks * 0.3; // 30% per stack

            EntityAttributeModifier modifier = new EntityAttributeModifier(
                    PROJECTION_SPEED_MODIFIER,
                    boostValue,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            speedAttribute.addTemporaryModifier(modifier);
        }
    }
    @Override
    public int getSpeedStacks() {
        return this.projectionSpeedStacks;
    }

    @Override
    public void setFrameMeter(int value) {
        this.frameMeter = clamp(value);
        ModEntityComponents.SPELL_DATA.sync(this.player);
    }

    @Override
    public void incrementFrameMeter() {
        this.frameMeter = clamp(this.frameMeter + 1);
        ModEntityComponents.SPELL_DATA.sync(this.player);
    }

    @Override
    public void addFrameMeter(int value) {
        this.frameMeter = clamp(this.frameMeter + value);
        ModEntityComponents.SPELL_DATA.sync(this.player);
    }

    @Override
    public int getFrameMeter() {
        return this.frameMeter;
    }

    @Override
    public int clamp(int newValue) {
        if (newValue > 100) return 100;
        return Math.max(newValue, 0);
    }

    @Override
    public void serverTick() {
        boolean[] needsSync = {false};

        // Ticks down the alt cast windows
        this.activeAltCasts.entrySet().removeIf(entry -> {
            int newTime = entry.getValue() - 1;
            entry.setValue(newTime);

            // If the timer hits 0 (Alt cast spell expired)
            if (newTime <= 0) {
                // FAILURE CONDITION (Projection Sorcery)
                if (entry.getKey().equals(ModSpells.PROJECTION_SORCERY.getId())) {
                    // Check if the player had images left
                    if (this.projectionIndex < this.projectionImages.size()) {
                        // Get the player and apply the penalty
                        this.player.addStatusEffect(new StatusEffectInstance(ModEffects.FRAME_FREEZE, 40, 0, false, false, true));
                        this.player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 255, false, false, true));
                        // Delete speed stacks
                        this.projectionSpeedStacks = 0;
                    }
                    // Clean up the projection images
                    this.projectionImages.clear();
                    this.projectionIndex = 0;
                }

                needsSync[0] = true;
                // Clear the alt cast window when the time runs out
                return true;
            }
            return false; // Keep counting down
        });
        // If a timer expired naturally, sync the component to the client
        if (needsSync[0]) ModEntityComponents.SPELL_DATA.sync(this.player);

        if (this.speedStackDecayTimer > 0) {
            this.speedStackDecayTimer--;
            if (this.speedStackDecayTimer <= 0 && this.projectionSpeedStacks > 0) {
                this.projectionSpeedStacks--; // Lose one stack
                // Update the speed attribute
                updateSpeedAttribute();

                if (this.projectionSpeedStacks > 0) {
                    this.speedStackDecayTimer = 30; // The next stack decays in 1.5 seconds
                }
                ModEntityComponents.SPELL_DATA.sync(this.player);
            }
        }
    }
}

package com.yuno.yunosbosses.component;

import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.spell.Spell;
import net.minecraft.entity.LivingEntity;
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

    // Timer tracking for alt cast windows
    private final Map<Identifier, Integer> activeAltCasts = new HashMap<>();

    // Projection Sorcery memory
    private List<Vec3d> projectionImages = new ArrayList<>();
    private int projectionIndex = 0;

    // Contructor to grab the player
    public PlayerSpellComponent(LivingEntity player) {
        this.player = player;
    }

    // Logic methods

    @Override
    public Spell getActiveSpell() {
        return this.activeSpell;
    }

    @Override
    public void setActiveSpell(Spell spell) {
        this.activeSpell = spell;
    }

    @Override
    public List<Spell> getKnownSpells() {
        return this.knownSpells;
    }

    @Override
    public void learnSpell(Spell spell) {
        if (!knownSpells.contains(spell)) {
            knownSpells.add(spell);
        }
    }

    @Override
    public void cycleSpell() {
        if (!canChangeSpell() || knownSpells.isEmpty()) return;

        int currentIndex = knownSpells.indexOf(this.activeSpell);
        int nextIndex = (currentIndex + 1) % knownSpells.size();
        this.activeSpell = knownSpells.get(nextIndex);
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
    public void serverTick() {
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
                        this.player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 255, false, false, false));
                        this.player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 255, false, false, false));
                    }
                    // Clean up the projection images
                    this.projectionImages.clear();
                    this.projectionIndex = 0;
                }

                // Clear the alt cast window when the time runs out
                return true;
            }
            return false; // Keep counting down
        });
    }


}

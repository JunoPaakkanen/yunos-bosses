package com.yuno.yunosbosses.component;

import com.yuno.yunosbosses.spell.Spell;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class PlayerSpellComponent implements SpellComponent{
    private Spell activeSpell;
    private final List<Spell> knownSpells = new ArrayList<>();

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

    // NBT Serialization

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (tag.contains("ActiveSpellId")) {
            Identifier id = Identifier.of(tag.getString("ActiveSpellId"));
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (activeSpell != null) {
            tag.putString("ActiveSpellId", activeSpell.getId().toString());
        }
    }
}

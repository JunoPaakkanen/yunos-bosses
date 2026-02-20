package com.yuno.yunosbosses.component;

import com.yuno.yunosbosses.spell.ModSpells;
import com.yuno.yunosbosses.spell.Spell;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
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

    @Override
    public void cycleSpell() {
        if (knownSpells.isEmpty()) return;

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
    }
}

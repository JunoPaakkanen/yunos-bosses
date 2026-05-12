package com.yuno.yunosbosses.component;

import com.yuno.yunosbosses.spell.Spell;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.List;

public interface SpellComponent extends Component, AutoSyncedComponent {
    Spell getActiveSpell();

    void setActiveSpell(Spell spell);

    List<Spell> getKnownSpells();

    void learnSpell(Spell spell);

    void cycleSpell();

    void setCanChangeSpell(boolean value);

    boolean canChangeSpell();
}

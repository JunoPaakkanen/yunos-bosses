package com.yuno.yunosbosses.component;

import com.yuno.yunosbosses.spell.Spell;
import org.ladysnake.cca.api.v3.component.Component;
import java.util.List;

public interface SpellComponent extends Component {
    Spell getActiveSpell();

    void setActiveSpell(Spell spell);

    List<Spell> getKnownSpells();

    void learnSpell(Spell spell);
}

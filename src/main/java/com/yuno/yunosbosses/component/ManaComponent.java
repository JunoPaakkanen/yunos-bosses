package com.yuno.yunosbosses.component;

import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public interface ManaComponent extends Component, ServerTickingComponent {
    public float getMana();
    public float getMaxMana();
    public boolean useMana(float amount);
    public void setMana(float mana);
    public void addMana(float mana);
}

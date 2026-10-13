package com.yuno.yunosbosses.entity;

/**
 * Interface implemented by bosses that track player damage contributions
 * and grant first-defeat rewards (such as mana cap expansions).
 */
public interface YunosBossEntity {
    /**
     * Unique identifier string for this boss type (e.g. "ubel", "methode", "naoya").
     */
    String getBossIdentifier();

    /**
     * Remaining ticks that cursed energy reinforcement is shattered.
     */
    int getReinforcementBrokenTicks();

    /**
     * Sets remaining broken ticks.
     */
    void setReinforcementBrokenTicks(int ticks);

    /**
     * Whether reinforcement is currently broken.
     */
    default boolean isReinforcementBroken() {
        return getReinforcementBrokenTicks() > 0;
    }

    /**
     * Shatters reinforcement for the given duration.
     */
    void breakReinforcement(int ticks, net.minecraft.entity.damage.DamageSource source);
}

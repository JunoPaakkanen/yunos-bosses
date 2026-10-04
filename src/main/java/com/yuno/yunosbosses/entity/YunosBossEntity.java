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
}

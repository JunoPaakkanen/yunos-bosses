package com.yuno.yunosbosses.binding_vow;

import net.minecraft.util.Identifier;

import java.util.*;

public class ModBindingVows {
    public static final BindingVow GOJO = new GojoBindingVow();
    public static final BindingVow FURNACE = new FurnaceBindingVow();
    public static final BindingVow VEX_LIFE_LEECH = new VexLifeLeechBindingVow();
    public static final BindingVow BLOOD_FOR_MANA = new BloodForManaBindingVow();

    private static final Map<Identifier, BindingVow> VOWS_BY_ID = new LinkedHashMap<>();
    private static final Map<String, BindingVow> VOWS_BY_NAME = new LinkedHashMap<>();

    static {
        register(GOJO);
        register(FURNACE);
        register(VEX_LIFE_LEECH);
        register(BLOOD_FOR_MANA);
    }

    public static void register(BindingVow vow) {
        VOWS_BY_ID.put(vow.getId(), vow);
        VOWS_BY_NAME.put(vow.getId().getPath().toLowerCase(Locale.ROOT), vow);
    }

    public static BindingVow get(Identifier id) {
        return VOWS_BY_ID.get(id);
    }

    public static BindingVow get(String name) {
        if (name == null) return null;
        Identifier id = Identifier.tryParse(name);
        if (id != null && VOWS_BY_ID.containsKey(id)) {
            return VOWS_BY_ID.get(id);
        }
        return VOWS_BY_NAME.get(name.toLowerCase(Locale.ROOT));
    }

    public static Collection<BindingVow> getAll() {
        return Collections.unmodifiableCollection(VOWS_BY_ID.values());
    }

    public static Set<String> getNames() {
        return Collections.unmodifiableSet(VOWS_BY_NAME.keySet());
    }
}

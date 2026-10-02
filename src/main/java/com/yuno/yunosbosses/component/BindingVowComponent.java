package com.yuno.yunosbosses.component;

import com.yuno.yunosbosses.binding_vow.BindingVow;
import net.minecraft.util.Identifier;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.Set;

public interface BindingVowComponent extends Component, AutoSyncedComponent {

    boolean hasVow(Identifier vowId);

    default boolean hasVow(BindingVow vow) {
        return vow != null && hasVow(vow.getId());
    }

    Set<Identifier> getActiveVows();

    void addVow(Identifier vowId);

    void removeVow(Identifier vowId);

    void clearVows();
}

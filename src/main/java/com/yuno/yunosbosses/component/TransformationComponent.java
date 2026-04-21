package com.yuno.yunosbosses.component;

import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public interface TransformationComponent extends Component, AutoSyncedComponent {
    boolean isTransformed();

    void setTransformed(boolean transformed);

    void kick();
}
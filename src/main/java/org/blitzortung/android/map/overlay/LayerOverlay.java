package org.blitzortung.android.map.overlay;

public interface LayerOverlay {

    String getName();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    boolean isVisible();

    void setVisibility(boolean visible);
}

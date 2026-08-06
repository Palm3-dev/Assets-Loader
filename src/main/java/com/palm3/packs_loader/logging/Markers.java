package com.palm3.packs_loader.logging;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public enum Markers {
    INIT(create("INIT")),
    SEARCH(create("SEARCH")),
    MOVE(create("MOVE")),
    LOAD(create("LOAD")),
    EXTRACT(create("EXTRACT")),
    PATCH(create("PATCH")),
    FOLDER_CREATION(create("FOLDER_CREATION"));

    public final Marker marker;

    Markers(Marker marker) {
        this.marker = marker;
    }

    private static Marker create(String marker) {
        return MarkerFactory.getMarker(marker);
    }
}

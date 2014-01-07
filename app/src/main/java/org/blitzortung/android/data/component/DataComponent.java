package org.blitzortung.android.data.component;

import org.blitzortung.android.data.beans.AbstractStroke;
import org.blitzortung.android.data.beans.Station;

import fj.data.Array;

public class DataComponent {
    private final Array<AbstractStroke> strokes;
    private final Array<Station> participants;
    private final int[] histogram;

    public DataComponent(Array<AbstractStroke> strokes, Array<Station> participants, int[] histogram) {
        this.strokes = strokes;
        this.participants = participants;
        this.histogram = histogram;
    }

    public Array<AbstractStroke> getStrokes() {
        return strokes;
    }

    public Array<Station> getParticipants() {
        return participants;
    }

    public int[] getHistogram() {
        return histogram;
    }
}

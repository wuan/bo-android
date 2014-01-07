package org.blitzortung.android.data.component;

import org.blitzortung.android.data.beans.Station;

import java.util.Collection;

import fj.data.Array;

public class ParticipantsComponent {
    private Array<Station> participants;

    public ParticipantsComponent() {
        participants = Array.empty();
    }

    public void setParticipants(Array<Station> participants) {
        this.participants = participants;
    }

    public Collection<Station> getParticipants() {
        return participants.toCollection();
    }

    public void clear() {
       participants = Array.empty();
    }
}

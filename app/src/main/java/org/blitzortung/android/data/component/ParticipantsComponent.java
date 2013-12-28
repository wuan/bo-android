package org.blitzortung.android.data.component;

import org.blitzortung.android.data.beans.Station;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ParticipantsComponent {
    private final List<Station> participants;

    public ParticipantsComponent() {
        participants = new ArrayList<Station>();
    }

    public void setParticipants(Collection<Station> participants) {
        this.participants.clear();
        this.participants.addAll(participants);
    }

    public Collection<Station> getParticipants() {
        return participants;
    }

    public void clear() {
       participants.clear();
    }
}

package org.blitzortung.android.data;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode
@ToString
public class TaskParameters {
    private final Parameters parameters;
    private final boolean updateParticipants;
}

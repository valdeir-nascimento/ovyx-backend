package io.github.ovyx.identity.application;

import io.github.ovyx.identity.domain.model.AccessEvent;
import io.github.ovyx.identity.domain.model.AccessOutcome;
import io.github.ovyx.identity.domain.port.AccessEventRecorder;
import java.util.ArrayList;
import java.util.List;

/** Dublê do gravador de auditoria, que guarda o que foi registrado para o teste inspecionar. */
public final class RecordingAccessEventRecorder implements AccessEventRecorder {

    private final List<AccessEvent> recorded = new ArrayList<>();

    @Override
    public void record(AccessEvent event) {
        recorded.add(event);
    }

    public List<AccessEvent> recorded() {
        return List.copyOf(recorded);
    }

    public AccessOutcome lastOutcome() {
        if (recorded.isEmpty()) {
            return null;
        }
        return recorded.getLast().outcome();
    }
}

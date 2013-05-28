package platform.server.logics.property;

import platform.server.logics.property.actions.BaseEvent;
import platform.server.logics.property.actions.SessionEnvEvent;
import platform.server.logics.property.actions.SystemEvent;

public class Event {
    public final BaseEvent base;
    public final SessionEnvEvent session;

    public static final Event APPLY = new Event(SystemEvent.APPLY, SessionEnvEvent.ALWAYS);
    public static final Event SESSION = new Event(SystemEvent.SESSION, SessionEnvEvent.ALWAYS);

    public Event(BaseEvent base, SessionEnvEvent session) {
        this.base = base;
        this.session = session;
    }

    @Override
    public String toString() {
        return base + "," + session;
    }
}

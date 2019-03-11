package lsfusion.erp.daemon;

import lsfusion.interop.event.EventBus;

public abstract class AbstractDaemonListener {
    protected EventBus eventBus;

    public abstract String start() throws Exception;

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }
}
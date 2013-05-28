package platform.gwt.base.server.spring;

import platform.interop.RemoteLogicsInterface;

import java.util.TimeZone;

public interface BusinessLogicsProvider<T extends RemoteLogicsInterface> {
    String getRegistryHost();
    int getRegistryPort();
    String getExportName();

    TimeZone getTimeZone();

    T getLogics();

    void invalidate();

    void addInvalidateListener(InvalidateListener listener);
    void removeInvalidateListener(InvalidateListener listener);
}

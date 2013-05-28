package platform.gwt.form.shared.view.reader;

import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;

import java.io.Serializable;
import java.util.Map;

public interface GPropertyReader extends Serializable {
    void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values, boolean updateKeys);
    int getGroupObjectID();
}

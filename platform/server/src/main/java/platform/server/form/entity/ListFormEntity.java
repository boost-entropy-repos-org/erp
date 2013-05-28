package platform.server.form.entity;

import platform.server.classes.CustomClass;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;

public class ListFormEntity<T extends BusinessLogics<T>> extends BaseClassFormEntity<T> {

    protected ListFormEntity(BaseLogicsModule<T> LM, CustomClass cls, String sID, String caption) {
        super(LM, cls, sID, caption);

        LM.addObjectActions(this, object);
    }

    public ListFormEntity(BaseLogicsModule<T> LM, CustomClass cls) {
        this(LM, cls, "listForm_" + cls.getSID(), cls.caption);
    }
}

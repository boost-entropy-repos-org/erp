package platform.server.logics.property;

import platform.base.col.interfaces.immutable.ImOrderSet;

abstract public class FormulaProperty<T extends PropertyInterface> extends NoIncrementProperty<T> {

    protected FormulaProperty(String sID, String caption, ImOrderSet<T> interfaces) {
        super(sID, caption, interfaces);
    }

    @Override
    public boolean check() {
        return true;
    }
}

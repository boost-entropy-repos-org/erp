package platform.server.logics.property;

import platform.base.TwinImmutableObject;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;

public class ActionPropertyImplement<P extends PropertyInterface, T> extends TwinImmutableObject {
    public ActionProperty<P> property;
    public ImMap<P, T> mapping;

    public String toString() {
        return property.toString();
    }

    public ActionPropertyImplement(ActionProperty<P> property, ImMap<P, T> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public ActionPropertyImplement(ActionProperty<P> property) {
        this.property = property;
        mapping = MapFact.EMPTY();
    }

    public <L> ActionPropertyImplement<P, L> mapImplement(ImMap<T, L> mapImplement) {
        return new ActionPropertyImplement<P, L>(property, mapping.join(mapImplement));
    }

    public boolean twins(TwinImmutableObject o) {
        return property.equals(((ActionPropertyImplement) o).property) && mapping.equals(((ActionPropertyImplement) o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }
}

package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;

// только с интерфейсом хэширования, нужен в группировках на "стыке" внешнего и внутреннего контекста 
public interface InnerHashContext {

    public abstract int hashInner(HashContext hashContext);

    public abstract ImSet<ParamExpr> getInnerKeys();
    public abstract ImSet<Value> getInnerValues();

    // строит hash с точностью до перестановок
    public int hashValues(HashValues hashValues);
    public BaseUtils.HashComponents<ParamExpr> getComponents(final HashValues hashValues);
}

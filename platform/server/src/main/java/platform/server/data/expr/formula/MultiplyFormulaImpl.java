package platform.server.data.expr.formula;

import platform.server.data.expr.formula.conversion.*;
import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

public class MultiplyFormulaImpl extends ArithmeticFormulaImpl {

    public MultiplyFormulaImpl() {
        super(IntegralTypeConversion.instance, MultiplyConversionSource.instance);
    }

    @Override
    public String getOperationName() {
        return "multiplication";
    }

    private static class MultiplyConversionSource extends AbstractConversionSource {
        public final static MultiplyConversionSource instance = new MultiplyConversionSource();

        protected MultiplyConversionSource() {
            super(IntegralTypeConversion.instance);
        }

        @Override
        public String getSource(CompileSource compile, Type type1, Type type2, String src1, String src2) {
            Type type = conversion.getType(type1, type2);
            if (type != null) {
                return "(" + src1 + "*" + src2 + ")";
            }
            return null;
        }
    }
}

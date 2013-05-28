package platform.client.logics.classes;

import platform.client.form.PropertyEditor;
import platform.client.form.PropertyRenderer;
import platform.client.form.renderer.IntegerPropertyRenderer;
import platform.client.logics.ClientPropertyDraw;

import java.text.*;

abstract public class ClientIntegralClass extends ClientDataClass {

    protected ClientIntegralClass() {
    }

    @Override
    public String getMinimumMask() {
        return "99 999 999";
    }

    public String getPreferredMask() {
        return "99 999 999";
    }

    public Format getDefaultFormat() {
        NumberFormat format = new DecimalFormat() {
            @Override
            public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
                if (obj == null) {
                    try {
                        return super.formatToCharacterIterator(parseString("0"));
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    return super.formatToCharacterIterator(obj);
                }
            }
        }; // временно так чтобы устранить баг, но теряется locale, NumberFormat.getInstance()
        format.setGroupingUsed(true);
        return format;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new IntegerPropertyRenderer(property);
    }

    public abstract PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property);
}

package platform.client.logics.classes;

import platform.interop.Compare;

import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;

import static platform.interop.Compare.EQUALS;
import static platform.interop.Compare.NOT_EQUALS;

public abstract class ClientFileClass extends ClientDataClass implements ClientTypeClass {

    public final boolean multiple;
    public final boolean storeName;

    protected ClientFileClass(boolean multiple, boolean storeName) {
        this.multiple = multiple;
        this.storeName = storeName;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeBoolean(multiple);
        outStream.writeBoolean(storeName);
    }

    public String getSID() {
        return getFileSID() + (multiple ? "_Multiple" : "") + (storeName ? "_StoreName" : "");
    }

    public abstract String getFileSID();

    public abstract String[] getExtensions();

    public String getPreferredMask() {
        return "1234567";
    }

    public Format getDefaultFormat() {
        return null;
    }

    public Object parseString(String s) throws ParseException {
        throw new RuntimeException("not supported");
    }

    @Override
    public Compare[] getFilterCompares() {
        return new Compare[] {EQUALS, NOT_EQUALS};
    }

    @Override
    public Compare getDefaultCompare() {
        return EQUALS;
    }
}
package platform.client.logics.classes;

import platform.interop.Data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientObjectClass extends ClientClass {

    public final static ClientObjectType type = new ClientObjectType();

    private final int ID;
    private final String sID;

    private final boolean concreate;
    private final List<ClientObjectClass> children;
    private final String caption;

    private ClientObjectClass(int ID, String sID, String caption, boolean concreate, List<ClientObjectClass> children) {
        this.ID = ID;
        this.sID = sID;
        this.concreate = concreate;
        this.children = children;
        this.caption = caption;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(Data.OBJECT);
        outStream.writeInt(ID);
    }

    public int getID() {
        return ID;
    }

    @Override
    public String getSID(){
        return sID;
    }

    public boolean isConcreate() {
        return concreate;
    }

    public String getCaption() {
        return caption;
    }

    @Override
    public String getCode() {
        return sID;
    }

    public List<ClientObjectClass> getChildren() {
        return children;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public ClientType getType() {
        return type;
    }

    public boolean equals(Object o) {
        return this == o || o instanceof ClientObjectClass && ID == ((ClientObjectClass) o).ID;
    }

    @Override
    public int hashCode() {
        return ID;
    }

    public String toString() {
        return caption;
    }

    public static ClientObjectClass deserialize(DataInputStream inStream) throws IOException {
        boolean concreate = inStream.readBoolean();
        String caption = inStream.readUTF();
        int ID = inStream.readInt();
        String sID = inStream.readUTF();

        int count = inStream.readInt();
        List<ClientObjectClass> children = new ArrayList<ClientObjectClass>();
        for (int i = 0; i < count; i++) {
            children.add(ClientTypeSerializer.deserializeClientObjectClass(inStream));
        }

        return new ClientObjectClass(ID, sID, caption, concreate, children);
    }
}

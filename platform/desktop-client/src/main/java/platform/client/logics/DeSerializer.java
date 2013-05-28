package platform.client.logics;

import platform.client.logics.classes.ClientClass;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.client.navigator.ClientAbstractWindow;
import platform.client.navigator.ClientNavigatorElement;
import platform.client.navigator.ClientNavigatorWindow;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

public class DeSerializer {

    public static class NavigatorData {
        public final ClientNavigatorElement root;
        public final Map<String, ClientNavigatorWindow> windows;

        public NavigatorData(ClientNavigatorElement root,Map<String, ClientNavigatorWindow> windows) {
            this.root = root;
            this.windows = windows;
        }
    }

    public static NavigatorData deserializeListClientNavigatorElementWithChildren(byte[] state) throws IOException {
        DataInputStream inStream = new DataInputStream(new ByteArrayInputStream(state));

        Map<String, ClientNavigatorWindow> windows = new HashMap<String, ClientNavigatorWindow>();
        List<ClientNavigatorElement> elements = deserializeListClientNavigatorElement(inStream, windows);

        Map<String, ClientNavigatorElement> elementsMap = new HashMap<String, ClientNavigatorElement>();
        for (ClientNavigatorElement element : elements) {
            elementsMap.put(element.getSID(), element);
        }

        for (ClientNavigatorElement element : elements) {
            if (element.window != null) {
                element.window.elements.add(element);
            }
            int cnt = inStream.readInt();
            for (int i = 0; i < cnt; i++) {
                ClientNavigatorElement child = elementsMap.get(inStream.readUTF());

                element.children.add(child);
                child.parents.add(element);
            }
        }

        return new NavigatorData(elements.get(0), windows);
    }

    public static List<ClientNavigatorElement> deserializeListClientNavigatorElement(byte[] state, Map<String, ClientNavigatorWindow> windows) throws IOException {
        return deserializeListClientNavigatorElement(new DataInputStream(new ByteArrayInputStream(state)), windows);
    }

    private static List<ClientNavigatorElement> deserializeListClientNavigatorElement(DataInputStream dataStream, Map<String, ClientNavigatorWindow> windows) throws IOException {
        List<ClientNavigatorElement> listElements = new ArrayList<ClientNavigatorElement>();
        int elementsCount = dataStream.readInt();
        for (int i = 0; i < elementsCount; i++) {
            listElements.add(ClientNavigatorElement.deserialize(dataStream, windows));
        }
        return listElements;
    }

    public static List<ClientAbstractWindow> deserializeListClientNavigatorWindow(byte[] state) throws IOException {
        List<ClientAbstractWindow> windows = new ArrayList<ClientAbstractWindow>();
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(state));
        for (int i = 0; i < 5; i++) {
            windows.add(new ClientAbstractWindow(dataStream));
        }
        return windows;
    }

    public static List<ClientClass> deserializeListClientClass(byte[] state) throws IOException {

        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(state));
        List<ClientClass> classes = new ArrayList<ClientClass>();
        int count = dataStream.readInt();
        for (int i = 0; i < count; i++)
            classes.add(ClientTypeSerializer.deserializeClientClass(dataStream));
        return classes;
    }

}

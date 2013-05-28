package platform.interop.form.layout;

public interface AbstractContainer<C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> extends AbstractComponent<C, T> {

    void setTitle(String caption);
    void setDescription(String description);
    void setSID(String sID);
    void setType(byte type);

    void add(T child);
}

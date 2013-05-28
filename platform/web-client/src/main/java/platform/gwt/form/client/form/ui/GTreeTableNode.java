package platform.gwt.form.client.form.ui;

import platform.gwt.form.shared.view.GGroupObject;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;

import java.util.ArrayList;
import java.util.List;

public class GTreeTableNode {
    private GGroupObject group;
    private GGroupObjectValue key;
    private GTreeTableNode parent;
    private List<GTreeTableNode> children;
    private boolean open = false;

    public GTreeTableNode() {
        this(null, GGroupObjectValue.EMPTY);
    }

    public GTreeTableNode(GGroupObject group, GGroupObjectValue key) {
        this.group = group;
        this.key = key;
        children = new ArrayList<GTreeTableNode>();
    }

    public GGroupObject getGroup() {
        return group;
    }

    public GGroupObjectValue getKey() {
        return key;
    }

    public GTreeTableNode getParent() {
        return parent;
    }

    public List<GTreeTableNode> getChildren() {
        return children;
    }

    public GTreeTableNode getChild(int index) {
        return children.get(index);
    }

    public void setParent(GTreeTableNode parent) {
        this.parent = parent;
    }

    public void addNode(GTreeTableNode child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeNode(GTreeTableNode child) {
        int index = children.indexOf(child);
        if (index > -1) {
            removeNode(index);
        }
    }

    public void removeNode(int index) {
        children.remove(index);
    }

    public void removeFromParent() {
        if (parent != null) {
            parent.removeNode(this);
        }
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}

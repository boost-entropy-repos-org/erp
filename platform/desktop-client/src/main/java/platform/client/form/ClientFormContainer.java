package platform.client.form;

import platform.client.SwingUtils;
import platform.client.logics.ClientContainer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

import static platform.client.SwingUtils.getNewBoundsIfNotAlmostEquals;

class ClientFormContainer extends JPanel implements AutoHideableContainer {

    public final ClientContainer key;

    public ClientFormContainer(ClientContainer key) {

        this.key = key;

        setOpaque(false);

        key.design.designComponent(this);

        setSizes();
    }

    @Override
    public void add(Component comp, Object constraints) {
        SimplexLayout.showHideableContainers(this);
        super.add(comp, constraints);
    }

    @Override
    public String toString() {
        return key.toString();
    }

    public String getTitle() {
        return key.getTitle();
    }

    public void addBorder() {
        String title = getTitle();
        if (title != null) {
            TitledBorder border = BorderFactory.createTitledBorder(title);
            setBorder(border);
        }
    }

    private void setSizes() {
        if (key.minimumSize != null)
            setMinimumSize(SwingUtils.getOverridedSize(getMinimumSize(), key.minimumSize));
        if (key.preferredSize != null)
            setPreferredSize(SwingUtils.getOverridedSize(getPreferredSize(), key.preferredSize));
        if (key.maximumSize != null)
            setMaximumSize(SwingUtils.getOverridedSize(getMaximumSize(), key.maximumSize));
    }

    //Чтобы лэйаут не прыгал игнорируем мелкие изменения координат
    @Override
    public void setBounds(int x, int y, int width, int height) {
        Rectangle newBounds = getNewBoundsIfNotAlmostEquals(this, x, y, width, height);
        super.setBounds(newBounds.x, newBounds.y, newBounds.width,  newBounds.height);
    }
}
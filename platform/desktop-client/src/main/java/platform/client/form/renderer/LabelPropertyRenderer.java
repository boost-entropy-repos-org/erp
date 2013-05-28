package platform.client.form.renderer;

import platform.client.form.PropertyRenderer;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.Format;

public abstract class LabelPropertyRenderer extends JLabel implements PropertyRenderer {
    protected Format format;

    private Color defaultBackground = Color.WHITE;

    protected LabelPropertyRenderer(ClientPropertyDraw property) {
        super();
        if (property != null) {
            format = property.getFormat();
            setOpaque(true);
            property.design.designCell(this);
            defaultBackground = getBackground();
        }
    }

    void setSelected(boolean isSelected, boolean hasFocus) {
        drawBackground(isSelected, hasFocus);
        drawBorder(isSelected, hasFocus);
    }

    protected void drawBorder(boolean isSelected, boolean hasFocus) {
        if (isSelected) {
            if (hasFocus) {
                setBorder(BorderFactory.createCompoundBorder(PropertyRenderer.FOCUSED_CELL_BORDER, BorderFactory.createEmptyBorder(0, 1, 0, 1)));
            } else {
                setBorder(new EmptyBorder(2, 2, 2, 2));
                setBorder(BorderFactory.createCompoundBorder(PropertyRenderer.SELECTED_ROW_BORDER, BorderFactory.createEmptyBorder(1, 2, 1, 2)));
            }
        } else {
            setBorder(new EmptyBorder(1, 2, 1, 2));
        }
    }

    protected void drawBackground(boolean isSelected, boolean hasFocus) {
        if (isSelected) {
            if (hasFocus) {
                setBackground(PropertyRenderer.FOCUSED_CELL_BACKGROUND);
            } else {
                setBackground(PropertyRenderer.SELECTED_ROW_BACKGROUND);
            }
        } else {
            setBackground(defaultBackground);
        }
    }

    public void paintAsSelected() {
        setBackground(PropertyRenderer.SELECTED_CELL_BACKGROUND);
    }
}

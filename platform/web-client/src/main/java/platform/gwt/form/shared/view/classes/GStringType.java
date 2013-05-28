package platform.gwt.form.shared.view.classes;

import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.StringGridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.StringGridCellRenderer;

public class GStringType extends GAbstractStringType {
    protected int length = 50;

    private String minimumMask;
    private String preferredMask;

    public GStringType() {}

    public GStringType(int length) {
        this(length, false);
    }

    public GStringType(int length, boolean caseInsensitive) {
        super(caseInsensitive);

        this.length = length;

        minimumMask = GwtSharedUtils.replicate('0', correctMinimumCharWidth(length));
        preferredMask = GwtSharedUtils.replicate('0', correctPreferredCharWidth(length));
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new StringGridCellRenderer(property, false);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new StringGridCellEditor(editManager, editProperty, false);
    }

    @Override
    public String getMinimumMask() {
        return minimumMask;
    }

    public String getPreferredMask() {
        return preferredMask;
    }

    private int correctMinimumCharWidth(int charWidth) {
        return charWidth <= 3
                ? charWidth
                : charWidth <= 40
                    ? (int) Math.round(Math.pow(charWidth, 0.87))
                    : charWidth <= 80
                        ? (int) Math.round(Math.pow(charWidth, 0.7))
                        : (int) Math.round(Math.pow(charWidth, 0.65));
    }

    private int correctPreferredCharWidth(int charWidth) {
        return charWidth <= 20
                ? charWidth
                : charWidth <= 80
                    ? (int) Math.round(Math.pow(charWidth, 0.8))
                    : (int) Math.round(Math.pow(charWidth, 0.7));
    }

    @Override
    public int getMinimumCharWidth(int definedMinimumCharWidth) {
        return definedMinimumCharWidth > 0 ? correctMinimumCharWidth(definedMinimumCharWidth) : minimumMask.length();
    }

    @Override
    public int getPreferredCharWidth(int definedPreferredCharWidth) {
        return definedPreferredCharWidth > 0 ? correctPreferredCharWidth(definedPreferredCharWidth) : preferredMask.length();
    }

    @Override
    public String toString() {
        return "Строка" + (caseInsensitive ? " без регистра" : "") + "(" + length + ")";
    }
}

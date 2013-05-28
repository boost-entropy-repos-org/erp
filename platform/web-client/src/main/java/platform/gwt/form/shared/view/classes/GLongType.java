package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.editor.LongGridCellEditor;

import java.text.ParseException;

public class GLongType extends GIntegralType {
    public static GLongType instance = new GLongType();

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new LongGridCellEditor(editManager, editProperty);
    }

    @Override
    public Object parseString(String s) throws ParseException {
        try {
            return s.isEmpty() ? null : Long.parseLong(s.replaceAll(" ", ""));
        } catch (NumberFormatException e) {
            throw new ParseException("string " + s + "can not be converted to long", 0);
        }
    }

    @Override
    public String toString() {
        return "Длинное целое число";
    }
}

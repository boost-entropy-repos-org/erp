package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style;
import platform.gwt.base.client.EscapeUtils;
import platform.gwt.cellview.client.cell.Cell;

public class ColorGridCellRenderer extends AbstractGridCellRenderer {

    @Override
    public void renderDom(Cell.Context context, DivElement cellElement, Object value) {
        String color = getColorValue(value);

        cellElement.setInnerText(EscapeUtils.UNICODE_NBSP);

        cellElement.getStyle().setBorderWidth(0, Style.Unit.PX);

        updateElement(cellElement, color);
    }

    @Override
    public void updateDom(DivElement cellElement, Cell.Context context, Object value) {
        String color = getColorValue(value);
        updateElement(cellElement, color);
    }

    private void updateElement(DivElement div, String colorValue) {
        div.getStyle().setColor(colorValue);
        div.getStyle().setBackgroundColor(colorValue);
        div.setTitle(colorValue);
    }

    private String getColorValue(Object value) {
        return value == null ? "" : value.toString();
    }
}

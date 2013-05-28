package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.cellview.client.cell.Cell;

public class ImageGridCellRenderer extends AbstractGridCellRenderer {
    @Override
    public void renderDom(Cell.Context context, DivElement cellElement, Object value) {
        cellElement.setAttribute("align", "center");

        ImageElement img = cellElement.appendChild(Document.get().createImageElement());
        img.getStyle().setVerticalAlign(Style.VerticalAlign.MIDDLE);
        if (value instanceof String) {
            img.setSrc(imageSrc(value));
        }
    }

    @Override
    public void updateDom(DivElement cellElement, Cell.Context context, Object value) {
        ImageElement img = cellElement.getFirstChild().cast();
        if (value instanceof String) {
            img.setSrc(imageSrc(value));
        }
    }

    private String imageSrc(Object value) {
        return GwtClientUtils.getWebAppBaseURL() + "propertyImage?sid=" + value;
    }
}

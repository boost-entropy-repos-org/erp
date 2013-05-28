package platform.gwt.form.shared.view.window;

import platform.gwt.form.client.navigator.GINavigatorController;
import platform.gwt.form.client.navigator.GMenuNavigatorView;
import platform.gwt.form.client.navigator.GNavigatorView;

public class GMenuNavigatorWindow extends GNavigatorWindow {
    public int showLevel;
    public int orientation;

    @Override
    public GNavigatorView createView(GINavigatorController navigatorController) {
        return new GMenuNavigatorView(this, navigatorController);
    }
}

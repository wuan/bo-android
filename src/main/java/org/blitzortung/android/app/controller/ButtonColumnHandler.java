package org.blitzortung.android.app.controller;

import android.view.View;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ButtonColumnHandler<V extends View> {

    private List<V> elements;

    private final RelativeLayout parentLayout;

    public ButtonColumnHandler(RelativeLayout parentLayout) {
        this.parentLayout = parentLayout;

        elements = new ArrayList<V>();
    }

    public void addElement(V element) {
        elements.add(element);
    }

    public void addAllElements(Collection<V> elements) {
        this.elements.addAll(elements);
    }

    public void disableButtonColumn() {
        setEnableForButtonColumn(false);
    }

    public void enableButtonColumn() {
        setEnableForButtonColumn(true);
    }

    private void setEnableForButtonColumn(boolean enabled) {
        for (V element : elements) {
            element.setEnabled(enabled);
        }
    }

    public void updateButtonColumn() {
        int previousIndex = -1;
        for (int currentIndex = 0; currentIndex < elements.size(); currentIndex++) {
            V element = elements.get(currentIndex);
            if (element.getVisibility() == View.VISIBLE) {
                //RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) element.getLayoutParams();
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                lp.width = 70;
                lp.height = 70;
                lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
                if (previousIndex < 0) {
                    lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 1);
                } else {
                    lp.addRule(RelativeLayout.BELOW, elements.get(previousIndex).getId());
                }
                element.setLayoutParams(lp);
                previousIndex = currentIndex;
            }
        }
    }
}

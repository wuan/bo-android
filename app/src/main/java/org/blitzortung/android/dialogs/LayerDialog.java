package org.blitzortung.android.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.Overlay;
import org.blitzortung.android.app.R;
import org.blitzortung.android.map.OwnMapView;
import org.blitzortung.android.map.overlay.LayerOverlay;

import java.util.ArrayList;
import java.util.List;

public class LayerDialog extends AlertDialog {

    private int[] backgroundColors = {0x666, 0x555};

    private int currentRow = 0;

    public LayerDialog(Context context, OwnMapView mapView) {
        super(context);

        setTitle(getContext().getResources().getText(R.string.layers));
        View dialog = getLayoutInflater().inflate(R.layout.layer_dialog, null);

        int layer_list_id = R.id.layer_list;
        ListView tableLayout = (ListView) dialog.findViewById(layer_list_id);

        List<LayerOverlay> layers = new ArrayList<LayerOverlay>();

        for (Overlay overlay : mapView.getOverlays()) {
            if (overlay instanceof LayerOverlay) {
                layers.add((LayerOverlay) overlay);
            }
        }
        tableLayout.setAdapter(new LayerAdapter(getContext(), layers.toArray(new LayerOverlay[layers.size()])));

        tableLayout.setBackgroundColor(backgroundColors[0]);
        setView(dialog);
    }

    private void addRow(TableLayout tableLayout, LayerOverlay layerOverlay) {
        TableRow tableRow = new TableRow(getContext());

        TextView layerName = new TextView(getContext());
        layerName.setText(layerOverlay.getName());
        tableRow.addView(layerName);

        CheckBox visible = new CheckBox(getContext());
        visible.setChecked(layerOverlay.isVisible());
        tableRow.addView(visible);

        tableRow.setBackgroundColor(getRowBackgroundColor());
        tableRow.getBackground().setAlpha(40);

        tableLayout.addView(tableRow);
    }

    private void addTitleRow(TableLayout tableLayout) {
        TableRow titleRow = new TableRow(getContext());
        TextView layerNameTitle = new TextView(getContext());
        layerNameTitle.setText(getContext().getResources().getText(R.string.layer_name));
        titleRow.addView(layerNameTitle);

        TextView visibleCheckbox = new TextView(getContext());
        visibleCheckbox.setText(getContext().getResources().getText(R.string.visible));
        titleRow.addView(visibleCheckbox);

        titleRow.setBackgroundColor(getRowBackgroundColor());

        tableLayout.addView(titleRow);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            dismiss();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    public int getRowBackgroundColor() {
        int backgroundColor = backgroundColors[currentRow % 2];
        currentRow += 1;
        return backgroundColor;
    }
}

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

public class LayerDialog extends AlertDialog {

    public LayerDialog(Context context, OwnMapView mapView) {
        super(context);

        setTitle(context.getResources().getText(R.string.layers));
        View dialog = getLayoutInflater().inflate(R.layout.layer_dialog, null);

        int layer_table_id = R.id.layer_table;
        TableLayout tableLayout = (TableLayout) dialog.findViewById(layer_table_id);

        TableRow titleRow = new TableRow(context);
        TextView layerNameTitle = new TextView(context);
        layerNameTitle.setText(context.getResources().getText(R.string.layer_name));
        titleRow.addView(layerNameTitle);

        TextView visibleCheckbox = new TextView(context);
        visibleCheckbox.setText(context.getResources().getText(R.string.visible));
        titleRow.addView(visibleCheckbox);

        titleRow.setBackgroundColor(0);
        tableLayout.addView(titleRow);


        for (Overlay overlay : mapView.getOverlays()) {
            if (overlay instanceof LayerOverlay) {
                LayerOverlay layerOverlay = (LayerOverlay) overlay;
                TableRow tableRow = new TableRow(context);

                TextView layerName = new TextView(context);
                layerName.setText(layerOverlay.getName());
                tableRow.addView(layerName);

                CheckBox visible = new CheckBox(context);
                visible.setChecked(layerOverlay.isVisible());
                tableRow.addView(visible);

                tableLayout.addView(tableRow);
            }
        }

        setView(dialog);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            dismiss();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}

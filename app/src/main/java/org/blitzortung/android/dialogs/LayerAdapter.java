package org.blitzortung.android.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import org.blitzortung.android.app.R;
import org.blitzortung.android.map.overlay.LayerOverlay;

public class LayerAdapter extends ArrayAdapter<LayerOverlay> {

    private final Context context;
    private final LayerOverlay[] layers;

    public LayerAdapter(Context context, LayerOverlay[] layers) {
        super(context, R.layout.layer_list_row, layers);
        this.context = context;
        this.layers = layers;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.layer_list_row, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.layerName);
        CheckBox imageView = (CheckBox) rowView.findViewById(R.id.layerVisible);
        textView.setText(layers[position].getName());
        imageView.setChecked(layers[position].isVisible());

        return rowView;
    }
}

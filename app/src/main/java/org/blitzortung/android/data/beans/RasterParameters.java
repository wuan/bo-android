package org.blitzortung.android.data.beans;

import android.graphics.Point;
import android.graphics.RectF;

import com.google.android.maps.Projection;

import org.blitzortung.android.data.Coordsys;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RasterParameters {

    private final float longitudeStart;
    private final float latitudeStart;
    private final float longitudeDelta;
    private final float latitudeDelta;
    private final int longitudeBins;
    private final int latitudeBins;
    private final String info;

    public float getRectCenterLongitude() {
        return longitudeStart + longitudeDelta * longitudeBins / 2f;
    }

    public float getRectCenterLatitude() {
        return latitudeStart - latitudeDelta * latitudeBins / 2f;
    }

    public float getCenterLongitude(int offset) {
        return longitudeStart + longitudeDelta * (offset + 0.5f);
    }

    public float getCenterLatitude(int offset) {
        return latitudeStart - latitudeDelta * (offset + 0.5f);
    }

    public float getRectLongitudeDelta() {
        return longitudeDelta * longitudeBins;
    }

    public float getRectLatitudeDelta() {
        return latitudeDelta * latitudeBins;
    }

    public RectF getRect(Projection projection) {
        Point leftTop = new Point();
        leftTop = projection.toPixels(
                Coordsys.toMapCoords(longitudeStart, latitudeStart), leftTop);
        Point bottomRight = new Point();
        bottomRight = projection.toPixels(
                Coordsys.toMapCoords(longitudeStart + longitudeBins * longitudeDelta,
                        latitudeStart - latitudeBins * latitudeDelta), bottomRight);
        return new RectF(leftTop.x, leftTop.y, bottomRight.x, bottomRight.y);
    }

    public int getLongitudeIndex(double longitude) {
        return (int) ((longitude - longitudeStart) / longitudeDelta + 0.5);
    }

    public int getLatitudeIndex(double latitude) {
        return (int) ((latitudeStart - latitude) / latitudeDelta + 0.5);
    }
}

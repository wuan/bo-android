package org.blitzortung.android.map.overlay.color;

public enum ColorScheme {
    BLITZORTUNG(new int[]{ 0xffe4f9f9, 0xffd8f360, 0xffdfbc51, 0xffe48044, 0xffe73c3b, 0xffb82e2d }),
    RAINBOW(new int[]{ 0xffff0000, 0xffff9900, 0xffffff00, 0xff99ff22, 0xff00ffff, 0xff6699ff });

    private final int[] colors;

    private ColorScheme(int[] colors) {
        this.colors = colors;
    }

    public int[] getStrokeColors()
    {
        return colors;
    }
}

package org.blitzortung.android.map.overlay.color;

public enum ColorScheme {
    BLITZORTUNG(new int[]{ 0xffffffff, 0xffffff00, 0xffffaa00, 0xffff6600, 0xffbb4400, 0xff882200 }),
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

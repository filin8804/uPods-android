package com.chickenkiller.upods2.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.support.v7.graphics.Palette;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by alonzilberman on 7/28/15.
 */
public class UIHelper {


    public static int getDominantColor(Bitmap bitmap) {
        List<Palette.Swatch> swatchesTemp = Palette.from(bitmap).generate().getSwatches();
        List<Palette.Swatch> swatches = new ArrayList<Palette.Swatch>(swatchesTemp);
        Collections.sort(swatches, new Comparator<Palette.Swatch>() {
            @Override
            public int compare(Palette.Swatch swatch1, Palette.Swatch swatch2) {
                return swatch2.getPopulation() - swatch1.getPopulation();
            }
        });
        return swatches.get(0).getRgb();
    }

    /**
     * Creates new bitmap by scaling given one
     *
     * @param scaleFactor
     * @param bitmap
     * @return new bitmap
     */
    public static Bitmap createScaledBitmap(Bitmap bitmap, float scaleFactor) {
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()),
                new RectF(0, 0, bitmap.getWidth() * scaleFactor, bitmap.getHeight() * scaleFactor), Matrix.ScaleToFit.CENTER);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }
}

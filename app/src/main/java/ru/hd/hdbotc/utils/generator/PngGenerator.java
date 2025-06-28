package ru.hd.hdbotc.utils.generator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;

import org.json.JSONArray;

import ru.hd.hdbotc.model.ScriptStyles;

public class PngGenerator {

    public static Bitmap generatePng(Context context, JSONArray script, ScriptStyles selectedStyle) {
        switch (selectedStyle) {
            case TRANSPARENT_CLASSIC:
                return createTransparentClassic(context, script);
            case  WHITE_CLASSIC:
                return createWhiteClassic(context, script);
            case TRANSPARENT_AMATIC:
                return createTransparentAmatic(context, script);
            case BLACK_PRINT:
                return applyContrastFilter(createTransparentClassic(context, script), 200);
            default:
                return createTransparentClassic(context, script);
        }
    }

    private static Bitmap createTransparentClassic(Context context, JSONArray script) {
        Typeface typefaceR = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
        Typeface typefaceB = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        return new TransparentTwoRow().generateImageFromJson(context, script, typefaceR, typefaceB);
    }

    private static Bitmap createTransparentAmatic(Context context, JSONArray script) {
        Typeface typefaceR = Typeface.createFromAsset(context.getAssets(), "fonts/AmaticSC-Regular.ttf");
        Typeface typefaceB = Typeface.createFromAsset(context.getAssets(), "fonts/AmaticSC-Bold.ttf");
        return new TransparentTwoRow().generateImageFromJson(context, script, typefaceR, typefaceB);
    }

    private static Bitmap createWhiteClassic(Context context, JSONArray script) {
        Bitmap transparentBmp = createTransparentClassic(context, script);
        if (transparentBmp == null) {
            return null;
        }
        Bitmap whiteBmp = Bitmap.createBitmap(
                transparentBmp.getWidth(),
                transparentBmp.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(whiteBmp);
        canvas.drawColor(Color.WHITE); // Заливка белым цветом
        canvas.drawBitmap(transparentBmp, 0, 0, null);

        transparentBmp.recycle();

        return whiteBmp;
    }

    private static Bitmap applyContrastFilter(Bitmap source, int threshold) {
        int width = source.getWidth();
        int height = source.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, source.getConfig());

        int[] pixels = new int[width * height];
        source.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int alpha = (pixel >> 24) & 0xFF;
            int red = (pixel >> 16) & 0xFF;
            int green = (pixel >> 8) & 0xFF;
            int blue = pixel & 0xFF;

            int brightness = (red + green + blue) / 3;

            if (brightness < threshold) {
                pixels[i] = (alpha << 24) | (0);
            } else {
                pixels[i] = (alpha << 24) | (255 << 16) | (255 << 8) | 255;
            }
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }
}

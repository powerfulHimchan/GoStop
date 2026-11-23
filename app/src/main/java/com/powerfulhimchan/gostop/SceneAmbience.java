package com.powerfulhimchan.gostop;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

final class SceneAmbience {
    private static final float WIDTH = 240f;
    private static final float HEIGHT = 160f;
    private static final int WARM = 0xFFFFD799;
    private static final int RAIN = 0xFFBDD9F5;
    private static final int WATER = 0xFFBDE6EC;
    private final Paint paint = new Paint();

    void draw(Canvas canvas, RectF bounds, Story.Art art, float seconds) {
        canvas.save();
        canvas.clipRect(bounds);
        canvas.translate(bounds.left, bounds.top);
        canvas.scale(bounds.width() / WIDTH, bounds.height() / HEIGHT);
        switch (art) {
            case ALLEY, CHASE -> {
                rain(canvas, seconds, 0, 240, 4, 145, 30);
                shimmer(canvas, seconds, 147, 122, 77, 35, 9, WATER);
                glow(canvas, seconds, 8, 43, 61, 5, WARM);
            }
            case ENTRY -> {
                rain(canvas, seconds, 0, 240, 0, 153, 19);
                shimmer(canvas, seconds, 5, 123, 215, 35, 17, WARM);
            }
            case DEPOT, PRESS -> {
                rain(canvas, seconds, 20, 240, 3, 154, 24);
                shimmer(canvas, seconds, 20, 123, 217, 30, 15, WARM);
            }
            case RIVER -> {
                shimmer(canvas, seconds, 142, 69, 93, 48, 23, WATER);
                shimmer(canvas, seconds + 2f, 3, 70, 71, 26, 10, WARM);
            }
            case PORT -> {
                shimmer(canvas, seconds, 111, 84, 126, 69, 30, WATER);
                shimmer(canvas, seconds + 3f, 56, 84, 51, 22, 11, WATER);
            }
            case KITCHEN -> steam(canvas, seconds, 170, 118, 15);
            case DESK -> rain(canvas, seconds, 133, 239, 3, 86, 19);
            case CAFE -> {
                steam(canvas, seconds, 111, 110, 10);
                dust(canvas, seconds, 12, 32, 90, 73, 10, WARM);
            }
            case OFFICE, PUBLISHER -> {
                glow(canvas, seconds, 180, 94, 41, 8, WATER);
                shimmer(canvas, seconds, 174, 93, 51, 23, 7, WATER);
            }
            case CONTROL -> {
                shimmer(canvas, seconds, 18, 91, 214, 30, 16, WATER);
                glow(canvas, seconds, 123, 122, 41, 8, WATER);
            }
            case SUBWAY -> {
                glow(canvas, seconds, 97, 34, 104, 7, WARM);
                dust(canvas, seconds, 17, 27, 143, 78, 13, WARM);
            }
            case WALK -> dust(canvas, seconds, 2, 28, 227, 109, 20, WARM);
            case ROOFTOP -> {
                dust(canvas, seconds, 132, 21, 105, 115, 17, WARM);
                glow(canvas, seconds, 184, 29, 48, 13, WARM);
            }
            case BOOKSHOP -> dust(canvas, seconds, 75, 13, 134, 91, 17, WARM);
            case STORE_MEET -> {
                rain(canvas, seconds, 122, 240, 0, 142, 16);
                shimmer(canvas, seconds, 5, 120, 225, 35, 13, WATER);
            }
            case BUSAN_PUBLISHER -> dust(canvas, seconds, 86, 27, 122, 97, 16, WARM);
            case PROOFS -> dust(canvas, seconds, 12, 15, 215, 113, 14, WARM);
            case PRINTSHOP -> {
                dust(canvas, seconds, 86, 25, 144, 99, 17, WARM);
                shimmer(canvas, seconds, 105, 110, 125, 31, 11, WARM);
            }
            case MARKET -> dust(canvas, seconds, 96, 33, 133, 101, 18, WARM);
            case MUSICSHOP -> dust(canvas, seconds, 125, 39, 111, 106, 19, WARM);
            case VAN -> {
                dust(canvas, seconds, 81, 25, 124, 106, 16, WARM);
                glow(canvas, seconds, 77, 15, 42, 6, WARM);
            }
            case HOME -> {
                dust(canvas, seconds, 68, 56, 101, 93, 13, WARM);
                shimmer(canvas, seconds, 107, 65, 82, 26, 9, WATER);
            }
            case GUITAR -> {
                dust(canvas, seconds, 18, 53, 96, 99, 14, WARM);
                shimmer(canvas, seconds, 114, 50, 108, 28, 9, WATER);
            }
        }
        canvas.restore();
    }

    private void rain(Canvas canvas, float seconds, int left, int right, int top, int bottom, int count) {
        for (int i = 0; i < count; i++) {
            float travel = cycle(seconds * (0.52f + i % 4 * 0.065f) + i * 0.381966f);
            int x = left + Math.floorMod(i * 71 + 13, right - left) - (int) (travel * 5);
            int y = top + (int) (travel * (bottom - top));
            pixel(canvas, x, y, 1, 3 + i % 4, RAIN, (int) (8 + 59 * envelope(travel)));
        }
    }

    private void shimmer(Canvas canvas, float seconds, int x, int y, int width, int height,
                         int count, int color) {
        for (int i = 0; i < count; i++) {
            float life = cycle(seconds * (0.32f + i % 5 * 0.036f) + i * 0.381966f);
            int left = x + (i * 43 + 5) % width + (int) (life * (4 + i % 5));
            int top = y + (i * 11 + 7) % height;
            int length = 2 + i % 5 + (int) (5 * envelope(life));
            pixel(canvas, left, top, length, 1, color, (int) (75 * envelope(life)));
        }
    }

    private void dust(Canvas canvas, float seconds, int x, int y, int width, int height,
                      int count, int color) {
        for (int i = 0; i < count; i++) {
            float life = cycle(seconds * (0.13f + i % 5 * 0.025f) + i * 0.381966f);
            int left = x + (i * 53 + 11) % width + (int) (life * (13 + i % 5 * 3));
            int top = y + (i * 19 + 5) % height - (int) (life * (4 + i % 3 * 4));
            int size = i % 5 == 0 ? 2 : 1;
            pixel(canvas, left, top, size, size, color, (int) (105 * envelope(life)));
        }
    }

    private void steam(Canvas canvas, float seconds, int x, int y, int count) {
        for (int i = 0; i < count; i++) {
            float rise = cycle(seconds * (0.42f + i % 3 * 0.03f) + i * 0.381966f);
            int left = x + i * 11 % 24 + (int) (rise * 9);
            int top = y - (int) (rise * (20 + i % 7));
            pixel(canvas, left, top, 2 + i % 3, 2, 0xFFFFF3DB, (int) (93 * envelope(rise)));
        }
    }

    private void glow(Canvas canvas, float seconds, int x, int y, int width, int height, int color) {
        float pulse = (float) (0.5 + 0.5 * Math.sin(seconds * 2.6));
        pixel(canvas, x, y, width, height, color, (int) (18 * pulse));
    }

    private void pixel(Canvas canvas, int x, int y, int width, int height, int color, int alpha) {
        paint.setColor(color);
        paint.setAlpha(alpha);
        canvas.drawRect(x, y, x + width, y + height, paint);
    }

    private static float cycle(float value) {
        return value - (float) Math.floor(value);
    }

    private static float envelope(float progress) {
        return 4f * progress * (1f - progress);
    }
}

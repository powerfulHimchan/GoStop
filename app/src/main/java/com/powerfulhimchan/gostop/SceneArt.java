package com.powerfulhimchan.gostop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;

final class SceneArt extends View {
    private static final int ART_WIDTH = 240;
    private static final int ART_HEIGHT = 160;

    private static final String[] FIGURE = {
            ".....#####....",
            "....#HHHHH#...",
            "...#HAAAAHH#..",
            "...#HAAAAAA#..",
            "...#H#A#AA#...",
            "...#AAAFAA#...",
            "....#AAAA#....",
            "...##RRR###...",
            "..#CCCRCCCC#..",
            "..#CCRRCCSC#..",
            ".#ACRCCCSSC#..",
            ".#ACCCCCSCA#..",
            "..#CCCCCCC#...",
            "...#DDDDDD#...",
            "...#DD#DDD#...",
            "...#DD##DD#...",
            "..#BBB##BBB#..",
            "..#####.####..",
    };

    private final Bitmap background;
    private final Paint paint = new Paint();
    private final RectF bounds = new RectF(0, 0, ART_WIDTH, ART_HEIGHT);
    private BranchingGame.Scene scene = BranchingGame.Scene.ARRIVAL;
    private long sceneStart;

    SceneArt(Context context) {
        super(context);
        background = BitmapFactory.decodeResource(getResources(), R.drawable.bridge);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void show(BranchingGame.Scene next) {
        scene = next;
        sceneStart = SystemClock.uptimeMillis();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = width * ART_HEIGHT / ART_WIDTH;
        float screenHeight = getResources().getConfiguration().screenHeightDp
                * getResources().getDisplayMetrics().density;
        if (screenHeight > 0) {
            height = Math.min(height, Math.round(screenHeight * 0.32f));
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        float scale = Math.min(getWidth() / (float) ART_WIDTH, getHeight() / (float) ART_HEIGHT);
        canvas.translate((getWidth() - ART_WIDTH * scale) / 2,
                (getHeight() - ART_HEIGHT * scale) / 2);
        canvas.scale(scale, scale);
        paint.setAlpha(255);
        paint.setFilterBitmap(false);
        canvas.drawBitmap(background, null, bounds, paint);

        long elapsed = SystemClock.uptimeMillis() - sceneStart;
        boolean bob = elapsed > 140 && elapsed < 280;
        switch (scene) {
            case ARRIVAL:
                figure(canvas, 88, 131, 1.45f, false, false, bob);
                figure(canvas, 180, 78, 0.8f, true, true, false);
                glint(canvas, 113, 118);
                break;
            case GUIDE:
                figure(canvas, 86, 133, 1.45f, false, false, false);
                figure(canvas, 114, 126, 1.45f, true, true, bob);
                glint(canvas, 106, 114);
                break;
            case BANK:
                figure(canvas, 135, 139, 1.45f, false, false, bob);
                figure(canvas, 181, 78, 0.8f, true, true, false);
                stones(canvas);
                break;
            case ROPES:
                figure(canvas, 94, 117, 1.35f, false, false, false);
                figure(canvas, 119, 110, 1.25f, true, true, bob);
                glint(canvas, 119, 93);
                break;
            case LEDGE:
                figure(canvas, 138, 137, 1.45f, false, false, bob);
                stones(canvas);
                break;
            case TOGETHER:
                figure(canvas, 168, 90, 0.95f, false, false, bob);
                figure(canvas, 194, 75, 0.8f, true, false, false);
                glint(canvas, 200, 66);
                break;
            case SIDE_PATH:
                stones(canvas);
                figure(canvas, 198, 83, 0.95f, false, false, bob);
                break;
            case WAIT:
                canvas.drawColor(0x12FFC47D);
                figure(canvas, 42, 136, 1.55f, false, false, bob);
                glint(canvas, 68, 128);
                break;
        }
        canvas.restore();
        if (elapsed < 300) {
            postInvalidateDelayed(80);
        }
    }

    private void stones(Canvas canvas) {
        block(canvas, 166, 130, 177, 133, 0xFF637591);
        block(canvas, 176, 117, 186, 120, 0xFF7E94A6);
        block(canvas, 190, 106, 198, 108, 0xFF88A3BA);
    }

    private void glint(Canvas canvas, int x, int y) {
        block(canvas, x - 3, y - 1, x + 4, y + 2, 0x50FFB658);
        block(canvas, x - 1, y - 3, x + 2, y + 4, 0xBFFFBE69);
        block(canvas, x - 1, y - 1, x + 2, y + 2, 0xFFFFEB9B);
    }

    private void figure(Canvas canvas, int x, int y, float size, boolean guide,
                        boolean facesLeft, boolean bob) {
        block(canvas, x + 1, y - 2, x + Math.round(14 * size), y + 2, 0x800C1423);
        canvas.save();
        canvas.translate(x + (facesLeft ? 14 * size : 0), y - 18 * size - (bob ? 2 : 0));
        canvas.scale(facesLeft ? -size : size, size);
        for (int row = 0; row < FIGURE.length; row++) {
            String pixels = FIGURE[row];
            for (int column = 0; column < pixels.length(); column++) {
                char pixel = pixels.charAt(column);
                if (pixel != '.') {
                    block(canvas, column, row, column + 1, row + 1, spriteColor(pixel, guide));
                }
            }
        }
        canvas.restore();
    }

    private static int spriteColor(char pixel, boolean guide) {
        switch (pixel) {
            case '#': return 0xFF17172B;
            case 'H': return guide ? 0xFF805B48 : 0xFF594060;
            case 'A': return 0xFFF0BD91;
            case 'F': return 0xFFE08778;
            case 'R': return guide ? 0xFFF1C86D : 0xFFEFA379;
            case 'C': return guide ? 0xFFB17054 : 0xFF479B9C;
            case 'S': return guide ? 0xFFDF9C62 : 0xFF69C3B1;
            case 'D': return 0xFF344356;
            case 'B': return 0xFF23253C;
            default: return Color.TRANSPARENT;
        }
    }

    private void block(Canvas canvas, int left, int top, int right, int bottom, int color) {
        paint.setColor(color);
        canvas.drawRect(left, top, right, bottom, paint);
    }
}

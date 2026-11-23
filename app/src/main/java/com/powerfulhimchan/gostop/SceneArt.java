package com.powerfulhimchan.gostop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.View;
import java.util.EnumMap;

final class SceneArt extends View {
    private static final float ASPECT = 1.5f;
    private final EnumMap<Story.Art, Bitmap> illustrations = new EnumMap<>(Story.Art.class);
    private final RectF bounds = new RectF();
    private Bitmap illustration;

    SceneArt(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void show(Story.Art art) {
        Bitmap next = illustrations.get(art);
        if (next == null) {
            int resource = switch (art) {
                case ALLEY -> R.drawable.story_alley;
                case OFFICE -> R.drawable.story_office;
                case KITCHEN -> R.drawable.story_kitchen;
                case RIVER -> R.drawable.story_river;
                case DESK -> R.drawable.story_desk;
                case CAFE -> R.drawable.story_cafe;
                case SUBWAY -> R.drawable.story_subway;
                case HOME -> R.drawable.story_home;
            };
            next = BitmapFactory.decodeResource(getResources(), resource);
            illustrations.put(art, next);
        }
        illustration = next;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = Math.round(width / ASPECT);
        int heightDp = getResources().getConfiguration().screenHeightDp;
        float screenHeight = heightDp * getResources().getDisplayMetrics().density;
        if (screenHeight > 0) {
            float maxScreenShare = heightDp < 550 ? 0.24f : 0.32f;
            height = Math.min(height, Math.round(screenHeight * maxScreenShare));
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (illustration == null) {
            return;
        }
        float width = Math.min(getWidth(), getHeight() * ASPECT);
        float height = width / ASPECT;
        float left = (getWidth() - width) / 2;
        float top = (getHeight() - height) / 2;
        bounds.set(left, top, left + width, top + height);
        canvas.drawBitmap(illustration, null, bounds, null);
    }
}

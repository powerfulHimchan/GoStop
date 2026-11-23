package com.powerfulhimchan.gostop;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.LinearInterpolator;
import java.util.EnumMap;

final class SceneArt extends View {
    private static final float ASPECT = 1.5f;
    private static final int AMBIENT_FRAME_MS = 70;
    private final EnumMap<Story.Art, Bitmap> illustrations = new EnumMap<>(Story.Art.class);
    private final RectF bounds = new RectF();
    private final Paint paint = new Paint();
    private final SceneAmbience ambience = new SceneAmbience();
    private final ChaseRunners runners = new ChaseRunners();
    private final Rect visible = new Rect();
    private final Runnable ambientFrame = this::advanceAmbience;
    private Bitmap illustration;
    private Story.Art currentArt;
    private long atmosphereStarted;
    private ValueAnimator animator;
    private Story.Motion motion;
    private float progress = 1f;

    SceneArt(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void show(Story.Art art) {
        stopMotion();
        currentArt = art;
        atmosphereStarted = SystemClock.uptimeMillis();
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
                case HOME -> R.drawable.story_apartment;
                case GUITAR -> R.drawable.story_home;
                case ENTRY -> R.drawable.story_entry;
                case WALK -> R.drawable.story_walk;
                case DEPOT -> R.drawable.story_depot;
                case BOOKSHOP -> R.drawable.story_bookshop;
                case PORT -> R.drawable.story_port;
                case MUSICSHOP -> R.drawable.story_musicshop;
                case ROOFTOP -> R.drawable.story_rooftop;
                case CONTROL -> R.drawable.story_control;
                case PRINTSHOP -> R.drawable.story_printshop;
                case MARKET -> R.drawable.story_market;
                case VAN -> R.drawable.story_van;
                case CHASE -> R.drawable.story_chase;
                case CHASE_STILL -> R.drawable.story_chase_still;
                case STORE_MEET -> R.drawable.story_store_meet;
                case PUBLISHER -> R.drawable.story_publisher;
                case PRESS -> R.drawable.story_press;
                case BUSAN_PUBLISHER -> R.drawable.story_busan_publisher;
                case PROOFS -> R.drawable.story_proofs;
            };
            next = BitmapFactory.decodeResource(getResources(), resource);
            illustrations.put(art, next);
        }
        illustration = next;
        invalidate();
        updateAmbience();
    }

    void play(Story.Motion nextMotion) {
        if (currentArt == Story.Art.CHASE_STILL) {
            return;
        }
        motion = nextMotion;
        progress = 0f;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(switch (motion) {
            case TRUCK -> 1000L;
            case PHONE, HANDOFF, REVEAL -> 700L;
            case RUN -> 520L;
            default -> 330L;
        });
        animator.addUpdateListener(frame -> {
            progress = (Float) frame.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(ambientFrame);
        stopMotion();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateAmbience();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        updateAmbience();
    }

    @Override
    public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        updateAmbience();
    }

    private boolean shouldAnimate() {
        return currentArt != null && currentArt != Story.Art.CHASE_STILL
                && isAttachedToWindow() && getWindowVisibility() == VISIBLE
                && hasWindowFocus() && (Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled());
    }

    private void updateAmbience() {
        removeCallbacks(ambientFrame);
        if (shouldAnimate()) {
            invalidate();
            postDelayed(ambientFrame, AMBIENT_FRAME_MS);
        }
    }

    private void advanceAmbience() {
        if (shouldAnimate()) {
            if (getLocalVisibleRect(visible)) {
                invalidate();
            }
            postDelayed(ambientFrame, AMBIENT_FRAME_MS);
        }
    }

    private void stopMotion() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        progress = 1f;
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
        if (motion == null || progress >= 1f) {
            drawIllustration(canvas);
            return;
        }
        float remaining = 1f - progress;
        float ease = remaining * remaining;
        canvas.save();
        canvas.clipRect(bounds);
        canvas.save();
        float shift = switch (motion) {
            case ENTER_LEFT -> -0.024f * width * ease;
            case ENTER_RIGHT -> 0.024f * width * ease;
            case RUN -> 0.04f * width * ease;
            default -> 0f;
        };
        float bounce = motion == Story.Motion.RUN
                ? (float) Math.sin(progress * Math.PI * 6) * height * 0.018f * remaining : 0f;
        float zoom = 1f + (motion == Story.Motion.RUN ? 0.12f : 0.08f) * remaining;
        canvas.translate(shift, bounce);
        canvas.scale(zoom, zoom, bounds.centerX(), bounds.centerY());
        drawIllustration(canvas);
        canvas.restore();
        drawMotionDetail(canvas, width, height);
        canvas.restore();
    }

    private void drawIllustration(Canvas canvas) {
        canvas.drawBitmap(illustration, null, bounds, null);
        boolean animate = shouldAnimate();
        float seconds = animate ? (SystemClock.uptimeMillis() - atmosphereStarted) / 1000f : 0f;
        if (currentArt == Story.Art.CHASE) {
            runners.draw(canvas, bounds, seconds);
        }
        if (animate) {
            ambience.draw(canvas, bounds, currentArt, seconds);
        }
    }

    private void drawMotionDetail(Canvas canvas, float width, float height) {
        float unit = width / 120f;
        float fade = (float) Math.sin(progress * Math.PI);
        canvas.save();
        canvas.translate(bounds.left, bounds.top);
        switch (motion) {
            case PHONE -> {
                canvas.translate(width * 0.12f, height * 0.67f - fade * unit * 4);
                canvas.scale(unit, unit);
                motionRect(canvas, 0, 0, 10, 16, 0xFF252C37, fade);
                motionRect(canvas, 1, 2, 9, 13, 0xFFF4CD83, fade);
                motionRect(canvas, 3, 5, 7, 6, 0xFF943C31, fade);
                motionRect(canvas, 3, 8, 6, 9, 0xFF943C31, fade);
            }
            case HANDOFF -> {
                canvas.translate(width * (0.25f + progress * 0.38f), height * 0.7f);
                canvas.scale(unit, unit);
                motionRect(canvas, 0, 0, 15, 11, 0xFF3E302C, fade);
                motionRect(canvas, 1, 1, 14, 10, 0xFFF6DEAD, fade);
                motionRect(canvas, 3, 3, 12, 4, 0xFF527380, fade);
                motionRect(canvas, 3, 6, 10, 7, 0xFFAF6248, fade);
            }
            case TRUCK -> {
                canvas.translate(width * (0.84f - progress * 1.16f), height * 0.81f);
                canvas.scale(unit, unit);
                motionRect(canvas, 9, 0, 30, 11, 0xFFF2DBB5, fade);
                motionRect(canvas, 2, 4, 10, 12, 0xFFB5C5C9, fade);
                motionRect(canvas, 3, 5, 8, 8, 0xFF32435A, fade);
                motionRect(canvas, 0, 10, 31, 13, 0xFF454555, fade);
                motionRect(canvas, 0, 9, 2, 11, 0xFFFFDD8C, fade);
                motionRect(canvas, 5, 11, 10, 16, 0xFF282731, fade);
                motionRect(canvas, 23, 11, 28, 16, 0xFF282731, fade);
            }
            case REVEAL -> motionRect(canvas, 0, 0, width, height, 0xFFF9D68F, fade * 0.18f);
            default -> { }
        }
        canvas.restore();
    }

    private void motionRect(Canvas canvas, float left, float top, float right, float bottom,
                            int color, float alpha) {
        paint.setColor(color);
        paint.setAlpha(Math.round(alpha * 255));
        canvas.drawRect(left, top, right, bottom, paint);
    }
}

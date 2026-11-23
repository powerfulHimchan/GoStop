package com.powerfulhimchan.gostop;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

final class ChaseRunners {
    private static final double TAU = Math.PI * 2;
    private static final int OUTLINE = 0xFF161C28;
    private static final int SKIN = 0xFFD29A73;
    private static final int SKIN_LIGHT = 0xFFE3B691;
    private static final int SHOE = 0xFF283143;

    private enum Runner {
        JUNHO(0xFF596149, 0xFF82816A, 0xFF434D57),
        GUARD(0xFF37475D, 0xFF657A90, 0xFF303C4E),
        TAESIK(0xFF694F46, 0xFF997567, 0xFF45444A);

        final int jacket;
        final int light;
        final int jeans;

        Runner(int jacket, int light, int jeans) {
            this.jacket = jacket;
            this.light = light;
            this.jeans = jeans;
        }
    }

    private final Paint paint = new Paint();
    private final Path body = new Path();

    void draw(Canvas canvas, RectF bounds, float seconds) {
        canvas.save();
        canvas.clipRect(bounds);
        canvas.translate(bounds.left, bounds.top);
        canvas.scale(bounds.width() / 240f, bounds.height() / 160f);

        float tracking = (float) ((1 - Math.cos(seconds * 0.94)) / 2);
        runner(canvas, 173 - tracking * 23, 106 + tracking * 12,
                0.85f + tracking * 0.22f, seconds, 0.35f, Runner.GUARD);
        runner(canvas, 151 - tracking * 25, 116 + tracking * 13,
                1f + tracking * 0.22f, seconds, 0.1f, Runner.TAESIK);
        runner(canvas, 58 - tracking * 4, 150 + tracking * 2,
                1.77f, seconds, 0.66f, Runner.JUNHO);
        canvas.restore();
    }

    private void runner(Canvas canvas, float x, float y, float size, float seconds,
                        float offset, Runner runner) {
        float phase = ((seconds * 2.35f + offset) % 1f);
        float stride = (float) Math.sin(phase * TAU);
        int bob = Math.round(2 * Math.abs(stride));
        canvas.save();
        canvas.translate(Math.round(x), Math.round(y));
        canvas.scale(size, size);
        pixel(canvas, -11, -1, 13, 2, 0x76202C3D);
        canvas.translate(0, -bob);

        leg(canvas, -stride, 2, runner.jeans);
        arm(canvas, stride, 0, runner.light);
        leg(canvas, stride, -2, runner.jeans);

        body.reset();
        body.moveTo(-8, -30);
        body.lineTo(1, -32);
        body.lineTo(7, -26);
        body.lineTo(8, -17);
        body.lineTo(4, -13);
        body.lineTo(-4, -14);
        body.lineTo(-8, -21);
        body.close();
        paint.setColor(OUTLINE);
        canvas.drawPath(body, paint);
        body.offset(0, -0.35f);
        paint.setColor(runner.jacket);
        canvas.save();
        canvas.scale(0.82f, 0.93f, 0, -23);
        canvas.drawPath(body, paint);
        canvas.restore();

        if (runner == Runner.JUNHO) {
            pixel(canvas, 4, -27, 9, -19, 0xFF342F36);
            limb(canvas, -5, -29, 4, -17, 2, 0xFFA8946C);
            pixel(canvas, -7, -30, -4, -25, 0xFFD6C2AB);
        } else {
            pixel(canvas, -5, -27, 0, -25, runner.light);
        }
        arm(canvas, -stride, -1, runner.jacket);
        head(canvas, runner);
        canvas.restore();
    }

    private void leg(Canvas canvas, float swing, int offset, int jeans) {
        int kneeX = offset + Math.round(swing * 11);
        int kneeY = -8 - Math.max(0, Math.round(-swing * 5));
        int footX = kneeX + Math.round(6 + swing * 3);
        int footY = swing < 0 ? kneeY + 5 : -1 - Math.round(swing * 3);
        limb(canvas, offset + 1, -15, kneeX, kneeY, 7, OUTLINE);
        limb(canvas, kneeX, kneeY, footX, footY, 6, OUTLINE);
        limb(canvas, offset + 1, -15, kneeX, kneeY, 4, jeans);
        limb(canvas, kneeX, kneeY, footX, footY, 3, jeans);
        pixel(canvas, footX - 4, footY - 1, footX + 3, footY + 2, SHOE);
        pixel(canvas, footX - 4, footY + 1, footX + 2, footY + 2, 0xFF708093);
    }

    private void arm(Canvas canvas, float swing, int offset, int jacket) {
        int elbowX = offset + 4 + Math.round(8 * swing);
        int elbowY = -20 - Math.round(Math.abs(swing) * 2);
        int handX = elbowX - 7;
        int handY = elbowY - 4;
        limb(canvas, offset - 3, -28, elbowX, elbowY, 6, OUTLINE);
        limb(canvas, elbowX, elbowY, handX, handY, 5, OUTLINE);
        limb(canvas, offset - 3, -28, elbowX, elbowY, 4, jacket);
        limb(canvas, elbowX, elbowY, handX, handY, 3, jacket);
        pixel(canvas, handX - 2, handY - 1, handX + 1, handY + 2, SKIN);
    }

    private void head(Canvas canvas, Runner runner) {
        pixel(canvas, -11, -41, 2, -30, OUTLINE);
        pixel(canvas, -12, -38, -8, -34, SKIN);
        pixel(canvas, -10, -39, 0, -32, SKIN);
        pixel(canvas, -9, -36, -4, -32, SKIN_LIGHT);
        pixel(canvas, -3, -33, 1, -29, SKIN);
        pixel(canvas, -11, -40, 3, -38, OUTLINE);
        pixel(canvas, -9, -43, 0, -39, OUTLINE);
        pixel(canvas, 0, -40, 3, -34, OUTLINE);
        pixel(canvas, -9, -37, -7, -36, OUTLINE);
        if (runner == Runner.GUARD) {
            pixel(canvas, -10, -44, 1, -40, runner.light);
            pixel(canvas, -15, -40, 2, -38, runner.jacket);
        } else {
            pixel(canvas, -8, -42, -1, -41, 0xFF3F3843);
        }
    }

    private void limb(Canvas canvas, int x, int y, int toX, int toY, int width, int color) {
        paint.setColor(color);
        paint.setStrokeWidth(width);
        canvas.drawLine(x, y, toX, toY, paint);
    }

    private void pixel(Canvas canvas, int left, int top, int right, int bottom, int color) {
        paint.setColor(color);
        canvas.drawRect(left, top, right, bottom, paint);
    }
}

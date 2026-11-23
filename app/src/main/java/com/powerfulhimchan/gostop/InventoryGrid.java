package com.powerfulhimchan.gostop;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import java.util.Set;

final class InventoryGrid extends GridLayout {
    private static final int SIZE = 8;
    private static final int ICON_SIZE = 24;

    interface ItemListener {
        void onSelected(BranchingGame.Item item);
    }

    private ItemListener listener;

    InventoryGrid(Context context, Set<BranchingGame.Item> items, boolean legacy) {
        super(context);
        setRowCount(SIZE);
        setColumnCount(SIZE);
        Bitmap icons = items.isEmpty() ? null : BitmapFactory.decodeResource(
                getResources(), R.drawable.inventory_icons);
        int index = 0;
        for (BranchingGame.Item item : items) {
            ImageButton button = new ImageButton(context);
            button.setContentDescription(item.displayName(legacy));
            button.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int inset = dp(4);
            button.setPadding(inset, inset, inset, inset);
            Bitmap icon = Bitmap.createBitmap(icons, item.ordinal() * ICON_SIZE, 0, ICON_SIZE, ICON_SIZE);
            BitmapDrawable drawable = new BitmapDrawable(getResources(), icon);
            drawable.setFilterBitmap(false);
            button.setImageDrawable(drawable);
            StateListDrawable backgrounds = new StateListDrawable();
            backgrounds.addState(new int[]{android.R.attr.state_selected}, slot(0xFFE4B985, 0xFF963F2D));
            backgrounds.addState(new int[0], slot(0xFFF9ECD7, 0xFFA47D64));
            button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x25963F2D), backgrounds, null));
            button.setOnClickListener(view -> {
                for (int cell = 0; cell < items.size(); cell++) {
                    getChildAt(cell).setSelected(getChildAt(cell) == view);
                }
                if (listener != null) {
                    listener.onSelected(item);
                }
            });
            addView(button, cell(index++));
        }
        while (index < SIZE * SIZE) {
            View empty = new View(context);
            empty.setBackground(slot(0xFFF0DFC3, 0x55A47D64));
            empty.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(empty, cell(index++));
        }
    }

    void setOnItemSelected(ItemListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY));
    }

    private LayoutParams cell(int index) {
        LayoutParams params = new LayoutParams(spec(index / SIZE, 1, 1f), spec(index % SIZE, 1, 1f));
        params.width = 0;
        params.height = 0;
        int gap = dp(2);
        params.setMargins(gap, gap, gap, gap);
        return params;
    }

    private GradientDrawable slot(int color, int border) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setStroke(dp(1), border);
        drawable.setCornerRadius(dp(3));
        return drawable;
    }

    private int dp(float value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}

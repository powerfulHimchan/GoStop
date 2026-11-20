package com.powerfulhimchan.gostop;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final String SAVE = "choices_v1";
    private static final int BACKGROUND = 0xFF0E1524;
    private static final int PANEL = 0xFF182438;
    private static final int BORDER = 0xFF3B4B62;
    private static final int TITLE = 0xFFFFF2DC;
    private static final int TEXT = 0xFFDDE6F2;
    private static final int MUTED = 0xFFB2C0D4;
    private static final int GOLD = 0xFFF5C482;
    private static final int MINT = 0xFF8CE0CC;

    private SharedPreferences progress;
    private BranchingGame game;
    private ScrollView scroll;
    private SceneArt artwork;
    private TextView phase;
    private TextView title;
    private TextView description;
    private LinearLayout influences;
    private final Button[] buttons = new Button[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progress = getSharedPreferences("playthrough", MODE_PRIVATE);
        game = BranchingGame.restore(progress.getString(SAVE, ""));
        buildLayout();
        showScene();
    }

    private void buildLayout() {
        LinearLayout root = column();
        root.setBackgroundColor(BACKGROUND);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                Insets safe = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        setContentView(root);

        LinearLayout content = column();
        content.setPadding(dp(16), 0, dp(16), 0);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
                Math.min(getResources().getDisplayMetrics().widthPixels, dp(600)), -1,
                Gravity.CENTER_HORIZONTAL);
        FrameLayout widthLimit = new FrameLayout(this);
        widthLimit.addView(content, contentParams);
        root.addView(widthLimit, new LinearLayout.LayoutParams(-1, -1));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text(20, TITLE, true);
        brand.setText(R.string.app_name);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        phase = text(13, GOLD, true);
        header.addView(phase);
        add(content, header, 10);

        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setFillViewport(true);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = column();
        body.setPadding(0, 0, 0, dp(12));
        scroll.addView(body);

        FrameLayout picture = new FrameLayout(this);
        picture.setBackground(surface(PANEL, BORDER, 16));
        picture.setClipToOutline(true);
        artwork = new SceneArt(this);
        picture.addView(artwork, new FrameLayout.LayoutParams(-1, -2));
        add(body, picture, 14);

        LinearLayout story = column();
        story.setPadding(dp(14), dp(12), dp(14), dp(14));
        story.setBackground(surface(PANEL, BORDER, 16));
        title = text(23, TITLE, true);
        if (Build.VERSION.SDK_INT >= 28) {
            title.setAccessibilityHeading(true);
        }
        add(story, title, 0);
        description = text(16, TEXT, false);
        description.setLineSpacing(dp(4), 1);
        add(story, description, 8);
        add(body, story, 12);

        influences = column();
        add(body, influences, 12);

        LinearLayout footer = column();
        footer.setPadding(0, dp(4), 0, dp(10));
        for (int i = 0; i < buttons.length; i++) {
            Button button = new Button(this);
            button.setAllCaps(false);
            button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            button.setTextSize(16);
            button.setTypeface(null, Typeface.BOLD);
            button.setTextColor(TITLE);
            button.setMinHeight(dp(54));
            button.setPadding(dp(16), dp(8), dp(16), dp(8));
            button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x38F5C482),
                    surface(0xFF223349, BORDER, 12), null));
            buttons[i] = button;
            add(footer, button, i == 0 ? 0 : 8);
        }
        content.addView(footer, new LinearLayout.LayoutParams(-1, -2));
    }

    private void showScene() {
        artwork.show(game.scene);
        title.setText(game.scene.title);
        description.setText(game.scene.description);
        influences.removeAllViews();
        if (game.isEnding()) {
            phase.setText(R.string.ending);
            TextView heading = text(14, MINT, true);
            heading.setText(R.string.ending_choices);
            add(influences, heading, 0);
            for (BranchingGame.Influence influence : game.endingInfluences()) {
                addInfluence(influence);
            }
            buttons[0].setText(R.string.replay);
            buttons[0].setOnClickListener(view -> changeGame(game.replayBranch()));
            buttons[1].setText(R.string.restart);
            buttons[1].setOnClickListener(view -> changeGame(BranchingGame.start()));
        } else {
            phase.setText(getString(R.string.chapter_progress,
                    game.decisionNumber(), BranchingGame.DECISIONS_PER_RUN));
            for (int i = 0; i < buttons.length; i++) {
                BranchingGame.Choice choice = game.scene.choices.get(i);
                buttons[i].setText(choice.label);
                buttons[i].setOnClickListener(view -> changeGame(game.choose(choice)));
            }
        }
    }

    private void changeGame(BranchingGame next) {
        game = next;
        progress.edit().putString(SAVE, game.save()).apply();
        showScene();
        scroll.scrollTo(0, 0);
        title.announceForAccessibility(game.scene.title);
    }

    private void addInfluence(BranchingGame.Influence influence) {
        LinearLayout card = column();
        card.setPadding(dp(12), dp(8), dp(12), dp(8));
        card.setBackground(surface(PANEL, BORDER, 10));
        TextView chosen = text(14, TITLE, true);
        chosen.setText(influence.label);
        add(card, chosen, 0);
        TextView result = text(13, MUTED, false);
        result.setText(influence.effect);
        add(card, result, 3);
        add(influences, card, 6);
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private TextView text(int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setTextColor(color);
        view.setTextSize(size);
        if (bold) {
            view.setTypeface(null, Typeface.BOLD);
        }
        return view;
    }

    private void add(LinearLayout parent, View child, int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(topMargin);
        parent.addView(child, params);
    }

    private GradientDrawable surface(int fill, int border, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(fill);
        background.setCornerRadius(dp(radius));
        background.setStroke(dp(1), border);
        return background;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

package com.powerfulhimchan.gostop;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final String SAVE = "choices_v1";
    private static final String INVENTORY_OPEN = "inventory_open";
    private static final int PAPER = 0xFFF6EAD5;
    private static final int BUTTON_PAPER = 0xFFF9ECD7;
    private static final int BORDER = 0xFFA47D64;
    private static final int INK = 0xFF402E25;
    private static final int BRICK = 0xFF963F2D;

    private SharedPreferences progress;
    private BranchingGame game;
    private Typeface regular;
    private Typeface bold;
    private ScrollView scroll;
    private SceneArt artwork;
    private TextView phase;
    private TextView title;
    private TextView description;
    private Button inventoryButton;
    private AlertDialog inventoryDialog;
    private LinearLayout influences;
    private final Button[] buttons = new Button[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        regular = Typeface.createFromAsset(getAssets(), "fonts/noto_sans_kr_regular.ttf");
        bold = Typeface.createFromAsset(getAssets(), "fonts/noto_sans_kr_bold.ttf");
        progress = getSharedPreferences("playthrough", MODE_PRIVATE);
        game = BranchingGame.restore(progress.getString(SAVE, ""));
        buildLayout();
        showScene();
        if (savedInstanceState != null && savedInstanceState.getBoolean(INVENTORY_OPEN)) {
            showInventory();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean(INVENTORY_OPEN, inventoryDialog != null && inventoryDialog.isShowing());
    }

    @Override
    protected void onDestroy() {
        if (inventoryDialog != null) {
            inventoryDialog.dismiss();
        }
        super.onDestroy();
    }

    private void buildLayout() {
        LinearLayout root = column();
        root.setBackgroundColor(getColor(R.color.journal_background));
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
        if (Build.VERSION.SDK_INT >= 26 && Build.VERSION.SDK_INT < 35) {
            getWindow().setNavigationBarColor(getColor(R.color.journal_background));
        }
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(lightBars, lightBars);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            | (Build.VERSION.SDK_INT >= 26 ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0));
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
        TextView brand = text(25, INK, true);
        brand.setText(R.string.app_name);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        inventoryButton = actionButton(14, 48);
        inventoryButton.setBackground(paperButton(0xFFEEDBC0, false));
        inventoryButton.setGravity(Gravity.CENTER);
        inventoryButton.setOnClickListener(view -> showInventory());
        header.addView(inventoryButton, new LinearLayout.LayoutParams(-2, -2));
        add(content, header, 10);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = column();
        body.setPadding(0, 0, 0, dp(12));
        scroll.addView(body);

        FrameLayout picture = new FrameLayout(this);
        picture.setBackground(paper(PAPER));
        picture.setPadding(dp(5), dp(5), dp(5), dp(5));
        picture.setElevation(dp(2));
        picture.setRotation(-1.2f);
        artwork = new SceneArt(this);
        picture.addView(artwork, new FrameLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(-1, -2);
        photoParams.setMargins(dp(3), dp(14), dp(3), 0);
        body.addView(picture, photoParams);

        FrameLayout storyCard = new FrameLayout(this);
        storyCard.setBackground(paper(PAPER));
        storyCard.setElevation(dp(2));
        LinearLayout story = column();
        story.setPadding(dp(22), dp(12), dp(14), dp(14));
        phase = text(13, BRICK, true);
        add(story, phase, 0);
        title = text(24, INK, true);
        if (Build.VERSION.SDK_INT >= 28) {
            title.setAccessibilityHeading(true);
        }
        add(story, title, 3);
        description = text(18, INK, false);
        description.setLineSpacing(dp(6), 1);
        add(story, description, 8);
        storyCard.addView(story, new FrameLayout.LayoutParams(-1, -2));
        View seam = new View(this);
        GradientDrawable seamLine = new GradientDrawable();
        seamLine.setStroke(dp(1), BORDER, dp(2), dp(2));
        seam.setBackground(seamLine);
        FrameLayout.LayoutParams seamParams = new FrameLayout.LayoutParams(dp(1), -1, Gravity.START);
        seamParams.setMargins(dp(10), dp(12), 0, dp(12));
        storyCard.addView(seam, seamParams);
        ImageView bookmark = new ImageView(this);
        bookmark.setImageResource(R.drawable.journal_bookmark);
        bookmark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams bookmarkParams = new FrameLayout.LayoutParams(
                dp(14), dp(23), Gravity.TOP | Gravity.END);
        bookmarkParams.setMarginEnd(dp(17));
        storyCard.addView(bookmark, bookmarkParams);
        add(body, storyCard, 14);

        influences = column();
        add(body, influences, 12);

        LinearLayout footer = column();
        footer.setPadding(0, dp(4), 0, dp(10));
        for (int i = 0; i < buttons.length; i++) {
            Button button = actionButton(16, 54);
            button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            button.setBackground(paperButton(BUTTON_PAPER, true));
            buttons[i] = button;
            add(footer, button, i == 0 ? 0 : 8);
        }
        content.addView(footer, new LinearLayout.LayoutParams(-1, -2));
    }

    private void showScene() {
        artwork.show(game.scene);
        title.setText(game.scene.title);
        String narrative = game.description();
        SpannableStringBuilder body = new SpannableStringBuilder(narrative);
        for (BranchingGame.Item item : game.inventory()) {
            int start = narrative.indexOf(item.name);
            if (start >= 0) {
                int end = start + item.name.length();
                body.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                body.setSpan(new ForegroundColorSpan(BRICK), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        description.setText(body);
        inventoryButton.setText(getString(R.string.inventory_count, game.inventory().size()));
        influences.removeAllViews();
        if (game.isEnding()) {
            phase.setText(R.string.ending);
            TextView heading = text(16, BRICK, true);
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
                BranchingGame.Choice choice = game.choices().get(i);
                buttons[i].setText(choice.label);
                buttons[i].setOnClickListener(view -> selectChoice(choice));
            }
        }
    }

    private void selectChoice(BranchingGame.Choice choice) {
        BranchingGame.Item reward = choice.reward;
        boolean gainedItem = reward != null && !game.inventory().contains(reward);
        changeGame(game.choose(choice));
        if (gainedItem) {
            Toast.makeText(this, getString(R.string.item_acquired, reward.name), Toast.LENGTH_SHORT).show();
        }
    }

    private void showInventory() {
        SpannableStringBuilder items = new SpannableStringBuilder();
        for (BranchingGame.Item item : game.inventory()) {
            if (items.length() > 0) {
                items.append("\n\n");
            }
            int nameStart = items.length();
            items.append(item.name);
            items.setSpan(new StyleSpan(Typeface.BOLD), nameStart, items.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            items.append('\n').append(item.description);
        }
        if (items.length() == 0) {
            items.append(getText(R.string.inventory_empty));
        }
        TextView heading = text(21, INK, true);
        heading.setText(R.string.inventory);
        heading.setPadding(dp(24), dp(20), dp(24), dp(8));
        TextView message = text(17, INK, false);
        message.setText(items);
        message.setPadding(dp(24), 0, dp(24), dp(12));
        message.setLineSpacing(dp(4), 1);
        ScrollView inventoryScroll = new ScrollView(this);
        inventoryScroll.addView(message);
        inventoryDialog = new AlertDialog.Builder(this)
                .setCustomTitle(heading)
                .setView(inventoryScroll)
                .setPositiveButton(R.string.close, null)
                .show();
        Button close = inventoryDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        close.setTypeface(bold);
        close.setTextColor(BRICK);
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
        card.setBackground(paper(PAPER));
        TextView chosen = text(16, INK, true);
        chosen.setText(influence.label);
        add(card, chosen, 0);
        TextView result = text(16, INK, false);
        result.setLineSpacing(dp(4), 1);
        result.setText(influence.effect);
        add(card, result, 4);
        add(influences, card, 8);
    }

    private Button actionButton(int size, int minHeight) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setTextSize(size);
        button.setTypeface(bold);
        button.setIncludeFontPadding(false);
        button.setTextColor(INK);
        button.setMinHeight(dp(minHeight));
        button.setPadding(dp(16), dp(8), dp(16), dp(8));
        return button;
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
        view.setTypeface(bold ? this.bold : regular);
        view.setIncludeFontPadding(false);
        return view;
    }

    private void add(LinearLayout parent, View child, int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(topMargin);
        parent.addView(child, params);
    }

    private GradientDrawable paper(int fill) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(fill);
        background.setCornerRadius(dp(3));
        background.setStroke(dp(1), BORDER);
        return background;
    }

    private RippleDrawable paperButton(int fill, boolean stripe) {
        GradientDrawable surface = paper(fill);
        if (!stripe) {
            return new RippleDrawable(ColorStateList.valueOf(0x25963F2D), surface, null);
        }
        GradientDrawable edge = new GradientDrawable();
        float radius = dp(3);
        edge.setCornerRadii(new float[]{radius, radius, 0, 0, 0, 0, radius, radius});
        edge.setColor(BRICK);
        LayerDrawable layers = new LayerDrawable(new Drawable[]{surface, edge});
        layers.setLayerWidth(1, dp(5));
        layers.setLayerGravity(1, Gravity.START | Gravity.FILL_VERTICAL);
        return new RippleDrawable(ColorStateList.valueOf(0x25963F2D), layers, null);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

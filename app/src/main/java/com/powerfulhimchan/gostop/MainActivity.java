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
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final String SAVE = "choices_v3";
    private static final String PREVIOUS_SAVE = "choices_v2";
    private static final String STORY_EDITION = "story_edition";
    private static final String NEW_STORY = "branches";
    private static final String LEGACY_STORY = "dispatch";
    private static final String ENDING_PREFIX = "seen_ending_";
    private static final String INTERLUDE_INDEX = "interlude_index";
    private static final String INVENTORY_OPEN = "inventory_open";
    private static final int PAPER = 0xFFF6EAD5;
    private static final int BUTTON_PAPER = 0xFFF9ECD7;
    private static final int BORDER = 0xFFA47D64;
    private static final int INK = 0xFF402E25;
    private static final int BRICK = 0xFF963F2D;

    private SharedPreferences progress;
    private Story story;
    private String edition;
    private BranchingGame game;
    private int interludeIndex = -1;
    private Typeface regular;
    private Typeface bold;
    private ScrollView scroll;
    private SceneArt artwork;
    private TextView phase;
    private TextView title;
    private TextView description;
    private Button inventoryButton;
    private Button newStoryButton;
    private AlertDialog inventoryDialog;
    private LinearLayout influences;
    private final Button[] buttons = new Button[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        regular = Typeface.createFromAsset(getAssets(), "fonts/noto_sans_kr_regular.ttf");
        bold = Typeface.createFromAsset(getAssets(), "fonts/noto_sans_kr_bold.ttf");
        progress = getSharedPreferences("playthrough", MODE_PRIVATE);
        String oldSave = progress.getString(SAVE, progress.getString(PREVIOUS_SAVE, ""));
        edition = progress.getString(STORY_EDITION, oldSave.isEmpty() ? NEW_STORY : LEGACY_STORY);
        story = loadStory(edition);
        if (progress.contains(SAVE)) {
            game = BranchingGame.restore(story, progress.getString(SAVE, ""));
            int savedInterlude = progress.getInt(INTERLUDE_INDEX, -1);
            if (savedInterlude >= 0 && savedInterlude < game.interludes().size()) {
                interludeIndex = savedInterlude;
            }
        } else {
            game = BranchingGame.restorePrevious(story, progress.getString(PREVIOUS_SAVE, ""));
            saveProgress();
        }
        recordEnding(progress.edit()).apply();
        buildLayout();
        showScene();
        if (savedInstanceState != null && savedInstanceState.getBoolean(INVENTORY_OPEN)) {
            showInventory();
        }
    }

    private InputStreamReader storyFile(String name) throws IOException {
        return new InputStreamReader(getAssets().open("story/" + name), StandardCharsets.UTF_8);
    }

    private Story loadStory(String storyEdition) {
        String prefix = LEGACY_STORY.equals(storyEdition) ? "legacy_" : "";
        try (InputStreamReader scenes = storyFile(prefix + "scenes.txt");
             InputStreamReader endings = storyFile(prefix + "endings.txt")) {
            return Story.read(scenes, endings);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load GoStop story", e);
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
        setContentView(root);
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
        Button endingsButton = headerButton();
        endingsButton.setText(R.string.ending_archive);
        endingsButton.setOnClickListener(view -> showEndingArchive());
        LinearLayout.LayoutParams endingsParams = new LinearLayout.LayoutParams(-2, -2);
        endingsParams.setMarginEnd(dp(6));
        header.addView(endingsButton, endingsParams);
        inventoryButton = headerButton();
        inventoryButton.setOnClickListener(view -> showInventory());
        header.addView(inventoryButton, new LinearLayout.LayoutParams(-2, -2));
        add(content, header, 10);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = column();
        body.setPadding(0, 0, 0, dp(12));
        scroll.addView(body);
        newStoryButton = headerButton();
        newStoryButton.setText(R.string.new_story);
        newStoryButton.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setMessage(R.string.new_story_message)
                .setNegativeButton(R.string.close, null)
                .setPositiveButton(R.string.new_story, (dialog, which) -> startNewStory())
                .show());
        add(body, newStoryButton, 8);

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
        newStoryButton.setVisibility(LEGACY_STORY.equals(edition) ? View.VISIBLE : View.GONE);
        Story.Interlude interlude = interludeIndex < 0 ? null : game.interludes().get(interludeIndex);
        artwork.show(interlude == null ? game.art() : interlude.art);
        title.setText(interlude == null ? game.title() : interlude.title);
        String narrative = interlude == null ? game.description() : interlude.description;
        SpannableStringBuilder body = new SpannableStringBuilder(narrative);
        Set<BranchingGame.Item> inventory = game.inventory();
        for (BranchingGame.Item item : inventory) {
            int start = narrative.indexOf(item.name);
            if (start >= 0) {
                int end = start + item.name.length();
                body.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                body.setSpan(new ForegroundColorSpan(BRICK), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        description.setText(body);
        inventoryButton.setText(getString(R.string.inventory_count, inventory.size()));
        influences.removeAllViews();
        buttons[1].setVisibility(interlude == null ? View.VISIBLE : View.GONE);
        if (interlude != null) {
            int act = (game.decisionNumber() - 2) / BranchingGame.DECISIONS_PER_ACT + 1;
            phase.setText(getString(R.string.chapter, act));
            buttons[0].setText(R.string.continue_story);
            buttons[0].setOnClickListener(view -> advanceInterlude());
            buttons[1].setOnClickListener(null);
        } else if (game.isEnding()) {
            phase.setText(getString(R.string.ending_type, game.ending().category));
            TextView heading = text(16, BRICK, true);
            heading.setText(R.string.ending_choices);
            add(influences, heading, 0);
            for (BranchingGame.Choice influence : game.endingInfluences()) {
                addInfluence(influence);
            }
            buttons[0].setText(R.string.replay);
            buttons[0].setOnClickListener(view -> showReplay());
            buttons[1].setText(R.string.restart);
            buttons[1].setOnClickListener(view -> startNewStory());
        } else {
            phase.setText(getString(R.string.chapter, game.act()));
            for (int i = 0; i < buttons.length; i++) {
                BranchingGame.Choice choice = game.choices().get(i);
                buttons[i].setText(choice.label);
                buttons[i].setOnClickListener(view -> selectChoice(choice));
            }
        }
    }

    private void selectChoice(BranchingGame.Choice choice) {
        Set<BranchingGame.Item> previousItems = game.inventory();
        BranchingGame next = game.choose(choice);
        boolean hasInterludes = !choice.interludes.isEmpty();
        changeGame(next, hasInterludes ? 0 : -1,
                hasInterludes ? choice.interludes.get(0).motion : choice.motion);
        for (BranchingGame.Item item : next.inventory()) {
            if (!previousItems.contains(item)) {
                Toast.makeText(this, getString(R.string.item_acquired, item.name), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void advanceInterlude() {
        int nextIndex = interludeIndex + 1;
        if (nextIndex < game.interludes().size()) {
            changeGame(game, nextIndex, game.interludes().get(nextIndex).motion);
        } else {
            changeGame(game, -1, Story.Motion.STOP);
        }
    }

    private void showReplay() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.replay)
                .setItems(LEGACY_STORY.equals(edition) ? R.array.legacy_acts : R.array.story_acts,
                        (dialog, index) -> changeGame(game.replayAct(index + 1), -1, null))
                .setNegativeButton(R.string.close, null)
                .show();
    }

    private void startNewStory() {
        if (LEGACY_STORY.equals(edition)) {
            edition = NEW_STORY;
            story = loadStory(edition);
        }
        changeGame(BranchingGame.start(story), -1, null);
    }

    private void showEndingArchive() {
        List<String> keys = new ArrayList<>();
        for (String key : progress.getAll().keySet()) {
            if (key.startsWith(ENDING_PREFIX)) {
                keys.add(key);
            }
        }
        Collections.sort(keys);
        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.ending_archive_title)
                .setPositiveButton(R.string.close, null);
        if (keys.isEmpty()) {
            dialog.setMessage(R.string.ending_archive_empty).show();
            return;
        }
        List<EndingRecord> endings = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (String key : keys) {
            EndingRecord record = EndingRecord.decode(progress.getString(key, ""));
            if (record != null) {
                endings.add(record);
                labels.add(getString(R.string.ending_archive_entry, record.title, record.category));
            }
        }
        if (endings.isEmpty()) {
            dialog.setMessage(R.string.ending_archive_empty);
        } else {
            dialog.setItems(labels.toArray(new String[0]), (window, index) -> showArchivedEnding(endings.get(index)));
        }
        dialog.show();
    }

    private void showArchivedEnding(EndingRecord record) {
        LinearLayout contents = column();
        contents.setPadding(dp(18), dp(4), dp(18), dp(12));
        SceneArt illustration = new SceneArt(this);
        illustration.show(record.art);
        add(contents, illustration, 0);
        TextView category = text(14, BRICK, true);
        category.setText(getString(R.string.ending_type, record.category));
        add(contents, category, 12);
        TextView epilogue = text(17, INK, false);
        epilogue.setLineSpacing(dp(5), 1);
        epilogue.setText(record.description);
        add(contents, epilogue, 8);
        ScrollView frame = new ScrollView(this);
        frame.addView(contents);
        new AlertDialog.Builder(this)
                .setTitle(record.title)
                .setView(frame)
                .setNegativeButton(R.string.ending_archive, (dialog, which) -> showEndingArchive())
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private void showInventory() {
        TextView heading = text(21, INK, true);
        heading.setText(R.string.inventory);
        heading.setPadding(dp(24), dp(20), dp(24), dp(8));
        LinearLayout contents = column();
        contents.setPadding(dp(16), dp(4), dp(16), dp(12));
        Set<BranchingGame.Item> items = game.inventory();
        InventoryGrid grid = new InventoryGrid(this, items);
        add(contents, grid, 0);
        TextView itemName = text(17, INK, true);
        itemName.setVisibility(View.GONE);
        add(contents, itemName, 10);
        TextView itemDescription = text(15, INK, false);
        itemDescription.setLineSpacing(dp(3), 1);
        itemDescription.setText(items.isEmpty() ? R.string.inventory_empty : R.string.inventory_hint);
        add(contents, itemDescription, 6);
        ScrollView inventoryScroll = new ScrollView(this);
        inventoryScroll.addView(contents);
        grid.setOnItemSelected(item -> {
            itemName.setText(item.name);
            itemName.setVisibility(View.VISIBLE);
            itemDescription.setText(item.description);
            itemDescription.post(() -> {
                itemDescription.requestRectangleOnScreen(
                        new android.graphics.Rect(0, 0, itemDescription.getWidth(),
                                itemDescription.getHeight()));
            });
            itemDescription.announceForAccessibility(item.name + ". " + item.description);
        });
        inventoryDialog = new AlertDialog.Builder(this)
                .setCustomTitle(heading)
                .setView(inventoryScroll)
                .setPositiveButton(R.string.close, null)
                .show();
        Button close = inventoryDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        close.setTypeface(bold);
        close.setTextColor(BRICK);
    }

    private void changeGame(BranchingGame next, int nextInterlude, Story.Motion motion) {
        game = next;
        interludeIndex = nextInterlude;
        saveProgress();
        showScene();
        if (motion != null) {
            artwork.play(motion);
        }
        scroll.scrollTo(0, 0);
        title.announceForAccessibility(title.getText());
    }

    private void saveProgress() {
        SharedPreferences.Editor editor = progress.edit().putString(SAVE, game.save())
                .putInt(INTERLUDE_INDEX, interludeIndex).putString(STORY_EDITION, edition);
        recordEnding(editor).apply();
    }

    private SharedPreferences.Editor recordEnding(SharedPreferences.Editor editor) {
        if (game.isEnding() && interludeIndex < 0) {
            Story.Ending ending = game.ending();
            String key = ENDING_PREFIX + edition + "_" + ending.id;
            editor.putString(key, new EndingRecord(ending.title, ending.category,
                    ending.art, game.description()).encode());
        }
        return editor;
    }

    private static final class EndingRecord {
        final String title;
        final String category;
        final Story.Art art;
        final String description;

        EndingRecord(String title, String category, Story.Art art, String description) {
            this.title = title;
            this.category = category;
            this.art = art;
            this.description = description;
        }

        String encode() {
            return title + "\n" + category + "\n" + art.name() + "\n" + description;
        }

        static EndingRecord decode(String saved) {
            String[] fields = saved.split("\n", 4);
            if (fields.length != 4) {
                return null;
            }
            try {
                return new EndingRecord(fields[0], fields[1], Story.Art.valueOf(fields[2]), fields[3]);
            } catch (IllegalArgumentException invalidArt) {
                return null;
            }
        }
    }

    private void addInfluence(BranchingGame.Choice influence) {
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

    private Button headerButton() {
        Button button = actionButton(14, 48);
        button.setPadding(dp(10), dp(8), dp(10), dp(8));
        button.setBackground(paperButton(0xFFEEDBC0, false));
        button.setGravity(Gravity.CENTER);
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

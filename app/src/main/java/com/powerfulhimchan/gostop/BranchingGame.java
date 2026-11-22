package com.powerfulhimchan.gostop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

final class BranchingGame {
    static final int DECISIONS_PER_RUN = 3;

    private final List<Choice> history;
    private final Set<Item> inventory;
    final Scene scene;

    private BranchingGame(Scene scene, List<Choice> history) {
        this.scene = scene;
        this.history = history;
        Set<Item> items = EnumSet.noneOf(Item.class);
        for (Choice choice : history) {
            if (choice.reward != null) {
                items.add(choice.reward);
            }
        }
        inventory = Collections.unmodifiableSet(items);
    }

    static BranchingGame start() {
        return new BranchingGame(Scene.ARRIVAL, Collections.emptyList());
    }

    BranchingGame choose(Choice choice) {
        if (!choices().contains(choice)) {
            throw new IllegalArgumentException("Choice is unavailable at " + scene);
        }
        return advance(choice);
    }

    private BranchingGame advance(Choice choice) {
        List<Choice> nextHistory = new ArrayList<>(history);
        nextHistory.add(choice);
        return new BranchingGame(choice.destination(), nextHistory);
    }

    int decisionNumber() {
        return history.size() + 1;
    }

    boolean isEnding() {
        return scene.pair == null;
    }

    Set<Item> inventory() {
        return inventory;
    }

    List<Choice> choices() {
        if (isEnding()) {
            return Collections.emptyList();
        }
        if (scene == Scene.ROPES && inventory.contains(Item.SAFETY_CLIP)) {
            return Pair.CLIPPED_ROPES.choices;
        }
        return scene.pair.choices;
    }

    String description() {
        if (scene == Scene.ROPES && inventory.contains(Item.SAFETY_CLIP)) {
            return "안내인이 매듭을 단단히 조였다.\n안전 고리를 걸면 흔들리는 다리에서 몸을 지탱할 수 있다.";
        }
        if (scene == Scene.TOGETHER && history.get(history.size() - 1) == Choice.CLIP_AND_CROSS) {
            return "안전 고리를 밧줄에 건 채 다리를 건넜다.\n맞은편에 도착해 고리를 풀자 안내인이 따라온다.";
        }
        return scene.description;
    }

    BranchingGame replayBranch() {
        if (!isEnding()) {
            throw new IllegalStateException("Replay is available at an ending");
        }
        return start().choose(history.get(0));
    }

    String save() {
        StringBuilder value = new StringBuilder();
        for (Choice choice : history) {
            if (value.length() > 0) {
                value.append(',');
            }
            value.append(choice.saveId);
        }
        return value.toString();
    }

    static BranchingGame restore(String saved) {
        BranchingGame game = start();
        for (String id : saved.split(",")) {
            Choice next = game.findChoice(id);
            if (next == null) {
                break;
            }
            game = game.advance(next);
        }
        return game;
    }

    private Choice findChoice(String id) {
        for (Choice choice : choices()) {
            if (choice.saveId.equals(id)) {
                return choice;
            }
        }
        // Earlier saves could cross after helping the guide, before that action awarded a clip.
        if (scene == Scene.ROPES && Choice.CROSS.saveId.equals(id)) {
            return Choice.CROSS;
        }
        return null;
    }

    List<Influence> endingInfluences() {
        if (!isEnding()) {
            throw new IllegalStateException("Influences are available at an ending");
        }

        Choice opening = history.get(0);
        Choice preparation = history.get(1);
        Choice decision = history.get(2);
        if (scene == Scene.WAIT) {
            return Arrays.asList(
                    new Influence(preparation.label, preparation.waitEffect),
                    new Influence(decision.label, "물이 잦아들 때까지 안전한 곳에서 기다렸어요."));
        }
        return Arrays.asList(
                new Influence(opening.label, opening.crossingEffect),
                new Influence(preparation.label, preparation.crossingEffect),
                new Influence(decision.label, decision.crossingEffect));
    }

    enum Scene {
        ARRIVAL("흔들리는 다리",
                "불어난 물이 발밑을 휘감는다.\n다리 끝에 밧줄을 붙잡고 있는 사람이 보인다.",
                Pair.OPENING),
        GUIDE("밧줄을 붙든 사람",
                "안내인이 느슨해진 밧줄을 붙잡고 있다.\n내 손에는 매듭을 비춰 줄 등불이 있다.",
                Pair.GUIDE),
        BANK("강가의 흔적",
                "물보라 사이로 돌턱이 보인다.\n맞은편에서는 안내인이 풀린 매듭을 더듬는다.",
                Pair.BANK),
        ROPES("다시 팽팽해진 밧줄",
                "안내인이 매듭을 단단히 조인다.\n“한 사람씩 발을 옮겨요.”",
                Pair.ROPES),
        LEDGE("바위 아래 좁은 길",
                "강물이 다리 기둥을 때린다.\n바위 아래 돌턱은 맞은편 기슭까지 이어진다.",
                Pair.LEDGE),
        TOGETHER("함께 건넌 길",
                "안내인과 번갈아 밧줄을 잡으며 다리를 건넜다.\n뒤를 돌아보니 숲 양쪽에서 등불이 흔들린다.",
                null),
        SIDE_PATH("물가에 남은 발자국",
                "돌턱을 하나씩 밟아 맞은편 기슭에 닿았다.\n건너온 길에 발자국이 길게 남아 있다.",
                null),
        WAIT("비가 그칠 때까지",
                "나무 아래로 물러나 등불을 내려놓았다.\n물이 빠지고 잎 사이로 빛이 든다.",
                null);

        final String title;
        final String description;
        private final Pair pair;

        Scene(String title, String description, Pair pair) {
            this.title = title;
            this.description = description;
            this.pair = pair;
        }
    }

    private enum Pair {
        OPENING(Choice.HELP, Choice.LOOK),
        GUIDE(Choice.LAMP, Choice.ASK),
        BANK(Choice.INSPECT, Choice.SIGNAL),
        ROPES(Choice.CROSS, Choice.RETREAT),
        CLIPPED_ROPES(Choice.CLIP_AND_CROSS, Choice.RETREAT),
        LEDGE(Choice.FOLLOW, Choice.RETREAT);

        final List<Choice> choices;

        Pair(Choice first, Choice second) {
            choices = Collections.unmodifiableList(Arrays.asList(first, second));
        }
    }

    enum Item {
        SAFETY_CLIP("안전 고리", "안내인이 건네준 금속 고리. 난간 밧줄에 걸어 몸을 지탱할 수 있다.");

        final String name;
        final String description;

        Item(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }

    enum Choice {
        HELP("help", "밧줄을 붙든 사람에게 다가간다",
                "안내인에게 말을 걸어 다리를 살필 기회를 얻었어요.", null),
        LOOK("look", "강가를 먼저 살핀다",
                "강가에서 돌턱과 안내인의 위치를 확인했어요.", null),
        LAMP("lamp", "안내인에게 등불을 비춰 준다",
                "안내인이 매듭을 다시 묶고 안전 고리를 건네줬어요.",
                "새로 묶은 난간 아래로도 강물이 세차게 흐르는 걸 봤어요.", Item.SAFETY_CLIP),
        ASK("ask", "다리 말고 다른 길을 묻는다",
                "안내인에게서 물가의 돌턱이 맞은편까지 이어진다는 걸 들었어요.",
                "안내인이 물이 더 오르면 돌턱도 잠긴다고 알려 줬어요."),
        INSPECT("inspect", "물가의 돌턱이 이어지는지 확인한다",
                "직접 확인한 돌턱이 반대편 기슭까지 길을 이어 줬어요.",
                "제일 낮은 돌턱까지 물이 차오르는 걸 확인했어요."),
        SIGNAL("signal", "등불을 들어 안내인에게 신호를 보낸다",
                "등불 신호로 안내인에게 다리의 풀린 매듭을 알렸어요.",
                "안내인과 함께 물살이 약해질 때까지 기다릴 곳을 확인했어요."),
        CROSS("cross", "안내인과 함께 다리를 건넌다",
                "서로가 발을 옮길 때마다 밧줄을 잡아 주며 건넜어요.", null),
        CLIP_AND_CROSS("clip_cross", "안전 고리를 걸고 다리를 건넌다",
                "안전 고리를 밧줄에 걸어 몸을 지탱하며 건넜어요.", null),
        FOLLOW("follow", "물가의 돌턱을 따라 건넌다",
                "확인한 길을 따라 반대편 기슭에 닿았어요.", null),
        RETREAT("retreat", "안전한 곳으로 돌아가 기다린다", null, null);

        final String saveId;
        final String label;
        final String crossingEffect;
        final String waitEffect;
        final Item reward;

        Choice(String saveId, String label, String crossingEffect, String waitEffect) {
            this(saveId, label, crossingEffect, waitEffect, null);
        }

        Choice(String saveId, String label, String crossingEffect, String waitEffect, Item reward) {
            this.saveId = saveId;
            this.label = label;
            this.crossingEffect = crossingEffect;
            this.waitEffect = waitEffect;
            this.reward = reward;
        }

        Scene destination() {
            switch (this) {
                case HELP: return Scene.GUIDE;
                case LOOK: return Scene.BANK;
                case LAMP:
                case SIGNAL: return Scene.ROPES;
                case ASK:
                case INSPECT: return Scene.LEDGE;
                case CROSS:
                case CLIP_AND_CROSS: return Scene.TOGETHER;
                case FOLLOW: return Scene.SIDE_PATH;
                case RETREAT: return Scene.WAIT;
                default: throw new AssertionError(this);
            }
        }
    }

    static final class Influence {
        final String label;
        final String effect;

        Influence(String label, String effect) {
            this.label = label;
            this.effect = effect;
        }
    }
}

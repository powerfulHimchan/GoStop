package com.powerfulhimchan.gostop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

final class BranchingGame {
    static final int DECISIONS_PER_ACT = 10;
    static final int ACTS = 5;
    static final int DECISIONS_PER_RUN = ACTS * DECISIONS_PER_ACT;

    private final Story story;
    private final List<Choice> history;
    private final EnumSet<Mark> marks;
    private final Story.Ending ending;

    private BranchingGame(Story story, List<Choice> history, EnumSet<Mark> marks, Story.Ending ending) {
        this.story = story;
        this.history = history;
        this.marks = marks;
        this.ending = ending;
    }

    static BranchingGame start(Story story) {
        return new BranchingGame(story, Collections.emptyList(), EnumSet.noneOf(Mark.class), null);
    }

    static BranchingGame restore(Story story, String saved) {
        return restore(story, saved, false);
    }

    static BranchingGame restorePrevious(Story story, String saved) {
        return restore(story, saved, true);
    }

    private static BranchingGame restore(Story story, String saved, boolean previousVersion) {
        BranchingGame game = start(story);
        for (String key : saved.split(",")) {
            List<Choice> options = game.choices();
            Choice next = null;
            for (Choice option : options) {
                if (option.saveId.equals(key)) {
                    next = option;
                    break;
                }
            }
            // Updated branches can introduce a new scene at an existing decision.
            if (next == null && previousVersion && !options.isEmpty() && key.matches("[a-z0-9_]+:[ab]")) {
                next = options.get(key.endsWith(":a") ? 0 : 1);
            }
            if (next == null) {
                break;
            }
            game = game.choose(next);
        }
        return game;
    }

    BranchingGame choose(Choice choice) {
        if (!choices().contains(choice)) {
            throw new IllegalArgumentException("Unavailable choice: " + choice.saveId);
        }
        List<Choice> nextHistory = new ArrayList<>(history);
        nextHistory.add(choice);
        EnumSet<Mark> nextMarks = marks.clone();
        nextMarks.removeAll(choice.removed);
        nextMarks.addAll(choice.added);
        return new BranchingGame(story, nextHistory, nextMarks, story.ending(choice.endingId));
    }

    int decisionNumber() {
        return history.size() + 1;
    }

    int act() {
        return (decisionNumber() - 1) / DECISIONS_PER_ACT + 1;
    }

    boolean isEnding() {
        return ending != null;
    }

    Story.Ending ending() {
        return ending;
    }

    Story.Scene scene() {
        return story.scene(decisionNumber(), marks);
    }

    List<Choice> choices() {
        return isEnding() ? Collections.emptyList() : scene().choices;
    }

    String title() {
        return isEnding() ? ending.title : scene().title;
    }

    String description() {
        return isEnding() ? ending.description(marks) : scene().description;
    }

    Story.Art art() {
        return isEnding() ? ending.art : scene().art;
    }

    List<Story.Interlude> interludes() {
        return history.isEmpty() ? Collections.emptyList() : history.get(history.size() - 1).interludes;
    }

    Set<Item> inventory() {
        EnumSet<Item> items = EnumSet.noneOf(Item.class);
        for (Item item : Item.values()) {
            if (marks.contains(item.mark)) {
                items.add(item);
            }
        }
        return Collections.unmodifiableSet(items);
    }

    BranchingGame replayAct(int act) {
        if (!isEnding() || act < 1 || act > ACTS) {
            throw new IllegalArgumentException("Invalid replay act");
        }
        BranchingGame game = start(story);
        int decisions = (act - 1) * DECISIONS_PER_ACT;
        for (int index = 0; index < decisions; index++) {
            game = game.choose(history.get(index));
        }
        return game;
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

    List<Choice> endingInfluences() {
        if (!isEnding()) {
            throw new IllegalStateException("No ending");
        }

        List<Choice> evidence = new ArrayList<>();
        int last = history.size() - 1;
        for (Mark reason : ending.reasons) {
            for (int index = last - 1; index >= 0; index--) {
                Choice choice = history.get(index);
                if (choice.added.contains(reason) && marks.contains(reason)) {
                    if (!evidence.contains(choice)) {
                        evidence.add(choice);
                    }
                    break;
                }
            }
            if (evidence.size() == 2) {
                break;
            }
        }
        evidence.add(history.get(last));
        Collections.sort(evidence,
                (left, right) -> Integer.compare(history.indexOf(left), history.indexOf(right)));
        return evidence;
    }

    enum Mark {
        STOPPED, KEYS, NOTEBOOK, SHOES, GUITAR, HANDOFF, PORTFOLIO, MANUSCRIPT,
        REST, PRESSURE, FRIEND, QUIET, RUN, COOK, MUSIC, METRICS, RESTART,
        WORK, WRITE, SEARCH, CASH, DELEGATE, PILOT, EDITOR, APPLIED, OFFER,
        SOLD, NO_PLAN, STRAIN, OUT_OF_OFFICE, LOSING_FRIEND,
        TEAM, AUTHOR, LEAVE, STAY, BOOK, STRETCH, COUNT, HOUSE, DISTANCE,
        SLIP, SAVED_NIGHT, REPORTED_DELAY, ROUTE_TEST, SAW_PATTERN,
        FOLLOW_RECORD, FOLLOW_LIFE, PATH_COMPANY, PATH_BOOK, PATH_BUSAN, PATH_LOCAL,
        COMPANY_SHARED, COMPANY_ALONE, BOOK_TOGETHER, BOOK_DEADLINE, BUSAN_STAY, BUSAN_OTHER,
        LOCAL_PRESENT, LOCAL_MISSED, JOB_LETTER, LOCAL_JOB, TRUST, KEEP_DAY, PROJECT_LEFT, REGRET
    }

    enum Item {
        KEYS(Mark.KEYS, "사원증과 열쇠", "늦은 밤, 경비실에서 돌려받은 물건. 야근을 마치고 집에 가던 날 떨어뜨렸다."),
        JOURNAL(Mark.NOTEBOOK, "작은 노트", "회사 일과 상관없는 문장을 적기 시작한 노트."),
        SHOES(Mark.SHOES, "오래된 운동화", "현관 안쪽에 있던 운동화. 걸어서 돌아오기에도 충분하다."),
        GUITAR(Mark.GUITAR, "통기타", "준호가 오래 간직해 온 악기. 서랍에는 접힌 악보가 함께 있었다."),
        HANDOFF(Mark.HANDOFF, "인계 메모", "업무를 나눌 수 있도록 담당자와 남은 일정을 정리한 메모."),
        PORTFOLIO(Mark.PORTFOLIO, "포트폴리오", "준호가 맡았던 일과 함께 일한 사람을 기록한 문서."),
        MANUSCRIPT(Mark.MANUSCRIPT, "원고 묶음", "일 년 넘게 써 온 글 가운데 다시 읽고 싶은 문장들을 모은 원고."),
        SLIP(Mark.SLIP, "첫 교정지", "인쇄 전에 확인하려고 뽑은 종이. 작가가 고친 문장 옆에 준호의 개인 번호와 ‘이번 주만’이라는 메모가 남아 있다."),
        JOB_LETTER(Mark.JOB_LETTER, "서면 채용 제안서", "부산의 지역 출판사가 제안한 입사일과 편집 업무를 적은 종이. 채용이 미뤄졌을 때 원래 조건을 확인할 수 있다.");

        final Mark mark;
        final String name;
        final String description;

        Item(Mark mark, String name, String description) {
            this.mark = mark;
            this.name = name;
            this.description = description;
        }

        String displayName(boolean legacy) {
            return legacy && this == SLIP ? "야간 배송 전표" : name;
        }

        String displayDescription(boolean legacy) {
            return legacy && this == SLIP
                    ? "배송 기사가 건넨 종이. 맨 아래 야간 연락처에 이준호라는 이름이 적혀 있다."
                    : description;
        }
    }

    static final class Choice {
        final String saveId;
        final String label;
        final String effect;
        final EnumSet<Mark> added;
        final EnumSet<Mark> removed;
        final int endingId;
        final Story.Motion motion;
        final List<Story.Interlude> interludes;

        Choice(String saveId, String label, String effect, EnumSet<Mark> added,
               EnumSet<Mark> removed, int endingId, Story.Motion motion,
               List<Story.Interlude> interludes) {
            this.saveId = saveId;
            this.label = label;
            this.effect = effect;
            this.added = added;
            this.removed = removed;
            this.endingId = endingId;
            this.motion = motion;
            this.interludes = interludes;
        }
    }
}

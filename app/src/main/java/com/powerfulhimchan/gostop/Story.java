package com.powerfulhimchan.gostop;

import com.powerfulhimchan.gostop.BranchingGame.Choice;
import com.powerfulhimchan.gostop.BranchingGame.Mark;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Story {
    private final List<List<Scene>> decisions;
    private final Map<Integer, Ending> endings;

    private Story(List<List<Scene>> decisions, Map<Integer, Ending> endings) {
        this.decisions = decisions;
        this.endings = endings;
    }

    static Story read(Reader sceneSource, Reader endingSource) throws IOException {
        Map<Integer, Ending> endings = new HashMap<>();
        for (List<String> block : blocks(endingSource)) {
            require(block.size() >= 6, "Ending needs two epilogue paragraphs: " + block.get(0));
            String[] header = fields(block.get(0), 5);
            int id = Integer.parseInt(header[0]);
            List<List<Paragraph>> epilogue = new ArrayList<>();
            List<Paragraph> variants = new ArrayList<>();
            for (int index = 2; index < block.size(); index++) {
                String[] row = fields(block.get(index), 2);
                Condition condition = Condition.parse(row[0]);
                variants.add(new Paragraph(condition, row[1]));
                if (condition.isFallback()) {
                    require(variants.size() >= 2, "Missing epilogue variation: " + id);
                    epilogue.add(variants);
                    variants = new ArrayList<>();
                }
            }
            require(epilogue.size() == 2 && variants.isEmpty(),
                    "Missing epilogue fallback: " + id);
            Ending ending = new Ending(id, header[1], Art.valueOf(header[2]),
                    header[3], block.get(1), parseMarks(header[4]), epilogue);
            require(id > 0 && endings.put(id, ending) == null, "Duplicate ending: " + id);
        }

        int count = BranchingGame.DECISIONS_PER_RUN;
        List<List<Scene>> decisions = new ArrayList<>();
        for (int step = 0; step < count; step++) {
            decisions.add(new ArrayList<>());
        }
        Set<String> sceneIds = new HashSet<>();
        Set<Integer> referencedEndings = new HashSet<>();
        for (List<String> block : blocks(sceneSource)) {
            require(block.size() >= 4, "Scene needs two choices: " + block.get(0));
            String[] header = fields(block.get(0), 5);
            int step = Integer.parseInt(header[0]);
            require(step >= 1 && step <= count, "Step out of range: " + step);
            String id = header[1];
            require(id.matches("[a-z0-9_]+") && sceneIds.add(id), "Invalid or duplicate scene ID: " + id);
            List<Interlude> firstInterludes = new ArrayList<>();
            List<Interlude> secondInterludes = new ArrayList<>();
            for (int index = 4; index < block.size(); index++) {
                String[] row = fields(block.get(index), 5);
                require(row[0].equals(">a") || row[0].equals(">b") || row[0].equals(">both"),
                        "Unknown interlude branch: " + id);
                Interlude interlude = new Interlude(Art.valueOf(row[1]), Motion.valueOf(row[2]),
                        row[3], row[4].replace("\\n", "\n"));
                if (!row[0].equals(">b")) {
                    firstInterludes.add(interlude);
                }
                if (!row[0].equals(">a")) {
                    secondInterludes.add(interlude);
                }
            }
            Choice first = parseChoice(id + ":a", block.get(2), firstInterludes, Motion.ENTER_LEFT);
            Choice second = parseChoice(id + ":b", block.get(3), secondInterludes, Motion.ENTER_RIGHT);
            for (Choice choice : Arrays.asList(first, second)) {
                if (step == count) {
                    require(endings.containsKey(choice.endingId), "Final choice needs a known ending: " + id);
                    referencedEndings.add(choice.endingId);
                } else {
                    require(choice.endingId == 0, "Early ending: " + id);
                }
            }
            List<Scene> stage = decisions.get(step - 1);
            require(stage.isEmpty() || !stage.get(stage.size() - 1).condition.isFallback(),
                    "Fallback must come last: " + id);
            stage.add(new Scene(id, Art.valueOf(header[2]), Condition.parse(header[3]),
                    header[4], block.get(1).replace("\\n", "\n"),
                    Collections.unmodifiableList(Arrays.asList(first, second))));
        }
        for (int step = 0; step < count; step++) {
            List<Scene> stage = decisions.get(step);
            require(!stage.isEmpty() && stage.get(stage.size() - 1).condition.isFallback(),
                    "Missing scene fallback at " + (step + 1));
        }
        require(referencedEndings.equals(endings.keySet()), "Unreferenced ending");
        return new Story(decisions, endings);
    }

    Scene scene(int step, EnumSet<Mark> marks) {
        for (Scene scene : decisions.get(step - 1)) {
            if (scene.condition.matches(marks)) {
                return scene;
            }
        }
        throw new IllegalStateException("Missing scene at " + step);
    }

    Ending ending(int id) {
        return endings.get(id);
    }

    private static Choice parseChoice(String id, String line, List<Interlude> interludes,
                                      Motion defaultMotion) throws IOException {
        String[] row = line.split("\\|", -1);
        require(row.length == 4 || row.length == 5, "Invalid choice: " + id);
        EnumSet<Mark> added = EnumSet.noneOf(Mark.class);
        EnumSet<Mark> removed = EnumSet.noneOf(Mark.class);
        if (!row[2].isEmpty()) {
            for (String token : row[2].split(",")) {
                if (token.startsWith("-")) {
                    removed.add(Mark.valueOf(token.substring(1)));
                } else {
                    added.add(Mark.valueOf(token));
                }
            }
        }
        require(Collections.disjoint(added, removed), "Conflicting mark changes: " + id);
        return new Choice(id, row[0], row[1], added, removed,
                row[3].isEmpty() ? 0 : Integer.parseInt(row[3]),
                row.length == 5 ? Motion.valueOf(row[4]) : defaultMotion,
                Collections.unmodifiableList(interludes));
    }

    private static List<List<String>> blocks(Reader source) throws IOException {
        BufferedReader input = new BufferedReader(source);
        List<List<String>> blocks = new ArrayList<>();
        List<String> current = null;
        for (String next = input.readLine(); next != null; next = input.readLine()) {
            String line = next.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("@")) {
                current = new ArrayList<>();
                blocks.add(current);
                current.add(line.substring(1));
            } else {
                require(current != null, "Content before record header");
                current.add(line);
            }
        }
        return blocks;
    }

    private static String[] fields(String line, int size) throws IOException {
        String[] row = line.split("\\|", -1);
        require(row.length == size, "Expected " + size + " fields: " + line);
        return row;
    }

    private static List<Mark> parseMarks(String field) {
        List<Mark> result = new ArrayList<>();
        if (!field.isEmpty()) {
            for (String token : field.split(",")) {
                result.add(Mark.valueOf(token));
            }
        }
        return result;
    }

    private static void require(boolean condition, String error) throws IOException {
        if (!condition) {
            throw new IOException(error);
        }
    }

    enum Art {
        ALLEY, OFFICE, KITCHEN, RIVER, DESK, CAFE, SUBWAY, HOME, ENTRY, WALK, DEPOT, GUITAR
    }

    enum Motion {
        ENTER_LEFT, ENTER_RIGHT, RUN, STOP, PHONE, HANDOFF, TRUCK, REVEAL
    }

    static final class Interlude {
        final Art art;
        final Motion motion;
        final String title;
        final String description;

        Interlude(Art art, Motion motion, String title, String description) {
            this.art = art;
            this.motion = motion;
            this.title = title;
            this.description = description;
        }
    }

    static final class Scene {
        final String id;
        final Art art;
        final Condition condition;
        final String title;
        final String description;
        final List<Choice> choices;

        Scene(String id, Art art, Condition condition, String title, String description, List<Choice> choices) {
            this.id = id;
            this.art = art;
            this.condition = condition;
            this.title = title;
            this.description = description;
            this.choices = choices;
        }
    }

    static final class Ending {
        final int id;
        final String category;
        final Art art;
        final String title;
        final String body;
        final List<Mark> reasons;
        private final List<List<Paragraph>> epilogue;

        Ending(int id, String category, Art art, String title, String body, List<Mark> reasons,
               List<List<Paragraph>> epilogue) {
            this.id = id;
            this.category = category;
            this.art = art;
            this.title = title;
            this.body = body;
            this.reasons = reasons;
            this.epilogue = epilogue;
        }

        String description(EnumSet<Mark> marks) {
            return body + "\n\n" + paragraph(0, marks) + "\n\n" + paragraph(1, marks);
        }

        private String paragraph(int index, EnumSet<Mark> marks) {
            for (Paragraph variant : epilogue.get(index)) {
                if (variant.condition.matches(marks)) {
                    return variant.text;
                }
            }
            throw new IllegalStateException("Missing epilogue paragraph: " + id);
        }
    }

    private static final class Paragraph {
        final Condition condition;
        final String text;

        Paragraph(Condition condition, String text) {
            this.condition = condition;
            this.text = text;
        }
    }

    private static final class Condition {
        private final EnumSet<Mark> required;
        private final EnumSet<Mark> forbidden;

        Condition(EnumSet<Mark> required, EnumSet<Mark> forbidden) {
            this.required = required;
            this.forbidden = forbidden;
        }

        static Condition parse(String field) throws IOException {
            EnumSet<Mark> required = EnumSet.noneOf(Mark.class);
            EnumSet<Mark> forbidden = EnumSet.noneOf(Mark.class);
            if (!field.isEmpty()) {
                for (String token : field.split(",")) {
                    if (token.startsWith("!")) {
                        forbidden.add(Mark.valueOf(token.substring(1)));
                    } else {
                        required.add(Mark.valueOf(token));
                    }
                }
            }
            require(Collections.disjoint(required, forbidden), "Conflicting requirements: " + field);
            return new Condition(required, forbidden);
        }

        boolean matches(EnumSet<Mark> marks) {
            return marks.containsAll(required) && Collections.disjoint(marks, forbidden);
        }

        boolean isFallback() {
            return required.isEmpty() && forbidden.isEmpty();
        }
    }
}

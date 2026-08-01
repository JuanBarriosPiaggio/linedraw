import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Level + sound asset generator for Line Draw.
 *
 * Run from the repo root with:  java tools/LevelGen.java
 *
 * Generates:
 *   app/src/main/assets/levels.json   — 60 levels in 4 difficulty tiers (3x3..6x6).
 *     Each level is built around a randomly generated Hamiltonian path (so a
 *     solution is guaranteed by construction) and then re-verified with an
 *     independent DFS solver before being accepted.
 *   app/src/main/res/raw/{connect,complete,stuck}.wav — soft UI chimes.
 */
public class LevelGen {

    static final Random RNG = new Random(20260801L);

    public static void main(String[] args) throws IOException {
        Path root = Paths.get("").toAbsolutePath();
        Path assets = root.resolve("app/src/main/assets");
        Path raw = root.resolve("app/src/main/res/raw");
        Files.createDirectories(assets);
        Files.createDirectories(raw);

        StringBuilder json = new StringBuilder();
        json.append("{\n  \"levels\": [\n");

        Set<String> seenCanonical = new HashSet<>();
        for (int id = 1; id <= 60; id++) {
            Level level;
            Shape shape = SHAPES.get(id);
            if (shape != null) {
                level = buildShapeLevel(id, shape);
                System.out.printf("Level %2d: shape \"%s\", %d dots, %d edges%n",
                    id, shape.name, level.dotsOverride.size(), level.edges.size());
            } else {
                LevelSpec spec = specFor(id);
                level = generateLevel(id, spec, seenCanonical);
                System.out.printf(
                    "Level %2d: %dx%d, %2d edges, solvable from %d/%d starts%n",
                    id, spec.gridSize, spec.gridSize, level.edges.size(),
                    level.solvableStarts, spec.gridSize * spec.gridSize);
            }
            json.append(level.toJson());
            if (id < 60) json.append(",");
            json.append("\n");
        }
        json.append("  ]\n}\n");

        Files.write(assets.resolve("levels.json"), json.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("Wrote " + assets.resolve("levels.json") + " (60 levels, all validated)");

        writeWav(raw.resolve("connect.wav"), connectSound());
        writeWav(raw.resolve("complete.wav"), completeSound());
        writeWav(raw.resolve("stuck.wav"), stuckSound());
        System.out.println("Wrote sound effects to " + raw);
    }

    // ── Difficulty curve ─────────────────────────────────────────

    static class LevelSpec {
        int gridSize;
        int extraMin, extraMax;
        /** Accepted range for the number of dots the level can be solved from. */
        int minStarts, maxStarts;

        LevelSpec(int gridSize, int extraMin, int extraMax, int minStarts, int maxStarts) {
            this.gridSize = gridSize;
            this.extraMin = extraMin;
            this.extraMax = extraMax;
            this.minStarts = minStarts;
            this.maxStarts = maxStarts;
        }
    }

    /**
     * Per-level difficulty targets.
     *
     * Levels 1-5 are a forgiving 3x3 tutorial (solvable from many starts).
     * From level 6 the grid grows quickly and — the real difficulty lever —
     * the number of starting dots that can complete the puzzle is pushed
     * steadily down, so late levels require hunting for one of only 2-3
     * viable starting dots and route-planning around decoy edges.
     */
    static LevelSpec specFor(int id) {
        if (id <= 3)  return new LevelSpec(3, 2, 3, 5, 9);   // teach the mechanic
        if (id <= 5)  return new LevelSpec(3, 3, 4, 3, 6);   // first gentle bite
        if (id <= 9)  return new LevelSpec(4, 4, 6, 4, 8);   // bigger board
        if (id <= 14) return new LevelSpec(4, 6, 8, 2, 4);   // start-dot hunting begins
        if (id <= 20) return new LevelSpec(5, 6, 9, 3, 6);
        if (id <= 26) return new LevelSpec(5, 8, 11, 1, 3);
        if (id <= 34) return new LevelSpec(6, 8, 11, 3, 6);
        if (id <= 44) return new LevelSpec(6, 10, 13, 2, 4);
        return new LevelSpec(6, 11, 14, 1, 2);               // endgame: 1-2 viable starts
    }

    // ── Shape levels ─────────────────────────────────────────────

    static class Shape {
        String name;
        int[][] dots;   // {x, y} per dot id, on a 0..12 virtual grid
        int[][] edges;  // {dotA, dotB}

        Shape(String name, int[][] dots, int[][] edges) {
            this.name = name;
            this.dots = dots;
            this.edges = edges;
        }
    }

    /**
     * Hand-crafted shape levels woven into the progression: geometric forms
     * mid-game, animals later. Each is validated and given a computed solution
     * before shipping, exactly like procedural levels.
     */
    static final java.util.Map<Integer, Shape> SHAPES = java.util.Map.of(
        8, new Shape("Star",
            new int[][]{{6, 0}, {12, 4}, {10, 11}, {2, 11}, {0, 4}},
            new int[][]{{0, 2}, {2, 4}, {4, 1}, {1, 3}, {3, 0}}),
        14, new Shape("Octagon",
            new int[][]{{6, 0}, {10, 2}, {12, 6}, {10, 10}, {6, 12}, {2, 10}, {0, 6}, {2, 2}, {6, 6}},
            new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7}, {7, 0},
                        {8, 0}, {8, 2}, {8, 4}, {8, 6}}),
        20, new Shape("Diamond",
            new int[][]{{6, 0}, {12, 6}, {6, 12}, {0, 6}, {6, 3}, {9, 6}, {6, 9}, {3, 6}},
            new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4},
                        {0, 4}, {1, 5}, {2, 6}, {3, 7}}),
        26, new Shape("Rings",
            new int[][]{{6, 0}, {10, 2}, {12, 6}, {10, 10}, {6, 12}, {2, 10}, {0, 6}, {2, 2},
                        {6, 3}, {8, 4}, {9, 6}, {8, 8}, {6, 9}, {4, 8}, {3, 6}, {4, 4}},
            new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7}, {7, 0},
                        {8, 9}, {9, 10}, {10, 11}, {11, 12}, {12, 13}, {13, 14}, {14, 15}, {15, 8},
                        {0, 8}, {2, 10}, {4, 12}, {6, 14}}),
        34, new Shape("Butterfly",
            new int[][]{{6, 3}, {6, 6}, {6, 9}, {2, 2}, {0, 6}, {2, 10}, {10, 2}, {12, 6}, {10, 10}},
            new int[][]{{0, 1}, {1, 2}, {0, 3}, {3, 4}, {4, 5}, {5, 2}, {0, 6}, {6, 7}, {7, 8}, {8, 2}}),
        40, new Shape("Fish",
            new int[][]{{0, 6}, {3, 3}, {8, 3}, {10, 6}, {8, 9}, {3, 9}, {12, 4}, {12, 8}},
            new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 0}, {3, 6}, {6, 7}, {7, 3}}),
        48, new Shape("Bird",
            new int[][]{{0, 3}, {3, 5}, {6, 7}, {9, 5}, {12, 3}, {5, 10}, {7, 10}},
            new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {2, 5}, {5, 6}, {6, 2}}),
        56, new Shape("Cat",
            new int[][]{{4, 2}, {8, 2}, {11, 5}, {11, 9}, {8, 12}, {4, 12}, {1, 9}, {1, 5}, {2, 0}, {10, 0}},
            new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7}, {7, 0},
                        {0, 8}, {8, 7}, {1, 9}, {9, 2}})
    );

    static Level buildShapeLevel(int id, Shape shape) {
        Level level = new Level();
        level.id = id;
        level.gridSize = 13; // fine virtual grid the shape coordinates live on
        level.name = shape.name;
        level.dotsOverride = new ArrayList<>();
        for (int[] dot : shape.dots) level.dotsOverride.add(dot);
        Set<Long> edgeSet = new HashSet<>();
        for (int[] edge : shape.edges) addEdge(level.edges, edgeSet, edge[0], edge[1]);

        level.solution = findSolutionPath(level);
        if (level.solution == null) {
            throw new IllegalStateException("Shape \"" + shape.name + "\" (level " + id + ") is not solvable");
        }
        return level;
    }

    /** Finds one full path visiting all dots without edge reuse, or null. */
    static List<Integer> findSolutionPath(Level level) {
        int n = level.dotCount();
        List<List<int[]>> adj = buildAdjacency(level, n);
        long full = (1L << n) - 1;
        for (int start = 0; start < n; start++) {
            List<Integer> path = new ArrayList<>();
            path.add(start);
            long[] budget = {400_000};
            if (pathDfs(adj, start, 0L, 1L << start, full, budget, path)) return path;
        }
        return null;
    }

    static boolean pathDfs(List<List<int[]>> adj, int current, long used, long visited, long full,
                           long[] budget, List<Integer> path) {
        if (visited == full) return true;
        if (budget[0]-- <= 0) return false;
        for (int[] option : adj.get(current)) {
            int edge = option[0], next = option[1];
            if ((used & (1L << edge)) != 0) continue;
            path.add(next);
            if (pathDfs(adj, next, used | (1L << edge), visited | (1L << next), full, budget, path)) {
                return true;
            }
            path.remove(path.size() - 1);
        }
        return false;
    }

    // ── Level generation ─────────────────────────────────────────

    static class Level {
        int id;
        int gridSize;
        List<int[]> edges = new ArrayList<>(); // pairs of dot ids
        List<Integer> solution;
        int solvableStarts; // metric only, not serialized
        String name; // shape levels only
        List<int[]> dotsOverride; // shape levels: explicit {x,y} per dot id

        /** Number of dots: full lattice for square levels, explicit list for shapes. */
        int dotCount() {
            return dotsOverride != null ? dotsOverride.size() : gridSize * gridSize;
        }

        String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("    {\"id\": ").append(id)
              .append(", \"gridSize\": ").append(gridSize);
            if (name != null) sb.append(", \"name\": \"").append(name).append("\"");
            sb.append(", \"dots\": [");
            int n = dotCount();
            for (int d = 0; d < n; d++) {
                if (d > 0) sb.append(", ");
                int x = dotsOverride != null ? dotsOverride.get(d)[0] : d % gridSize;
                int y = dotsOverride != null ? dotsOverride.get(d)[1] : d / gridSize;
                sb.append("{\"id\": ").append(d)
                  .append(", \"x\": ").append(x)
                  .append(", \"y\": ").append(y).append("}");
            }
            sb.append("], \"edges\": [");
            for (int e = 0; e < edges.size(); e++) {
                if (e > 0) sb.append(", ");
                sb.append("[").append(edges.get(e)[0]).append(", ").append(edges.get(e)[1]).append("]");
            }
            sb.append("], \"solution\": [");
            for (int s = 0; s < solution.size(); s++) {
                if (s > 0) sb.append(", ");
                sb.append(solution.get(s));
            }
            sb.append("]}");
            return sb.toString();
        }
    }

    static Level generateLevel(int id, LevelSpec spec, Set<String> seenCanonical) {
        int gridSize = spec.gridSize;
        int attempts = 0;
        while (true) {
            attempts++;
            // If the difficulty window is too tight to satisfy, gradually widen it
            // so generation always terminates.
            int relax = attempts / 400;
            int minStarts = Math.max(1, spec.minStarts - relax);
            int maxStarts = Math.min(gridSize * gridSize, spec.maxStarts + relax);

            List<Integer> path = randomHamiltonianPath(gridSize);
            if (path == null) continue;

            Set<Long> edgeSet = new HashSet<>();
            List<int[]> edges = new ArrayList<>();
            for (int i = 1; i < path.size(); i++) {
                addEdge(edges, edgeSet, path.get(i - 1), path.get(i));
            }

            // Extra decoy edges between orthogonally adjacent dots.
            int extra = spec.extraMin + RNG.nextInt(spec.extraMax - spec.extraMin + 1);
            List<int[]> candidates = new ArrayList<>();
            int n = gridSize * gridSize;
            for (int a = 0; a < n; a++) {
                int ax = a % gridSize, ay = a / gridSize;
                if (ax + 1 < gridSize) candidates.add(new int[]{a, a + 1});
                if (ay + 1 < gridSize) candidates.add(new int[]{a, a + gridSize});
            }
            Collections.shuffle(candidates, RNG);
            int added = 0;
            for (int[] c : candidates) {
                if (added >= extra) break;
                if (addEdge(edges, edgeSet, c[0], c[1])) added++;
            }
            if (edges.size() > 63) continue; // solver bitmask capacity

            Level level = new Level();
            level.id = id;
            level.gridSize = gridSize;
            level.edges = edges;
            level.solution = path;

            // Difficulty gate: how many starting dots can complete the puzzle.
            int starts = countSolvableStarts(level, maxStarts);
            if (starts < minStarts || starts > maxStarts) continue;
            level.solvableStarts = starts;

            // Uniqueness gate: reject rotations/reflections of any earlier level.
            String canonical = canonicalForm(level);
            if (!seenCanonical.add(canonical)) continue;

            // Independent re-verification (defense in depth against generator bugs).
            if (verifySolvable(level)) return level;
            seenCanonical.remove(canonical);
        }
    }

    /**
     * Counts starting dots from which the puzzle can be completed. Stops early
     * once the count exceeds [cap] (the level will be rejected anyway).
     */
    static int countSolvableStarts(Level level, int cap) {
        int n = level.dotCount();
        List<List<int[]>> adj = buildAdjacency(level, n);
        long full = (1L << n) - 1;
        int count = 0;
        for (int start = 0; start < n; start++) {
            long[] budget = {150_000};
            if (solverDfs(adj, start, 0L, 1L << start, full, budget)) {
                count++;
                if (count > cap) return count;
            }
        }
        return count;
    }

    /**
     * Canonical fingerprint of the level's edge set under the 8 symmetries of
     * the square (rotations + reflections). Two levels that are the same puzzle
     * "from a different perspective" share a canonical form.
     */
    static String canonicalForm(Level level) {
        int g = level.gridSize;
        String best = null;
        for (int t = 0; t < 8; t++) {
            List<String> transformed = new ArrayList<>();
            for (int[] edge : level.edges) {
                int a = transformDot(edge[0], g, t);
                int b = transformDot(edge[1], g, t);
                transformed.add(Math.min(a, b) + "-" + Math.max(a, b));
            }
            Collections.sort(transformed);
            String key = g + "|" + String.join(",", transformed);
            if (best == null || key.compareTo(best) < 0) best = key;
        }
        return best;
    }

    /** Applies one of the 8 dihedral transforms to a dot id on a g x g grid. */
    static int transformDot(int dot, int g, int transform) {
        int x = dot % g, y = dot / g, m = g - 1;
        int nx, ny;
        switch (transform % 4) {
            case 1 -> { nx = m - y; ny = x; }       // 90°
            case 2 -> { nx = m - x; ny = m - y; }   // 180°
            case 3 -> { nx = y; ny = m - x; }       // 270°
            default -> { nx = x; ny = y; }
        }
        if (transform >= 4) nx = m - nx;            // mirror
        return ny * g + nx;
    }

    static List<List<int[]>> buildAdjacency(Level level, int n) {
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int i = 0; i < level.edges.size(); i++) {
            int[] edge = level.edges.get(i);
            adj.get(edge[0]).add(new int[]{i, edge[1]});
            adj.get(edge[1]).add(new int[]{i, edge[0]});
        }
        return adj;
    }

    static boolean addEdge(List<int[]> edges, Set<Long> set, int a, int b) {
        int lo = Math.min(a, b), hi = Math.max(a, b);
        long key = (long) lo * 10000 + hi;
        if (set.add(key)) {
            edges.add(new int[]{lo, hi});
            return true;
        }
        return false;
    }

    /** Randomized DFS for a Hamiltonian path on the gridSize x gridSize orthogonal lattice. */
    static List<Integer> randomHamiltonianPath(int gridSize) {
        int n = gridSize * gridSize;
        int start = RNG.nextInt(n);
        List<Integer> path = new ArrayList<>();
        boolean[] visited = new boolean[n];
        path.add(start);
        visited[start] = true;
        long[] steps = {0};
        if (hamiltonianDfs(gridSize, path, visited, n, steps)) return path;
        return null;
    }

    static boolean hamiltonianDfs(int gridSize, List<Integer> path, boolean[] visited, int n, long[] steps) {
        if (path.size() == n) return true;
        if (steps[0]++ > 2_000_000) return false;
        int current = path.get(path.size() - 1);
        int cx = current % gridSize, cy = current / gridSize;
        List<Integer> next = new ArrayList<>(4);
        if (cx > 0) next.add(current - 1);
        if (cx + 1 < gridSize) next.add(current + 1);
        if (cy > 0) next.add(current - gridSize);
        if (cy + 1 < gridSize) next.add(current + gridSize);
        Collections.shuffle(next, RNG);
        for (int candidate : next) {
            if (visited[candidate]) continue;
            visited[candidate] = true;
            path.add(candidate);
            if (hamiltonianDfs(gridSize, path, visited, n, steps)) return true;
            path.remove(path.size() - 1);
            visited[candidate] = false;
        }
        return false;
    }

    /**
     * Mirrors the game's rules: edges single-use, dots freely revisitable,
     * solved when every dot has been visited. Must find a solution from at
     * least one starting dot.
     */
    static boolean verifySolvable(Level level) {
        int n = level.dotCount();
        if (level.edges.size() > 63) return false; // bitmask capacity guard
        List<List<int[]>> adj = buildAdjacency(level, n);
        long full = (1L << n) - 1;
        for (int start = 0; start < n; start++) {
            long[] budget = {400_000};
            if (solverDfs(adj, start, 0L, 1L << start, full, budget)) return true;
        }
        return false;
    }

    static boolean solverDfs(List<List<int[]>> adj, int current, long used, long visited, long full, long[] budget) {
        if (visited == full) return true;
        if (budget[0]-- <= 0) return false;
        List<int[]> options = new ArrayList<>(adj.get(current));
        // Unvisited-first ordering prunes hard.
        options.sort((a, b) -> Long.compare(visited >> a[1] & 1, visited >> b[1] & 1));
        for (int[] option : options) {
            int edge = option[0], next = option[1];
            if ((used & (1L << edge)) != 0) continue;
            if (solverDfs(adj, next, used | (1L << edge), visited | (1L << next), full, budget)) return true;
        }
        return false;
    }

    // ── Sound synthesis (44.1kHz 16-bit mono WAV) ────────────────

    static final int SAMPLE_RATE = 44100;

    /** Soft, short chime for dot-connect (~80ms sine with fast decay). */
    static short[] connectSound() {
        int len = (int) (SAMPLE_RATE * 0.085);
        short[] samples = new short[len];
        for (int i = 0; i < len; i++) {
            double t = (double) i / SAMPLE_RATE;
            double env = Math.exp(-t * 45) * Math.min(1, i / (SAMPLE_RATE * 0.004));
            double v = Math.sin(2 * Math.PI * 740 * t) * 0.8 + Math.sin(2 * Math.PI * 1480 * t) * 0.15;
            samples[i] = (short) (v * env * 0.55 * Short.MAX_VALUE);
        }
        return samples;
    }

    /** Warm two-note major chime for level complete (E5 then A5, ~600ms). */
    static short[] completeSound() {
        int len = (int) (SAMPLE_RATE * 0.62);
        short[] samples = new short[len];
        for (int i = 0; i < len; i++) {
            double t = (double) i / SAMPLE_RATE;
            double v = 0;
            // Note 1: E5 659Hz starting at 0
            double env1 = Math.exp(-t * 7) * Math.min(1, i / (SAMPLE_RATE * 0.006));
            v += (Math.sin(2 * Math.PI * 659.25 * t) + 0.25 * Math.sin(2 * Math.PI * 1318.5 * t)) * env1 * 0.5;
            // Note 2: A5 880Hz starting at 140ms
            double t2 = t - 0.14;
            if (t2 > 0) {
                double env2 = Math.exp(-t2 * 5.5) * Math.min(1, t2 / 0.006);
                v += (Math.sin(2 * Math.PI * 880 * t2) + 0.25 * Math.sin(2 * Math.PI * 1760 * t2)) * env2 * 0.55;
            }
            samples[i] = (short) (Math.max(-1, Math.min(1, v * 0.6)) * Short.MAX_VALUE);
        }
        return samples;
    }

    /** Low, brief soft-edged tone for the stuck state (not a buzzer). */
    static short[] stuckSound() {
        int len = (int) (SAMPLE_RATE * 0.19);
        short[] samples = new short[len];
        for (int i = 0; i < len; i++) {
            double t = (double) i / SAMPLE_RATE;
            double attack = Math.min(1, t / 0.02);
            double release = Math.min(1, (0.19 - t) / 0.05);
            double v = Math.sin(2 * Math.PI * 185 * t) * 0.9 + Math.sin(2 * Math.PI * 92.5 * t) * 0.3;
            samples[i] = (short) (v * attack * release * 0.4 * Short.MAX_VALUE);
        }
        return samples;
    }

    static void writeWav(Path file, short[] samples) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int dataLen = samples.length * 2;
        // RIFF header
        out.writeBytes("RIFF".getBytes(StandardCharsets.US_ASCII));
        writeIntLE(out, 36 + dataLen);
        out.writeBytes("WAVE".getBytes(StandardCharsets.US_ASCII));
        // fmt chunk
        out.writeBytes("fmt ".getBytes(StandardCharsets.US_ASCII));
        writeIntLE(out, 16);
        writeShortLE(out, 1);  // PCM
        writeShortLE(out, 1);  // mono
        writeIntLE(out, SAMPLE_RATE);
        writeIntLE(out, SAMPLE_RATE * 2);
        writeShortLE(out, 2);
        writeShortLE(out, 16);
        // data chunk
        out.writeBytes("data".getBytes(StandardCharsets.US_ASCII));
        writeIntLE(out, dataLen);
        for (short s : samples) writeShortLE(out, s);
        Files.write(file, out.toByteArray());
    }

    static void writeIntLE(ByteArrayOutputStream out, int v) {
        out.write(v & 0xFF); out.write((v >> 8) & 0xFF); out.write((v >> 16) & 0xFF); out.write((v >> 24) & 0xFF);
    }

    static void writeShortLE(ByteArrayOutputStream out, int v) {
        out.write(v & 0xFF); out.write((v >> 8) & 0xFF);
    }
}

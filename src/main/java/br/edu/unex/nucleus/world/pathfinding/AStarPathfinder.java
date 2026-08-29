package br.edu.unex.nucleus.world.pathfinding;

import br.edu.unex.nucleus.world.CollisionMap;

import java.util.*;

/**
 * A* para os inimigos comuns.
 *
 * Não altera a CollisionMap: apenas pergunta a ela se a pequena área dos pés
 * do inimigo pode ocupar cada célula candidata.
 */
public final class AStarPathfinder {

    private static final double CELL = 24.0;
    private static final double FOOT_W = 18.0;
    private static final double FOOT_H = 14.0;
    private static final double FOOT_BOTTOM_MARGIN = 5.0;

    private static final int MAX_EXPANDED_NODES = 12000;
    private static final int SEARCH_MARGIN_CELLS = 22;

    private static final int[][] DIRECTIONS = {
            { 1, 0}, {-1, 0}, {0, 1}, {0, -1},
            { 1, 1}, { 1,-1}, {-1, 1}, {-1,-1}
    };

    public List<PathPoint> findPath(
            CollisionMap map,
            double startX, double startY,
            double targetX, double targetY,
            double spriteWidth, double spriteHeight) {

        if (map == null) return List.of();

        int sx = worldToCell(startX);
        int sy = worldToCell(startY);
        int gx = worldToCell(targetX);
        int gy = worldToCell(targetY);

        Cell start = nearestWalkable(map, sx, sy, spriteWidth, spriteHeight, 3);
        Cell goal = nearestWalkable(map, gx, gy, spriteWidth, spriteHeight, 4);

        if (start == null || goal == null) return List.of();

        int minX = Math.min(start.x, goal.x) - SEARCH_MARGIN_CELLS;
        int maxX = Math.max(start.x, goal.x) + SEARCH_MARGIN_CELLS;
        int minY = Math.min(start.y, goal.y) - SEARCH_MARGIN_CELLS;
        int maxY = Math.max(start.y, goal.y) + SEARCH_MARGIN_CELLS;

        PriorityQueue<Node> open = new PriorityQueue<>(
                Comparator.comparingDouble(Node::f)
                        .thenComparingDouble(n -> n.h)
        );
        Map<Long, Node> best = new HashMap<>();
        Set<Long> closed = new HashSet<>();

        Node startNode = new Node(start.x, start.y, 0,
                heuristic(start.x, start.y, goal.x, goal.y), null);
        open.add(startNode);
        best.put(key(start.x, start.y), startNode);

        int expanded = 0;

        while (!open.isEmpty() && expanded < MAX_EXPANDED_NODES) {
            Node current = open.poll();
            long currentKey = key(current.x, current.y);

            if (closed.contains(currentKey)) continue;
            closed.add(currentKey);
            expanded++;

            if (current.x == goal.x && current.y == goal.y) {
                return simplify(toWorldPath(current, targetX, targetY));
            }

            for (int[] d : DIRECTIONS) {
                int nx = current.x + d[0];
                int ny = current.y + d[1];

                if (nx < minX || nx > maxX || ny < minY || ny > maxY) continue;
                if (!isWalkable(map, nx, ny, spriteWidth, spriteHeight)) continue;

                // Não permite cortar diagonalmente o canto de uma parede.
                if (d[0] != 0 && d[1] != 0) {
                    if (!isWalkable(map, current.x + d[0], current.y, spriteWidth, spriteHeight)
                            || !isWalkable(map, current.x, current.y + d[1], spriteWidth, spriteHeight)) {
                        continue;
                    }
                }

                long nk = key(nx, ny);
                if (closed.contains(nk)) continue;

                double stepCost = (d[0] != 0 && d[1] != 0) ? Math.sqrt(2.0) : 1.0;
                double ng = current.g + stepCost;
                Node previous = best.get(nk);

                if (previous == null || ng < previous.g) {
                    Node next = new Node(nx, ny, ng,
                            heuristic(nx, ny, goal.x, goal.y), current);
                    best.put(nk, next);
                    open.add(next);
                }
            }
        }

        return List.of();
    }

    private Cell nearestWalkable(CollisionMap map, int cx, int cy,
                                 double spriteWidth, double spriteHeight, int radius) {
        if (isWalkable(map, cx, cy, spriteWidth, spriteHeight)) {
            return new Cell(cx, cy);
        }

        for (int r = 1; r <= radius; r++) {
            for (int y = cy - r; y <= cy + r; y++) {
                for (int x = cx - r; x <= cx + r; x++) {
                    if (Math.max(Math.abs(x - cx), Math.abs(y - cy)) != r) continue;
                    if (isWalkable(map, x, y, spriteWidth, spriteHeight)) {
                        return new Cell(x, y);
                    }
                }
            }
        }
        return null;
    }

    private boolean isWalkable(CollisionMap map, int cellX, int cellY,
                               double spriteWidth, double spriteHeight) {
        double centerX = cellToWorld(cellX);
        double centerY = cellToWorld(cellY);

        double spriteLeft = centerX - spriteWidth / 2.0;
        double spriteTop = centerY - spriteHeight / 2.0;

        double footX = spriteLeft + (spriteWidth - FOOT_W) / 2.0;
        double footY = spriteTop + spriteHeight - FOOT_H - FOOT_BOTTOM_MARGIN;

        return map.canMoveTo(footX, footY, FOOT_W, FOOT_H);
    }

    private List<PathPoint> toWorldPath(Node goal, double exactTargetX, double exactTargetY) {
        LinkedList<PathPoint> result = new LinkedList<>();
        Node n = goal;
        while (n != null) {
            result.addFirst(new PathPoint(cellToWorld(n.x), cellToWorld(n.y)));
            n = n.parent;
        }

        // O primeiro ponto é a célula onde o inimigo já está.
        if (!result.isEmpty()) result.removeFirst();

        return result;
    }

    /**
     * Remove pontos intermediários que seguem exatamente a mesma direção.
     * Assim o Enemy precisa acompanhar menos waypoints.
     */
    private List<PathPoint> simplify(List<PathPoint> input) {
        if (input.size() <= 2) return input;

        List<PathPoint> output = new ArrayList<>();
        output.add(input.get(0));

        double prevDx = 0;
        double prevDy = 0;

        for (int i = 1; i < input.size(); i++) {
            PathPoint a = input.get(i - 1);
            PathPoint b = input.get(i);

            double dx = Math.signum(b.x() - a.x());
            double dy = Math.signum(b.y() - a.y());

            if (i == 1) {
                prevDx = dx;
                prevDy = dy;
                continue;
            }

            if (dx != prevDx || dy != prevDy) {
                output.add(a);
                prevDx = dx;
                prevDy = dy;
            }
        }

        output.add(input.get(input.size() - 1));
        return output;
    }

    private int worldToCell(double value) {
        return (int) Math.floor(value / CELL);
    }

    private double cellToWorld(int cell) {
        return cell * CELL + CELL / 2.0;
    }

    private double heuristic(int x, int y, int gx, int gy) {
        int dx = Math.abs(gx - x);
        int dy = Math.abs(gy - y);
        int diagonal = Math.min(dx, dy);
        int straight = Math.max(dx, dy) - diagonal;
        return diagonal * Math.sqrt(2.0) + straight;
    }

    private long key(int x, int y) {
        return (((long) x) << 32) ^ (y & 0xffffffffL);
    }

    private static final class Cell {
        final int x;
        final int y;
        Cell(int x, int y) { this.x = x; this.y = y; }
    }

    private static final class Node {
        final int x;
        final int y;
        final double g;
        final double h;
        final Node parent;

        Node(int x, int y, double g, double h, Node parent) {
            this.x = x;
            this.y = y;
            this.g = g;
            this.h = h;
            this.parent = parent;
        }

        double f() { return g + h; }
    }
}

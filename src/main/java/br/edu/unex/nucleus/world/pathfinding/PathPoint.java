package br.edu.unex.nucleus.world.pathfinding;

/** Um ponto em coordenadas do mundo retornado pelo A*. */
public final class PathPoint {
    private final double x;
    private final double y;

    public PathPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double x() { return x; }
    public double y() { return y; }
}

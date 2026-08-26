package br.edu.unex.nucleus.entity;

public class Portal {

    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final String targetIslandId;
    private final double targetSpawnX;
    private final double targetSpawnY;

    public Portal(double x, double y, String targetIslandId, double targetSpawnX, double targetSpawnY) {
        this(x, y, 40, 40, targetIslandId, targetSpawnX, targetSpawnY);
    }

    public Portal(double x, double y, double width, double height,
                  String targetIslandId, double targetSpawnX, double targetSpawnY) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.targetIslandId = targetIslandId;
        this.targetSpawnX = targetSpawnX;
        this.targetSpawnY = targetSpawnY;
    }

    public boolean isPlayerColliding(Player player) {
        return player.getX() < x + width
                && player.getX() + player.getWidth() > x
                && player.getY() < y + height
                && player.getY() + player.getHeight() > y;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public String getTargetIslandId() { return targetIslandId; }
    public double getTargetSpawnX() { return targetSpawnX; }
    public double getTargetSpawnY() { return targetSpawnY; }
}

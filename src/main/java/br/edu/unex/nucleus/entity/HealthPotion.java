package br.edu.unex.nucleus.entity;

public class HealthPotion {
    private final double x;
    private final double y;
    private final double healAmount;
    private boolean collected;

    public HealthPotion(double x, double y, double healAmount) {
        this.x = x;
        this.y = y;
        this.healAmount = healAmount;
        this.collected = false;
    }

    public boolean isCollected() { return collected; }
    public void collect() { collected = true; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getHealAmount() { return healAmount; }

    public boolean isCollidingWith(Player player) {
        double dx = x - (player.getX() + player.getWidth() / 2);
        double dy = y - (player.getY() + player.getHeight() / 2);
        return Math.sqrt(dx * dx + dy * dy) <= 28;
    }
}

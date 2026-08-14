package br.edu.unex.nucleus.entity;

public class Projectile {

    private double x;
    private double y;

    private double velocityX;
    private double velocityY;

    private final double width;
    private final double height;

    private double remainingRange;

    private final Player homingTarget;
    private final double damage;
    private final double turnSpeedDegreesPerSecond;

    public Projectile(double x, double y, double velocityX, double velocityY, double range) {
        this(x, y, velocityX, velocityY, range, null, 0, 12);
    }

    public Projectile(double x, double y, double velocityX, double velocityY, double range,
                       Player homingTarget, double turnSpeedDegreesPerSecond) {
        this(x, y, velocityX, velocityY, range, homingTarget, turnSpeedDegreesPerSecond, 12);
    }

    public Projectile(double x, double y, double velocityX, double velocityY, double range,
                       Player homingTarget, double turnSpeedDegreesPerSecond, double damage) {

        this.x = x;
        this.y = y;

        this.velocityX = velocityX;
        this.velocityY = velocityY;

        this.width = 10;
        this.height = 10;

        this.remainingRange = range;

        this.homingTarget = homingTarget;
        this.turnSpeedDegreesPerSecond = turnSpeedDegreesPerSecond;
        this.damage = damage;
    }

    public void update(double deltaTime) {

        if (homingTarget != null) {
            steerTowardsTarget(deltaTime);
        }

        double dx = velocityX * deltaTime;
        double dy = velocityY * deltaTime;

        x += dx;
        y += dy;

        remainingRange -= Math.sqrt(dx * dx + dy * dy);
    }

    private void steerTowardsTarget(double deltaTime) {

        double targetX = homingTarget.getX() + homingTarget.getWidth() / 2;
        double targetY = homingTarget.getY() + homingTarget.getHeight() / 2;

        double currentAngle = Math.toDegrees(Math.atan2(velocityY, velocityX));
        double desiredAngle = Math.toDegrees(Math.atan2(targetY - y, targetX - x));

        double diff = angleDifference(currentAngle, desiredAngle);

        double maxTurn = turnSpeedDegreesPerSecond * deltaTime;

        double turn = Math.max(-maxTurn, Math.min(maxTurn, diff));

        double newAngleRad = Math.toRadians(currentAngle + turn);

        double speed = Math.sqrt(velocityX * velocityX + velocityY * velocityY);

        velocityX = Math.cos(newAngleRad) * speed;
        velocityY = Math.sin(newAngleRad) * speed;
    }

    private double angleDifference(double a, double b) {

        double diff = (b - a) % 360;

        if (diff < -180) {
            diff += 360;
        }

        if (diff > 180) {
            diff -= 360;
        }

        return diff;
    }

    public boolean isExpired() {
        return remainingRange <= 0;
    }

    public boolean isCollidingWith(Player player) {

        return x < player.getX() + player.getWidth()
                && x + width > player.getX()
                && y < player.getY() + player.getHeight()
                && y + height > player.getY();
    }

    public double getDamage() {
        return damage;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }
}

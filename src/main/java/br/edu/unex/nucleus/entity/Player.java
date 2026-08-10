package br.edu.unex.nucleus.entity;

public class Player {

    private double x;
    private double y;

    private final double width;
    private final double height;

    private final double speed;

    public Player(double x, double y) {
        this.x = x;
        this.y = y;

        this.width = 32;
        this.height = 32;

        this.speed = 200;
    }

    public void move(double dx, double dy, double deltaTime) {

        x += dx * speed * deltaTime;
        y += dy * speed * deltaTime;
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

    public double getSpeed() {
        return speed;
    }
}
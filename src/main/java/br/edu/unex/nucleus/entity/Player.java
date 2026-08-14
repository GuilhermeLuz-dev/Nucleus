package br.edu.unex.nucleus.entity;

import br.edu.unex.nucleus.rendering.CharacterAnimator;

public class Player {

    private double x;
    private double y;

    private final double width;
    private final double height;

    private final double speed;
    private double health;
    private final double maxHealth;
    private double attackCooldown;
    private int healthPotions;

    private final CharacterAnimator animator;
    private CharacterAnimator.Direction direction;
    private boolean moving;
    private double attackAnimationTimer;

    public Player(double x, double y) {
        this.x = x;
        this.y = y;

        this.width = 80;
        this.height = 94;

        this.speed = 200;
        this.maxHealth = 250;
        this.health = maxHealth;
        this.attackCooldown = 0;
        this.healthPotions = 0;
        this.direction = CharacterAnimator.Direction.FRONT;
        this.moving = false;
        this.attackAnimationTimer = 0;
        this.animator = new CharacterAnimator("/sprites/vampire1");
    }

    public void move(double dx, double dy, double deltaTime) {

        if (dx != 0 || dy != 0) {
            moving = true;
            setDirection(dx, dy);
        }

        double length = Math.sqrt(dx * dx + dy * dy);
        if (length > 0) {
            dx /= length;
            dy /= length;
        }

        x += dx * speed * deltaTime;
        y += dy * speed * deltaTime;
    }

    public void updateCooldowns(double deltaTime) {
        attackCooldown = Math.max(0, attackCooldown - deltaTime);
        attackAnimationTimer = Math.max(0, attackAnimationTimer - deltaTime);

        if (attackAnimationTimer > 0) {
            animator.setAction(CharacterAnimator.Action.ATTACK);
        } else if (moving) {
            animator.setAction(CharacterAnimator.Action.WALK);
        } else {
            animator.setAction(CharacterAnimator.Action.IDLE);
        }

        animator.update(deltaTime);
        moving = false;
    }

    public boolean canAttack() {
        return attackCooldown <= 0 && !isDead();
    }

    public void startAttackCooldown() {
        attackCooldown = 0.35;
        startAttackAnimation();
    }

    public void startAttackAnimation() {
        attackAnimationTimer = 0.55;
        animator.setAction(CharacterAnimator.Action.ATTACK);
    }

    public void setDirection(double dx, double dy) {
        if (Math.abs(dx) > Math.abs(dy)) {
            direction = dx < 0
                    ? CharacterAnimator.Direction.LEFT
                    : CharacterAnimator.Direction.RIGHT;
        } else if (dy != 0) {
            direction = dy < 0
                    ? CharacterAnimator.Direction.BACK
                    : CharacterAnimator.Direction.FRONT;
        }
        animator.setDirection(direction);
    }

    public void takeDamage(double amount) {
        health = Math.max(0, health - amount);
    }

    public void healFull() {
        health = maxHealth;
    }

    public void heal(double amount) {
        health = Math.min(maxHealth, health + Math.max(0, amount));
    }

    public void addHealthPotion() {
        healthPotions++;
    }

    public int getHealthPotions() {
        return healthPotions;
    }

    public boolean useHealthPotion() {
        if (healthPotions <= 0 || health >= maxHealth || isDead()) {
            return false;
        }
        healthPotions--;
        heal(100);
        return true;
    }

    public boolean isDead() {
        return health <= 0;
    }

    public void setPosition(double newX, double newY) {
        this.x = newX;
        this.y = newY;
    }

    public void clampToWorld(double worldWidth, double worldHeight) {
        x = Math.max(0, Math.min(x, worldWidth - width));
        y = Math.max(0, Math.min(y, worldHeight - height));
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

    public double getHealth() {
        return health;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public CharacterAnimator getAnimator() {
        return animator;
    }

    public CharacterAnimator.Direction getDirection() {
        return direction;
    }
}

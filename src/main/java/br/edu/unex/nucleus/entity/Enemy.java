package br.edu.unex.nucleus.entity;

import br.edu.unex.nucleus.ai.EnemyPower;
import br.edu.unex.nucleus.ai.EnemyState;
import br.edu.unex.nucleus.ai.SingleBoltPower;
import br.edu.unex.nucleus.rendering.CharacterAnimator;

import java.util.List;

public class Enemy {

    private double x;
    private double y;

    private final double width;
    private final double height;

    private double directionAngle;

    private EnemyState state;

    private final double visionRange;
    private final double visionHalfAngle;
    private final double hearingRadius;

    private double alertTimer;
    private final double alertDuration;
    private final double alertConfirmThreshold;

    private double lastKnownX;
    private double lastKnownY;

    private final double chaseSpeed;

    private final double attackRange;
    private final double attackCooldownDuration;
    private double attackCooldownTimer;

    private final EnemyPower power;
    private final double maxHealth;
    private double health;
    private final boolean boss;
    private final double contactDamage;

    private final CharacterAnimator animator;
    private double hurtTimer;

    public Enemy(double x, double y) {
        this(x, y, new SingleBoltPower());
    }

    public Enemy(double x, double y, EnemyPower power) {
        this(x, y, power, 80, 32, 32, false, 8);
    }

    public Enemy(double x, double y, EnemyPower power, double maxHealth,
                 double width, double height, boolean boss, double contactDamage) {

        this.x = x;
        this.y = y;

        this.width = 100;
        this.height = 124;

        this.directionAngle = 90;

        this.state = EnemyState.PATROL;

        this.visionRange = 220;
        this.visionHalfAngle = 45;
        this.hearingRadius = 120;

        this.alertDuration = 1.2;
        this.alertConfirmThreshold = 0.6;

        this.chaseSpeed = 140;

        this.attackRange = 90;
        this.attackCooldownDuration = 1.0;
        this.attackCooldownTimer = 0;

        this.power = power;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.boss = boss;
        this.contactDamage = contactDamage;
        this.hurtTimer = 0;

        // Inimigos comuns usam Vampire2; bosses usam Vampire3.
        this.animator = new CharacterAnimator(
                boss ? "/sprites/vampire3" : "/sprites/vampire2"
        );
    }

    public List<Projectile> update(Player player, double deltaTime, boolean playerIsMakingNoise) {

        if (isDead()) {
            animator.setAction(CharacterAnimator.Action.DEATH);
            animator.update(deltaTime);
            return List.of();
        }

        hurtTimer = Math.max(0, hurtTimer - deltaTime);

        boolean seesPlayer = canSeePlayer(player);
        boolean hearsPlayer = playerIsMakingNoise && canHearPlayer(player);

        List<Projectile> spawnedProjectiles = List.of();

        switch (state) {

            case PATROL -> updatePatrol(seesPlayer, hearsPlayer, player);

            case ALERT -> updateAlert(deltaTime, seesPlayer, hearsPlayer, player);

            case CHASE -> updateChase(deltaTime, seesPlayer, player);

            case ATTACK -> spawnedProjectiles = updateAttack(deltaTime, player);

            case RETURN -> updateReturn();
        }

        updateAnimation(deltaTime);

        return spawnedProjectiles;
    }

    private void updatePatrol(boolean seesPlayer, boolean hearsPlayer, Player player) {

        // TODO: mover em rota de patrulha (waypoints), fora do escopo desta etapa

        if (seesPlayer || hearsPlayer) {
            enterAlert(player);
        }
    }

    private void updateAlert(double deltaTime, boolean seesPlayer, boolean hearsPlayer, Player player) {

        alertTimer -= deltaTime;

        if (seesPlayer) {
            lastKnownX = playerCenterX(player);
            lastKnownY = playerCenterY(player);
        }

        faceTowards(lastKnownX, lastKnownY);

        if (seesPlayer && alertTimer <= alertConfirmThreshold) {
            state = EnemyState.CHASE;
            return;
        }

        if (hearsPlayer) {
            alertTimer = alertDuration;
        }

        if (alertTimer <= 0) {
            state = EnemyState.RETURN;
        }
    }

    private void updateChase(double deltaTime, boolean seesPlayer, Player player) {

        if (seesPlayer) {
            lastKnownX = playerCenterX(player);
            lastKnownY = playerCenterY(player);
        }

        double distanceToPlayer = distanceTo(playerCenterX(player), playerCenterY(player));

        if (seesPlayer && distanceToPlayer <= attackRange) {
            state = EnemyState.ATTACK;
            return;
        }

        moveTowards(lastKnownX, lastKnownY, chaseSpeed, deltaTime);

        double distanceToLastKnown = distanceTo(lastKnownX, lastKnownY);

        if (!seesPlayer && distanceToLastKnown < 10) {
            state = EnemyState.RETURN;
        }
    }

    private List<Projectile> updateAttack(double deltaTime, Player player) {

        faceTowards(playerCenterX(player), playerCenterY(player));

        double distanceToPlayer = distanceTo(playerCenterX(player), playerCenterY(player));

        if (distanceToPlayer > attackRange) {
            state = EnemyState.CHASE;
            return List.of();
        }

        attackCooldownTimer -= deltaTime;

        if (attackCooldownTimer > 0) {
            return List.of();
        }

        attackCooldownTimer = attackCooldownDuration;

        return power.cast(x, y, player);
    }

    private void updateReturn() {

        // TODO: mover de volta ao ponto de patrulha original

        state = EnemyState.PATROL;
    }

    private void enterAlert(Player player) {

        state = EnemyState.ALERT;

        alertTimer = alertDuration;

        lastKnownX = playerCenterX(player);
        lastKnownY = playerCenterY(player);
    }

    private void updateAnimation(double deltaTime) {
        animator.setDirection(directionFromAngle());

        if (hurtTimer > 0) {
            animator.setAction(CharacterAnimator.Action.HURT);
        } else {
            switch (state) {
                case CHASE, RETURN -> animator.setAction(CharacterAnimator.Action.WALK);
                case ATTACK -> animator.setAction(CharacterAnimator.Action.ATTACK);
                default -> animator.setAction(CharacterAnimator.Action.IDLE);
            }
        }

        animator.update(deltaTime);
    }

    private CharacterAnimator.Direction directionFromAngle() {
        double normalized = (directionAngle % 360 + 360) % 360;

        if (normalized >= 45 && normalized < 135) {
            return CharacterAnimator.Direction.FRONT;
        }
        if (normalized >= 135 && normalized < 225) {
            return CharacterAnimator.Direction.LEFT;
        }
        if (normalized >= 225 && normalized < 315) {
            return CharacterAnimator.Direction.BACK;
        }
        return CharacterAnimator.Direction.RIGHT;
    }

    public boolean canSeePlayer(Player player) {

        double distance = distanceTo(playerCenterX(player), playerCenterY(player));

        if (distance > visionRange) {
            return false;
        }

        double angleToPlayer = Math.toDegrees(
                Math.atan2(playerCenterY(player) - y, playerCenterX(player) - x)
        );

        double diff = angleDifference(directionAngle, angleToPlayer);

        return Math.abs(diff) <= visionHalfAngle;

        // Observação: quando o cenário tiver paredes/tiles com colisão,
        // adicionar aqui uma checagem de linha reta entre inimigo e jogador
        // (equivalente a um raycast) para bloquear a visão atrás de obstáculos.
    }

    public boolean canHearPlayer(Player player) {

        return distanceTo(playerCenterX(player), playerCenterY(player)) <= hearingRadius;
    }

    private double playerCenterX(Player player) {
        return player.getX() + player.getWidth() / 2;
    }

    private double playerCenterY(Player player) {
        return player.getY() + player.getHeight() / 2;
    }

    private double distanceTo(double targetX, double targetY) {

        double dx = targetX - x;
        double dy = targetY - y;

        return Math.sqrt(dx * dx + dy * dy);
    }

    private void faceTowards(double targetX, double targetY) {

        directionAngle = Math.toDegrees(Math.atan2(targetY - y, targetX - x));
    }

    private void moveTowards(double targetX, double targetY, double speed, double deltaTime) {

        double dx = targetX - x;
        double dy = targetY - y;

        double length = Math.sqrt(dx * dx + dy * dy);

        if (length < 1) {
            return;
        }

        dx /= length;
        dy /= length;

        x += dx * speed * deltaTime;
        y += dy * speed * deltaTime;

        directionAngle = Math.toDegrees(Math.atan2(dy, dx));
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

    public void setPosition(double newX, double newY) {
        this.x = newX;
        this.y = newY;
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

    public double getDirectionAngle() {
        return directionAngle;
    }

    public double getVisionRange() {
        return visionRange;
    }

    public double getVisionHalfAngle() {
        return visionHalfAngle;
    }

    public double getHearingRadius() {
        return hearingRadius;
    }

    public EnemyState getState() {
        return state;
    }

    public CharacterAnimator getAnimator() {
        return animator;
    }

    public double getHealth() {
        return health;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public boolean isDead() {
        return health <= 0;
    }

    public boolean isBoss() {
        return boss;
    }

    public double getContactDamage() {
        return contactDamage;
    }

    public void takeDamage(double amount) {
        if (isDead()) return;
        health = Math.max(0, health - amount);
        hurtTimer = 0.14;
        if (isDead()) {
            animator.setAction(CharacterAnimator.Action.DEATH);
        }
    }

    public boolean isCollidingWith(Player player) {
        return x - width / 2 < player.getX() + player.getWidth()
                && x + width / 2 > player.getX()
                && y - height / 2 < player.getY() + player.getHeight()
                && y + height / 2 > player.getY();
    }
}

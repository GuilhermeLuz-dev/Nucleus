package br.edu.unex.nucleus.entity;

import br.edu.unex.nucleus.ai.EnemyPower;
import br.edu.unex.nucleus.ai.EnemyState;
import br.edu.unex.nucleus.ai.SingleBoltPower;
import br.edu.unex.nucleus.rendering.CharacterAnimator;
import br.edu.unex.nucleus.world.CollisionMap;
import br.edu.unex.nucleus.world.pathfinding.AStarPathfinder;
import br.edu.unex.nucleus.world.pathfinding.PathPoint;

import java.util.List;

public class Enemy {

    private double x;
    private double y;
    private double homeX;
    private double homeY;

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

    private final EnemyPower basePower;
    private final double maxHealth;
    private double health;
    private final boolean boss;
    private final double contactDamage;

    private final CharacterAnimator animator;
    private double hurtTimer;

    // Patrulha real em torno do ponto onde o monstro foi colocado no Tiled.
    private double patrolRadius = 90;
    private double patrolSpeed = 65;
    private int patrolWaypointIndex;
    private double patrolPauseTimer;
    private static final double PATROL_POINT_TOLERANCE = 8;
    private static final double PATROL_PAUSE = 0.45;

    // A* para inimigos comuns
    private final AStarPathfinder pathfinder = new AStarPathfinder();
    private List<PathPoint> currentPath = List.of();
    private int currentPathIndex;
    private double pathRecalculationTimer;
    private double pathTargetX = Double.NaN;
    private double pathTargetY = Double.NaN;
    private static final double PATH_RECALCULATION_INTERVAL = 0.40;
    private static final double PATH_POINT_TOLERANCE = 10.0;
    private static final double PATH_TARGET_CHANGE_DISTANCE = 24.0;

    public Enemy(double x, double y) {
        this(x, y, new SingleBoltPower());
    }

    public Enemy(double x, double y, EnemyPower power) {
        this(x, y, power, 80, 80, 94, false, 8, null, 0);
    }

    /**
     * Inimigo comum usando um dos slimes do pack (1, 2 ou 3).
     * O tamanho visual permanece 80x94, igual ao Player.
     */
    public Enemy(double x, double y, EnemyPower power, int slimeVariant) {
        this(x, y, power, 80, 80, 94, false, 8,
                "/sprites/slime" + Math.max(1, Math.min(3, slimeVariant)),
                Math.max(1, Math.min(3, slimeVariant)));
    }

    public Enemy(double x, double y, EnemyPower power, double maxHealth,
                 double width, double height, boolean boss, double contactDamage) {
        this(x, y, power, maxHealth, width, height, boss, contactDamage, null, 0);
    }

    /**
     * Permite um boss manter toda a IA/atributos existentes usando um sprite próprio.
     */
    public Enemy(double x, double y, EnemyPower power, double maxHealth,
                 double width, double height, boolean boss, double contactDamage,
                 String spriteBasePath,
                 int idleFrames, int walkFrames, int attackFrames,
                 int hurtFrames, int deathFrames) {
        this(x, y, power, maxHealth, width, height, boss, contactDamage,
                spriteBasePath, -1,
                idleFrames, walkFrames, attackFrames, hurtFrames, deathFrames);
    }

    private Enemy(double x, double y, EnemyPower power, double maxHealth,
                  double width, double height, boolean boss, double contactDamage,
                  String spriteBasePath, int slimeVariant) {
        this(x, y, power, maxHealth, width, height, boss, contactDamage,
                spriteBasePath, slimeVariant, -1, -1, -1, -1, -1);
    }

    private Enemy(double x, double y, EnemyPower power, double maxHealth,
                  double width, double height, boolean boss, double contactDamage,
                  String spriteBasePath, int slimeVariant,
                  int customIdleFrames, int customWalkFrames, int customAttackFrames,
                  int customHurtFrames, int customDeathFrames) {
        this.x = x;
        this.y = y;
        this.homeX = x;
        this.homeY = y;
        this.width = width;
        this.height = height;
        this.directionAngle = 90;
        this.state = EnemyState.PATROL;
        this.visionRange = boss ? 300 : 220;
        this.visionHalfAngle = boss ? 70 : 45;
        this.hearingRadius = boss ? 170 : 120;
        this.alertDuration = 1.2;
        this.alertConfirmThreshold = 0.6;
        this.chaseSpeed = boss ? 115 : 140;
        this.attackRange = boss ? 180 : 90;
        this.attackCooldownDuration = boss ? 0.85 : 1.0;
        this.attackCooldownTimer = 0;
        this.basePower = power;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.boss = boss;
        this.contactDamage = contactDamage;
        this.hurtTimer = 0;
        if (boss) this.patrolRadius = 0;
        if (spriteBasePath != null && customIdleFrames > 0) {
            this.animator = new CharacterAnimator(
                    spriteBasePath,
                    customIdleFrames, customWalkFrames, customAttackFrames,
                    customHurtFrames, customDeathFrames
            );
        } else if (!boss && spriteBasePath != null) {
            int attackFrames = switch (slimeVariant) {
                case 2 -> 11;
                case 3 -> 9;
                default -> 10;
            };
            this.animator = new CharacterAnimator(
                    spriteBasePath,
                    6, 8, attackFrames, 5, 10
            );
        } else {
            this.animator = new CharacterAnimator(boss ? "/sprites/vampire3" : "/sprites/vampire2");
        }
    }

    /** Compatibilidade com chamadas antigas. */
    public List<Projectile> update(Player player, double deltaTime, boolean playerIsMakingNoise) {
        return update(player, deltaTime, playerIsMakingNoise, null);
    }

    /** Atualização da IA com acesso à colisão para navegação e linha de visão. */
    public List<Projectile> update(Player player, double deltaTime,
                                   boolean playerIsMakingNoise, CollisionMap collisionMap) {
        if (isDead()) {
            animator.setAction(CharacterAnimator.Action.DEATH);
            animator.update(deltaTime);
            return List.of();
        }

        hurtTimer = Math.max(0, hurtTimer - deltaTime);
        onBeforeUpdate(deltaTime, player);

        boolean seesPlayer = canSeePlayer(player, collisionMap);
        boolean hearsPlayer = playerIsMakingNoise && canHearPlayer(player);
        List<Projectile> spawnedProjectiles = List.of();

        switch (state) {
            case PATROL -> updatePatrol(deltaTime, seesPlayer, hearsPlayer, player, collisionMap);
            case ALERT -> updateAlert(deltaTime, seesPlayer, hearsPlayer, player);
            case CHASE -> updateChase(deltaTime, seesPlayer, player, collisionMap);
            case ATTACK -> spawnedProjectiles = updateAttack(deltaTime, player, collisionMap);
            case RETURN -> updateReturn(deltaTime, collisionMap);
        }

        updateAnimation(deltaTime);
        return spawnedProjectiles;
    }

    protected void onBeforeUpdate(double deltaTime, Player player) {
        // Hook para bosses/fases especiais.
    }

    private void updatePatrol(double deltaTime, boolean seesPlayer, boolean hearsPlayer,
                              Player player, CollisionMap collisionMap) {
        if (seesPlayer || hearsPlayer) {
            enterAlert(player);
            return;
        }

        if (patrolRadius <= 0) return;
        if (patrolPauseTimer > 0) {
            patrolPauseTimer -= deltaTime;
            return;
        }

        double[][] route = patrolRoute();
        double targetX = route[patrolWaypointIndex][0];
        double targetY = route[patrolWaypointIndex][1];

        if (distanceTo(targetX, targetY) <= PATROL_POINT_TOLERANCE) {
            patrolWaypointIndex = (patrolWaypointIndex + 1) % route.length;
            patrolPauseTimer = PATROL_PAUSE;
            return;
        }

        boolean moved = moveTowards(targetX, targetY, patrolSpeed, deltaTime, collisionMap);
        if (!moved) {
            // Se o ponto estiver atrás de uma parede, tenta o próximo em vez de travar.
            patrolWaypointIndex = (patrolWaypointIndex + 1) % route.length;
            patrolPauseTimer = 0.15;
        }
    }

    private double[][] patrolRoute() {
        // Oito direções evitam que inimigos próximos a paredes dependam apenas
        // dos quatro pontos cardeais. Pontos diagonais costumam oferecer uma
        // saída válida quando o spawn foi colocado perto de uma colisão.
        double d = patrolRadius * 0.72;
        return new double[][]{
                {homeX + patrolRadius, homeY},
                {homeX + d, homeY + d},
                {homeX, homeY + patrolRadius},
                {homeX - d, homeY + d},
                {homeX - patrolRadius, homeY},
                {homeX - d, homeY - d},
                {homeX, homeY - patrolRadius},
                {homeX + d, homeY - d}
        };
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
        if (hearsPlayer) alertTimer = alertDuration;
        if (alertTimer <= 0) state = EnemyState.RETURN;
    }

    private void updateChase(double deltaTime, boolean seesPlayer, Player player, CollisionMap collisionMap) {
        if (seesPlayer) {
            lastKnownX = playerCenterX(player);
            lastKnownY = playerCenterY(player);
        }

        double distanceToPlayer = distanceTo(playerCenterX(player), playerCenterY(player));
        if (seesPlayer && distanceToPlayer <= attackRange) {
            clearPath();
            state = EnemyState.ATTACK;
            return;
        }

        if (!boss && collisionMap != null) {
            followPathTo(lastKnownX, lastKnownY, chaseSpeed, deltaTime, collisionMap);
        } else {
            moveTowards(lastKnownX, lastKnownY, chaseSpeed, deltaTime, collisionMap);
        }

        if (!seesPlayer && distanceTo(lastKnownX, lastKnownY) < 10) {
            clearPath();
            state = EnemyState.RETURN;
        }
    }

    private List<Projectile> updateAttack(double deltaTime, Player player, CollisionMap collisionMap) {
        faceTowards(playerCenterX(player), playerCenterY(player));
        double distanceToPlayer = distanceTo(playerCenterX(player), playerCenterY(player));

        if (distanceToPlayer > attackRange || !canSeePlayer(player, collisionMap)) {
            state = EnemyState.CHASE;
            return List.of();
        }

        attackCooldownTimer -= deltaTime;
        if (attackCooldownTimer > 0) return List.of();

        attackCooldownTimer = getAttackCooldownDuration();
        EnemyPower power = selectPower(player);
        List<Projectile> result = power.cast(x, y, player);
        onAttackPerformed(player);
        return result;
    }

    /** Bosses sobrescrevem para trocar ataques conforme a fase. */
    protected EnemyPower selectPower(Player player) {
        return basePower;
    }

    protected void onAttackPerformed(Player player) {
        // Hook para bosses.
    }

    protected double getAttackCooldownDuration() {
        return attackCooldownDuration;
    }

    private void updateReturn(double deltaTime, CollisionMap collisionMap) {
        if (distanceTo(homeX, homeY) <= PATROL_POINT_TOLERANCE) {
            clearPath();
            state = EnemyState.PATROL;
            patrolWaypointIndex = 0;
            return;
        }

        boolean moved;
        if (!boss && collisionMap != null) {
            moved = followPathTo(homeX, homeY, patrolSpeed * 1.15, deltaTime, collisionMap);
        } else {
            moved = moveTowards(homeX, homeY, patrolSpeed * 1.15, deltaTime, collisionMap);
        }

        if (!moved) {
            faceTowards(homeX, homeY);
        }
    }

    private void enterAlert(Player player) {
        clearPath();
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
                case PATROL, CHASE, RETURN -> animator.setAction(CharacterAnimator.Action.WALK);
                case ATTACK -> animator.setAction(CharacterAnimator.Action.ATTACK);
                default -> animator.setAction(CharacterAnimator.Action.IDLE);
            }
        }
        animator.update(deltaTime);
    }

    private CharacterAnimator.Direction directionFromAngle() {
        double normalized = (directionAngle % 360 + 360) % 360;
        if (normalized >= 45 && normalized < 135) return CharacterAnimator.Direction.FRONT;
        if (normalized >= 135 && normalized < 225) return CharacterAnimator.Direction.LEFT;
        if (normalized >= 225 && normalized < 315) return CharacterAnimator.Direction.BACK;
        return CharacterAnimator.Direction.RIGHT;
    }

    public boolean canSeePlayer(Player player) {
        return canSeePlayer(player, null);
    }

    public boolean canSeePlayer(Player player, CollisionMap collisionMap) {
        double targetX = playerCenterX(player);
        double targetY = playerCenterY(player);
        double distance = distanceTo(targetX, targetY);
        if (distance > visionRange) return false;

        double angleToPlayer = Math.toDegrees(Math.atan2(targetY - y, targetX - x));
        double diff = angleDifference(directionAngle, angleToPlayer);
        if (Math.abs(diff) > visionHalfAngle) return false;

        return collisionMap == null || hasClearLineOfSight(collisionMap, x, y, targetX, targetY);
    }

    private boolean hasClearLineOfSight(CollisionMap collisionMap,
                                        double startX, double startY,
                                        double endX, double endY) {
        double dx = endX - startX;
        double dy = endY - startY;
        double distance = Math.hypot(dx, dy);
        if (distance < 1) return true;

        // Amostra a linha em passos pequenos usando a colisão ORIGINAL do projeto.
        // Não altera CollisionMap nem a forma como o Player colide com o cenário.
        double step = 6.0;
        int samples = Math.max(1, (int) Math.ceil(distance / step));
        for (int i = 1; i < samples; i++) {
            double t = i / (double) samples;
            double sx = startX + dx * t;
            double sy = startY + dy * t;
            if (!collisionMap.canMoveTo(sx - 1, sy - 1, 2, 2)) {
                return false;
            }
        }
        return true;
    }

    public boolean canHearPlayer(Player player) {
        return distanceTo(playerCenterX(player), playerCenterY(player)) <= hearingRadius;
    }

    private double playerCenterX(Player player) { return player.getX() + player.getWidth() / 2; }
    private double playerCenterY(Player player) { return player.getY() + player.getHeight() / 2; }

    protected double distanceTo(double targetX, double targetY) {
        return Math.hypot(targetX - x, targetY - y);
    }

    private void faceTowards(double targetX, double targetY) {
        directionAngle = Math.toDegrees(Math.atan2(targetY - y, targetX - x));
    }

    /**
     * Segue um caminho A*. Recalcula apenas periodicamente ou quando o alvo
     * mudou bastante de posição, evitando executar A* em todos os frames.
     */
    private boolean followPathTo(double targetX, double targetY, double speed,
                                 double deltaTime, CollisionMap collisionMap) {
        pathRecalculationTimer -= deltaTime;

        boolean targetChanged = Double.isNaN(pathTargetX)
                || Math.hypot(targetX - pathTargetX, targetY - pathTargetY)
                >= PATH_TARGET_CHANGE_DISTANCE;

        boolean pathFinished = currentPathIndex >= currentPath.size();

        if (pathRecalculationTimer <= 0 || targetChanged || pathFinished) {
            currentPath = pathfinder.findPath(
                    collisionMap,
                    x, y,
                    targetX, targetY,
                    width, height
            );
            currentPathIndex = 0;
            pathTargetX = targetX;
            pathTargetY = targetY;
            pathRecalculationTimer = PATH_RECALCULATION_INTERVAL;
        }

        while (currentPathIndex < currentPath.size()) {
            PathPoint point = currentPath.get(currentPathIndex);

            if (distanceTo(point.x(), point.y()) <= PATH_POINT_TOLERANCE) {
                currentPathIndex++;
                continue;
            }

            boolean moved = moveTowards(point.x(), point.y(), speed, deltaTime, collisionMap);
            if (!moved) {
                // Alguma condição do mapa mudou ou a aproximação ficou presa:
                // força novo A* na próxima atualização.
                pathRecalculationTimer = 0;
            }
            return moved;
        }

        // Se o A* não encontrou rota, usa o desvio local antigo como fallback.
        return moveTowards(targetX, targetY, speed, deltaTime, collisionMap);
    }

    private void clearPath() {
        currentPath = List.of();
        currentPathIndex = 0;
        pathRecalculationTimer = 0;
        pathTargetX = Double.NaN;
        pathTargetY = Double.NaN;
    }

    private boolean moveTowards(double targetX, double targetY, double speed,
                                double deltaTime, CollisionMap collisionMap) {
        double dx = targetX - x;
        double dy = targetY - y;
        double length = Math.hypot(dx, dy);
        if (length < 1) return true;

        dx /= length;
        dy /= length;
        double moveX = dx * speed * deltaTime;
        double moveY = dy * speed * deltaTime;
        directionAngle = Math.toDegrees(Math.atan2(dy, dx));

        if (collisionMap == null) {
            x += moveX;
            y += moveY;
            return true;
        }

        boolean moved = false;
        if (canOccupy(collisionMap, x + moveX, y)) {
            x += moveX;
            moved = true;
        }
        if (canOccupy(collisionMap, x, y + moveY)) {
            y += moveY;
            moved = true;
        }

        if (moved) return true;

        // Desvio local simples: quando a direção direta está bloqueada,
        // tenta escorregar perpendicularmente à parede em vez de ficar parado.
        // Isso é usado tanto na patrulha quanto no retorno/perseguição.
        double step = speed * deltaTime;
        double sideAX = -dy * step;
        double sideAY = dx * step;
        double sideBX = dy * step;
        double sideBY = -dx * step;

        if (canOccupy(collisionMap, x + sideAX, y + sideAY)) {
            x += sideAX;
            y += sideAY;
            directionAngle = Math.toDegrees(Math.atan2(sideAY, sideAX));
            return true;
        }
        if (canOccupy(collisionMap, x + sideBX, y + sideBY)) {
            x += sideBX;
            y += sideBY;
            directionAngle = Math.toDegrees(Math.atan2(sideBY, sideBX));
            return true;
        }

        return false;
    }

    private boolean canOccupy(CollisionMap map, double centerX, double centerY) {
        if (!boss) {
            // Para navegação, o slime consulta somente uma pequena área nos pés.
            // O sprite continua 80x94; isso não altera a CollisionMap nem a
            // hitbox do Player. Evita spawns próximos a paredes ficarem presos.
            final double footW = 18;
            final double footH = 14;
            final double spriteLeft = centerX - width / 2.0;
            final double spriteTop = centerY - height / 2.0;
            final double footX = spriteLeft + (width - footW) / 2.0;
            final double footY = spriteTop + height - footH - 5;
            return map.canMoveTo(footX, footY, footW, footH);
        }

        // Bosses permanecem com a navegação anterior.
        double hitboxW = Math.max(14, width * 0.72);
        double hitboxH = Math.max(14, height * 0.72);
        return map.canMoveTo(centerX - hitboxW / 2.0, centerY - hitboxH / 2.0, hitboxW, hitboxH);
    }

    private double angleDifference(double a, double b) {
        double diff = (b - a) % 360;
        if (diff < -180) diff += 360;
        if (diff > 180) diff -= 360;
        return diff;
    }

    public void configurePatrol(double radius, double speed) {
        patrolRadius = Math.max(0, radius);
        patrolSpeed = Math.max(20, speed);
    }

    public void setPosition(double newX, double newY) { this.x = newX; this.y = newY; }
    public void setHomePosition(double newX, double newY) {
        this.homeX = newX;
        this.homeY = newY;
    }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getDirectionAngle() { return directionAngle; }
    public double getVisionRange() { return visionRange; }
    public double getVisionHalfAngle() { return visionHalfAngle; }
    public double getHearingRadius() { return hearingRadius; }
    public EnemyState getState() { return state; }
    public CharacterAnimator getAnimator() { return animator; }
    public double getHealth() { return health; }
    public double getMaxHealth() { return maxHealth; }
    public double getHealthRatio() { return maxHealth <= 0 ? 0 : health / maxHealth; }
    public boolean isDead() { return health <= 0; }
    public boolean isBoss() { return boss; }
    public double getContactDamage() { return contactDamage; }

    public void reset() {
        x = homeX;
        y = homeY;
        health = maxHealth;
        state = EnemyState.PATROL;
        alertTimer = 0;
        attackCooldownTimer = 0;
        hurtTimer = 0;
        patrolWaypointIndex = 0;
        patrolPauseTimer = 0;
        clearPath();
        onReset();
    }

    protected void onReset() { }

    public void takeDamage(double amount) {
        if (isDead()) return;
        double oldHealth = health;
        health = Math.max(0, health - amount);
        hurtTimer = 0.14;
        onHealthChanged(oldHealth, health);
        if (isDead()) animator.setAction(CharacterAnimator.Action.DEATH);
    }

    protected void onHealthChanged(double oldHealth, double newHealth) { }

    public boolean isCollidingWith(Player player) {
        return x - width / 2 < player.getX() + player.getWidth()
                && x + width / 2 > player.getX()
                && y - height / 2 < player.getY() + player.getHeight()
                && y + height / 2 > player.getY();
    }
}

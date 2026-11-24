package br.edu.unex.nucleus.world;

import br.edu.unex.nucleus.ai.TripleBoltPower;
import br.edu.unex.nucleus.entity.Enemy;

/** Boss elemental usado nos mapas da campanha. */
public class ElementBoss extends Enemy {
    private final String bossName;
    private double specialCooldown = 3;
    private double warningTimer;
    private double activeTimer;
    private double specialX, specialY;
    private boolean specialTriggered;


    public ElementBoss(double x, double y, String bossName, double maxHealth, double damage) {
        super(x, y, new TripleBoltPower(), maxHealth, 72, 72, true, damage);
        this.bossName = bossName;
    }

    @Override
    protected void onBeforeUpdate(double dt, br.edu.unex.nucleus.entity.Player player) {
        if (!hasSpecialPower()) return;
        if (activeTimer > 0) activeTimer = Math.max(0, activeTimer - dt);
        if (warningTimer > 0) {
            warningTimer -= dt;
            if (warningTimer <= 0) {
                warningTimer = 0;
                activeTimer = isFireSpecial() ? 0.35 : 3.0;
                specialTriggered = false;
            }
        } else if (activeTimer <= 0 && (getState() == br.edu.unex.nucleus.ai.EnemyState.ATTACK || getState() == br.edu.unex.nucleus.ai.EnemyState.CHASE)) {
            specialCooldown -= dt;
            if (specialCooldown <= 0) {
                specialX = isFireSpecial() ? getX() : player.getX() + player.getWidth()/2;
                specialY = isFireSpecial() ? getY() : player.getY() + player.getHeight()/2;
                warningTimer = 1.5;
                specialCooldown = 10;
            }
        }
    }

    public boolean hasSpecialPower() { return this instanceof IgnarBoss || this instanceof NerionBoss; }
    public boolean isFireSpecial() { return this instanceof IgnarBoss; }
    public boolean isSpecialWarning() { return warningTimer > 0; }
    public boolean isSpecialActive() { return activeTimer > 0; }
    public double getSpecialX() { return specialX; }
    public double getSpecialY() { return specialY; }
    public double getSpecialRadius() { return isFireSpecial() ? 110 : 85; }
    public boolean consumeSpecialHit() {
        if (!specialTriggered && isSpecialActive()) { specialTriggered = true; return true; }
        return false;
    }
    @Override
    protected void onReset() {
        specialCooldown = 3; warningTimer = 0; activeTimer = 0; specialTriggered = false;
    }
    public String getBossName() { return bossName; }
}

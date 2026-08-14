package br.edu.unex.nucleus.world;

import br.edu.unex.nucleus.ai.TripleBoltPower;
import br.edu.unex.nucleus.entity.Enemy;

/** Boss elemental usado nos mapas da campanha. */
public class ElementBoss extends Enemy {
    private final String bossName;

    public ElementBoss(double x, double y, String bossName, double maxHealth, double damage) {
        super(x, y, new TripleBoltPower(), maxHealth, 72, 72, true, damage);
        this.bossName = bossName;
    }

    public String getBossName() { return bossName; }
}

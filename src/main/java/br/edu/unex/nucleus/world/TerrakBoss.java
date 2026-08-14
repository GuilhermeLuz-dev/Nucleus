package br.edu.unex.nucleus.world;

import br.edu.unex.nucleus.ai.TripleBoltPower;
import br.edu.unex.nucleus.entity.Enemy;

/** Boss final do Vale dos Colossos. */
public class TerrakBoss extends Enemy {

    public TerrakBoss(double x, double y) {
        super(x, y, new TripleBoltPower(), 1200, 72, 72, true, 20);
    }
}

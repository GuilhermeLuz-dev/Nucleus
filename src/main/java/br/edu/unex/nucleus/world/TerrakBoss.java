package br.edu.unex.nucleus.world;

import br.edu.unex.nucleus.ai.TripleBoltPower;
import br.edu.unex.nucleus.entity.Enemy;

/** Boss final do Vale dos Colossos. */
public class TerrakBoss extends Enemy {

    public TerrakBoss(double x, double y) {
        // Plant3: spritesheet 64x64, 4 direções.
        // Frames: Idle 4, Walk 6, Attack 7, Hurt 5, Death 10.
        super(x, y, new TripleBoltPower(), 1200, 72, 72, true, 20,
                "/sprites/terrakplant", 4, 6, 7, 5, 10);
    }
}

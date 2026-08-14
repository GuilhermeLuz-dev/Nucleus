package br.edu.unex.nucleus.ai;

import br.edu.unex.nucleus.entity.Player;
import br.edu.unex.nucleus.entity.Projectile;

import java.util.List;

public class SingleBoltPower implements EnemyPower {

    private final double speed;
    private final double range;

    public SingleBoltPower() {
        this.speed = 260;
        this.range = 400;
    }

    @Override
    public List<Projectile> cast(double originX, double originY, Player player) {

        double[] direction = PowerMath.directionTo(originX, originY, player);

        Projectile bolt = new Projectile(
                originX,
                originY,
                direction[0] * speed,
                direction[1] * speed,
                range
        );

        return List.of(bolt);
    }
}

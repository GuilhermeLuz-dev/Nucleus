package br.edu.unex.nucleus.ai;

import br.edu.unex.nucleus.entity.Player;
import br.edu.unex.nucleus.entity.Projectile;

import java.util.List;

public class HomingOrbPower implements EnemyPower {

    private final double speed;
    private final double range;
    private final double turnSpeedDegreesPerSecond;

    public HomingOrbPower() {
        this.speed = 140;
        this.range = 500;
        this.turnSpeedDegreesPerSecond = 90;
    }

    @Override
    public List<Projectile> cast(double originX, double originY, Player player) {

        double[] direction = PowerMath.directionTo(originX, originY, player);

        Projectile orb = new Projectile(
                originX,
                originY,
                direction[0] * speed,
                direction[1] * speed,
                range,
                player,
                turnSpeedDegreesPerSecond
        );

        return List.of(orb);
    }
}

package br.edu.unex.nucleus.ai;

import br.edu.unex.nucleus.entity.Player;
import br.edu.unex.nucleus.entity.Projectile;

import java.util.ArrayList;
import java.util.List;

public class TripleBoltPower implements EnemyPower {

    private final double speed;
    private final double range;
    private final double spreadAngleDegrees;

    public TripleBoltPower() {
        this.speed = 220;
        this.range = 350;
        this.spreadAngleDegrees = 18;
    }

    @Override
    public List<Projectile> cast(double originX, double originY, Player player) {

        double[] baseDirection = PowerMath.directionTo(originX, originY, player);

        List<Projectile> bolts = new ArrayList<>();

        double[] angles = { -spreadAngleDegrees, 0, spreadAngleDegrees };

        for (double angle : angles) {

            double[] direction = PowerMath.rotate(baseDirection[0], baseDirection[1], angle);

            bolts.add(new Projectile(
                    originX,
                    originY,
                    direction[0] * speed,
                    direction[1] * speed,
                    range
            ));
        }

        return bolts;
    }
}

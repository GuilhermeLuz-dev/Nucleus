package br.edu.unex.nucleus.ai;

import br.edu.unex.nucleus.entity.Player;

public final class PowerMath {

    private PowerMath() {
        // classe utilitária, não deve ser instanciada
    }

    public static double[] directionTo(double originX, double originY, Player player) {

        double targetX = player.getX() + player.getWidth() / 2;
        double targetY = player.getY() + player.getHeight() / 2;

        double dx = targetX - originX;
        double dy = targetY - originY;

        double length = Math.sqrt(dx * dx + dy * dy);

        if (length < 1) {
            length = 1;
        }

        return new double[] { dx / length, dy / length };
    }

    public static double[] rotate(double dirX, double dirY, double angleDegrees) {

        double angleRad = Math.toRadians(angleDegrees);

        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);

        double rotatedX = dirX * cos - dirY * sin;
        double rotatedY = dirX * sin + dirY * cos;

        return new double[] { rotatedX, rotatedY };
    }
}

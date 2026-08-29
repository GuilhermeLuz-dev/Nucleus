package br.edu.unex.nucleus.world;

import br.edu.unex.nucleus.entity.Player;

/**
 * Área do mapa que causa dano contínuo sem bloquear movimento.
 * É usada atualmente pelas zonas de fogo/lava do mapa Ignar.
 */
public class DamageZone {

    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final double damagePerSecond;

    public DamageZone(double x, double y, double width, double height, double damagePerSecond) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        this.damagePerSecond = Math.max(0, damagePerSecond);
    }

    /**
     * Usa a mesma região física dos pés/corpo usada pela colisão do Player.
     * Assim a parte superior do sprite não toma dano apenas por "encostar"
     * visualmente na lava.
     */
    public boolean touches(Player player) {
        final double hitboxWidth = 34;
        final double hitboxHeight = 44;
        final double offsetX = (player.getWidth() - hitboxWidth) / 2.0;
        final double offsetY = player.getHeight() - hitboxHeight - 6;

        double px = player.getX() + offsetX;
        double py = player.getY() + offsetY;

        return px < x + width
                && px + hitboxWidth > x
                && py < y + height
                && py + hitboxHeight > y;
    }

    public double getDamagePerSecond() {
        return damagePerSecond;
    }
}

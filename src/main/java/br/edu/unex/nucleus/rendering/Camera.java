package br.edu.unex.nucleus.rendering;

import br.edu.unex.nucleus.entity.Player;

/**
 * Câmera 2D do jogo. Mantém o jogador próximo do centro da tela e limita
 * o deslocamento aos limites do mapa atual.
 */
public class Camera {

    private final double viewportWidth;
    private final double viewportHeight;

    private double x;
    private double y;

    public Camera(double viewportWidth, double viewportHeight) {
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    public void update(Player player, double worldWidth, double worldHeight) {
        double targetX = player.getX() + player.getWidth() / 2.0 - viewportWidth / 2.0;
        double targetY = player.getY() + player.getHeight() / 2.0 - viewportHeight / 2.0;

        x = clamp(targetX, 0, Math.max(0, worldWidth - viewportWidth));
        y = clamp(targetY, 0, Math.max(0, worldHeight - viewportHeight));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    public double getX() { return x; }
    public double getY() { return y; }
}

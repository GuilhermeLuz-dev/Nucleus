package br.edu.unex.nucleus.rendering;

import br.edu.unex.nucleus.entity.Player;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public class Renderer {

    private final Canvas canvas;
    private final GraphicsContext graphicsContext;

    public Renderer(Canvas canvas) {

        this.canvas = canvas;

        this.graphicsContext =
                canvas.getGraphicsContext2D();
    }

    public void clear() {

        graphicsContext.clearRect(
                0,
                0,
                canvas.getWidth(),
                canvas.getHeight()
        );
    }

    public void drawPlayer(Player player) {

        graphicsContext.fillRect(
                player.getX(),
                player.getY(),
                player.getWidth(),
                player.getHeight()
        );
    }

    public void drawDebug(Player player, double deltaTime) {

        double fps = deltaTime > 0 ? 1.0 / deltaTime : 0;

        graphicsContext.fillText(
                String.format(
                        "Posição: X=%.1f Y=%.1f",
                        player.getX(),
                        player.getY()
                ),
                10,
                20
        );

        graphicsContext.fillText(
                String.format(
                        "FPS: %.1f",
                        fps
                ),
                10,
                40
        );

        graphicsContext.fillText(
                String.format(
                        "Delta Time: %.4f s",
                        deltaTime
                ),
                10,
                60
        );
    }
}
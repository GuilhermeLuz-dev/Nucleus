package br.edu.unex.nucleus.core;

import br.edu.unex.nucleus.entity.Player;
import br.edu.unex.nucleus.input.InputManager;
import br.edu.unex.nucleus.rendering.Renderer;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;

public class GameEngine {

    private final Canvas canvas;
    private final Scene scene;

    private final Renderer renderer;
    private final Player player;
    private final InputManager inputManager;

    private double deltaTime;
    private AnimationTimer gameLoop;

    private long lastTime;

    public GameEngine(Canvas canvas, Scene scene) {

        this.canvas = canvas;
        this.scene = scene;

        this.renderer = new Renderer(canvas);

        this.player = new Player(
                canvas.getWidth() / 2 - 16,
                canvas.getHeight() / 2 - 16
        );

        this.inputManager = new InputManager(scene);
    }

    public void start() {

        lastTime = System.nanoTime();

        gameLoop = new AnimationTimer() {

            @Override
            public void handle(long currentTime) {

                deltaTime =
                        (currentTime - lastTime) / 1_000_000_000.0;

                lastTime = currentTime;

                update(deltaTime);

                render();
            }
        };

        gameLoop.start();
    }

    private void update(double deltaTime) {

        double dx = 0;
        double dy = 0;

        if (inputManager.isPressed(KeyCode.W)
                || inputManager.isPressed(KeyCode.UP)) {

            dy -= 1;
        }

        if (inputManager.isPressed(KeyCode.S)
                || inputManager.isPressed(KeyCode.DOWN)) {

            dy += 1;
        }

        if (inputManager.isPressed(KeyCode.A)
                || inputManager.isPressed(KeyCode.LEFT)) {

            dx -= 1;
        }

        if (inputManager.isPressed(KeyCode.D)
                || inputManager.isPressed(KeyCode.RIGHT)) {

            dx += 1;
        }

        player.move(dx, dy, deltaTime);
    }

    private void render() {

        renderer.clear();

        renderer.drawPlayer(player);

        renderer.drawDebug(player, deltaTime);
    }
}
package br.edu.unex.nucleus.core;

import br.edu.unex.nucleus.entity.Player;
import br.edu.unex.nucleus.input.InputManager;
import br.edu.unex.nucleus.rendering.Camera;
import br.edu.unex.nucleus.rendering.Renderer;
import br.edu.unex.nucleus.world.Island;
import br.edu.unex.nucleus.world.WorldBuilder;
import br.edu.unex.nucleus.world.WorldManager;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;

/**

 * Entrada -> movimento do jogador ->
 * simulação do mundo -> câmera -> renderização. Regras de mundo ficam no
 * WorldManager e desenho fica no Renderer.
 */
public class GameEngine {

    private final Canvas canvas;
    private final Renderer renderer;
    private final Camera camera;
    private final Player player;
    private final WorldManager worldManager;
    private final InputManager inputManager;

    private AnimationTimer gameLoop;
    private long lastTime;
    private boolean attackWasPressed;
    private boolean potionWasPressed;
    private boolean restartWasPressed;
    private boolean gameOver;
    private boolean gameWon;

    public GameEngine(Canvas canvas, Scene scene) {
        this.canvas = canvas;
        this.renderer = new Renderer(canvas);
        this.camera = new Camera(canvas.getWidth(), canvas.getHeight());
        this.player = new Player(0, 0);
        this.worldManager = WorldBuilder.createCampaign();
        this.worldManager.initializePlayer(player);
        this.inputManager = new InputManager(scene);
    }

    public void start() {
        lastTime = System.nanoTime();

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long currentTime) {
                double deltaTime = (currentTime - lastTime) / 1_000_000_000.0;
                lastTime = currentTime;

                // Evita saltos gigantes se a janela for pausada/minimizada.
                deltaTime = Math.min(deltaTime, 0.05);

                update(deltaTime);
                render(deltaTime);
            }
        };

        gameLoop.start();
    }

    private void update(double deltaTime) {
        if (gameOver || gameWon) {
            updateRestartInput();
            return;
        }

        if (player.isDead()) {
            gameOver = true;
            return;
        }

        handlePotionInput();
        handlePlayerMovement(deltaTime);
        handleAttackInput();

        Island island = worldManager.getCurrentIsland();
        if (island != null) {
            player.clampToWorld(island.getWorldWidth(), island.getWorldHeight());
        }

        boolean makingNoise = isMovementKeyPressed();
        worldManager.update(player, deltaTime, makingNoise);
        player.updateCooldowns(deltaTime);

        gameWon = worldManager.isCampaignComplete();
    }

    private void handlePotionInput() {
        boolean pressed = inputManager.isPressed(KeyCode.H);
        if (pressed && !potionWasPressed) {
            player.useHealthPotion();
        }
        potionWasPressed = pressed;
    }

    private void handleAttackInput() {
        boolean pressed = inputManager.isPressed(KeyCode.SPACE);
        if (pressed && !attackWasPressed) {
            worldManager.playerAttack(player);
        }
        attackWasPressed = pressed;
    }

    private void handlePlayerMovement(double deltaTime) {
        double dx = 0;
        double dy = 0;

        if (inputManager.isPressed(KeyCode.W) || inputManager.isPressed(KeyCode.UP)) dy -= 1;
        if (inputManager.isPressed(KeyCode.S) || inputManager.isPressed(KeyCode.DOWN)) dy += 1;
        if (inputManager.isPressed(KeyCode.A) || inputManager.isPressed(KeyCode.LEFT)) dx -= 1;
        if (inputManager.isPressed(KeyCode.D) || inputManager.isPressed(KeyCode.RIGHT)) dx += 1;

        if (dx == 0 && dy == 0) return;

        Island island = worldManager.getCurrentIsland();
        double length = Math.hypot(dx, dy);
        dx /= length;
        dy /= length;

        double distance = player.getSpeed() * deltaTime;
        int steps = Math.max(1, (int) Math.ceil(distance / 4.0));
        double stepDistance = distance / steps;

        for (int i = 0; i < steps; i++) {
            tryMoveAxis(island, dx * stepDistance, 0);
            tryMoveAxis(island, 0, dy * stepDistance);
        }

        player.setMoving(true);
    }

    private void tryMoveAxis(Island island, double dx, double dy) {
        if (island == null) return;

        double oldX = player.getX();
        double oldY = player.getY();
        player.moveByPixels(dx, dy);

        if (!isValidPlayerPosition(island)) {
            player.setPosition(oldX, oldY);
        }
    }

    /**
     A imagem do personagem é 80x94, mas a área física é menor. A hitbox
     fica sobre o corpo/pés para permitir passagem por pontes estreitas.
     */
    private boolean isValidPlayerPosition(Island island) {
        final double hitboxWidth = 34;
        final double hitboxHeight = 44;
        final double offsetX = (player.getWidth() - hitboxWidth) / 2.0;
        final double offsetY = player.getHeight() - hitboxHeight - 6;

        return island.getCollisionMap().canMoveTo(
                player.getX() + offsetX,
                player.getY() + offsetY,
                hitboxWidth,
                hitboxHeight
        );
    }

    private boolean isMovementKeyPressed() {
        return inputManager.isPressed(KeyCode.W)
                || inputManager.isPressed(KeyCode.A)
                || inputManager.isPressed(KeyCode.S)
                || inputManager.isPressed(KeyCode.D)
                || inputManager.isPressed(KeyCode.UP)
                || inputManager.isPressed(KeyCode.DOWN)
                || inputManager.isPressed(KeyCode.LEFT)
                || inputManager.isPressed(KeyCode.RIGHT);
    }

    private void updateRestartInput() {
        boolean pressed = inputManager.isPressed(KeyCode.R);
        if (pressed && !restartWasPressed) {
            restartCampaign();
        }
        restartWasPressed = pressed;
    }

    private void restartCampaign() {
        player.resetState();
        worldManager.resetCampaign();
        worldManager.initializePlayer(player);
        gameOver = false;
        gameWon = false;
    }

    private void render(double deltaTime) {
        Island island = worldManager.getCurrentIsland();
        if (island == null) return;

        camera.update(player, island.getWorldWidth(), island.getWorldHeight());

        renderer.render(
                island,
                player,
                camera,
                deltaTime,
                gameOver,
                gameWon
        );
    }

    public Player getPlayer() { return player; }
    public WorldManager getWorldManager() { return worldManager; }
}

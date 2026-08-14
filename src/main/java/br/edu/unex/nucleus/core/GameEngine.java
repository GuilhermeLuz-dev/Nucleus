package br.edu.unex.nucleus.core;

import br.edu.unex.nucleus.ai.HomingOrbPower;
import br.edu.unex.nucleus.ai.SingleBoltPower;
import br.edu.unex.nucleus.ai.TripleBoltPower;
import br.edu.unex.nucleus.entity.Enemy;
import br.edu.unex.nucleus.entity.HealthPotion;
import br.edu.unex.nucleus.entity.Player;
import br.edu.unex.nucleus.entity.Portal;
import br.edu.unex.nucleus.entity.Projectile;
import br.edu.unex.nucleus.input.InputManager;
import br.edu.unex.nucleus.rendering.Renderer;
import br.edu.unex.nucleus.world.Island;
import br.edu.unex.nucleus.world.WorldManager;
import br.edu.unex.nucleus.world.TiledMap;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import java.util.List;

public class GameEngine {

    private final Canvas canvas;
    private final Scene scene;

    private final Renderer renderer;
    private final Player player;
    private final WorldManager worldManager;
    private final InputManager inputManager;

    private boolean playerIsMakingNoise;

    private double deltaTime;
    private AnimationTimer gameLoop;

    private long lastTime;
    private boolean attackWasPressed;
    private boolean potionWasPressed;
    private boolean gameWon;
    private boolean gameOver;
    private boolean restartWasPressed;

    public GameEngine(Canvas canvas, Scene scene) {

        this.canvas = canvas;
        this.scene = scene;

        this.renderer = new Renderer(canvas);

        this.player = new Player(
                canvas.getWidth() / 2 - 16,
                canvas.getHeight() / 2 - 16
        );

        this.worldManager = buildWorld();

        // O spawn é carregado da camada "Player" do mapa inicial.

        this.inputManager = new InputManager(scene);
    }

    private WorldManager buildWorld() {

        WorldManager world = new WorldManager();

        Island terrak = createTiledIsland(
                "terrak",
                "Terrak — O Vale dos Colossos",
                "/backgrounds/terrak.tmx",
                Color.rgb(34, 65, 30),
                new br.edu.unex.nucleus.world.TerrakBoss(615, 135)
        );

        Island nerion = createTiledIsland(
                "nerion",
                "Nerion — O Abismo Azul",
                "/backgrounds/nerion.tmx",
                Color.rgb(15, 80, 120),
                new br.edu.unex.nucleus.world.ElementBoss(615, 135,
                        "NERION — SENHORA DAS ÁGUAS", 1500, 26)
        );

        Island ignar = createTiledIsland(
                "ignar",
                "Ignar — A Forja Infernal",
                "/backgrounds/ignar.tmx",
                Color.rgb(120, 35, 15),
                new br.edu.unex.nucleus.world.ElementBoss(615, 135,
                        "IGNAR — SENHOR DO FOGO", 1800, 30)
        );

        world.addIsland(terrak);
        world.addIsland(nerion);
        world.addIsland(ignar);

        // O primeiro mapa continua sendo o Terrak.
        world.setInitialIsland("terrak");

        // O spawn do jogador vem da camada "Player" de cada TMX.
        setPlayerSpawnFromMap(world, "terrak");

        return world;
    }

    /**
     * Cria um mapa usando diretamente as camadas do Tiled:
     * Colisão, Monstros, Poções e Player.
     */
    private Island createTiledIsland(
            String id,
            String displayName,
            String mapPath,
            Color fallbackColor,
            Enemy boss) {

        Island island = new Island(
                id,
                displayName,
                new java.util.ArrayList<>(),
                new java.util.ArrayList<>(),
                fallbackColor,
                null,
                mapPath
        );

        TiledMap map = island.getTiledMap();

        // Monstros: cada objeto da camada "Monstros" vira um inimigo.
        br.edu.unex.nucleus.ai.EnemyPower[] powers = {
                new SingleBoltPower(),
                new HomingOrbPower(),
                new TripleBoltPower()
        };

        int index = 0;
        for (TiledMap.MapObject object : map.getObjects("Monstros")) {
            double x = object.x() + object.width() / 2.0;
            double y = object.y() + object.height() / 2.0;

            island.getEnemies().add(
                    new Enemy(x, y, powers[index % powers.length])
            );
            index++;
        }

        // O boss permanece no centro da arena superior.
        island.getEnemies().add(boss);

        // Poções: a posição também vem diretamente do Tiled.
        for (TiledMap.MapObject object : map.getObjects("Poções")) {
            double x = object.x() + object.width() / 2.0;
            double y = object.y() + object.height() / 2.0;
            island.getHealthPotions().add(new HealthPotion(x, y, 100));
        }

        return island;
    }

    private void setPlayerSpawnFromMap(WorldManager world, String islandId) {
        Island island = world.getIsland(islandId);
        if (island == null || island.getTiledMap() == null) return;

        java.util.List<TiledMap.MapObject> spawns =
                island.getTiledMap().getObjects("Player");

        if (!spawns.isEmpty()) {
            TiledMap.MapObject spawn = spawns.get(0);
            player.setPosition(spawn.x(), spawn.y());
        }
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

        // H usa uma poção de vida.
        boolean potionPressed = inputManager.isPressed(KeyCode.H);
        if (potionPressed && !potionWasPressed) {
            player.useHealthPotion();
        }
        potionWasPressed = potionPressed;

        if (player.isDead()) {
            gameOver = true;
            boolean restartPressed = inputManager.isPressed(KeyCode.R);
            if (restartPressed && !restartWasPressed) {
                player.setPosition(512, 900);
                player.healFull();
                gameOver = false;
            }
            restartWasPressed = restartPressed;
            return;
        }

        player.updateCooldowns(deltaTime);

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

        movePlayerWithCollision(dx, dy, deltaTime);

        boolean attackPressed = inputManager.isPressed(KeyCode.SPACE);
        if (attackPressed && !attackWasPressed) {
            worldManager.playerAttack(player);
        }
        attackWasPressed = attackPressed;
        restartWasPressed = inputManager.isPressed(KeyCode.R);

        // Mantém o personagem dentro dos limites do mapa atual.
        Island currentIsland = worldManager.getCurrentIsland();
        player.clampToWorld(
                currentIsland.getWorldWidth(),
                currentIsland.getWorldHeight()
        );

        // Jogador "faz barulho" (som que o inimigo pode ouvir) sempre que se move.
        playerIsMakingNoise = (dx != 0 || dy != 0);

        worldManager.update(player, deltaTime, playerIsMakingNoise);
        // Derrotar Ignar encerra a campanha.
        gameWon = worldManager.isCampaignComplete();
    }

    private void movePlayerWithCollision(double dx, double dy, double deltaTime) {
        Island island = worldManager.getCurrentIsland();

        if (dx == 0 && dy == 0) {
            return;
        }

        // Normaliza o vetor para que diagonal não fique mais rápida.
        double length = Math.hypot(dx, dy);
        double normalizedX = dx / length;
        double normalizedY = dy / length;

        // Divide o deslocamento em pequenos passos para impedir que o
        // personagem atravesse uma parede fina em um único frame.
        double totalDistance = player.getSpeed() * deltaTime;
        int steps = Math.max(1, (int) Math.ceil(totalDistance / 4.0));
        double stepDeltaTime = deltaTime / steps;

        for (int i = 0; i < steps; i++) {

            // Primeiro eixo X: permite deslizar pela parede.
            double oldX = player.getX();
            double oldY = player.getY();

            player.move(normalizedX, 0, stepDeltaTime);

            if (!isValidPlayerPosition(island)) {
                player.setPosition(oldX, oldY);
            }

            // Depois eixo Y.
            oldX = player.getX();
            oldY = player.getY();

            player.move(0, normalizedY, stepDeltaTime);

            if (!isValidPlayerPosition(island)) {
                player.setPosition(oldX, oldY);
            }
        }
    }

    private boolean isValidPlayerPosition(Island island) {
        // O sprite tem 80x94 px, mas o corpo real do personagem é menor.
        // Usar o sprite inteiro como hitbox impedia a passagem pelas pontes
        // estreitas dos mapas novos e fazia o personagem travar nas bordas.
        // O hitbox fica centralizado e um pouco mais baixo, acompanhando os
        // pés/corpo do personagem.
        final double hitboxWidth = 34;
        final double hitboxHeight = 44;
        final double hitboxOffsetX = (player.getWidth() - hitboxWidth) / 2.0;
        final double hitboxOffsetY = 42;

        return island.getCollisionMap().canMoveTo(
                player.getX() + hitboxOffsetX,
                player.getY() + hitboxOffsetY,
                hitboxWidth,
                hitboxHeight
        );
    }

    private void render() {

        Island currentIsland = worldManager.getCurrentIsland();

        double cameraX = renderer.calculateCameraX(
                player.getX(),
                currentIsland.getWorldWidth()
        );
        double cameraY = renderer.calculateCameraY(
                player.getY(),
                currentIsland.getWorldHeight()
        );

        renderer.drawBackground(currentIsland, cameraX, cameraY);

        for (Portal portal : currentIsland.getPortals()) {
            renderer.drawPortal(portal, cameraX, cameraY);
        }

        for (HealthPotion potion : currentIsland.getHealthPotions()) {
            renderer.drawHealthPotion(potion, cameraX, cameraY);
        }

        renderer.drawPlayer(player, cameraX, cameraY);

        for (Enemy enemy : currentIsland.getEnemies()) {
            renderer.drawEnemy(enemy, cameraX, cameraY);
        }

        for (Projectile projectile : currentIsland.getProjectiles()) {
            renderer.drawProjectile(projectile, cameraX, cameraY);
        }

        renderer.drawDebug(player, deltaTime, cameraX, cameraY);
        renderer.drawPlayerHealth(player);
        renderer.drawHealthPotionHud(player);
        renderer.drawBossBars(currentIsland);
        if (gameWon) {
            renderer.drawVictoryOverlay();
        } else if (gameOver) {
            renderer.drawGameOverOverlay();
        }

        renderer.drawIslandDebug(currentIsland);
    }
}

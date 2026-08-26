package br.edu.unex.nucleus.rendering;

import br.edu.unex.nucleus.ai.EnemyState;
import br.edu.unex.nucleus.entity.Enemy;
import br.edu.unex.nucleus.entity.HealthPotion;
import br.edu.unex.nucleus.entity.Player;
import br.edu.unex.nucleus.entity.Portal;
import br.edu.unex.nucleus.entity.Projectile;
import br.edu.unex.nucleus.world.Island;
import br.edu.unex.nucleus.world.ElementBoss;
import br.edu.unex.nucleus.world.TiledMap;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Camada visual do jogo. Não decide regras: apenas desenha o estado recebido.
 */
public class Renderer {

    private final Canvas canvas;
    private final GraphicsContext graphicsContext;

    public Renderer(Canvas canvas) {
        this.canvas = canvas;
        this.graphicsContext = canvas.getGraphicsContext2D();
        this.graphicsContext.setImageSmoothing(false);
        this.graphicsContext.setFont(Font.font("System", 14));
    }

    public void render(Island island, Player player, Camera camera,
                       double deltaTime, boolean gameOver, boolean gameWon) {
        drawBackground(island, camera.getX(), camera.getY());

        for (Portal portal : island.getPortals()) {
            drawPortal(portal, camera.getX(), camera.getY());
        }

        for (HealthPotion potion : island.getHealthPotions()) {
            drawHealthPotion(potion, camera.getX(), camera.getY());
        }

        for (Projectile projectile : island.getProjectiles()) {
            drawProjectile(projectile, camera.getX(), camera.getY());
        }

        for (Enemy enemy : island.getEnemies()) {
            drawEnemy(enemy, camera.getX(), camera.getY());
        }

        drawPlayer(player, camera.getX(), camera.getY());

        drawHud(island, player, deltaTime);

        if (gameOver) drawGameOverOverlay();
        if (gameWon) drawVictoryOverlay();
    }

    private void drawHud(Island island, Player player, double deltaTime) {
        drawIslandDebug(island);
        drawDebug(player, deltaTime);
        drawPlayerHealth(player);
        drawHealthPotionHud(player);
        drawBossBar(island);
    }

    public void clear() {
        graphicsContext.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    public void drawBackground(Island island, double cameraX, double cameraY) {
        clear();
        graphicsContext.setImageSmoothing(false);

        if (island.getTiledMap() != null) {
            drawTiledMap(island.getTiledMap(), cameraX, cameraY);
        } else if (island.getBackgroundImage() != null) {
            graphicsContext.drawImage(island.getBackgroundImage(), -cameraX, -cameraY);
        } else {
            graphicsContext.setFill(island.getBackgroundColor());
            graphicsContext.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        }
    }

    private void drawTiledMap(TiledMap map, double cameraX, double cameraY) {
        TiledMap.TileSet ts = map.getTileSet();
        int tw = ts.tileWidth();
        int th = ts.tileHeight();

        int firstX = map.getMinTileX() + Math.max(0, (int) Math.floor(cameraX / tw) - 1);
        int firstY = map.getMinTileY() + Math.max(0, (int) Math.floor(cameraY / th) - 1);
        int lastX = Math.min(
                map.getMaxTileX(),
                map.getMinTileX() + (int) Math.ceil((cameraX + canvas.getWidth()) / tw)
        );
        int lastY = Math.min(
                map.getMaxTileY(),
                map.getMinTileY() + (int) Math.ceil((cameraY + canvas.getHeight()) / th)
        );

        for (int y = firstY; y <= lastY; y++) {
            for (int x = firstX; x <= lastX; x++) {
                int gid = map.getGid(x, y);
                if (gid <= 0) continue;

                int local = gid - 1;
                int sx = (local % ts.columns()) * tw;
                int sy = (local / ts.columns()) * th;

                double dx = (x - map.getMinTileX()) * tw - cameraX;
                double dy = (y - map.getMinTileY()) * th - cameraY;

                graphicsContext.drawImage(ts.image(), sx, sy, tw, th, dx, dy, tw, th);
            }
        }
    }

    private void drawPlayer(Player player, double cameraX, double cameraY) {
        player.getAnimator().draw(
                graphicsContext,
                player.getX() - cameraX,
                player.getY() - cameraY,
                player.getWidth(),
                player.getHeight()
        );
    }

    private void drawEnemy(Enemy enemy, double cameraX, double cameraY) {
        if (enemy.isDead()) return;

        double drawX = enemy.getX() - enemy.getWidth() / 2.0 - cameraX;
        double drawY = enemy.getY() - enemy.getHeight() / 2.0 - cameraY;

        if (enemy.isBoss()) {
            graphicsContext.setFill(Color.rgb(70, 180, 50, 0.18));
            graphicsContext.fillOval(
                    drawX - 18, drawY - 18,
                    enemy.getWidth() + 36, enemy.getHeight() + 36
            );
        }

        enemy.getAnimator().draw(
                graphicsContext,
                drawX,
                drawY,
                enemy.getWidth(),
                enemy.getHeight()
        );
    }

    private void drawPortal(Portal portal, double cameraX, double cameraY) {
        double x = portal.getX() - cameraX;
        double y = portal.getY() - cameraY;
        graphicsContext.setFill(Color.rgb(180, 100, 220, 0.7));
        graphicsContext.fillOval(x, y, portal.getWidth(), portal.getHeight());
        graphicsContext.setStroke(Color.rgb(120, 40, 160));
        graphicsContext.strokeOval(x, y, portal.getWidth(), portal.getHeight());
    }

    private void drawHealthPotion(HealthPotion potion, double cameraX, double cameraY) {
        if (potion.isCollected()) return;

        double x = potion.getX() - cameraX;
        double y = potion.getY() - cameraY;

        graphicsContext.setFill(Color.rgb(0, 0, 0, 0.35));
        graphicsContext.fillOval(x - 11, y - 7, 22, 14);
        graphicsContext.setFill(Color.CRIMSON);
        graphicsContext.fillRoundRect(x - 7, y - 12, 14, 20, 5, 5);
        graphicsContext.setFill(Color.LIGHTPINK);
        graphicsContext.fillRect(x - 5, y - 9, 4, 8);
        graphicsContext.setFill(Color.LIGHTGRAY);
        graphicsContext.fillRect(x - 5, y - 17, 10, 6);
    }

    private void drawProjectile(Projectile projectile, double cameraX, double cameraY) {
        graphicsContext.setFill(Color.rgb(255, 80, 0));
        graphicsContext.fillOval(
                projectile.getX() - cameraX,
                projectile.getY() - cameraY,
                projectile.getWidth(),
                projectile.getHeight()
        );
    }

    private void drawPlayerHealth(Player player) {
        double x = 18;
        double y = canvas.getHeight() - 38;
        double width = 220;
        double height = 18;

        graphicsContext.setFill(Color.rgb(0, 0, 0, 0.75));
        graphicsContext.fillRoundRect(x - 4, y - 4, width + 8, height + 8, 8, 8);
        graphicsContext.setFill(Color.DARKRED);
        graphicsContext.fillRect(x, y, width, height);
        graphicsContext.setFill(Color.LIMEGREEN);
        graphicsContext.fillRect(
                x, y,
                width * (player.getHealth() / player.getMaxHealth()), height
        );
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.fillText(
                String.format("Vida: %.0f / %.0f", player.getHealth(), player.getMaxHealth()),
                x + 8, y + 14
        );
    }

    private void drawHealthPotionHud(Player player) {
        double x = 255;
        double y = canvas.getHeight() - 40;

        graphicsContext.setFill(Color.rgb(0, 0, 0, 0.75));
        graphicsContext.fillRoundRect(x - 8, y - 5, 150, 28, 8, 8);
        graphicsContext.setFill(Color.CRIMSON);
        graphicsContext.fillRoundRect(x, y, 18, 18, 4, 4);
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.fillText(
                "Poções: " + player.getHealthPotions() + "  [H]",
                x + 26, y + 14
        );
    }

    private void drawBossBar(Island island) {
        for (Enemy enemy : island.getEnemies()) {
            if (!enemy.isBoss() || enemy.isDead()) continue;

            double width = 420;
            double height = 22;
            double x = (canvas.getWidth() - width) / 2;
            double y = 28;

            graphicsContext.setFill(Color.rgb(0, 0, 0, 0.85));
            graphicsContext.fillRoundRect(x - 6, y - 6, width + 12, height + 30, 10, 10);
            graphicsContext.setFill(Color.DARKRED);
            graphicsContext.fillRect(x, y, width, height);
            graphicsContext.setFill(Color.FORESTGREEN);
            graphicsContext.fillRect(
                    x, y,
                    width * (enemy.getHealth() / enemy.getMaxHealth()), height
            );
            graphicsContext.setFill(Color.WHITE);

            String name = enemy instanceof ElementBoss elementBoss
                    ? elementBoss.getBossName()
                    : "TERRAK — SENHOR DA TERRA";
            graphicsContext.fillText(name, x + 12, y + 16);
            return;
        }
    }

    private void drawIslandDebug(Island island) {
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.fillText(island.getDisplayName(), 8, 20);
    }

    private void drawDebug(Player player, double deltaTime) {
        graphicsContext.setFill(Color.rgb(255, 255, 255, 0.85));
        graphicsContext.fillText(
                String.format("X=%.0f Y=%.0f | FPS=%.0f",
                        player.getX(), player.getY(),
                        deltaTime > 0 ? 1.0 / deltaTime : 0),
                8, 40
        );
    }

    private void drawVictoryOverlay() {
        graphicsContext.setFill(Color.rgb(0, 0, 0, 0.68));
        graphicsContext.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphicsContext.setFill(Color.LIMEGREEN);
        graphicsContext.setFont(Font.font("System", FontWeight.BOLD, 34));
        graphicsContext.fillText("CAMPANHA CONCLUÍDA!", 245, 275);
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.setFont(Font.font("System", 18));
        graphicsContext.fillText(
                "Os três Senhores Elementais foram derrotados.",
                230, 312
        );
    }

    private void drawGameOverOverlay() {
        graphicsContext.setFill(Color.rgb(0, 0, 0, 0.72));
        graphicsContext.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphicsContext.setFill(Color.CRIMSON);
        graphicsContext.setFont(Font.font("System", FontWeight.BOLD, 36));
        graphicsContext.fillText("VOCÊ FOI DERROTADO", 220, 275);
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.setFont(Font.font("System", 18));
        graphicsContext.fillText("Pressione R para reiniciar.", 300, 312);
    }

}

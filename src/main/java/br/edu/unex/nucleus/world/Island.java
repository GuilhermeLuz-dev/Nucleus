package br.edu.unex.nucleus.world;

import br.edu.unex.nucleus.entity.Enemy;
import br.edu.unex.nucleus.entity.HealthPotion;
import br.edu.unex.nucleus.entity.Portal;
import br.edu.unex.nucleus.entity.Projectile;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class Island {

    private final String id;
    private final String displayName;

    private final List<Enemy> enemies;
    private final List<Portal> portals;

    private final Color backgroundColor;

    private final Image backgroundImage;
    private final TiledMap tiledMap;

    private final List<Projectile> projectiles;
    private final List<HealthPotion> healthPotions;
    private final List<DamageZone> damageZones;
    private final CollisionMap collisionMap;
    private Enemy fallbackBoss;

    // Dimensões do mundo em pixels. Mapas maiores que a janela usam câmera.
    private final double worldWidth;
    private final double worldHeight;

    public Island(String id, String displayName, List<Enemy> enemies, List<Portal> portals, Color backgroundColor) {
        this(id, displayName, enemies, portals, backgroundColor, null, null);
    }

    public Island(String id, String displayName, List<Enemy> enemies, List<Portal> portals,
                  Color backgroundColor, String backgroundImagePath) {
        this(id, displayName, enemies, portals, backgroundColor, backgroundImagePath, null);
    }

    public Island(String id, String displayName, List<Enemy> enemies, List<Portal> portals,
                  Color backgroundColor, String backgroundImagePath, String tiledMapPath) {

        this.id = id;
        this.displayName = displayName;
        this.enemies = enemies;
        this.portals = portals;
        this.backgroundColor = backgroundColor;
        this.tiledMap = tiledMapPath == null ? null : new TiledMap(tiledMapPath);
        this.backgroundImage = tiledMap != null ? null : loadBackgroundImage(backgroundImagePath);

        this.worldWidth = tiledMap != null ? tiledMap.getPixelWidth() : (backgroundImage != null ? backgroundImage.getWidth() : 800);
        this.worldHeight = tiledMap != null ? tiledMap.getPixelHeight() : (backgroundImage != null ? backgroundImage.getHeight() : 600);

        this.projectiles = new ArrayList<>();
        this.healthPotions = new ArrayList<>();
        this.damageZones = new ArrayList<>();
        this.collisionMap = new CollisionMap(id, tiledMap, tiledMapPath);
    }

    private Image loadBackgroundImage(String resourcePath) {

        if (resourcePath == null) {
            return null;
        }

        var stream = getClass().getResourceAsStream(resourcePath);

        if (stream == null) {
            // Sem imagem encontrada: o Renderer cai de volta pra cor sólida.
            return null;
        }

        return new Image(stream);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<Portal> getPortals() {
        return portals;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Image getBackgroundImage() {
        return backgroundImage;
    }

    public TiledMap getTiledMap() {
        return tiledMap;
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    public List<HealthPotion> getHealthPotions() {
        return healthPotions;
    }

    public List<DamageZone> getDamageZones() {
        return damageZones;
    }

    public double getWorldWidth() {
        return worldWidth;
    }

    public double getWorldHeight() {
        return worldHeight;
    }

    public CollisionMap getCollisionMap() {
        return collisionMap;
    }

    public void setFallbackBoss(Enemy boss) {
        this.fallbackBoss = boss;
    }

    public Enemy getFallbackBoss() {
        return fallbackBoss;
    }

    public void resetEnemies() {
        for (Enemy enemy : enemies) {
            enemy.reset();
        }
    }
}

package br.edu.unex.nucleus.world;

import br.edu.unex.nucleus.entity.Player;
import br.edu.unex.nucleus.entity.HealthPotion;
import br.edu.unex.nucleus.entity.Portal;
import br.edu.unex.nucleus.entity.Projectile;
import br.edu.unex.nucleus.entity.Enemy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WorldManager {

    private final Map<String, Island> islands;

    private String currentIslandId;
    private boolean terrakDefeated;
    private boolean nerionDefeated;
    private boolean ignarDefeated;
    private boolean transitionedAfterTerrak;
    private boolean transitionedAfterNerion;

    public WorldManager() {
        this.islands = new HashMap<>();
    }

    public void addIsland(Island island) {
        islands.put(island.getId(), island);
    }

    public void setInitialIsland(String islandId) {
        this.currentIslandId = islandId;
    }

    public Island getCurrentIsland() {
        return islands.get(currentIslandId);
    }

    public Island getIsland(String islandId) {
        return islands.get(islandId);
    }

    public void update(Player player, double deltaTime, boolean playerIsMakingNoise) {

        Island island = getCurrentIsland();

        for (var enemy : island.getEnemies()) {
            if (enemy.isDead()) {
                continue;
            }

            double oldX = enemy.getX();
            double oldY = enemy.getY();

            List<Projectile> spawnedProjectiles = enemy.update(player, deltaTime, playerIsMakingNoise);

            if (!island.getCollisionMap().canMoveTo(
                    enemy.getX() - enemy.getWidth() / 2,
                    enemy.getY() - enemy.getHeight() / 2,
                    enemy.getWidth(), enemy.getHeight())) {
                enemy.setPosition(oldX, oldY);
            }

            island.getProjectiles().addAll(spawnedProjectiles);

            if (enemy.isCollidingWith(player)) {
                player.takeDamage(enemy.getContactDamage() * deltaTime);
            }
        }

        updateProjectiles(island, player, deltaTime);
        collectHealthPotions(island, player);

        checkPortals(player);
    }

    private void collectHealthPotions(Island island, Player player) {
        for (HealthPotion potion : island.getHealthPotions()) {
            if (!potion.isCollected() && potion.isCollidingWith(player)) {
                potion.collect();
                player.addHealthPotion();
            }
        }
    }

    private void updateProjectiles(Island island, Player player, double deltaTime) {

        Iterator<Projectile> iterator = island.getProjectiles().iterator();

        while (iterator.hasNext()) {

            Projectile projectile = iterator.next();

            projectile.update(deltaTime);

            if (projectile.isCollidingWith(player)) {
                player.takeDamage(projectile.getDamage());
                iterator.remove();
                continue;
            }

            if (projectile.isExpired()) {
                iterator.remove();
            }
        }
    }

    public void playerAttack(Player player) {
        if (!player.canAttack()) {
            return;
        }

        Island island = getCurrentIsland();
        player.startAttackAnimation();
        boolean hit = false;

        for (Enemy enemy : island.getEnemies()) {
            if (enemy.isDead()) {
                continue;
            }

            double dx = (enemy.getX()) - (player.getX() + player.getWidth() / 2);
            double dy = (enemy.getY()) - (player.getY() + player.getHeight() / 2);
            double distance = Math.sqrt(dx * dx + dy * dy);
            double attackRange = enemy.isBoss() ? 105 : 65;

            if (distance <= attackRange) {
                enemy.takeDamage(enemy.isBoss() ? 75 : 45);
                hit = true;

                if (enemy.isBoss() && enemy.isDead()) {
                    handleBossDefeat(island.getId(), player);
                }
            }
        }

        if (hit) {
            player.startAttackCooldown();
        }
    }

    private void handleBossDefeat(String islandId, Player player) {
        switch (islandId) {
            case "terrak" -> {
                terrakDefeated = true;
                transitionAfterTerrak(player);
            }
            case "nerion" -> {
                nerionDefeated = true;
                transitionAfterNerion(player);
            }
            case "ignar" -> {
                ignarDefeated = true;
            }
        }
    }

    private void transitionAfterTerrak(Player player) {
        if (!islands.containsKey("nerion")) return;
        currentIslandId = "nerion";
        setPlayerSpawnFromMap(player, getCurrentIsland());
        player.heal(player.getMaxHealth() * 0.60);
        player.addHealthPotion();
    }

    private void transitionAfterNerion(Player player) {
        if (!islands.containsKey("ignar")) return;
        currentIslandId = "ignar";
        setPlayerSpawnFromMap(player, getCurrentIsland());
        player.heal(player.getMaxHealth() * 0.60);
        player.addHealthPotion();
    }

    public boolean isTerrakDefeated() { return terrakDefeated; }
    public boolean isNerionDefeated() { return nerionDefeated; }
    public boolean isIgnarDefeated() { return ignarDefeated; }

    public boolean isCampaignComplete() {
        return ignarDefeated;
    }

    private void setPlayerSpawnFromMap(Player player, Island island) {
        if (island == null || island.getTiledMap() == null) return;
        java.util.List<TiledMap.MapObject> spawns =
                island.getTiledMap().getObjects("Player");
        if (!spawns.isEmpty()) {
            TiledMap.MapObject spawn = spawns.get(0);
            player.setPosition(spawn.x(), spawn.y());
        }
    }

    private void checkPortals(Player player) {

        Island island = getCurrentIsland();

        for (Portal portal : island.getPortals()) {

            if (portal.isPlayerColliding(player)) {
                travelThrough(portal, player);
                break;
            }
        }
    }

    private void travelThrough(Portal portal, Player player) {

        currentIslandId = portal.getTargetIslandId();

        player.setPosition(portal.getTargetSpawnX(), portal.getTargetSpawnY());
    }
}

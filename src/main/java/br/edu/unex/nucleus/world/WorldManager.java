package br.edu.unex.nucleus.world;

import br.edu.unex.nucleus.ai.EnemyPower;
import br.edu.unex.nucleus.ai.HomingOrbPower;
import br.edu.unex.nucleus.ai.SingleBoltPower;
import br.edu.unex.nucleus.ai.TripleBoltPower;
import br.edu.unex.nucleus.entity.Enemy;
import br.edu.unex.nucleus.entity.HealthPotion;
import br.edu.unex.nucleus.entity.Player;
import br.edu.unex.nucleus.entity.Portal;
import br.edu.unex.nucleus.entity.Projectile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Responsável pelo estado do mundo e da campanha.
 *
 * Não conhece JavaFX nem teclado. Ele atualiza entidades, combate, colisões
 * de inimigos/projéteis, coleta de poções e troca de mapas.
 */
public class WorldManager {

    private final Map<String, Island> islands = new HashMap<>();
    private String currentIslandId;

    private boolean terrakDefeated;
    private boolean nerionDefeated;
    private boolean ignarDefeated;

    public void addIsland(Island island) {
        islands.put(island.getId(), island);
    }

    public void setInitialIsland(String islandId) {
        if (!islands.containsKey(islandId)) {
            throw new IllegalArgumentException("Mapa inicial inexistente: " + islandId);
        }
        currentIslandId = islandId;
    }

    public Island getCurrentIsland() {
        return islands.get(currentIslandId);
    }

    public Island getIsland(String islandId) {
        return islands.get(islandId);
    }

    /**
     * Lê do Tiled as camadas Monstros, Poções, Player e Portais.
     * Boss pode ser colocado em uma camada "Boss"; se ela não existir,
     * usa a posição definida no WorldBuilder.
     */
    public void initializeEntitiesFromMaps() {
        for (Island island : islands.values()) {
            TiledMap map = island.getTiledMap();
            if (map == null) continue;

            createMonsters(island, map);
            createPotions(island, map);
            createPortals(island, map);
            createDamageZones(island, map);
            createBoss(island, map);
        }
    }

    private void createMonsters(Island island, TiledMap map) {
        EnemyPower[] powers = {
                new SingleBoltPower(),
                new HomingOrbPower(),
                new TripleBoltPower()
        };

        int index = 0;
        for (TiledMap.MapObject object : map.getObjects("Monstros")) {
            double x = object.centerX();
            double y = object.centerY();
            EnemyPower power = powerFromObject(object, powers[index % powers.length]);
            int slimeVariant = slimeVariantForIsland(island.getId());
            Enemy enemy = new Enemy(x, y, power, slimeVariant);

            double patrolRadius = parseDouble(firstNonBlank(
                    object.property("patrolRadius"),
                    object.property("raioPatrulha"),
                    object.property("raio_patrol")), 90);
            double patrolSpeed = parseDouble(firstNonBlank(
                    object.property("patrolSpeed"),
                    object.property("velocidadePatrulha"),
                    object.property("velocidade_patrol")), 65);
            enemy.configurePatrol(patrolRadius, patrolSpeed);

            island.getEnemies().add(enemy);
            index++;
        }
    }

    private int slimeVariantForIsland(String islandId) {
        if (islandId == null) return 1;
        return switch (islandId.trim().toLowerCase()) {
            case "nerion" -> 2;
            case "ignar" -> 3;
            default -> 1; // Terrak e mapas novos usam Slime1 por padrão.
        };
    }

    private EnemyPower powerFromObject(TiledMap.MapObject object, EnemyPower fallback) {
        String value = object.property("power");
        if (value == null) value = object.property("poder");
        if (value == null) return fallback;

        return switch (value.trim().toLowerCase()) {
            case "single", "singlebolt", "bolt" -> new SingleBoltPower();
            case "triple", "triplebolt" -> new TripleBoltPower();
            case "homing", "homingorb", "orb" -> new HomingOrbPower();
            default -> fallback;
        };
    }

    private void createPotions(Island island, TiledMap map) {
        for (TiledMap.MapObject object : map.getObjects("Poções")) {
            double amount = 100;
            String property = object.property("heal");
            if (property == null) property = object.property("cura");
            if (property != null) {
                try { amount = Double.parseDouble(property); } catch (NumberFormatException ignored) {}
            }
            island.getHealthPotions().add(
                    new HealthPotion(object.centerX(), object.centerY(), amount)
            );
        }
    }

    private void createDamageZones(Island island, TiledMap map) {
        // O dano padrão pode ser alterado aqui ou pela propriedade
        // "damagePerSecond" / "danoPorSegundo" de cada objeto no Tiled.
        final double defaultDamagePerSecond = 25.0;

        for (TiledMap.MapObject object : map.getObjects("Fogo")) {
            double damage = parseDouble(firstNonBlank(
                    object.property("damagePerSecond"),
                    object.property("danoPorSegundo"),
                    object.property("dano")
            ), defaultDamagePerSecond);

            island.getDamageZones().add(new DamageZone(
                    object.x(), object.y(),
                    object.width(), object.height(),
                    damage
            ));
        }
    }

    private void createBoss(Island island, TiledMap map) {
        if (island.getEnemies().stream().anyMatch(Enemy::isBoss)) return;

        List<TiledMap.MapObject> bosses = map.getObjects("Boss");
        Enemy boss = island.getFallbackBoss();
        if (!bosses.isEmpty() && boss != null) {
            TiledMap.MapObject object = bosses.get(0);
            boss.setPosition(object.centerX(), object.centerY());
            boss.setHomePosition(object.centerX(), object.centerY());
        }
        if (boss != null) island.getEnemies().add(boss);
    }

    private void createPortals(Island island, TiledMap map) {
        for (TiledMap.MapObject object : map.getObjects("Portais")) {
            String target = firstNonBlank(
                    object.property("targetIsland"),
                    object.property("mapaDestino"),
                    object.property("destino")
            );
            if (target == null) continue;

            double spawnX = parseDouble(firstNonBlank(object.property("targetSpawnX"), object.property("spawnX")), 0);
            double spawnY = parseDouble(firstNonBlank(object.property("targetSpawnY"), object.property("spawnY")), 0);

            island.getPortals().add(new Portal(
                    object.x(), object.y(),
                    Math.max(40, object.width()),
                    Math.max(40, object.height()),
                    target, spawnX, spawnY
            ));
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private double parseDouble(String value, double fallback) {
        if (value == null) return fallback;
        try { return Double.parseDouble(value); } catch (NumberFormatException e) { return fallback; }
    }

    /** Atualiza IA, inimigos, projéteis, poções e portais. */
    public void update(Player player, double deltaTime, boolean playerIsMakingNoise) {
        Island island = getCurrentIsland();
        if (island == null || player.isDead()) return;

        updateEnemies(island, player, deltaTime, playerIsMakingNoise);
        updateProjectiles(island, player, deltaTime);
        collectHealthPotions(island, player);
        applyDamageZones(island, player, deltaTime);
        checkPortals(player);
    }

    private void updateEnemies(Island island, Player player, double deltaTime, boolean playerIsMakingNoise) {
        for (Enemy enemy : island.getEnemies()) {
            if (enemy.isDead()) continue;

            double oldX = enemy.getX();
            double oldY = enemy.getY();

            List<Projectile> spawned = enemy.update(
                    player, deltaTime, playerIsMakingNoise, island.getCollisionMap());

            // Bosses mantêm a validação antiga com o corpo inteiro.
            // Enemies comuns já validam cada passo pela hitbox dos pés dentro
            // de Enemy.moveTowards()/A*, evitando que um sprite 80x94 seja
            // devolvido à posição anterior só por encostar visualmente na parede.
            if (enemy.isBoss() && !island.getCollisionMap().canMoveTo(
                    enemy.getX() - enemy.getWidth() / 2.0,
                    enemy.getY() - enemy.getHeight() / 2.0,
                    enemy.getWidth(), enemy.getHeight())) {
                enemy.setPosition(oldX, oldY);
            }

            island.getProjectiles().addAll(spawned);

            if (enemy.isCollidingWith(player)) {
                player.takeDamage(enemy.getContactDamage() * deltaTime);
            }
        }
    }

    private void updateProjectiles(Island island, Player player, double deltaTime) {
        Iterator<Projectile> iterator = island.getProjectiles().iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.update(deltaTime);

            if (!island.getCollisionMap().canMoveTo(
                    projectile.getX(), projectile.getY(),
                    projectile.getWidth(), projectile.getHeight())) {
                iterator.remove();
                continue;
            }

            if (projectile.isCollidingWith(player)) {
                player.takeDamage(projectile.getDamage());
                iterator.remove();
                continue;
            }

            if (projectile.isExpired()) iterator.remove();
        }
    }

    private void applyDamageZones(Island island, Player player, double deltaTime) {
        for (DamageZone zone : island.getDamageZones()) {
            if (zone.touches(player)) {
                player.takeDamage(zone.getDamagePerSecond() * deltaTime);
                // Uma área por vez é suficiente; evita dano duplicado na borda
                // entre dois retângulos adjacentes da mesma lava.
                break;
            }
        }
    }

    private void collectHealthPotions(Island island, Player player) {
        for (HealthPotion potion : island.getHealthPotions()) {
            if (!potion.isCollected() && potion.isCollidingWith(player)) {
                potion.collect();
                player.addHealthPotion(potion.getHealAmount());
            }
        }
    }

    public void playerAttack(Player player) {
        if (!player.canAttack()) return;

        Island island = getCurrentIsland();
        player.startAttackAnimation();

        boolean hit = false;
        for (Enemy enemy : island.getEnemies()) {
            if (enemy.isDead()) continue;

            double dx = enemy.getX() - (player.getX() + player.getWidth() / 2.0);
            double dy = enemy.getY() - (player.getY() + player.getHeight() / 2.0);
            double distance = Math.hypot(dx, dy);
            double attackRange = enemy.isBoss() ? 105 : 65;

            if (distance <= attackRange) {
                enemy.takeDamage(enemy.isBoss() ? 75 : 45);
                hit = true;

                if (enemy.isBoss() && enemy.isDead()) {
                    handleBossDefeat(island.getId(), player);
                }
            }
        }

        if (hit) player.startAttackCooldown();
    }

    private void handleBossDefeat(String islandId, Player player) {
        switch (islandId) {
            case "terrak" -> {
                terrakDefeated = true;
                transitionTo("nerion", player);
            }
            case "nerion" -> {
                nerionDefeated = true;
                transitionTo("ignar", player);
            }
            case "ignar" -> ignarDefeated = true;
            default -> { }
        }
    }

    private void transitionTo(String islandId, Player player) {
        if (!islands.containsKey(islandId)) return;
        currentIslandId = islandId;
        placePlayerAtMapSpawn(player, getCurrentIsland());
        player.heal(player.getMaxHealth() * 0.60);
        player.addHealthPotion(100);
    }

    public void initializePlayer(Player player) {
        placePlayerAtMapSpawn(player, getCurrentIsland());
    }

    private void placePlayerAtMapSpawn(Player player, Island island) {
        if (island == null || island.getTiledMap() == null) return;
        List<TiledMap.MapObject> spawns = island.getTiledMap().getObjects("Player");
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
                return;
            }
        }
    }

    private void travelThrough(Portal portal, Player player) {
        if (!islands.containsKey(portal.getTargetIslandId())) return;
        currentIslandId = portal.getTargetIslandId();
        player.setPosition(portal.getTargetSpawnX(), portal.getTargetSpawnY());
    }

    public void resetCampaign() {
        currentIslandId = "terrak";
        terrakDefeated = false;
        nerionDefeated = false;
        ignarDefeated = false;

        for (Island island : islands.values()) {
            island.getProjectiles().clear();
            island.getHealthPotions().forEach(potion -> potion.reset());
            island.resetEnemies();
        }
    }

    public boolean isTerrakDefeated() { return terrakDefeated; }
    public boolean isNerionDefeated() { return nerionDefeated; }
    public boolean isIgnarDefeated() { return ignarDefeated; }
    public boolean isCampaignComplete() { return ignarDefeated; }

    public Map<String, Island> getIslands() { return Map.copyOf(islands); }
}

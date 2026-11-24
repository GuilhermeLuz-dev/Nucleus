package br.edu.unex.nucleus.world;

import br.edu.unex.nucleus.ai.EnemyPower;
import br.edu.unex.nucleus.ai.HomingOrbPower;
import br.edu.unex.nucleus.ai.SingleBoltPower;
import br.edu.unex.nucleus.ai.TripleBoltPower;
import br.edu.unex.nucleus.entity.Enemy;
import br.edu.unex.nucleus.entity.HealthPotion;
import br.edu.unex.nucleus.entity.FinalBrother;
import br.edu.unex.nucleus.entity.Player;
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

    private boolean finalSceneCompleted;
    private boolean finalDialogueActive;
    private boolean finalBrothersFreed;
    private boolean finalSealSequenceActive;
    private double finalSealSequenceTimer;
    private int finalDialogueIndex = -1;

    private static final String[] FINAL_DIALOGUE = {
            "Irmão: Você conseguiu... chegou até nós.",
            "Ficamos presos nesta ilha enquanto os Senhores Elementais guardavam o caminho.",
            "Irmão: As prisões se romperam. Estamos livres!",
            "Você veio nos salvar. Agora podemos voltar para casa juntos."
    };

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
     * Lê do Tiled as camadas Monstros, Poções e Player.
     * Boss pode ser colocado em uma camada "Boss"; se ela não existir,
     * usa a posição definida no WorldBuilder.
     */
    public void initializeEntitiesFromMaps() {
        for (Island island : islands.values()) {
            TiledMap map = island.getTiledMap();
            if (map == null) continue;

            createMonsters(island, map);
            createPotions(island, map);
            createDamageZones(island, map);
            createFinalBrothers(island, map);
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

    private void createFinalBrothers(Island island, TiledMap map) {
        if (!"final".equalsIgnoreCase(island.getId())) return;
        if (!island.getFinalBrothers().isEmpty()) return;

        List<TiledMap.MapObject> objects = map.getObjects("Irmãos");
        String[] sprites = {
                "/sprites/brothers/brother1.png",
                "/sprites/brothers/brother2.png",
                "/sprites/brothers/brother3.png"
        };

        for (int i = 0; i < Math.min(3, objects.size()); i++) {
            TiledMap.MapObject object = objects.get(i);
            island.getFinalBrothers().add(new FinalBrother(
                    "Irmão " + (i + 1),
                    object.x(),
                    object.y(),
                    sprites[i]
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

    /** Atualiza IA, inimigos, projéteis e poções. */
    public void update(Player player, double deltaTime, boolean playerIsMakingNoise) {
        Island island = getCurrentIsland();
        if (island == null || player.isDead()) return;

        updateEnemies(island, player, deltaTime, playerIsMakingNoise);
        updateProjectiles(island, player, deltaTime);
        collectHealthPotions(island, player);
        applyDamageZones(island, player, deltaTime);
        updateFinalEvent(island, player, deltaTime);
    }

    private void updateEnemies(Island island, Player player, double deltaTime, boolean playerIsMakingNoise) {
        for (Enemy enemy : island.getEnemies()) {
            if (enemy.isDead()) continue;

            double oldX = enemy.getX();
            double oldY = enemy.getY();

            List<Projectile> spawned = enemy.update(
                    player, deltaTime, playerIsMakingNoise, island.getCollisionMap());

            // O próprio Enemy valida cada passo com a mesma hitbox do jogador.
            // Não reverte a posição por colisão do sprite visual inteiro.

            island.getProjectiles().addAll(spawned);

            if (enemy instanceof ElementBoss elemental && elemental.isSpecialActive()) {
                double px = player.getX() + player.getWidth() / 2;
                double py = player.getY() + player.getHeight() / 2;
                double distance = Math.hypot(px - elemental.getSpecialX(), py - elemental.getSpecialY());
                if (distance <= elemental.getSpecialRadius() && elemental.isFireSpecial()
                        && elemental.consumeSpecialHit()) {
                    player.takeDamage(40);
                }
            }

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

    private void updateFinalEvent(Island island, Player player, double deltaTime) {
        if (!"final".equalsIgnoreCase(island.getId()) || finalSceneCompleted) {
            return;
        }

        // Sequência dos três selos. Durante esse período os irmãos NÃO falam.
        if (finalSealSequenceActive) {
            finalSealSequenceTimer -= deltaTime;

            // ~3 segundos: Terra -> Água -> Fogo -> prisões quebram.
            if (finalSealSequenceTimer <= 0) {
                finalSealSequenceActive = false;
                finalBrothersFreed = true;
                finalDialogueActive = true;
                finalDialogueIndex = 0;
            }
            return;
        }

        if (finalDialogueActive || finalBrothersFreed) {
            return;
        }

        List<TiledMap.MapObject> events = island.getTiledMap().getObjects("EventoFinal");
        if (events.isEmpty()) return;

        TiledMap.MapObject event = events.get(0);

        double playerLeft = player.getX();
        double playerTop = player.getY();
        double playerRight = playerLeft + player.getWidth();
        double playerBottom = playerTop + player.getHeight();

        double eventRight = event.x() + event.width();
        double eventBottom = event.y() + event.height();

        boolean intersects = playerRight > event.x()
                && playerLeft < eventRight
                && playerBottom > event.y()
                && playerTop < eventBottom;

        if (intersects) {
            finalSealSequenceActive = true;
            finalSealSequenceTimer = 3.2;
        }
    }

    public void advanceFinalDialogue() {
        if (!finalDialogueActive) return;

        finalDialogueIndex++;

        if (finalDialogueIndex >= FINAL_DIALOGUE.length) {
            finalDialogueActive = false;
            finalSceneCompleted = true;
            finalDialogueIndex = -1;
        }
    }

    public boolean isFinalDialogueActive() {
        return finalDialogueActive;
    }

    public boolean areFinalBrothersFreed() {
        return finalBrothersFreed;
    }

    public String getFinalDialogueText() {
        if (!finalDialogueActive
                || finalDialogueIndex < 0
                || finalDialogueIndex >= FINAL_DIALOGUE.length) {
            return null;
        }
        return FINAL_DIALOGUE[finalDialogueIndex];
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
                enemy.takeDamage(enemy.isBoss() ? 150 : 90);
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
            case "ignar" -> {
                ignarDefeated = true;
                transitionTo("final", player);
            }
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



    public void resetCampaign() {
        currentIslandId = "terrak";
        terrakDefeated = false;
        nerionDefeated = false;
        ignarDefeated = false;
        finalSceneCompleted = false;
        finalDialogueActive = false;
        finalBrothersFreed = false;
        finalDialogueIndex = -1;
        finalSealSequenceActive = false;
        finalSealSequenceTimer = 0;

        for (Island island : islands.values()) {
            island.getProjectiles().clear();
            island.getHealthPotions().forEach(potion -> potion.reset());
            island.resetEnemies();
        }
    }

    public boolean isTerrakDefeated() { return terrakDefeated; }
    public boolean isNerionDefeated() { return nerionDefeated; }
    public boolean isIgnarDefeated() { return ignarDefeated; }
    public boolean isCampaignComplete() { return finalSceneCompleted; }

    public Map<String, Island> getIslands() { return Map.copyOf(islands); }
}

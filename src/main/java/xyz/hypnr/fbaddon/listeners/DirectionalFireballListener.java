package xyz.hypnr.fbaddon.listeners;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.lang.reflect.Method;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DirectionalFireballListener implements Listener {

    private JavaPlugin plugin;
    private double fireballExplosionSize;
    private boolean fireballMakeFire;
    private double selfHor;
    private double selfVer;
    private double sideHor;
    private double sideVer;
    private double minHorizontal;
    private boolean selfJumpEnabled;
    private double selfJumpMaxDistance;
    private double pitchThreshold;
    private double frontCosThreshold;
    private double yUnderThreshold;
    private double sideFrontCosThreshold;
    private double selfZeroPitchThreshold;
    private double selfZeroMaxDistance;
    private double damageSelf;
    private double damageEnemy;
    private double damageTeammates;
    private boolean throwEnabled;
    private boolean throwConsumeItem;
    private double throwSpeed;
    private long throwCooldownTicks;
    private double selfZeroMovementThreshold;
    private double sideSemiPitchMin;
    private double sideSemiPitchMax;
    private double sideSemiHor;
    private double sideSemiVer;
    private double sideSelfPitchMax;
    private double sideSelfFrontCosThreshold;
    private double sideSelfHor;
    private double sideSelfVer;
    private final Set<Location> placedFires = new HashSet<>();
    private final Map<UUID, Long> throwCooldowns = new HashMap<>();

    public DirectionalFireballListener(JavaPlugin plugin) {
        this.plugin = plugin;
        refreshFromConfig();
    }

    public DirectionalFireballListener() {
        // plugin will be attached later
    }

    public void attach(JavaPlugin plugin) {
        this.plugin = plugin;
        refreshFromConfig();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void handleFireballThrow(PlayerInteractEvent e) {
        if (!featureEnabled() || !throwEnabled) return;
        Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.FIREBALL) return;

        Player player = e.getPlayer();

        if (throwCooldownTicks > 0) {
            long now = System.currentTimeMillis();
            long cooldownMs = throwCooldownTicks * 50L;
            Long last = throwCooldowns.get(player.getUniqueId());
            if (last != null && now - last < cooldownMs) {
                e.setCancelled(true);
                return;
            }
            throwCooldowns.put(player.getUniqueId(), now);
        }

        e.setCancelled(true);

        Vector dir = player.getLocation().getDirection();
        if (dir.lengthSquared() == 0) {
            dir = new Vector(0, 0, 1);
        }
        dir = dir.normalize().multiply(throwSpeed);

        Location spawnLoc = player.getEyeLocation().add(dir.clone().multiply(0.2));
        Fireball fireball = player.getWorld().spawn(spawnLoc, Fireball.class);
        fireball.setShooter(player);
        fireball.setVelocity(dir);
        fireball.setIsIncendiary(fireballMakeFire);
        fireball.setYield((float) fireballExplosionSize);

        if (throwConsumeItem && player.getGameMode() != GameMode.CREATIVE) {
            ItemStack inHand = player.getItemInHand();
            if (inHand != null && inHand.getType() == Material.FIREBALL) {
                int amount = inHand.getAmount();
                if (amount <= 1) {
                    player.setItemInHand(null);
                } else {
                    inHand.setAmount(amount - 1);
                    player.setItemInHand(inHand);
                }
                player.updateInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void fireballHit(ProjectileHitEvent e) {
        if (!featureEnabled()) return;
        if (!(e.getEntity() instanceof Fireball)) return;
        Location hitLocation = e.getEntity().getLocation();

        ProjectileSource projectileSource = e.getEntity().getShooter();
        if (!(projectileSource instanceof Player)) return;
        Player source = (Player) projectileSource;

        BedWars api = api();
        IArena arena = (api != null) ? api.getArenaUtil().getArenaByPlayer(source) : null;

        World world = hitLocation.getWorld();
        if (world == null) return;

        // Place fires once per explosion if in arena
        if (api != null && arena != null && fireballMakeFire) {
            placeTemporaryFires(hitLocation);
        }

        Collection<Entity> nearbyEntities = world.getNearbyEntities(hitLocation, fireballExplosionSize, fireballExplosionSize, fireballExplosionSize);

        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof Player)) continue;
            Player player = (Player) entity;

            IArena playerArena = (api != null) ? api.getArenaUtil().getArenaByPlayer(player) : null;
            if (api != null && arena != null) {
                if (playerArena != null && !arena.equals(playerArena)) {
                    continue;
                }
                if (playerArena == null) {
                    World arenaWorld = arena.getWorld();
                    if (arenaWorld != null && !arenaWorld.equals(player.getWorld())) {
                        continue;
                    }
                }
            }

            final Player finalPlayer = player;
            final Location snapshotLocation = player.getLocation().clone();
            final Vector snapshotDirection = snapshotLocation.getDirection().clone();
            final Vector snapshotVelocity = player.getVelocity().clone();
            final double snapshotHorSpeed = snapshotVelocity.clone().setY(0).length();
            final float snapshotPitch = snapshotLocation.getPitch();
            final double snapshotAbsPitch = Math.abs(snapshotPitch);
            final Vector explosionVector = hitLocation.toVector().subtract(snapshotLocation.toVector());

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!featureEnabled()) return;
                    Location playerLocation = snapshotLocation;
                    Vector diff = playerLocation.toVector().subtract(hitLocation.toVector());

                    Vector horizontal = new Vector(diff.getX(), 0, diff.getZ());
                    if (horizontal.lengthSquared() < 1.0E-6) {
                        Vector back = snapshotDirection.clone().setY(0).multiply(-1);
                        horizontal = back.lengthSquared() > 0 ? back : new Vector(0, 0, 0.001);
                    }
                    Vector hUnit = horizontal.lengthSquared() > 0 ? horizontal.clone().normalize() : new Vector(0, 0, 0);

                    double distance = diff.length();
                    boolean isSelf = finalPlayer.getUniqueId().equals(source.getUniqueId());

                    Vector fwdDir = snapshotDirection.clone().setY(0);
                    if (fwdDir.lengthSquared() > 0) {
                        fwdDir.normalize();
                    }
                    Vector toExp = explosionVector.clone();
                    Vector toExpH = new Vector(toExp.getX(), 0, toExp.getZ());
                    double frontDot = (fwdDir.lengthSquared() > 0 && toExpH.lengthSquared() > 0) ? fwdDir.dot(toExpH.clone().normalize()) : 0.0;
                    double yDiff = playerLocation.getY() - hitLocation.getY();
                    float pitch = snapshotPitch;
                    double absPitch = snapshotAbsPitch;
                    double playerHorSpeed = snapshotHorSpeed;
                    Vector backUnit = (fwdDir.lengthSquared() > 0) ? fwdDir.clone().multiply(-1) : hUnit.clone();
                    if (backUnit.lengthSquared() > 0) {
                        backUnit.normalize();
                    } else {
                        backUnit = new Vector(hUnit.getX(), hUnit.getY(), hUnit.getZ());
                    }

                    Vector horizontalDir = hUnit.lengthSquared() > 0 ? hUnit.clone().normalize() : new Vector(1, 0, 0);
                    double horizontalMag = 0.0;
                    double verticalMag = 0.0;

                    if (isSelf && selfJumpEnabled) {
                        boolean withinSelfRange = distance <= selfJumpMaxDistance || selfJumpMaxDistance <= 0;
                        boolean movingHorizontally = playerHorSpeed > selfZeroMovementThreshold;

                        double pitchWeight = 0.0;
                        if (pitch >= (float) pitchThreshold && selfZeroPitchThreshold > pitchThreshold) {
                            pitchWeight = clamp((pitch - pitchThreshold) / Math.max(1.0, selfZeroPitchThreshold - pitchThreshold), 0.0, 1.0);
                        }
                        double yWeight = yDiff >= yUnderThreshold ? clamp((yDiff - yUnderThreshold) / Math.max(0.4, selfJumpMaxDistance - yUnderThreshold + 0.4), 0.0, 1.0) : 0.0;
                        double alignmentWeight = frontDot <= frontCosThreshold ? clamp((frontCosThreshold - frontDot) / (frontCosThreshold + 1.0), 0.0, 1.0) : 0.0;

                        double selfWeight = withinSelfRange ? clamp(Math.max(Math.max(pitchWeight, yWeight), alignmentWeight), 0.0, 1.0) : 0.0;
                        if (withinSelfRange && distance <= selfZeroMaxDistance + 0.2) {
                            selfWeight = Math.max(selfWeight, 0.55);
                        }

                        double semiWeight = 0.0;
                        double semiSpan = sideSemiPitchMax - sideSemiPitchMin;
                        if (semiSpan > 0 && absPitch >= sideSemiPitchMin && absPitch <= sideSemiPitchMax) {
                            double mid = (sideSemiPitchMin + sideSemiPitchMax) * 0.5;
                            double half = Math.max(1.0, semiSpan * 0.5);
                            semiWeight = clamp(1.0 - Math.abs(absPitch - mid) / half, 0.0, 1.0) * (1.0 - selfWeight);
                        }

                        double sideWeight = clamp(1.0 - selfWeight - semiWeight, 0.0, 1.0);
                        double total = selfWeight + semiWeight + sideWeight;
                        if (total <= 1.0E-6) {
                            selfWeight = 0.0;
                            semiWeight = 0.0;
                            sideWeight = 1.0;
                            total = 1.0;
                        }
                        selfWeight /= total;
                        semiWeight /= total;
                        sideWeight /= total;

                        boolean underCone = Math.abs(frontDot) <= Math.max(0.35, frontCosThreshold + 0.1);
                        boolean highAbove = yDiff >= yUnderThreshold;
                        double lateralAlignment = clamp(1.0 - Math.abs(frontDot), 0.0, 1.0);

                        if (frontDot <= -0.15) {
                            double backwardEmphasis = clamp((-frontDot - 0.15) / 0.85, 0.0, 1.0);
                            double targetSelf = Math.max(selfWeight, 0.65 + backwardEmphasis * 0.25);
                            selfWeight = Math.max(selfWeight, targetSelf);
                            double reduceSide = Math.min(sideWeight, backwardEmphasis * 0.5);
                            sideWeight -= reduceSide;
                            double reduceSemi = Math.min(semiWeight, backwardEmphasis * 0.35);
                            semiWeight = clamp(semiWeight - reduceSemi, 0.0, 1.0);
                            double weightTotal = selfWeight + semiWeight + sideWeight;
                            if (weightTotal <= 1.0E-6) {
                                selfWeight = 1.0;
                                semiWeight = 0.0;
                                sideWeight = 0.0;
                            } else {
                                selfWeight /= weightTotal;
                                semiWeight /= weightTotal;
                                sideWeight /= weightTotal;
                            }
                        }

                        boolean stationaryDrop = !movingHorizontally && pitch >= (float) selfZeroPitchThreshold && distance <= selfZeroMaxDistance + 0.1 && lateralAlignment <= 0.35;
                        if (stationaryDrop) {
                            selfWeight = 1.0;
                            semiWeight = 0.0;
                            sideWeight = 0.0;
                        }

                        boolean zeroHorizontal = selfWeight >= 0.85 && !movingHorizontally && underCone && highAbove && pitch >= (float) selfZeroPitchThreshold && distance <= selfZeroMaxDistance;
                        if (stationaryDrop) {
                            zeroHorizontal = true;
                        }

                        double baseSelfHor = Math.max(selfHor, minHorizontal);
                        double baseSemiHor = Math.max(sideSemiHor, minHorizontal);
                        double baseSideHor = Math.max(sideHor, minHorizontal);
                        double baseSelfVer = selfVer;
                        double baseSemiVer = sideSemiVer;
                        double baseSideVer = sideVer;

                        double sideBoost = lateralAlignment * Math.max(0.0, 1.0 - sideWeight) * 0.6;
                        double semiReduction = Math.min(semiWeight, sideBoost * 0.5);
                        semiWeight = clamp(semiWeight - semiReduction, 0.0, 1.0);
                        sideWeight = clamp(sideWeight + sideBoost, 0.0, 1.0);
                        double weightSum = selfWeight + semiWeight + sideWeight;
                        if (weightSum <= 1.0E-6) {
                            selfWeight = 0.0;
                            semiWeight = 0.0;
                            sideWeight = 1.0;
                            weightSum = 1.0;
                        }
                        selfWeight /= weightSum;
                        semiWeight /= weightSum;
                        sideWeight /= weightSum;

                        double speedFactor = clamp(playerHorSpeed / 0.7, 0.0, 1.0);
                        double smoothFactor = 0.9 + 0.25 * speedFactor;

                        double baseHorizontal = selfWeight * baseSelfHor + semiWeight * baseSemiHor + sideWeight * baseSideHor;
                        horizontalMag = zeroHorizontal ? 0.0 : baseHorizontal * smoothFactor;
                        verticalMag = selfWeight * baseSelfVer + semiWeight * baseSemiVer + sideWeight * baseSideVer;

                        Vector strafe = hUnit.clone().crossProduct(new Vector(0, 1, 0));
                        if (strafe.lengthSquared() > 1.0E-6) {
                            strafe.normalize();
                            double strafeSign = Math.signum(strafe.dot(fwdDir));
                            if (strafeSign == 0.0) {
                                strafeSign = 1.0;
                            }
                            strafe.multiply(strafeSign);
                            double lateralBlend = clamp((semiWeight * 0.5 + sideWeight * 0.9) * lateralAlignment, 0.0, 0.85);
                            if (lateralBlend > 0.0) {
                                horizontalDir = horizontalDir.multiply(1.0 - lateralBlend).add(strafe.clone().multiply(lateralBlend));
                            }
                        }
                    } else {
                        horizontalDir = new Vector(1, 0, 0);
                    }

                    horizontalMag = Math.max(0.0, horizontalMag);
                    Vector horizontalVec = horizontalDir.clone().multiply(horizontalMag);
                    Vector knock = new Vector(horizontalVec.getX(), verticalMag, horizontalVec.getZ());

                    if (finalPlayer.isOnGround() && knock.getY() < 0.08) {
                        knock.setY(0.08);
                    }

                    final Vector knockFinal = knock.clone();
                    finalPlayer.setVelocity(knockFinal.clone());

                    for (long delay : new long[]{1L, 2L}) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (!featureEnabled()) return;
                                if (!finalPlayer.isOnline() || finalPlayer.isDead() || !finalPlayer.isValid()) return;
                                finalPlayer.setVelocity(knockFinal.clone());
                            }
                        }.runTaskLater(plugin, delay);
                    }
                }
            }.runTaskLater(plugin, 3L);

            if (player.equals(source)) {
                if (damageSelf > 0) {
                    player.damage(damageSelf);
                }
            } else if (api != null && arena != null) {
                ITeam sourceTeam = getTeamSafe(arena, source);
                ITeam targetTeam = getTeamSafe(arena, player);
                if (sourceTeam != null && targetTeam != null && sourceTeam.equals(targetTeam)) {
                    if (damageTeammates > 0) {
                        player.damage(damageTeammates);
                    }
                } else {
                    if (damageEnemy > 0) {
                        player.damage(damageEnemy);
                    }
                }
            } else {
                if (damageEnemy > 0) {
                    player.damage(damageEnemy);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void fireballDirectHit(EntityDamageByEntityEvent e) {
        if (!featureEnabled()) return;
        if (!(e.getDamager() instanceof Fireball)) return;
        if (!(e.getEntity() instanceof Player)) return;

        BedWars api = api();
        if (api == null) return;
        if (api.getArenaUtil().getArenaByPlayer((Player) e.getEntity()) == null) return;

        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void fireballPrime(ExplosionPrimeEvent e) {
        if (!featureEnabled()) return;
        if (!(e.getEntity() instanceof Fireball)) return;
        ProjectileSource shooter = ((Fireball) e.getEntity()).getShooter();
        if (!(shooter instanceof Player)) return;
        Player player = (Player) shooter;

        BedWars api = api();
        if (api != null && api.getArenaUtil().getArenaByPlayer(player) == null) return;

        e.setFire(fireballMakeFire);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent e) {
        if (e.getSource() != null && e.getSource().getType() == Material.FIRE) {
            if (isPlacedFire(e.getSource().getLocation())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent e) {
        if (e.getIgnitingBlock() != null && e.getIgnitingBlock().getType() == Material.FIRE) {
            if (isPlacedFire(e.getIgnitingBlock().getLocation())) {
                e.setCancelled(true);
            }
        }
    }

    private boolean isPlacedFire(Location loc) {
        if (loc == null) return false;
        for (Location l : placedFires) {
            if (l.getWorld() == loc.getWorld() && l.getBlockX() == loc.getBlockX() && l.getBlockY() == loc.getBlockY() && l.getBlockZ() == loc.getBlockZ()) {
                return true;
            }
        }
        return false;
    }

    private void placeTemporaryFires(Location hit) {
        if (hit == null || hit.getWorld() == null) return;
        Location base = hit.getBlock().getLocation();
        tryPlaceFireAbove(base);
        // Try one adjacent cardinal position
        tryPlaceFireAbove(base.clone().add(1, 0, 0));

        // Schedule removal after 10 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                clearPlacedFires();
            }
        }.runTaskLater(plugin, 200L);
    }

    private void tryPlaceFireAbove(Location blockLoc) {
        Block base = blockLoc.getBlock();
        Block above = base.getRelative(0, 1, 0);
        if (above.getType() != Material.AIR) return;
        if (!isFlammableBase(base.getType())) return;
        above.setType(Material.FIRE);
        placedFires.add(above.getLocation());
    }

    private boolean isFlammableBase(Material mat) {
        switch (mat) {
            case WOOL:
            case WOOD:
            case LOG:
            case LOG_2:
            case WOOD_STEP:
            case WOOD_STAIRS:
            case FENCE:
            case FENCE_GATE:
            case TRAP_DOOR:
            case LADDER:
                return true;
            default:
                return false;
        }
    }

    private void clearPlacedFires() {
        for (Location l : new HashSet<>(placedFires)) {
            Block b = l.getBlock();
            if (b.getType() == Material.FIRE) {
                b.setType(Material.AIR);
            }
            placedFires.remove(l);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        if (!featureEnabled()) return;
        if (e.getEntity() == null) return;
        BedWars api = api();
        if (api == null) return;
        Location loc = e.getEntity().getLocation();
        if (!isInActiveArenaWorld(loc)) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                removeCobwebsAround(loc, 3);
            }
        }.runTaskLater(plugin, 2L);
    }

    private void removeCobwebsAround(Location center, int radius) {
        if (center == null || center.getWorld() == null) return;
        World w = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = cy - radius; y <= cy + radius; y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    Block b = w.getBlockAt(x, y, z);
                    if (b.getType() == Material.WEB) {
                        b.setType(Material.AIR);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCobwebPlace(BlockPlaceEvent e) {
        if (!featureEnabled()) return;
        if (e.getBlockPlaced() == null) return;
        if (e.getBlockPlaced().getType() != Material.WEB) return;
        BedWars api = api();
        if (api == null) return;
        if (api.getArenaUtil().isPlaying(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCobwebDispense(BlockDispenseEvent e) {
        if (!featureEnabled()) return;
        if (e.getItem() == null || e.getItem().getType() != Material.WEB) return;
        BedWars api = api();
        if (api == null) return;
        Location loc = e.getBlock() != null ? new Location(e.getBlock().getWorld(), e.getBlock().getX(), e.getBlock().getY(), e.getBlock().getZ()) : null;
        if (isInActiveArenaWorld(loc)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlockToWeb(EntityChangeBlockEvent e) {
        if (!featureEnabled()) return;
        if (e.getTo() != Material.WEB) return;
        BedWars api = api();
        if (api == null) return;
        if (isInActiveArenaWorld(e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    private boolean isInActiveArenaWorld(Location loc) {
        BedWars api = api();
        if (api == null || loc == null || loc.getWorld() == null) return false;
        for (IArena a : api.getArenaUtil().getArenas()) {
            if (a != null && a.getWorld() != null && a.getWorld().equals(loc.getWorld())) {
                if (!a.getPlayers().isEmpty()) return true;
            }
        }
        return false;
    }

    private boolean featureEnabled() {
        try {
            Method m = plugin.getClass().getMethod("isFeatureEnabled");
            Object r = m.invoke(plugin);
            return r instanceof Boolean ? (Boolean) r : true;
        } catch (Throwable t) {
            return true;
        }
    }

    private BedWars api() {
        try {
            Method m = plugin.getClass().getMethod("getBedWarsAPI");
            Object r = m.invoke(plugin);
            return (BedWars) r;
        } catch (Throwable t) {
            return null;
        }
    }

    public void refreshFromConfig() {
        this.fireballExplosionSize = plugin.getConfig().getDouble("fireball.explosion-size", 3.0);
        this.fireballMakeFire = plugin.getConfig().getBoolean("fireball.make-fire", true);
        this.selfHor = plugin.getConfig().getDouble("fireball.self.horizontal", 1.2);
        this.selfVer = plugin.getConfig().getDouble("fireball.self.vertical", 1.2);
        this.sideHor = plugin.getConfig().getDouble("fireball.side.horizontal", 1.1);
        this.sideVer = plugin.getConfig().getDouble("fireball.side.vertical", 0.35);
        this.minHorizontal = plugin.getConfig().getDouble("fireball.min-horizontal", 0.15);
        this.selfJumpEnabled = plugin.getConfig().getBoolean("fireball.self.enabled", true);
        this.selfJumpMaxDistance = plugin.getConfig().getDouble("fireball.self.max-distance", 1.6);
        this.pitchThreshold = plugin.getConfig().getDouble("fireball.self.angle.pitch-threshold", 45.0);
        this.frontCosThreshold = plugin.getConfig().getDouble("fireball.self.angle.front-cos-threshold", 0.3);
        this.yUnderThreshold = plugin.getConfig().getDouble("fireball.self.y-under-threshold", 0.6);
        this.sideFrontCosThreshold = plugin.getConfig().getDouble("fireball.side.front-cos-threshold", 0.5);
        this.selfZeroPitchThreshold = plugin.getConfig().getDouble("fireball.self.zero-horizontal.pitch-threshold", 75.0);
        this.selfZeroMaxDistance = plugin.getConfig().getDouble("fireball.self.zero-horizontal.max-distance", 1.0);
        this.selfZeroMovementThreshold = plugin.getConfig().getDouble("fireball.self.zero-horizontal.movement-threshold", 0.05);
        this.sideSemiPitchMin = plugin.getConfig().getDouble("fireball.side.semi.pitch-min", 20.0);
        this.sideSemiPitchMax = plugin.getConfig().getDouble("fireball.side.semi.pitch-max", 40.0);
        this.sideSemiHor = plugin.getConfig().getDouble("fireball.side.semi.horizontal", 1.1);
        this.sideSemiVer = plugin.getConfig().getDouble("fireball.side.semi.vertical", 0.5);
        this.sideSelfPitchMax = plugin.getConfig().getDouble("fireball.side.self.pitch-max", 15.0);
        this.sideSelfFrontCosThreshold = plugin.getConfig().getDouble("fireball.side.self.front-cos-threshold", 0.6);
        this.sideSelfHor = plugin.getConfig().getDouble("fireball.side.self.horizontal", 1.1);
        this.sideSelfVer = plugin.getConfig().getDouble("fireball.side.self.vertical", 0.55);
        this.damageSelf = plugin.getConfig().getDouble("fireball.damage.self", 2.0);
        this.damageEnemy = plugin.getConfig().getDouble("fireball.damage.enemy", 2.0);
        this.damageTeammates = plugin.getConfig().getDouble("fireball.damage.teammates", 0.0);
        this.throwEnabled = plugin.getConfig().getBoolean("fireball.throw.enabled", true);
        this.throwConsumeItem = plugin.getConfig().getBoolean("fireball.throw.consume-item", true);
        this.throwSpeed = plugin.getConfig().getDouble("fireball.throw.speed", 1.25);
        this.throwCooldownTicks = plugin.getConfig().getLong("fireball.throw.cooldown-ticks", 20L);
        throwCooldowns.clear();
    }

    private ITeam getTeamSafe(IArena arena, Player player) {
        if (arena == null || player == null) return null;
        try {
            return arena.getTeam(player);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

}

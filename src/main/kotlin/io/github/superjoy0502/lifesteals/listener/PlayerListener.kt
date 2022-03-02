package io.github.superjoy0502.lifesteals.listener

import io.github.monun.heartbeat.coroutines.HeartbeatScope
import io.github.monun.heartbeat.coroutines.Suspension
import io.github.superjoy0502.lifesteals.math.PlayerSpawner
import io.github.superjoy0502.lifesteals.plugin.LifeStealPlugin
import io.github.superjoy0502.lifesteals.plugin.addHeart
import io.github.superjoy0502.lifesteals.plugin.deathChest
import io.github.superjoy0502.lifesteals.plugin.removeHeart
import kotlinx.coroutines.launch
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.block.Barrel
import org.bukkit.block.data.type.Bed
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.ceil


class PlayerListener(private val plugin: LifeStealPlugin) : Listener {

    var playerQuitTimeMap = mutableMapOf<Player, Int>()

    @EventHandler
    fun playerJoinEvent(event: PlayerJoinEvent) {
        val player = event.player
        if (!plugin.isGameStarted) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = 20.0
        }
        else {
            if (player in plugin.survivorList) {

            }
            else if (player in plugin.participantList) {
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = 20.0
                player.inventory.clear()
                player.gameMode = GameMode.SPECTATOR
                player.sendMessage(
                    "${ChatColor.WHITE}[${ChatColor.AQUA}Info${ChatColor.WHITE}] 현재 진행되고 있는 체력 약탈 야생 게임이 종료되지 않았습니다."
                )
                player.sendMessage(
                    "${ChatColor.WHITE}[${ChatColor.AQUA}Info${ChatColor.WHITE}] 게임이 종료될 때까지 기다리거나, /quit을 입력해 현재 참가 중인 게임에서 나갈 수 있습니다."
                )
            }
            else {
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = 20.0
                player.inventory.clear()
            }
        }
    }

    @EventHandler
    fun playerQuitEvent(event: PlayerQuitEvent) {
        if (!plugin.isGameStarted) return

        else {
            val quitScope = HeartbeatScope()
            val player = event.player

            // 생존자인 플레이어가 나간 경우
            if (player in plugin.survivorList) {
                quitScope.launch {
                    if (plugin.phaseManager.phase < 17) {
                        playerQuitTimeMap[player] = 0
                        var counter = 0
                        val suspension = Suspension()
                        repeat(180) {
                            suspension.delay(1000L)
                            if (plugin.server.onlinePlayers.contains(player)) {
                                playerQuitTimeMap.remove(player)
                                return@launch
                            }
                            counter++
                            playerQuitTimeMap[player] = counter

                        }

                    }
                }
                plugin.survivorList.remove(player)
                if (plugin.survivorList.size == 1) {
                    plugin.endGame(plugin.survivorList[0])
                }
            }

            else if (player in plugin.participantList) return
        }

    }

    @EventHandler
    fun playerKilledEvent(event: PlayerDeathEvent) {

        event.deathMessage(null)

        if (!plugin.isGameStarted) return // 게임 시작했는지 확인

        val victim = event.player
        val killer = victim.killer
        val deathReason = victim.lastDamageCause
        val location = victim.location.clone()
        val inventory = victim.inventory

        if (victim in plugin.survivorList) {  // 플레이어가 생존자 리스트에 존재하는 경우

            for (sender in plugin.participantList) {
                sender.sendMessage("${ChatColor.WHITE}[${ChatColor.AQUA}Info${ChatColor.WHITE}] ${ChatColor.DARK_RED}사람이 죽었다.")
            }
            if (killer != null) {
                if (victim != killer) { // 플레이어가 다른 플레이어에게 사망한 경우
                    val increaseRate = victim.removeHeart(plugin.lifeStealValue, plugin)
                    if (victim !in plugin.survivorList) { // 이번 죽음으로 인에 게임에서 탈락한 경우
                        for (sender in plugin.participantList) {
                            sender.sendMessage("${ChatColor.WHITE}[${ChatColor.AQUA}Info${ChatColor.WHITE}] ${ChatColor.YELLOW}" + victim.toString() + "${ChatColor.RED}님께서 게임에서 탈락하셨습니다.")
                            sender.sendMessage("${ChatColor.WHITE}[${ChatColor.AQUA}Info${ChatColor.WHITE}] 남은 인원 : " + plugin.survivorList.size.toString() + "/" + plugin.participantList.size.toString())
                        }
                        victim.deathChest(victim.location, plugin)
                    }
                    killer.addHeart(increaseRate, plugin)
                }
                else { // 플레이어가 스스로 사망한 경우
                    if (victim !in plugin.survivorList) {
                        for (item in inventory) {
                            if (item != null) {
                                location.world.dropItemNaturally(location, item.clone())
                            }
                        }
                        victim.inventory.clear()
                    }
                }
            }
            else { // 플레이어가 다른 엔티티에 의해 사망한 경우
                if (deathReason is EntityDamageByEntityEvent) {
                    if (deathReason.damager is Mob) {
                        victim.removeHeart(plugin.lifeStealValue, plugin)
                        if (victim !in plugin.survivorList) { // 플레이어가 이번 죽음으로 인해 아예 탈락한 경우
                            deathChest(victim, location, plugin)
                            victim.inventory.clear()
                        }
                        else {
                            val barrelLocation = ArrayList<Location>() // 플레이어가 죽은 위치에 생성될 통 위치
                            var rawLocation = location.clone()

                            if (ceil(rawLocation.y) >= 320.0) rawLocation.y = 319.0
                            else if (ceil(rawLocation.y) <=)
                            else rawLocation.y = ceil(rawLocation.y)
                            barrelLocation.add(rawLocation)
                            barrelLocation.
                            val playerItemList = ArrayList<ItemStack>()
                            for (i in victim.inventory.contents) {
                                if (i != null) {
                                    playerItemList.add(i)
                                }
                            }
                            for (i in victim.getInventory().getArmorContents()) {
                                if (i != null) {
                                    playerItemList.add(i)
                                }
                            }
                            if (playerItemList.size > 27) {
                                val barrel = barrelLocation.block.setType(Material.BARREL)
                                barrel.
                            }
                            victim.getLocation().getBlock().setType(Material.BARREL)
                            val chest: Barrel = victim.getLocation().getBlock().getState() as Barrel
                            victim.inventory.clear()
                        }
                    }
                }
            }
        }

        if (killer == null) {

            if (deathReason is EntityDamageByEntityEvent) {

                if (deathReason.damager is Mob) { // 몬스터에 의해 사망한 경우

//                    println("MONSTER Removing ${plugin.lifeStealValue.toDouble()} health from ${victim.name}")
                    victim.removeHeart(plugin.lifeStealValue.toDouble(), plugin)
                    return

                }
                else if (deathReason.damager is AbstractArrow) { // 화살에 의해 사망한 경우

                    val arrow = deathReason.damager as Arrow
                    if (arrow.shooter is AbstractSkeleton || arrow.shooter is Drowned) {

                        victim.removeHeart(plugin.lifeStealValue.toDouble(), plugin)
                        return

                    }

                }

                else if (deathReason.damager is Trident){

                    val trident = deathReason.damager as Trident
                    if (trident.shooter is Drowned) {

                        victim.removeHeart(plugin.lifeStealValue.toDouble(), plugin)
                        return
                    }
                }

            }

        } else { // 플레이어에 의해 사망한 경우

            if (victim != killer) {

//                println("PLAYER Removing ${plugin.lifeStealValue.toDouble()} health from ${victim.name}")
                victim.removeHeart(plugin.lifeStealValue.toDouble(), plugin)
                val attribute = killer.getAttribute(Attribute.GENERIC_MAX_HEALTH)
                if (attribute != null) {

                    attribute.baseValue = attribute.baseValue + plugin.lifeStealValue

                }

                return

            }

        }
        // 기타
        val attribute = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH)
        if (attribute != null) {

            var value = attribute.baseValue / 2
            if (value.toInt() % 2 == 1) value += 1
//            println("ELSE ${event.deathMessage()}")
            victim.removeHeart(value, plugin)

            return

        }

    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {

        val target = event.player

        if (plugin.phaseManager.isTrackingClosestPlayer) {

            for (player in getPlayersClosestToTarget(target)) player.compassTarget = target.location

        }

        if (plugin.phaseManager.phase == 17) {

            if (target.location.y <= 0) {

                target.addPotionEffect(PotionEffect(PotionEffectType.WITHER, Integer.MAX_VALUE, 1))

            }
            else {

                target.removePotionEffect(PotionEffectType.WITHER)

            }

        }

    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {

        val player = event.player
        if (!plugin.survivorList.contains(player)) return

        val playerSpawner = PlayerSpawner(plugin.survivorList.size, plugin.centreLocation!!)
        playerSpawner.radius = (plugin.centreLocation!!.world.worldBorder.size * 0.4).toInt()

        event.respawnLocation = playerSpawner.getPlayerSpawnLocation((0 until plugin.survivorList.size).random())

        val givePlayerEffectScope = HeartbeatScope()

        givePlayerEffectScope.launch {

            val suspension = Suspension()
            suspension.delay(50L)

            player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, 60 * 20, 1))
            player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 10 * 20, Integer.MAX_VALUE))
            player.addPotionEffect(PotionEffect(PotionEffectType.SATURATION, 10 * 20, Integer.MAX_VALUE))
            if (plugin.phaseManager.phase == 17) {

                player.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1))
                player.addPotionEffect(PotionEffect(PotionEffectType.HUNGER, Integer.MAX_VALUE, 1))
                player.addPotionEffect(PotionEffect(PotionEffectType.BAD_OMEN, Integer.MAX_VALUE, 1))

            }

        }

    }

    @EventHandler
    fun onCompassMoveInventory(event: InventoryMoveItemEvent) {

        var isDestinationPlayer = false

        if (event.item == ItemStack(Material.COMPASS)) {

            for (player in plugin.survivorList) {

                if (event.destination == player.inventory) isDestinationPlayer = true

            }

            if (isDestinationPlayer && !plugin.phaseManager.isTrackingClosestPlayer) event.isCancelled = true

        }

    }

    @EventHandler
    fun onCompassPickUp(event: EntityPickupItemEvent) {

        if (event.entity !is Player) return

        if (event.item == ItemStack(Material.COMPASS)) {

            if (!plugin.phaseManager.isTrackingClosestPlayer) event.isCancelled = true

        }

    }

    @EventHandler
    fun onSleep(event: PlayerBedEnterEvent) {

        event.isCancelled = true
        event.player.sendMessage("${ChatColor.RED}어디서 주무시려고")
        
    }

    @EventHandler
    fun onInteractBed(event: PlayerInteractEvent) {

        if (event.clickedBlock is Bed) {

            event.isCancelled = true
            event.player.sendMessage("${ChatColor.RED}일 하세요 일")

        }

    }

    @EventHandler
    fun onConsumeMilk(event: PlayerItemConsumeEvent) {

        if (plugin.phaseManager.phase != 17) return
        if (event.item != ItemStack(Material.MILK_BUCKET)) return

        event.isCancelled = true

    }

    fun getPlayersClosestToTarget(target: Player): List<Player> {

        val list = arrayListOf<Player>()

        if (!plugin.phaseManager.playerTrackingMap.containsValue(target)) return emptyList()

        for (pair in plugin.phaseManager.playerTrackingMap) if (pair.value == target) list.add(pair.key)

        return list

    }

}

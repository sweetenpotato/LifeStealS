package io.github.superjoy0502.lifesteals.plugin

import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.block.Barrel
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import sun.jvm.hotspot.HelloWorld.e
import kotlin.math.ceil


// 플레이어의 체력을 삭제하는 함수.
fun Player.removeHeart(health: Double, plugin: LifeStealPlugin): Double {

    // 감소되는 체력이 없으면 무시
    if (health <= 0) return 0.0

    if (this.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue!! <= health ||
        this.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue!! <= 2
        ) {
        val value = this.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue ?: 0.0
        this.gameMode = GameMode.SPECTATOR
        plugin.survivorList.remove(this)
        this.showTitle(Title.title(Component.text("${ChatColor.RED}탈락하셨습니다"), Component.empty()))
        if (plugin.survivorList.size == 1) {
            plugin.endGame(plugin.survivorList[0])
        }
        return value
    }

    else {
        this.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = this.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue!! - health
        return health
    }

}

// 플레이어 체력의 절반을 삭제하는 함수
fun Player.removeHeartHalf(health: Double, plugin: LifeStealPlugin) {

    if (health <= 0) return

    val maxHealthAttribute = this.getAttribute(Attribute.GENERIC_MAX_HEALTH)
    if (maxHealthAttribute != null) {
        var decreaseValue = maxHealthAttribute.value
        if (decreaseValue.toInt() % 2 == 1) {
            decreaseValue += 1
            decreaseValue /= 2
        }
        else {
            decreaseValue /= 2
        }
        removeHeart(decreaseValue, plugin)
    }

}

// 플레이어의 체력을 추가해 주는 함수
fun Player.addHeart(health: Double, plugin: LifeStealPlugin) {
    if (health <= 0) return

    val maxHealthAttribute = this.getAttribute(Attribute.GENERIC_MAX_HEALTH)
    if (maxHealthAttribute != null) {
        maxHealthAttribute.baseValue += health
    }
}

fun Player.deathChest(location: Location, plugin: LifeStealPlugin) {
    val deathBarrelLocation = ArrayList<Location>()
    val playerItemList = ArrayList<ItemStack>()
    val rawLocation = location.clone()

    // 플레이어가 소지하고 있는 모든 아이템을 리스트에 옮겨 담음
    for (item in this.inventory.contents!!) {
        if (item != null) {
            playerItemList.add(item)
        }
    }
    for (item in this.inventory.armorContents!!) {
        if (item != null) {
            playerItemList.add(item)
        }
    }

    // 차원 별 통 생성 위치 설정 : y좌표
    if (ceil(rawLocation.y) >= plugin.world!!.maxHeight.toDouble()) {
        rawLocation.y = (plugin.world!!.maxHeight - 1).toDouble()
    }
    else if (ceil(rawLocation.y) <= plugin.world!!.minHeight.toDouble()) {
        rawLocation.y = (plugin.world!!.minHeight + 1).toDouble()
    }
    else {
        rawLocation.y = ceil(rawLocation.y)
    }

    deathBarrelLocation.add(rawLocation)

    // 플레이어가 들고 있었던 아이템을 떨구는 통 생성
    if (playerItemList.size > 27) {
        rawLocation.x += 1.0
        deathBarrelLocation.add(rawLocation)

        deathBarrelLocation[0].block.type = Material.BARREL
        val barrel1: Barrel = deathBarrelLocation[0].block.state as Barrel
        val barrel2: Barrel = deathBarrelLocation[1].block.state as Barrel

        barrel1.inventory.contents = playerItemList.subList(0, 27).toTypedArray()
        barrel2.inventory.contents = playerItemList.subList(27, playerItemList.size).toTypedArray()
    }
    else if (playerItemList.size > 0) {
        deathBarrelLocation[0].block.type = Material.BARREL
        val barrel: Barrel = deathBarrelLocation[0].block.state as Barrel

        barrel.inventory.contents = playerItemList.toTypedArray()
    }
    else return
    this.inventory.clear()

    for (player in plugin.survivorList) {
        player.sendMessage(
            "${ChatColor.WHITE}[${ChatColor.AQUA}Info${ChatColor.WHITE}] 플레이어 상자 위치 : x = " + deathBarrelLocation[0].x.toString() + ", y = " + deathBarrelLocation[0].y.toString() + ", z = " + deathBarrelLocation[0].z.toString()
        )
    }
}
package io.github.superjoy0502.lifesteals.plugin

import io.github.monun.heartbeat.coroutines.HeartbeatScope
import io.github.monun.heartbeat.coroutines.Suspension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.ChatColor
import org.bukkit.Color.PURPLE
import org.bukkit.Difficulty
import org.bukkit.GameRule
import org.bukkit.Material
import org.bukkit.boss.BarColor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

class PhaseManager(private val plugin: LifeStealPlugin) {

    var phase = 0
    var phaseLength = 300
    var currentPhaseLength = phaseLength
    var phaseColor = BarColor.RED
    var penaltyString = ""
    lateinit var phaseScope: CoroutineScope
    lateinit var fixTimeScope: CoroutineScope
    lateinit var fixDifficultyScope: CoroutineScope
    lateinit var fixWeatherScope: CoroutineScope
    lateinit var worldBorderScope: CoroutineScope
    private val barColorList = listOf(
        BarColor.RED,
        BarColor.GREEN,
        BarColor.BLUE,
        BarColor.PURPLE,
        BarColor.PINK,
        BarColor.WHITE,
        BarColor.YELLOW
    )
    private val textColorList = listOf(
        ChatColor.RED,
        ChatColor.GREEN,
        ChatColor.BLUE,
        ChatColor.DARK_PURPLE,
        ChatColor.LIGHT_PURPLE,
        ChatColor.WHITE,
        ChatColor.YELLOW
    )
    var isTrackingClosestPlayer = false
    var playerTrackingMap: Map<Player, Player> = mapOf()
    var playersWithNoCompassList = arrayListOf<Player>()
    var increaseLifeStealValueCount = 0

    fun phaseCoroutine() {

        // Initialize
        phaseScope = HeartbeatScope()
        phase++
        phaseLength = if (phase < 7) 300 else 180


        // Reset Compass Tracking
        isTrackingClosestPlayer = false
        playerTrackingMap = mapOf()
        playersWithNoCompassList = arrayListOf()
        for (player in plugin.survivorList) player.inventory.removeItem(ItemStack(Material.COMPASS, Integer.MAX_VALUE))

        // Setup last phase
        if (phase == 17) {

            penaltyString = "마지막 페이즈입니다!"

            plugin.bossBar?.color = BarColor.RED
            plugin.bossBar?.setTitle("PHASE 17: ENDGAME")
            plugin.bossBar?.progress = 1.0
            plugin.lifeStealValue = 10
            plugin.world?.difficulty = Difficulty.HARD
            plugin.nether?.difficulty = Difficulty.HARD
            plugin.end?.difficulty = Difficulty.HARD

            plugin.world?.setStorm(true)
            plugin.world?.isThundering = true
            plugin.world?.setGameRule(GameRule.DO_WEATHER_CYCLE, false)

            applyPotionEffectToPlayers(PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1))
            applyPotionEffectToPlayers(PotionEffect(PotionEffectType.HUNGER, Integer.MAX_VALUE, 1))
            applyPotionEffectToPlayers(PotionEffect(PotionEffectType.BAD_OMEN, Integer.MAX_VALUE, 1))

            return

        }

        // Shrinking WorldBorder
        if (phase == 3) {

            // 인벤세이브 해제
            plugin.world?.setGameRule(GameRule.KEEP_INVENTORY, false)
            plugin.nether?.setGameRule(GameRule.KEEP_INVENTORY, false)
            plugin.end?.setGameRule(GameRule.KEEP_INVENTORY, false)

            worldBorderScope = HeartbeatScope()
            worldBorderScope.launch {

                val suspension = Suspension()
                repeat(6000000) {

//                    plugin.centreLocation!!.world.worldBorder.size -= ((2500 - 100) / 6000000)
                    plugin.world!!.worldBorder.size = plugin.world!!.worldBorder.size - 0.0004
                    if (plugin.nether != null) plugin.nether!!.worldBorder.size = plugin.nether!!.worldBorder.size - 0.0004
                    suspension.delay(1L)

                }

            }

        }

        // Setup General Phases
        val random = (0..6).random()
        phaseColor = barColorList[random]
        plugin.bossBar?.color = phaseColor

        phaseScope.launch {

            applyPenaltyToPlayers()

            for (player in plugin.server.onlinePlayers) {

                player.showTitle(
                    Title.title(
                        Component.text("${textColorList[random]}PHASE $phase"),
                        Component.text(
                            penaltyString
                        )
                    )
                )

            }

            val suspension = Suspension()
            currentPhaseLength = phaseLength
            repeat(phaseLength) {

                currentPhaseLength--
                updateBossBar()
                suspension.delay(1000L)

            }
            plugin.lifeStealValue = 2
            if (plugin.started) phaseCoroutine()

        }

    }

    fun updateBossBar() {

        val remainingTime = convertSecondsToMinutesAndSeconds(currentPhaseLength)
        plugin.bossBar?.setTitle("PHASE $phase: ${remainingTime.first}m ${remainingTime.second}s")
        plugin.bossBar?.progress = currentPhaseLength / phaseLength.toDouble()

    }

    fun applyPenaltyToPlayers() {

        if (phase == 1 || phase == 2) penaltyString = "평화 그 자체" // phase 1, 2 (Phase Time = 5 min)
        else if (phase in 3..6) { // phase 3 ~ 6 (Phase Time = 5 min)

            when ((0..3).random()) {

                0 -> { // 빼앗기는 하트 수 1개 증가

                    penaltyString = "${ChatColor.RED}하트${ChatColor.RESET}가 한개 더 빼앗깁니다"

                    plugin.lifeStealValue += 2

                }

/*                1 -> { // 5분 동안 모든 플레이어 발광 효과 부여
                    
                    penaltyString = "5분 동안 모든 플레이어에게 ${ChatColor.YELLOW}발광${ChatColor.RESET} 효과가 부여됩니다"

                    for (player in plugin.survivorList) {

                        player.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, phaseLength * 20, 1))

                    }

                }*/

                1 -> { // 모든 플레이어의 최대 체력이 영구적으로 하트 1개만큼 감소

                    penaltyString = "${ChatColor.RED}하트${ChatColor.RESET} 1개가 영구적으로 감소합니다"

                    for (player in plugin.survivorList) {

                        player.removeHeart(2.0, plugin)

                    }

                }

                2 -> { // 5분동안 밤 12시, 난이도 보통 유지

                    penaltyString = "5분 동안 ${ChatColor.DARK_BLUE}나쁜 밤${ChatColor.RESET}이 찾아옵니다"

                    fixTime()
                    fixDifficulty(Difficulty.NORMAL)

                }

                3 -> { // 5분동안 날씨 번개 유지

                    penaltyString = "5분 동안 ${ChatColor.DARK_AQUA}폭풍${ChatColor.RESET}이 휘몰아칩니다"

                    fixThunderStorm()

                }

            }

        } else if (phase in 7..16) { // phase 7 ~ 16 (Phase Time = 3 min)

            val decider = (1..100).random()

            when {

                // 빼앗기는 하트 수 2개 영구적 증가 (최대 2회) - 40% (조건부 -40%)
                decider <= 40 && increaseLifeStealValueCount < 2 -> {

                    penaltyString = "이제부터 하트를 두개씩 더 빼앗깁니다"

                    plugin.lifeStealValue += 4
                    increaseLifeStealValueCount++

                }
                // 가장 가까운 플레이어에게 위치를 가르키는 나침반 지급 - 15% (조건부 +10%)
                (decider in 41..55 && increaseLifeStealValueCount < 2) || decider in 1..25 -> {
                    
                    penaltyString = "가장 가까운 사람을 향한 ${ChatColor.DARK_GRAY}길잡이${ChatColor.RESET}를 부여받습니다"

                    for (player in plugin.survivorList) {

                        lateinit var closestPlayer: Player
                        var distanceToClosestPlayer: Double = 0.0
                        val pLoc = player.location

                        for (target in plugin.survivorList) {

                            if (player == target) continue

                            val tLoc = target.location
                            val distanceBetweenPlayerAndTarget =
                                sqrt(
                                    sqrt((pLoc.x - tLoc.x).pow(2) + (pLoc.z - tLoc.z).pow(2)).pow(2)
                                            + (pLoc.y - tLoc.y).pow(2)
                                )

                            if (distanceToClosestPlayer < distanceBetweenPlayerAndTarget) {

                                distanceToClosestPlayer = distanceBetweenPlayerAndTarget
                                closestPlayer = target

                            }

                        }

                        // Turn on player tracker
                        playerTrackingMap = playerTrackingMap + Pair(player, closestPlayer)

                    }

                    isTrackingClosestPlayer = true

                    for (player in plugin.survivorList) {

                        if (player.inventory.addItem(ItemStack(Material.COMPASS)).isNotEmpty()) {

                            playersWithNoCompassList.add(player)

                        }

                        player.sendMessage("인벤토리가 꽉 차있어 나침반이 지급되지 않았습니다. /compass 명령어를 사용해서 받아주세요.")

                    }

                }
                // 모든 플레이어의 최대 체력이 영구적으로 하트 1개만큼 감소 - 15% (조건부 +10%)
                (decider in 56..70 && increaseLifeStealValueCount < 2) || decider in 26..50 -> {
                    
                    penaltyString = "${ChatColor.RED}하트${ChatColor.RESET} 한개를 잃습니다"

                    for (player in plugin.survivorList) {

                        player.removeHeart(2.0, plugin)

                    }

                }
                // 5분동안 날씨 번개 유지, 밤 12시, 난이도 어려움, 발광 효과 고정 - 15% (조건부 +10%)
                (decider in 71..85 && increaseLifeStealValueCount < 2) || decider in 51..75 -> {

                    penaltyString = "최악의 ${ChatColor.DARK_AQUA}시간${ChatColor.RESET}이 찾아옵니다"

                    fixThunderStorm()
                    fixTime()
                    fixDifficulty(Difficulty.HARD)
                    applyPotionEffectToPlayers(PotionEffect(PotionEffectType.GLOWING, phaseLength * 20, 1))

                }
                // 디버프 - 15% (조건부 +10%)
                else -> {

                    penaltyString = "${ChatColor.DARK_RED}저주${ChatColor.RESET}에 걸립나다"

                    val debuffDecider = (1..10000).random()

                    when {

                        debuffDecider <= 1500 -> // Mining Fatigue Level 1 - 15%
                            applyPotionEffectToPlayers(PotionEffect(PotionEffectType.SLOW_DIGGING, phaseLength * 20, 1))
                        debuffDecider in 1501..2500 -> // Mining Fatigue Level 2 - 10%
                            applyPotionEffectToPlayers(PotionEffect(PotionEffectType.SLOW_DIGGING, phaseLength * 20, 2))
                        debuffDecider in 2501..6500 -> // Hunger - 40%
                            applyPotionEffectToPlayers(PotionEffect(PotionEffectType.HUNGER, phaseLength * 20, 1))
                        debuffDecider in 6501..8100 -> // Poison Level 1 - 16%
                            applyPotionEffectToPlayers(PotionEffect(PotionEffectType.POISON, phaseLength * 20, 1))
                        debuffDecider in 8101..8900 -> // Poison Level 2 - 8%
                            applyPotionEffectToPlayers(PotionEffect(PotionEffectType.POISON, phaseLength * 20, 2))
                        debuffDecider in 8901..8975 -> // Wither Level 1 - 0.75%
                            applyPotionEffectToPlayers(PotionEffect(PotionEffectType.WITHER, phaseLength * 20, 1))
                        debuffDecider in 8976..9000 -> // Wither Level 2 - 0.25%
                            applyPotionEffectToPlayers(PotionEffect(PotionEffectType.WITHER, phaseLength * 20, 2))
                        else -> // Bad Omen - 10%
                            applyPotionEffectToPlayers(PotionEffect(PotionEffectType.BAD_OMEN, phaseLength * 20, 1))

                    }

                }

            }

        }

    }

    fun fixThunderStorm() {

        fixWeatherScope = HeartbeatScope()
        fixWeatherScope.launch {

            val suspension = Suspension()

            plugin.world?.setStorm(true)
            plugin.world?.isThundering = true
            plugin.world?.setGameRule(GameRule.DO_WEATHER_CYCLE, false)

            suspension.delay(phaseLength * 1000L)

            plugin.world?.setStorm(false)
            plugin.world?.isThundering = false
            plugin.world?.setGameRule(GameRule.DO_WEATHER_CYCLE, true)

        }

    }

    fun fixTime() {

        fixTimeScope = HeartbeatScope()
        fixTimeScope.launch {

            val suspension = Suspension()

            plugin.world?.time = 18000L
            plugin.world?.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)

            suspension.delay(phaseLength * 1000L)

            plugin.world?.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true)


        }

    }

    fun fixDifficulty(difficulty: Difficulty) {

        fixDifficultyScope = HeartbeatScope()
        fixDifficultyScope.launch {

            val suspension = Suspension()

            plugin.world?.difficulty = difficulty
            plugin.nether?.difficulty = difficulty
            plugin.end?.difficulty = difficulty

            suspension.delay(phaseLength * 1000L)

            plugin.world?.difficulty = Difficulty.EASY
            plugin.nether?.difficulty = Difficulty.EASY
            plugin.end?.difficulty = Difficulty.EASY

        }

    }

    fun applyPotionEffectToPlayers(effect: PotionEffect) {

        for (player in plugin.survivorList) {

            player.addPotionEffect(effect)

        }

    }

    fun convertSecondsToMinutesAndSeconds(seconds: Int): Pair<Int, Int> {

        val minutes = floor(seconds / 60.0).toInt()
        val seconds = seconds % 60

        return Pair(minutes, seconds)

    }

}

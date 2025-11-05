package Perdume.rpg.core.player.Manager;

import Perdume.rpg.Rpg;
import Perdume.rpg.core.economy.EconomyManager;
import Perdume.rpg.core.player.data.PlayerData;
import Perdume.rpg.core.player.data.PlayerDataManager;
import Perdume.rpg.core.player.stats.PlayerStats;
import Perdume.rpg.core.player.stats.StatType;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.event.skill.XpGainEvent;
import dev.aurelium.auraskills.api.stat.StatModifier;
import dev.aurelium.auraskills.api.stat.Stats;
import dev.aurelium.auraskills.api.user.SkillsUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * [환산값 적용] 플레이어의 성장 로직을 총괄하고,
 * 우리 플러그인의 스탯을 AuraSkills 스탯에 '환산'하여 적용하는 관리자 클래스입니다.
 */
public class StatsManager implements Listener {

    private final Rpg plugin;
    private final PlayerDataManager playerDataManager;
    private final AuraSkillsApi auraSkillsApi;
    private final EconomyManager economyManager;
    private final double EXP_TO_MONEY_RATE = 0.1; // 1 경험치당 0.1원

    public StatsManager(Rpg plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
        this.auraSkillsApi = AuraSkillsApi.get();
        this.economyManager = plugin.getEconomyManager();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 플레이어 접속 시, 저장된 스탯을 불러와 AuraSkills에 환산 적용합니다.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 잠시 후 스탯을 적용하여 다른 플러그인과의 호환성을 높입니다.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            updateAllStatModifiers(event.getPlayer());
        }, 20L); // 1초 후
    }

    /**
     * 플레이어 퇴장 시, 적용했던 스탯 모디파이어를 제거합니다.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeAllStatModifiers(event.getPlayer());
    }

    /**
     * [핵심] 플레이어의 모든 스탯을 다시 계산하고 AuraSkills에 모디파이어로 적용합니다.
     * 스탯을 찍거나, 장비를 바꾸거나, 버프를 받을 때마다 이 메소드를 호출해야 합니다.
     */
    public void updateAllStatModifiers(Player player) {
        PlayerData data = playerDataManager.getPlayerData(player);
        if (data == null || data.getStats() == null) return;
        PlayerStats stats = data.getStats();
        SkillsUser user = auraSkillsApi.getUser(player.getUniqueId());

        // --- 힘 (Strength) 환산 ---
        // 힘 1당 AuraSkills 힘 1, 최대 체력 0.5 증가
        double strengthValue = stats.getStat(StatType.STRENGTH);
        user.addStatModifier(new StatModifier("rpg_strength_bonus", Stats.STRENGTH, strengthValue));
        user.addStatModifier(new StatModifier("rpg_strength_health_bonus", Stats.HEALTH, strengthValue * 0.5));

        // --- 지능 (Intelligence) 환산 ---
        // 지능 1당 AuraSkills 지혜(마법력) 1, 마나 재생 0.02 증가
        double intelligenceValue = stats.getStat(StatType.INTELLIGENCE);
        user.addStatModifier(new StatModifier("rpg_intelligence_bonus", Stats.WISDOM, intelligenceValue));
        // (마나 재생 등은 AuraSkills에서 커스텀 스탯으로 추가해야 할 수 있습니다)

        // --- 체력 (Vitality) 환산 ---
        // 체력 1당 AuraSkills 체력 2 증가
        double vitalityValue = stats.getStat(StatType.VITALITY);
        user.addStatModifier(new StatModifier("rpg_vitality_bonus", Stats.HEALTH, vitalityValue * 2.0));

        // --- 민첩 (Agility) 환산 ---
        // 민첩 1당 AuraSkills 민첩(공격속도 관련) 1 증가 (AuraSkills에 Agility 스탯이 있다면)
        // double agilityValue = stats.getStat(StatType.AGILITY);
        // user.addStatModifier(new StatModifier("rpg_agility_bonus", Stats.AGILITY, agilityValue));

        Rpg.log.info(player.getName() + "님의 스탯을 AuraSkills에 환산 적용했습니다.");
    }

    /**
     * 플레이어에게 적용된 모든 스탯 모디파이어를 제거합니다.
     */
    private void removeAllStatModifiers(Player player) {
        SkillsUser user = auraSkillsApi.getUser(player.getUniqueId());
        user.removeStatModifier("rpg_strength_bonus");
        user.removeStatModifier("rpg_strength_health_bonus");
        user.removeStatModifier("rpg_intelligence_bonus");
        user.removeStatModifier("rpg_vitality_bonus");
        // user.removeStatModifier("rpg_agility_bonus");
    }

    /**
     * [핵심] AuraSkills의 생활 스킬 경험치 획득 이벤트를 감지하여 돈으로 전환합니다.
     */
    @EventHandler
    public void onAuraSkillsXpGain(XpGainEvent event) {
        // 몬스터 처치 경험치는 PlayerExpChangeEvent에서 처리하므로 중복 방지
        if (event.getSource().name().equals("mob_kill")) return;

        double experience = event.getAmount();
        double moneyToGive = experience * EXP_TO_MONEY_RATE;

        if (moneyToGive > 0) {
            Player player = event.getPlayer();
            economyManager.depositPlayer(player, moneyToGive);
        }
    }

    /**
     * [핵심] 몬스터 사냥 등 바닐라 경험치 획득 이벤트를 감지하여 돈으로 전환하고,
     * 원래의 경험치 획득은 취소합니다.
     */
    @EventHandler
    public void onVanillaExpChange(PlayerExpChangeEvent event) {
        int experience = event.getAmount();
        if (experience <= 0) return;

        // 원래의 경험치 획득을 취소합니다.
        event.setAmount(0);

        double moneyToGive = experience * EXP_TO_MONEY_RATE;

        if (moneyToGive > 0) {
            Player player = event.getPlayer();
            economyManager.depositPlayer(player, moneyToGive);
        }
    }
}


package net.pherment.hide.arena;

import net.pherment.hide.ArmsHandle;
import net.pherment.hide.HideAndSeek;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {
    private boolean isGameAlive = false;

    private List<Player> playerInTeam = new ArrayList<>();

    private List<Player> teamSeekers = new ArrayList<>();
    private List<Player> teamHiders = new ArrayList<>();

    private int hidersCount = 0;
    private int seekersCount = 0;

    ScoreboardManager manager = Bukkit.getScoreboardManager();
    Scoreboard board = manager.getNewScoreboard();

    Team gamePlayers = board.registerNewTeam("has");

    Objective objective = board.registerNewObjective("Hide And Seek", "dummy", ChatColor.BLUE + "HIDE AND SEEK");
    private Score hiders = objective.getScore(ChatColor.DARK_AQUA + "Hiders:");
    private Score seekers = objective.getScore(ChatColor.RED + "Seekers:");
    private Score gameTimer = objective.getScore(ChatColor.DARK_GREEN + "Game End:");

    public void addPlayerToTeam(Player player) {
        playerInTeam.add(player);
    }

    public void startGame(HASArena arena) {
        isGameAlive = true;
        Random rand = new Random();

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        gamePlayers.setDisplayName("Hide And Seek");
        gamePlayers.setAllowFriendlyFire(false);

        hiders.setScore(0);
        seekers.setScore(0);
        gameTimer.setScore(300);

        for (Player p: getPlayerInTeam()) {
            gamePlayers.addPlayer(p);
            teamHiders.add(p);
            p.teleport(arena.getHidersLocation());
            p.setHealth(20);
            p.setFoodLevel(20);
            hiders.setScore(hidersCount++);
        }
        moveToSeekersTeam(getPlayerInTeam().get(rand.nextInt(getPlayerInTeam().size())), arena);
        for (Player p: getPlayerInTeam()) { p.setScoreboard(board); }
        startTimerToSeek(arena);
    }

    public void moveToSeekersTeam(Player player, HASArena arena) {
        teamHiders.remove(player);
        teamSeekers.add(player);
        hiders.setScore(hidersCount-=1);
        seekers.setScore(seekersCount+=1);

        if (teamHiders.size() == 0) {
            for (Player p: playerInTeam) {
                endGame(arena, p);
            }
        } else {
            player.teleport(arena.getSeekersLocation());
            ArmsHandle.handle(player);
            player.setHealth(20);
            player.setFoodLevel(20);
        }
    }

    private void startTimerToSeek(HASArena arena) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                int ctr = 30;
                while (ctr > 0) {
                    int finalCtr = ctr;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (Player p: playerInTeam) {
                                p.sendTitle("You have", finalCtr+" seconds until we begin! Hide!", 0, 20, 0);
                            }
                        }
                    }.runTask(HideAndSeek.getPlugin(HideAndSeek.class));
                    ctr -= 1;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                for (Player p: teamHiders) {
                    p.sendTitle(ChatColor.RED + "Seekers begin!", "", 0, 20, 0);
                }
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (Player p: teamSeekers) {
                            p.teleport(arena.getHidersLocation());
                        }
                        startGameTimer(arena);
                    }
                }.runTask(HideAndSeek.getPlugin(HideAndSeek.class));
            }
        };
        thread.start();
    }

    private void startGameTimer(HASArena arena) {
        Thread thread = new Thread(() -> {
            int ctr = 300;
            while (ctr > 0 && isGameAlive) {
                int finalCtr = ctr;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        gameTimer.setScore(finalCtr);
                    }
                }.runTask(HideAndSeek.getPlugin(HideAndSeek.class));
                ctr -= 1;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            isGameAlive = false;
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player p : playerInTeam) {
                        endGame(arena, p);
                    }
                }
            }.runTask(HideAndSeek.getPlugin(HideAndSeek.class));
        });
        thread.start();
    }

    private void endGame(HASArena arena, Player p) {
        isGameAlive = false;
        if (teamHiders.size() == 0) {
            p.sendTitle(ChatColor.RED + "All hiders have been found!", "", 0, 60, 0);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1, 1);
            p.setHealth(20);
            p.setFoodLevel(20);
            p.setScoreboard(manager.getNewScoreboard());
            arena.leavePlayer(p);
            gamePlayers.removePlayer(p);
            teamHiders.remove(p);
            playerInTeam.remove(p);
        } else {
            for (Player p: playerInTeam) {
                p.sendTitle(ChatColor.DARK_AQUA + "Not all hiders were found!", "", 0, 60, 0);
            }
        }

        for (Player p: playerInTeam) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1, 1);
            p.setHealth(20);
            p.setFoodLevel(20);
            p.setScoreboard(manager.getNewScoreboard());
            arena.leavePlayer(p);
            gamePlayers.removePlayer(p);
            teamHiders.remove(p);
            playerInTeam.remove(p);
        }
    }

    /* ===== GETTERS AND SETTERS ===== */

    public List<Player> getPlayerInTeam() {
        return playerInTeam;
    }
}

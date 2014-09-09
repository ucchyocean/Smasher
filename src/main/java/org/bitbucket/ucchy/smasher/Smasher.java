/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.smasher;

import java.io.File;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Smasher
 * @author ucchy
 */
public class Smasher extends JavaPlugin implements Listener {

    private static final String NAME = "smasher";
    private static final String DISPLAY_NAME = NAME;
    private static final String PERMISSION = NAME + ".";
    private static final String SKILL_METADATA = NAME + "skill";

    private SmasherConfig config;
    private ShapedRecipe recipe;

    /**
     * プラグインが有効化された時に呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        // リスナー登録
        getServer().getPluginManager().registerEvents(this, this);

        // コンフィグのロード
        config = new SmasherConfig(this);

        // 必要に応じてレシピ登録
        if ( config.isEnableCraft() ) {
            makeRecipe();
        }
    }

    /**
     * 新しいSmasherを作成して返す
     * @return
     */
    private ItemStack makeSmasher() {

        ItemStack item = new ItemStack(config.getSmasherMaterial(), 1);
        ItemMeta capturerodMeta = item.getItemMeta();
        capturerodMeta.setDisplayName(DISPLAY_NAME);
        item.setItemMeta(capturerodMeta);
        return item;
    }

    /**
     * 指定したプレイヤーにスマッシャーを与える
     * @param player プレイヤー
     */
    private void giveSmasher(Player player) {

        ItemStack rod = makeSmasher();
        ItemStack temp = player.getItemInHand();
        player.setItemInHand(rod);
        if ( temp != null ) {
            player.getInventory().addItem(temp);
        }
    }

    /**
     * レシピを登録する
     */
    private void makeRecipe() {

        recipe = new ShapedRecipe(makeSmasher());
        recipe.shape("  I", " I ", "S  ");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', Material.STICK);
        getServer().addRecipe(recipe);
    }

    /**
     * レシピを削除する
     */
    private void removeRecipe() {

        Iterator<Recipe> it = getServer().recipeIterator();
        while ( it.hasNext() ) {
            Recipe recipe = it.next();
            ItemStack result = recipe.getResult();
            if ( !result.hasItemMeta() ||
                    !result.getItemMeta().hasDisplayName() ||
                    !result.getItemMeta().getDisplayName().equals(DISPLAY_NAME) ) {
                continue;
            }
            it.remove();
        }

        this.recipe = null;
    }

    /**
     * プラグインのコマンドが実行された時に呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if ( args.length <= 0 ) {
            return false;
        }

        if (args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission(PERMISSION + "reload")) {
                sender.sendMessage(ChatColor.RED
                        + "You don't have permission \"" + PERMISSION + "reload\".");
                return true;
            }

            // コンフィグ再読込
            config.reloadConfig();

            if ( recipe == null && config.isEnableCraft() ) {
                makeRecipe();
            } else if ( recipe != null && !config.isEnableCraft() ) {
                removeRecipe();
            }

            sender.sendMessage(ChatColor.GREEN + NAME + " configuration was reloaded!");

            return true;

        } else if (args[0].equalsIgnoreCase("get")) {

            if ( !(sender instanceof Player) ) {
                sender.sendMessage(ChatColor.RED + "This command can be used only in game.");
                return true;
            }

            if (!sender.hasPermission(PERMISSION + "get")) {
                sender.sendMessage(ChatColor.RED
                        + "You don't have permission \"" + PERMISSION + "get\".");
                return true;
            }

            Player player = (Player)sender;
            giveSmasher(player);

            return true;

        } else if ( args.length >= 2 && args[0].equalsIgnoreCase("give") ) {

            if (!sender.hasPermission(PERMISSION + "give")) {
                sender.sendMessage(ChatColor.RED
                        + "You don't have permission \"" + PERMISSION + "give\".");
                return true;
            }

            Player player = getPlayer(args[1]);
            if ( player == null ) {
                sender.sendMessage(ChatColor.RED + "Player " + args[1] + " was not found.");
                return true;
            }

            giveSmasher(player);

            return true;
        }

        return false;
    }

    /**
     * プレイヤーがクリックをした時に呼び出されるイベント
     * @param event
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        final Player player = event.getPlayer();
        final JavaPlugin plugin = this;

        // Smasherを持っていない場合は、イベントを無視する
        if ( !isSmasher(player.getItemInHand()) ) {
            return;
        }

        // 右クリックでない場合は、イベントを無視する
        if ( event.getAction() == Action.LEFT_CLICK_AIR ||
                event.getAction() == Action.LEFT_CLICK_BLOCK ||
                event.getAction() == Action.PHYSICAL ) {
            return;
        }

        // パーミッションが無い場合は、イベントを無視する
        if ( !player.hasPermission(PERMISSION + ".action") ) {
            return;
        }

        // 既にスキル発動中の場合は、イベントを無視する
        if ( player.hasMetadata(SKILL_METADATA) ) {
            return;
        }

        // メタデータを設定して、スキル発動中フラグを設定する
        player.setMetadata(SKILL_METADATA, new FixedMetadataValue(this, true));

        // 耐久値を消費する
        decreaseDurabilityInHandItem(player);

        // 足を遅くする、音を鳴らす
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 30, 5));
        player.getWorld().playSound(
                player.getLocation(), Sound.PORTAL_TRIGGER, 1F, 2F);

        // 30ticks後に、ふっ飛ばし効果を発生させる
        new BukkitRunnable() {
            public void run() {
                if ( player.isDead() ) {
                    return;
                }
                Location location = player.getLocation().clone();

                // 前方3マスを中心に、半径3マスの範囲内にいるEntityを取得して吹っ飛ばす
                location.add(location.getDirection().multiply(3.0));
                double radius = 3.0;
                Entity orb = location.getWorld().spawnEntity(location, EntityType.EXPERIENCE_ORB);
                for ( Entity e : orb.getNearbyEntities(radius, radius, radius) ) {
                    if ( e instanceof LivingEntity && !e.equals(player) ) {
                        LivingEntity le = (LivingEntity)e;
                        le.damage(2, player);
                        Vector velocity = le.getLocation().subtract(location).toVector();
                        velocity.setY(0.3).normalize().multiply(10);
                        le.setVelocity(velocity);
                    }
                }
                orb.remove();

                // 爆発エフェクト
                player.getWorld().createExplosion(location, 0);
                player.getWorld().playSound(
                        player.getLocation(), Sound.WITHER_SHOOT, 0.6F, 1.2F);

                // メタデータを除去して、スキル発動中フラグを降ろす
                player.removeMetadata(SKILL_METADATA, plugin);
            }
        }.runTaskLater(this, 30);
    }

    /**
     * 指定されたItemStackがSmasherかどうかを判定する
     * @param item アイテム
     * @return Smasherかどうか
     */
    private boolean isSmasher(ItemStack item) {

        if ( item == null ) {
            return false;
        }

        if ( !item.hasItemMeta() ) {
            return false;
        }

        if ( !item.getItemMeta().hasDisplayName() ) {
            return false;
        }

        return item.getItemMeta().getDisplayName().equals(DISPLAY_NAME);
    }

    /**
     * このプラグインのJarファイルを返す
     * @return Jarファイル
     */
    protected File getJarFile() {
        return getFile();
    }

    /**
     * 指定したプレイヤーの手に持っているアイテムの耐久値を減らす
     * @param player
     */
    private void decreaseDurabilityInHandItem(Player player) {

        ItemStack item = player.getItemInHand();
        if ( item == null ) {
            return;
        }
        short durability = (short)(item.getDurability() + config.getDurabilityCost());
        if ( durability >= item.getType().getMaxDurability() ) {
            player.setItemInHand(null);
            player.getWorld().playSound(
                    player.getLocation(), Sound.ITEM_BREAK, 1, 1);
        } else {
            item.setDurability(durability);
        }
        updateInventory(player);
    }

    /**
     * プレイヤーのインベントリを更新する
     * @param player
     */
    @SuppressWarnings("deprecation")
    public static void updateInventory(Player player) {
        player.updateInventory();
    }

    /**
     * 指定した名前のプレイヤーを取得する
     * @param name プレイヤー名
     * @return プレイヤー
     */
    @SuppressWarnings("deprecation")
    public static Player getPlayer(String name) {
        return Bukkit.getPlayerExact(name);
    }
}

package cc.risingtide.risingtntwand;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;

import de.tr7zw.nbtapi.NBTItem;

public class RisingTNTWand extends JavaPlugin implements Listener {

	@Override
	public void onEnable() {
		saveDefaultConfig();

		getServer().getPluginManager().registerEvents(this, this);
		
		new Metrics(this, 7740);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (label.equalsIgnoreCase("risingtntwand")) {
			if (args.length == 1 && args[0].equalsIgnoreCase("reload")
					&& sender.hasPermission("risingtntwand.reload")) {
				sender.sendMessage(ChatColor.YELLOW + "Attempting to reload config...");
				reloadConfig();
				sender.sendMessage(ChatColor.GREEN + "Reloaded config!");
				return true;
			}
			sender.sendMessage(ChatColor.YELLOW + "RisingTNTWand " + ChatColor.BLUE + "v"
					+ getDescription().getVersion() + ChatColor.AQUA + " made by the RisingTide network.");
			return true;
		}

		if (args.length != 2) {
			sender.sendMessage(ChatColor.RED + "Usage: /tntwand <name> <uses|infinite>");
			return true;
		}
		if (!StringUtils.isNumeric(args[1]) && !args[1].equals("infinite")) {
			sender.sendMessage(ChatColor.RED + "Usage: /tntwand <name> <uses|infinite>");
			return true;
		}
		if (Bukkit.getPlayer(args[0]) == null) {
			sender.sendMessage(ChatColor.RED + "That player is not online!");
			return true;
		}
		int uses = -99;
		if (StringUtils.isNumeric(args[1])) {
			uses = Integer.parseInt(args[1]);
		}

		String usesstring = Integer.toString(uses);

		if (uses == -99) {
			usesstring = "∞";
		}

		ItemStack is = new ItemStack(Material.getMaterial(getConfig().getString("Item.type")));

		ItemMeta im = is.getItemMeta();

		im.setDisplayName(getConfigString("Item.name"));
		ArrayList<String> l = new ArrayList<String>();
		for (String li : getConfigString("Item.lore").split("\\\\n")) {
			l.add(li.replace("{uses}", usesstring));
		}
		im.setLore(l);

		is.setItemMeta(im);

		NBTItem nbti = new NBTItem(is);
		nbti.setInteger("risingtnt-uses", uses);

		Bukkit.getPlayer(args[0]).getInventory().addItem(nbti.getItem());

		sender.sendMessage(ChatColor.GREEN + "Gave " + args[0] + " a TNT wand with " + usesstring + " uses.");

		return true;
	}

	@EventHandler
	public void playerInteract(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		if (e.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		if (e.getItem() == null)
			return;
		if (!new NBTItem(e.getItem()).hasNBTData() || !new NBTItem(e.getItem()).hasKey("risingtnt-uses"))
			return;
		e.setCancelled(true);
		if (!FPlayers.getInstance().getByPlayer(p).hasFaction()) {
			p.sendMessage(getConfigString("Messages.nofaction"));
			return;
		}

		if (e.getClickedBlock().getType() != Material.CHEST
				&& e.getClickedBlock().getType() != Material.TRAPPED_CHEST) {
			p.sendMessage(getConfigString("Messages.notachest"));
			return;
		}

		Faction theirFaction = FPlayers.getInstance().getByPlayer(p).getFaction();
		Faction factionAt = Board.getInstance().getFactionAt(new FLocation(e.getClickedBlock().getLocation()));
		if (!factionAt.getId().equals(theirFaction.getId())) {
			p.sendMessage(getConfigString("Messages.notclaimed"));
			return;
		}

		Chest chest = (Chest) e.getClickedBlock().getState();

		int uses = new NBTItem(e.getItem()).getInteger("risingtnt-uses");

		int canputin = theirFaction.getTntBankLimit() - theirFaction.getTnt();

		if (canputin < 1) {
			p.sendMessage(getConfigString("Messages.bankfull"));
			return;
		}

		int tnttotal = 0;
		int ic = 0;
		for (ItemStack i : chest.getInventory()) {
			if (i != null && i.getType() == Material.TNT && canputin > 0) {
				if (canputin >= i.getAmount()) {
					tnttotal += i.getAmount();
					canputin -= i.getAmount();
					i.setType(Material.AIR);
				} else {
					int a = (canputin - i.getAmount());
					tnttotal += a;
					canputin -= a;
					if (a == i.getAmount()) {
						i.setType(Material.AIR);
						i.setAmount(0);
					} else {
						i.setAmount(i.getAmount() - a);
					}
				}
			}
			chest.getInventory().setItem(ic, i);
			ic++;
		}

		if (tnttotal == 0) {
			p.sendMessage(getConfigString("Messages.chestempty"));
			return;
		}

		theirFaction.addTnt(tnttotal);
		String msg = getConfigString("Messages.added");
		msg = msg.replace("{tnt}", Integer.toString(tnttotal));
		p.sendMessage(msg);

		if (uses != -99) {
			uses--;
		}

		ItemStack i = e.getItem();

		if (uses < 1 && uses != -99) {
			i.setType(Material.AIR);
		}

		if (i.getType() != Material.AIR) {
			NBTItem nbti = new NBTItem(i);
			nbti.setInteger("risingtnt-uses", uses);
			i = nbti.getItem();

			i.setType(Material.getMaterial(getConfig().getString("Item.type")));

			String usesstring = Integer.toString(uses);

			if (uses == -99) {
				usesstring = "∞";
			}

			ItemMeta im = i.getItemMeta();
			im.setDisplayName(getConfigString("Item.name"));
			ArrayList<String> l = new ArrayList<String>();
			for (String li : getConfigString("Item.lore").split("\\\\n")) {
				l.add(li.replace("{uses}", usesstring));
			}
			im.setLore(l);
			i.setItemMeta(im);
		}

		p.setItemInHand(i);
	}

	private String getConfigString(String k) {
		return ChatColor.translateAlternateColorCodes('&', getConfig().getString(k));
	}

}

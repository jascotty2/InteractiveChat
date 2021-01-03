package com.loohp.interactivechat.Listeners;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.cryptomorin.xseries.XMaterial;
import com.loohp.interactivechat.InteractiveChat;
import com.loohp.interactivechat.BungeeMessaging.BungeeMessageSender;
import com.loohp.interactivechat.ObjectHolders.CommandPlaceholderInfo;
import com.loohp.interactivechat.ObjectHolders.ICPlaceholder;
import com.loohp.interactivechat.ObjectHolders.MentionPair;
import com.loohp.interactivechat.ObjectHolders.PlayerWrapper;
import com.loohp.interactivechat.Utils.ChatColorUtils;
import com.loohp.interactivechat.Utils.CustomStringUtils;
import com.loohp.interactivechat.Utils.MessageUtils;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.BaseComponent;

public class Events implements Listener {
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		InteractiveChat.mentionCooldown.put(event.getPlayer(), (System.currentTimeMillis() - 3000));
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent event) {
		InteractiveChat.mentionCooldown.remove(event.getPlayer());
	}
	
	@EventHandler(priority=EventPriority.HIGH)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		
		translateAltColorCode(event);
		
		String command = event.getMessage();
		for (String parsecommand : InteractiveChat.commandList) {
			if (command.matches(parsecommand)) {
				command = MessageUtils.preprocessMessage(command, InteractiveChat.placeholderList, InteractiveChat.aliasesMapping);
				for (ICPlaceholder icplaceholder : InteractiveChat.placeholderList) {
					String placeholder = icplaceholder.getKeyword();
					if ((icplaceholder.isCaseSensitive() && command.contains(placeholder)) || (!icplaceholder.isCaseSensitive() && command.toLowerCase().contains(placeholder.toLowerCase()))) {
						String regexPlaceholder = (icplaceholder.isCaseSensitive() ? "" : "(?i)") + CustomStringUtils.escapeMetaCharacters(placeholder);
						String uuidmatch = "<" + UUID.randomUUID().toString() + ">";
						command = command.replaceFirst(regexPlaceholder,  uuidmatch);
						InteractiveChat.commandPlaceholderMatch.put(uuidmatch, new CommandPlaceholderInfo(new PlayerWrapper(event.getPlayer()), placeholder, uuidmatch, InteractiveChat.commandPlaceholderMatch));
						if (InteractiveChat.bungeecordMode) {
							try {
								BungeeMessageSender.addCommandMatch(event.getPlayer().getUniqueId(), placeholder, uuidmatch);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						event.setMessage(command);
						break;
					}
				}
				break;
			}
		}
	}

	@EventHandler(priority=EventPriority.HIGH)
	public void checkChat(AsyncPlayerChatEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (InteractiveChat.ChatManagerHook) {
			return;
		}

		String message = event.getMessage();
		if (InteractiveChat.maxPlacholders >= 0) {
			int count = 0;
			for (ICPlaceholder icplaceholder : InteractiveChat.placeholderList) {
				String findStr = icplaceholder.getKeyword();
				int lastIndex = 0;	
				while(lastIndex != -1) {	
				    lastIndex = icplaceholder.isCaseSensitive() ? message.indexOf(findStr, lastIndex) : message.toLowerCase().indexOf(findStr.toLowerCase(), lastIndex);	
				    if(lastIndex != -1) {
				        count++;
				        lastIndex += findStr.length();
				    }
				}
			}
			if (count > InteractiveChat.maxPlacholders) {
				event.setCancelled(true);
				String cancelmessage = ChatColorUtils.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(event.getPlayer(), InteractiveChat.limitReachMessage));
				event.getPlayer().sendMessage(cancelmessage);
				return;
			}
		}
		
		event.setMessage(MessageUtils.preprocessMessage(message, InteractiveChat.placeholderList, InteractiveChat.aliasesMapping));
		
		String mapKey = ChatColorUtils.stripColor(ChatColorUtils.translateAlternateColorCodes('&', event.getMessage()));
		InteractiveChat.messages.put(mapKey, event.getPlayer().getUniqueId());
		Bukkit.getScheduler().runTaskLater(InteractiveChat.plugin, () -> InteractiveChat.messages.remove(mapKey), 60);
		
		if (InteractiveChat.bungeecordMode) {
			try {
				BungeeMessageSender.addMessage(ChatColorUtils.stripColor(ChatColorUtils.translateAlternateColorCodes('&', event.getMessage())), event.getPlayer().getUniqueId());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@EventHandler(priority=EventPriority.LOW)
	public void checkChatforChatManagerOrTranslateChatColor(AsyncPlayerChatEvent event) {
		
		translateAltColorCode(event);
		
		if (event.isCancelled()) {
			return;
		}
		
		checkMention(event);
				
		if (!InteractiveChat.ChatManagerHook) {
			return;
		}
		
		String message = event.getMessage();
		Player player = event.getPlayer();
		
		if (InteractiveChat.maxPlacholders >= 0) {
			int count = 0;
			for (ICPlaceholder icplaceholder : InteractiveChat.placeholderList) {
				String findStr = icplaceholder.getKeyword();
				int lastIndex = 0;	
				while(lastIndex != -1) {	
				    lastIndex = icplaceholder.isCaseSensitive() ? message.indexOf(findStr, lastIndex) : message.toLowerCase().indexOf(findStr.toLowerCase(), lastIndex);	
				    if(lastIndex != -1) {
				        count++;
				        lastIndex += findStr.length();
				    }
				}
			}
			if (count > InteractiveChat.maxPlacholders) {
				event.setCancelled(true);
				String cancelmessage = ChatColorUtils.translateAlternateColorCodes('&', PlaceholderAPI.setPlaceholders(player, InteractiveChat.limitReachMessage));
				player.sendMessage(cancelmessage);
				return;
			}
		}
		
		message = MessageUtils.preprocessMessage(message, InteractiveChat.placeholderList, InteractiveChat.aliasesMapping);
		event.setMessage(message);
		
		String mapKey = ChatColorUtils.stripColor(ChatColorUtils.translateAlternateColorCodes('&', event.getMessage()));
		InteractiveChat.messages.put(mapKey, player.getUniqueId());
		Bukkit.getScheduler().runTaskLater(InteractiveChat.plugin, () -> InteractiveChat.messages.remove(mapKey), 60);
		
		if (InteractiveChat.bungeecordMode) {
			try {
				BungeeMessageSender.addMessage(ChatColorUtils.stripColor(ChatColorUtils.translateAlternateColorCodes('&', event.getMessage())), event.getPlayer().getUniqueId());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
    private void checkMention(AsyncPlayerChatEvent event) {
		String message = event.getMessage();		
		Player sender = event.getPlayer();
		if (InteractiveChat.AllowMention == true && sender.hasPermission("interactivechat.mention.player") && !InteractiveChat.playerDataManager.getPlayerData(sender).isMentionDisabled()) {
			Map<String, UUID> playernames = new HashMap<>();
			for (Player player : Bukkit.getOnlinePlayers()) {
    			playernames.put(ChatColorUtils.stripColor(player.getName()), player.getUniqueId());
    			if (!player.getName().equals(player.getDisplayName())) {
    				playernames.put(ChatColorUtils.stripColor(player.getDisplayName()), player.getUniqueId());
    			}
    			if (InteractiveChat.EssentialsHook) {
    				if (InteractiveChat.essenNick.containsKey(player)) {
    					playernames.put(ChatColorUtils.stripColor(InteractiveChat.essenNick.get(player)), player.getUniqueId());
    				}
    			}
			}
			for (Entry<UUID, PlayerWrapper> entry : InteractiveChat.remotePlayers.entrySet()) {
				playernames.put(ChatColorUtils.stripColor(entry.getValue().getName()), entry.getKey());
			}
			for (Entry<String, UUID> entry : playernames.entrySet()) {
				String name = entry.getKey();
				UUID uuid = entry.getValue();
   				int index = message.toLowerCase().indexOf(name.toLowerCase());
   				if (index >= 0) {
   					char before = (index - 1) < 0 ? ' ' : message.charAt(index - 1);
   					char after = (index + name.length()) >= message.length() ? ' ' : message.charAt(index + name.length());
   					if (String.valueOf(before).matches("[^a-zA-Z0-9]") && String.valueOf(after).matches("[^a-zA-Z0-9]")) {
   						if (!uuid.equals(sender.getUniqueId())) {
   							InteractiveChat.mentionPair.put(uuid, new MentionPair(sender.getUniqueId(), uuid, InteractiveChat.mentionPair));
   							if (InteractiveChat.bungeecordMode) {
   								try {
									BungeeMessageSender.forwardMentionPair(sender.getUniqueId(), uuid);
								} catch (IOException e) {
									e.printStackTrace();
								}
   							}
   						}
   						break;
   					}
   				}
			}
		}
	}
    
	private void translateAltColorCode(AsyncPlayerChatEvent event) {
		if (event.isCancelled()) {
			return;
		}
		
		if (!InteractiveChat.chatAltColorCode.isPresent()) {
			return;
		}
		
		Player player = event.getPlayer();
		
		if (!player.hasPermission("interactivechat.chatcolor.translate")) {
			return;
		}
		
		String message = ChatColorUtils.translateAlternateColorCodes(InteractiveChat.chatAltColorCode.get(), event.getMessage());
		event.setMessage(message);
		//Bukkit.getConsoleSender().sendMessage(message.replace(ChatColor.COLOR_CHAR, '&'));
	}
	
	private void translateAltColorCode(PlayerCommandPreprocessEvent event) {
		if (event.isCancelled()) {
			return;
		}
		
		if (!InteractiveChat.chatAltColorCode.isPresent()) {
			return;
		}
		
		Player player = event.getPlayer();
		
		if (!player.hasPermission("interactivechat.chatcolor.translate")) {
			return;
		}
		
		String message = ChatColorUtils.translateAlternateColorCodes(InteractiveChat.chatAltColorCode.get(), event.getMessage());
		event.setMessage(message);
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getClickedInventory() == null) {
			return;
		}
		if (event.getClickedInventory().getType().equals(InventoryType.CREATIVE)) {
			return;
		}
		if (event.getView().getTopInventory() == null) {
			return;
		}
		boolean block = false;
		if (!block && InteractiveChat.inventoryDisplay.inverse().containsKey(event.getView().getTopInventory())) {
			event.setCancelled(true);
			block = true;
		}
		if (!block && InteractiveChat.enderDisplay.inverse().containsKey(event.getView().getTopInventory())) {
			event.setCancelled(true);
			block = true;
		}
		if (!block && InteractiveChat.itemDisplay.inverse().containsKey(event.getView().getTopInventory())) {
			event.setCancelled(true);
			block = true;
		}
		if (block && event.getRawSlot() < event.getView().getTopInventory().getSize()) {
			if (event.getCurrentItem() != null) {
				XMaterial xmaterial = XMaterial.matchXMaterial(event.getCurrentItem());
				if (xmaterial.equals(XMaterial.WRITTEN_BOOK)) {
					((Player) event.getWhoClicked()).openBook(event.getCurrentItem().clone());
				} else if (xmaterial.equals(XMaterial.WRITABLE_BOOK)) {
					ItemStack book = XMaterial.WRITTEN_BOOK.parseItem();
					if (book != null && book.getItemMeta() instanceof BookMeta) { 
						BookMeta ori = (BookMeta) event.getCurrentItem().getItemMeta();
						BookMeta dis = (BookMeta) book.getItemMeta();
						List<BaseComponent[]> pages = new ArrayList<>(ori.spigot().getPages());
						dis.spigot().setPages(pages);
						dis.setTitle("Temp Book");
						dis.setAuthor("InteractiveChat");
						book.setItemMeta(dis);
						((Player) event.getWhoClicked()).openBook(book);
					}
				}
			}
		}
 	}
}
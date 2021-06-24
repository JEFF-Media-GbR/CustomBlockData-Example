package de.jeff_media.customblockdata;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class ExamplePlugin extends JavaPlugin implements Listener {

    private NamespacedKey storedItemKey;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        storedItemKey = new NamespacedKey(this, "storeditem");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void storeItemInBlock(final PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        final ItemStack itemStack = event.getPlayer().getInventory().getItemInMainHand();
        if (itemStack.getAmount() == 0) return;

        final Block block = event.getClickedBlock();
        assert block != null;

        // Get the PersistentDataContainer of this block
        final PersistentDataContainer customBlockData = new CustomBlockData(block, this);

        // Encode the item as byte array
        byte[] encodedItem;
        try {
            encodedItem = ItemSerializer.toBytes(itemStack);
        } catch (IOException e) {
            Bukkit.getLogger().severe("Could not convert ItemStack to byte array:");
            e.printStackTrace();
            return;
        }

        // Store the value in the block
        customBlockData.set(storedItemKey, PersistentDataType.BYTE_ARRAY, encodedItem);

        event.getPlayer().getInventory().setItemInMainHand(null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void dropStoredItemFromBlock(final BlockBreakEvent event) {
        final PersistentDataContainer customBlockData = new CustomBlockData(event.getBlock(), this);
        if (!customBlockData.has(storedItemKey, PersistentDataType.BYTE_ARRAY)) return;
        try {
            final ItemStack itemStack = ItemSerializer.fromBytes(customBlockData.get(storedItemKey, PersistentDataType.BYTE_ARRAY));
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), itemStack);
        } catch (IOException | ClassNotFoundException e) {
            Bukkit.getLogger().severe("Could not deserialize ItemStack from byte array:");
            e.printStackTrace();
        }
        customBlockData.remove(storedItemKey);
    }

}

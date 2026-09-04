package com.mira.kits.service;

import com.earth2me.essentials.Kit;
import com.earth2me.essentials.MetaItemStack;
import com.earth2me.essentials.User;
import com.mira.kits.MiraKitsPlugin;
import com.mira.kits.model.AdminEditSession;
import com.mira.kits.model.ClaimResult;
import com.mira.kits.model.KitMeta;
import com.mira.kits.util.KitText;
import net.ess3.api.IEssentials;
import net.ess3.provider.SerializationProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class EssentialsKitService {
    private final MiraKitsPlugin plugin;
    private final IEssentials essentials;
    private final KitMetadataStore metadata;
    private final KitWindowService windows;
    private final Set<UUID> internalClaims = ConcurrentHashMap.newKeySet();

    public EssentialsKitService(MiraKitsPlugin plugin, IEssentials essentials, KitMetadataStore metadata, KitWindowService windows) {
        this.plugin = plugin;
        this.essentials = essentials;
        this.metadata = metadata;
        this.windows = windows;
    }

    public IEssentials essentials() { return essentials; }
    public KitWindowService windows() { return windows; }

    public Set<String> kitIds() {
        List<String> sorted = new ArrayList<>(essentials.getKits().getKitKeys());
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        return new LinkedHashSet<>(sorted);
    }

    public boolean exists(String id) { return id != null && essentials.getKits().matchKit(id) != null; }

    public String match(String input) {
        if (input == null) return null;
        String direct = essentials.getKits().matchKit(input);
        if (direct != null) return direct;
        return essentials.getKits().matchKit(KitText.normalizeId(input));
    }

    public KitMeta meta(String id) { return metadata.get(id); }

    public String currencySymbol() {
        String symbol = essentials.getSettings().getCurrencySymbol();
        return symbol == null || symbol.isBlank() ? "$" : symbol;
    }

    public long delaySeconds(String id) {
        Map<String, Object> raw = essentials.getKits().getKit(id);
        if (raw == null) return 0L;
        Object delay = raw.get("delay");
        if (delay instanceof Number number) return number.longValue();
        try { return delay == null ? 0L : Long.parseLong(delay.toString()); }
        catch (NumberFormatException ex) { return 0L; }
    }

    public long nextUse(Player player, String id) {
        try { return new Kit(id, essentials).getNextUse(essentials.getUser(player)); }
        catch (Exception ex) { return 0L; }
    }

    public boolean hasKitPermission(Player player, String id) {
        try { return essentials.getUser(player).isAuthorized("essentials.kits." + id.toLowerCase(Locale.ROOT)); }
        catch (RuntimeException ex) { return false; }
    }

    public List<ItemStack> previewItems(String id) { return previewSlots(id).values().stream().map(ItemStack::clone).toList(); }

    public Map<Integer, ItemStack> previewSlots(String id) {
        ParsedKit parsed = parseKit(id);
        Map<Integer, ItemStack> copy = new LinkedHashMap<>();
        parsed.items().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> copy.put(entry.getKey(), entry.getValue().clone()));
        return copy;
    }

    public List<String> previewAdditionalLines(String id) { return List.copyOf(parseKit(id).passthrough()); }

    public AdminEditSession loadDraft(String id) {
        KitMeta meta = meta(id);
        AdminEditSession session = new AdminEditSession(id, false, meta.displayName(), delaySeconds(id), meta.price(), meta.visible(), meta.enabled());
        ParsedKit parsed = parseKit(id);
        parsed.items().forEach(session::putItem);
        session.passthroughLines().addAll(parsed.passthrough());
        return session;
    }

    public AdminEditSession newDraft(String displayName) {
        String id = KitText.normalizeId(displayName);
        return new AdminEditSession(id, true, displayName.trim(), 0L, BigDecimal.ZERO, true, true);
    }

    public void copyPlayerInventory(Player player, AdminEditSession session) {
        session.clearItems();
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length && slot < 45; slot++) {
            ItemStack item = contents[slot];
            if (item != null && !item.getType().isAir()) session.putItem(slot, item);
        }
    }

    public boolean saveDraft(AdminEditSession session) {
        if (session.items().isEmpty() && session.passthroughLines().isEmpty()) return false;
        List<String> lines = serialize(session);
        if (!session.newKit() && exists(session.id())) essentials.getKits().removeKit(session.id());
        essentials.getKits().addKit(session.id(), lines, session.delaySeconds());
        metadata.save(new KitMeta(session.id(), session.displayName(), session.price(), session.visible(), session.enabled()));
        return true;
    }

    public void deleteKit(String id) { essentials.getKits().removeKit(id); metadata.delete(id); }

    public void reloadAll() { essentials.getKits().reloadConfig(); metadata.reload(); }

    public ClaimResult claim(Player player, String id) {
        String matched = match(id);
        if (matched == null) return ClaimResult.NOT_FOUND;
        KitMeta meta = meta(matched);
        if (!meta.enabled() && !player.hasPermission("mirakits.admin")) return ClaimResult.DISABLED;
        if (!windows.active(matched) && !player.hasPermission("mirakits.admin")) return ClaimResult.DISABLED;

        try {
            User user = essentials.getUser(player);
            Kit kit = new Kit(matched, essentials);
            if (!user.isAuthorized("essentials.kits." + matched.toLowerCase(Locale.ROOT))) return ClaimResult.NO_PERMISSION;
            long nextUse = kit.getNextUse(user);
            if (nextUse != 0L) return ClaimResult.COOLDOWN;
            BigDecimal price = meta.price().max(BigDecimal.ZERO);
            if (price.signum() > 0 && !user.canAfford(price)) return ClaimResult.INSUFFICIENT_FUNDS;

            internalClaims.add(player.getUniqueId());
            boolean expanded;
            try { expanded = kit.expandItems(user); }
            finally { internalClaims.remove(player.getUniqueId()); }
            if (!expanded) return ClaimResult.INVENTORY_FULL_OR_CANCELLED;

            kit.setTime(user);
            if (price.signum() > 0) user.takeMoney(price);
            return ClaimResult.SUCCESS;
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Could not claim Essentials kit " + matched + " for " + player.getName(), ex);
            return ClaimResult.ERROR;
        }
    }

    public boolean isInternalClaim(UUID playerId) { return internalClaims.contains(playerId); }

    private ParsedKit parseKit(String id) {
        Map<Integer, ItemStack> items = new LinkedHashMap<>();
        List<String> passthrough = new ArrayList<>();
        try {
            Kit kit = new Kit(id, essentials);
            for (String line : kit.getItems()) {
                ParsedLine parsed = parseLine(line);
                if (parsed.item() == null) { passthrough.add(line); continue; }
                int slot = parsed.slot();
                if (slot < 0 || slot >= 45 || items.containsKey(slot)) slot = firstFree(items);
                if (slot < 0 || slot >= 45) passthrough.add(line); else items.put(slot, parsed.item());
            }
        } catch (Exception ex) { plugin.getLogger().log(Level.WARNING, "Could not read Essentials kit " + id, ex); }
        return new ParsedKit(items, passthrough);
    }

    private ParsedLine parseLine(String original) {
        if (original == null || original.isBlank()) return new ParsedLine(-1, null);
        String payload = original.trim();
        int slot = -1;
        if (payload.startsWith("slot:")) {
            int space = payload.indexOf(' ');
            if (space <= 5) return new ParsedLine(-1, null);
            try { slot = Integer.parseInt(payload.substring(5, space)); }
            catch (NumberFormatException ex) { return new ParsedLine(-1, null); }
            payload = payload.substring(space + 1).trim();
        }
        String currency = currencySymbol();
        if (payload.startsWith("/") || payload.startsWith("$") || (!currency.isBlank() && payload.startsWith(currency))) return new ParsedLine(slot, null);
        try {
            SerializationProvider provider = essentials.provider(SerializationProvider.class);
            if (payload.startsWith("@")) {
                if (provider == null) return new ParsedLine(slot, null);
                byte[] bytes = Base64.getMimeDecoder().decode(payload.substring(1));
                return new ParsedLine(slot, provider.deserializeItem(bytes));
            }
            String[] parts = payload.split(" +");
            if (parts.length == 0) return new ParsedLine(slot, null);
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            ItemStack stack = essentials.getItemDb().get(parts[0], amount);
            if (stack == null || stack.getType() == Material.AIR) return new ParsedLine(slot, null);
            MetaItemStack metaStack = new MetaItemStack(stack);
            if (parts.length > 2) metaStack.parseStringMeta(null, essentials.getSettings().allowUnsafeEnchantments(), parts, 2, essentials);
            return new ParsedLine(slot, metaStack.getItemStack());
        } catch (Exception ex) {
            plugin.getLogger().log(Level.FINE, "Could not parse kit line: " + original, ex);
            return new ParsedLine(slot, null);
        }
    }

    private List<String> serialize(AdminEditSession session) {
        List<Map.Entry<Integer, ItemStack>> sorted = new ArrayList<>(session.items().entrySet());
        sorted.sort(Comparator.comparingInt(Map.Entry::getKey));
        List<String> lines = new ArrayList<>();
        SerializationProvider provider = essentials.provider(SerializationProvider.class);
        boolean better = essentials.getSettings().isUseBetterKits() && provider != null;
        for (Map.Entry<Integer, ItemStack> entry : sorted) {
            ItemStack item = entry.getValue();
            try {
                String itemLine;
                if (better) itemLine = "@" + Base64.getMimeEncoder().encodeToString(provider.serializeItem(item));
                else itemLine = essentials.getItemDb().serialize(item);
                lines.add("slot:" + entry.getKey() + " " + itemLine);
            } catch (Exception ex) { plugin.getLogger().log(Level.WARNING, "Could not serialize kit item in slot " + entry.getKey(), ex); }
        }
        lines.addAll(session.passthroughLines());
        return lines;
    }

    private int firstFree(Map<Integer, ItemStack> items) {
        for (int slot = 0; slot < 45; slot++) if (!items.containsKey(slot)) return slot;
        return -1;
    }

    private record ParsedKit(Map<Integer, ItemStack> items, List<String> passthrough) { }
    private record ParsedLine(int slot, ItemStack item) { }
}

package com.hexzeug.forceitemchallenge.display;

import com.hexzeug.forceitemchallenge.persistence.ForceItemChallengeState;
import com.hexzeug.forceitemchallenge.persistence.HistoryEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class ResultsDisplay implements NamedScreenHandlerFactory, Inventory {
    private final ServerPlayerEntity player;
    private final List<HistoryEntry> history;

    public ResultsDisplay(ServerPlayerEntity player) {
        this.player = player;
        MinecraftServer server = Objects.requireNonNull(player.getServer());
        ForceItemChallengeState state = ForceItemChallengeState.ofServer(server);
        this.history = state.getOrCreatePlayerState(player.getUuid()).getHistory().stream().toList();
    }

    public ServerPlayerEntity getPlayer() {
        return player;
    }

    public Score getScore() {
        int skips = 0;
        int completions = 0;
        for (HistoryEntry entry : history) {
            switch (entry.type()) {
                case COMPLETED -> completions++;
                case SKIPPED -> skips++;
            }
        }
        return new Score(completions - skips, skips, completions);
    }

    @Override
    public Text getDisplayName() {
        Score score = getScore();
        return Text.empty()
                .append(player.getName().copy().formatted(Formatting.BOLD))
                .append(": ")
                .append(Text.literal(Integer.toString(score.total())).formatted(Formatting.BLUE, Formatting.BOLD))
                .append(" (")
                .append(Text.literal(Integer.toString(score.completions())).formatted(Formatting.BLUE))
                .append("c/")
                .append(Text.literal(Integer.toString(score.skips())).formatted(Formatting.BLUE))
                .append("s)")
                .formatted(Formatting.DARK_AQUA);
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return GenericContainerScreenHandler.createGeneric9x6(syncId, playerInventory, this);
    }

    @Override
    public int size() {
        return 9 * 6;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot >= history.size()) return ItemStack.EMPTY;
        HistoryEntry entry = history.get(slot);
        ItemStack stack = entry.item().copy();
        if (entry.type() == HistoryEntry.Type.SKIPPED) {
            stack.set(
                    DataComponentTypes.CUSTOM_NAME,
                    Text.literal("Skipped: ").append(stack.getName())
            );
        }
        return stack;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {}

    @Override
    public void markDirty() {}

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {}

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return false;
    }

    public record Score(int total, int skips, int completions) {}
}

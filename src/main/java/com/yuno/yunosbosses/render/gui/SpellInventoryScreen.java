package com.yuno.yunosbosses.render.gui;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.network.EquipSpellPayload;
import com.yuno.yunosbosses.spell.Spell;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class SpellInventoryScreen extends Screen {
    // Standard GUI size
    private final int guiWidth = 176;
    private final int guiHeight = 166;
    private int guiLeft;
    private int guiTop;

    // Selection state
    private Spell selectedSpell = null;

    public SpellInventoryScreen() {
        super(Text.literal("Spell Inventory"));
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.guiWidth) / 2;
        this.guiTop = (this.height - this.guiHeight) / 2;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Call super first so vanilla background rendering finishes before drawing custom UI
        super.render(context, mouseX, mouseY, delta);

        if (this.client == null || this.client.player == null) return;
        var component = ModEntityComponents.SPELL_DATA.get(this.client.player);

        // Draw GUI Background panel
        context.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + guiHeight, 0xFF1E1E2E); // Dark blue/gray background
        context.drawBorder(guiLeft, guiTop, guiWidth, guiHeight, 0xFF45475A); // Border

        // Title Text
        context.drawText(this.textRenderer, "SPELL INVENTORY", guiLeft + 12, guiTop + 10, 0xFFCDD6F4, false);

        // Draw Known Spells Grid & Equipped Sidebar
        drawKnownSpellsGrid(context, component.getKnownSpells(), mouseX, mouseY);
        drawEquippedSidebar(context, component, mouseX, mouseY);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw a standard dark overlay
        context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    private void drawKnownSpellsGrid(DrawContext context, List<Spell> knownSpells, int mouseX, int mouseY) {
        int gridX = guiLeft + 45;
        int gridY = guiTop + 30;
        int slotSize = 20;

        for (int i = 0; i < knownSpells.size(); i++) {
            Spell spell = knownSpells.get(i);
            int col = i % 6;
            int row = i / 6;
            int x = gridX + (col * (slotSize + 2));
            int y = gridY + (row * (slotSize + 2));

            // Slot Background
            context.fill(x, y, x + slotSize, y + slotSize, 0xFF313244);

            // Draw full 32x32 PNG scaled down to fit 16x16 on screen
            context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    spell.getIconTexture(),
                    x + 2, y + 2,     // Target screen X, Y
                    0.0F, 0.0F,       // Source U, V start
                    16, 16,           // Target width, height on screen
                    32, 32,           // Source region width, height to sample (Full 32x32 image)
                    32, 32            // Total file texture width, height
            );

            // Highlight Selected Spell
            if (this.selectedSpell == spell) {
                context.drawBorder(x - 1, y - 1, slotSize + 2, slotSize + 2, spell.getRarity().getColorHex());
            }

            // Mouse Hover Effect & Tooltip
            if (isHovering(x, y, slotSize, slotSize, mouseX, mouseY)) {
                context.fill(x, y, x + slotSize, y + slotSize, 0x40FFFFFF);

                // Colored name + rarity subtext
                List<Text> tooltip = List.of(
                        Text.literal(spell.getId().getPath().replace('_', ' ').toUpperCase())
                                .formatted(spell.getRarity().getFormatting()),
                        Text.literal(spell.getRarity().getName() + " Spell")
                                .formatted(Formatting.DARK_GRAY)
                );

                context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
            }
        }
    }

    private void drawEquippedSidebar(DrawContext context, com.yuno.yunosbosses.component.SpellComponent component, int mouseX, int mouseY) {
        int sidebarX = guiLeft + 10;
        int sidebarY = guiTop + 30;
        int slotSize = 24;

        for (int i = 0; i < component.getMaxSpellSlots(); i++) {
            int y = sidebarY + (i * (slotSize + 4));
            Spell equipped = component.getEquippedSpell(i);

            // Slot Box
            context.fill(sidebarX, y, sidebarX + slotSize, y + slotSize, 0xFF181825);

            // Draw dynamic Border
            int borderColor = (equipped != null) ? equipped.getRarity().getColorHex() : 0xFF585B70;
            context.drawBorder(sidebarX, y, slotSize, slotSize, borderColor);

            // Draw Slot Number Label
            context.drawText(this.textRenderer, String.valueOf(i + 1), sidebarX - 8, y + 8, 0xFF7F849C, false);

            // Draw full 32x32 PNG scaled down to fit 16x16 inside 24x24 slot
            if (equipped != null) {
                context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        equipped.getIconTexture(),
                        sidebarX + 4, y + 4, // Target screen X, Y
                        0.0F, 0.0F,         // Source U, V start
                        16, 16,             // Target width, height on screen
                        32, 32,             // Source region width, height to sample
                        32, 32              // Total file texture width, height
                );
            }

            // Hover / Target Highlight
            if (isHovering(sidebarX, y, slotSize, slotSize, mouseX, mouseY)) {
                if (this.selectedSpell != null) {
                    context.fill(sidebarX, y, sidebarX + slotSize, y + slotSize, 0x60A6E3A1); // Green preview
                } else {
                    context.fill(sidebarX, y, sidebarX + slotSize, y + slotSize, 0x40FFFFFF);
                }

                // Color-coded tooltip
                if (equipped != null) {
                    List<Text> tooltip = List.of(
                            Text.literal("Slot " + (i + 1) + ": " + equipped.getId().getPath().replace('_', ' ').toUpperCase())
                                    .formatted(equipped.getRarity().getFormatting()),
                            Text.literal(equipped.getRarity().getName() + " Spell")
                                    .formatted(Formatting.GRAY)
                    );
                    context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
                } else {
                    context.drawTooltip(this.textRenderer, Text.literal("Slot " + (i + 1) + ": Empty").formatted(Formatting.DARK_GRAY), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.client != null && this.client.player != null) { // Left Click
            var component = ModEntityComponents.SPELL_DATA.get(this.client.player);
            List<Spell> knownSpells = component.getKnownSpells();

            // Check if the user clicked a Known Spell
            int gridX = guiLeft + 45;
            int gridY = guiTop + 30;
            int slotSize = 20;

            for (int i = 0; i < knownSpells.size(); i++) {
                int col = i % 6;
                int row = i / 6;
                int x = gridX + (col * (slotSize + 2));
                int y = gridY + (row * (slotSize + 2));

                if (isHovering(x, y, slotSize, slotSize, (int) mouseX, (int) mouseY)) {
                    this.selectedSpell = knownSpells.get(i);
                    return true;
                }
            }

            // Check if the user clicked an Equipped Slot
            int sidebarX = guiLeft + 10;
            int sidebarY = guiTop + 30;
            int equipSlotSize = 24;

            for (int i = 0; i < component.getMaxSpellSlots(); i++) {
                int y = sidebarY + (i * (equipSlotSize + 4));

                if (isHovering(sidebarX, y, equipSlotSize, equipSlotSize, (int) mouseX, (int) mouseY)) {
                    if (this.selectedSpell != null) {
                        // Send a packet to Server to equip the selected spell in this slot
                        ClientPlayNetworking.send(new EquipSpellPayload(i, this.selectedSpell.getId().toString()));

                        // Clear selection after equipping
                        this.selectedSpell = null;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false; // Don't pause singleplayer game when menu is open
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}

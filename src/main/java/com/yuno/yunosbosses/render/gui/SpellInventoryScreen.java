package com.yuno.yunosbosses.render.gui;

import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.event.ModKeybindings;
import com.yuno.yunosbosses.network.EquipSpellPayload;
import com.yuno.yunosbosses.spell.Spell;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
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
        int startX = guiLeft + 45;
        int startY = guiTop + 30;
        int slotSize = 20;

        for (int i = 0; i < knownSpells.size(); i++) {
            Spell spell = knownSpells.get(i);
            int col = i % 6;
            int row = i / 6;
            int x = startX + (col * (slotSize + 2));
            int y = startY + (row * (slotSize + 2));

            // Slot Background
            context.fill(x, y, x + slotSize, y + slotSize, 0xFF181825);
            context.drawBorder(x, y, slotSize, slotSize, 0xFF313244);

            // Draw Spell Icon (Sample full 32x32 texture and scale down to 16x16 inside 20x20 slot)
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

                // Colored name + optional Innate Technique tag + rarity subtext
                List<Text> tooltip = new ArrayList<>();
                tooltip.add(spell.getName().copy().formatted(spell.getRarity().getFormatting()));

                if (spell.isInnateTechnique()) {
                    tooltip.add(Text.literal("✦ Innate Technique").formatted(Formatting.GOLD, Formatting.ITALIC));
                }

                tooltip.add(Text.literal(spell.getRarity().getName() + " Spell")
                        .formatted(Formatting.DARK_GRAY));

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
            boolean isInnateSlot = (i == 0);

            // Base Slot Box
            context.fill(sidebarX, y, sidebarX + slotSize, y + slotSize, 0xFF181825);

            // Border styling: Special Gold for Innate Slot, Standard for others
            if (isInnateSlot) {
                context.drawBorder(sidebarX, y, slotSize, slotSize, 0xFFF9E2AF); // Innate slot gold accent
            } else {
                context.drawBorder(sidebarX, y, slotSize, slotSize, 0xFF313244);
            }

            // Draw Equipped Icon
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
                    if (this.selectedSpell.isInnateTechnique() && !isInnateSlot) {
                        context.fill(sidebarX, y, sidebarX + slotSize, y + slotSize, 0x60F38BA8); // Red preview: Invalid slot for Innate Technique
                    } else {
                        context.fill(sidebarX, y, sidebarX + slotSize, y + slotSize, 0x60A6E3A1); // Green preview: Valid slot
                    }
                } else {
                    context.fill(sidebarX, y, sidebarX + slotSize, y + slotSize, 0x40FFFFFF);
                }

                // Tooltip construction
                List<Text> tooltip = new ArrayList<>();
                if (isInnateSlot) {
                    if (equipped != null) {
                        tooltip.add(Text.literal("Innate Slot: ").append(equipped.getName().copy().formatted(equipped.getRarity().getFormatting())));
                        if (equipped.isInnateTechnique()) {
                            tooltip.add(Text.literal("✦ Innate Technique").formatted(Formatting.GOLD, Formatting.ITALIC));
                        }
                        tooltip.add(Text.literal(equipped.getRarity().getName() + " Spell")
                                .formatted(Formatting.GRAY));
                        tooltip.add(Text.literal("Right-click to unequip").formatted(Formatting.DARK_GRAY));
                    } else {
                        tooltip.add(Text.literal("Innate Technique Slot: Empty").formatted(Formatting.GOLD));
                        tooltip.add(Text.literal("Can hold any spell (Required for Innate Techniques)").formatted(Formatting.DARK_GRAY));
                    }
                } else {
                    if (equipped != null) {
                        tooltip.add(Text.literal("Slot " + (i + 1) + ": ").append(equipped.getName().copy().formatted(equipped.getRarity().getFormatting())));
                        tooltip.add(Text.literal(equipped.getRarity().getName() + " Spell")
                                .formatted(Formatting.GRAY));
                        tooltip.add(Text.literal("Right-click to unequip").formatted(Formatting.DARK_GRAY));
                    } else {
                        tooltip.add(Text.literal("Slot " + (i + 1) + ": Empty").formatted(Formatting.DARK_GRAY));
                    }

                    if (this.selectedSpell != null && this.selectedSpell.isInnateTechnique()) {
                        tooltip.add(Text.literal("Cannot equip Innate Technique in this slot!").formatted(Formatting.RED));
                    }
                }
                context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.client != null && this.client.player != null) {
            var component = ModEntityComponents.SPELL_DATA.get(this.client.player);
            int sidebarX = guiLeft + 10;
            int sidebarY = guiTop + 30;
            int equipSlotSize = 24;

            // Left Click (button == 0)
            if (button == 0) {
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

                // Check if the user clicked an Equipped Slot with a spell selected
                for (int i = 0; i < component.getMaxSpellSlots(); i++) {
                    int y = sidebarY + (i * (equipSlotSize + 4));

                    if (isHovering(sidebarX, y, equipSlotSize, equipSlotSize, (int) mouseX, (int) mouseY)) {
                        if (this.selectedSpell != null) {
                            // Validate Innate Technique placement (can only be placed in slot 0)
                            if (this.selectedSpell.isInnateTechnique() && i != 0) {
                                return true; // Reject equipping Innate Technique to non-innate slot
                            }

                            // Send a packet to Server to equip the selected spell in this slot
                            ClientPlayNetworking.send(new EquipSpellPayload(i, this.selectedSpell.getId().toString()));

                            // Clear selection after equipping
                            this.selectedSpell = null;
                            return true;
                        }
                    }
                }
            }

            // Right Click (button == 1) on Equipped Slot -> Unequip
            if (button == 1) {
                for (int i = 0; i < component.getMaxSpellSlots(); i++) {
                    int y = sidebarY + (i * (equipSlotSize + 4));

                    if (isHovering(sidebarX, y, equipSlotSize, equipSlotSize, (int) mouseX, (int) mouseY)) {
                        if (component.getEquippedSpell(i) != null) {
                            ClientPlayNetworking.send(new EquipSpellPayload(i, "empty"));
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Close on 'G' (spell inventory key) or 'E' (inventory key)
        if (ModKeybindings.openSpellInventoryKey.matchesKey(keyCode, scanCode)
                || (this.client != null && this.client.options.inventoryKey.matchesKey(keyCode, scanCode))) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false; // Don't pause singleplayer game when menu is open
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}

package com.yuno.yunosbosses.render.gui;

import com.yuno.yunosbosses.binding_vow.BindingVow;
import com.yuno.yunosbosses.binding_vow.ModBindingVows;
import com.yuno.yunosbosses.component.BindingVowComponent;
import com.yuno.yunosbosses.component.ModEntityComponents;
import com.yuno.yunosbosses.event.ModKeybindings;
import com.yuno.yunosbosses.network.EquipSpellPayload;
import com.yuno.yunosbosses.network.ToggleBindingVowPayload;
import com.yuno.yunosbosses.spell.Spell;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class SpellInventoryScreen extends Screen {
    private static final Identifier GUI_TEXTURE = Identifier.of("yunosbosses", "textures/gui/spell_gui.png");

    // Standard GUI size
    private final int guiWidth = 176;
    private final int guiHeight = 166;
    private int guiLeft;
    private int guiTop;

    // Tabs: 0 = Spells, 1 = Binding Vows
    private int activeTab = 0;

    // Selection state
    private Spell selectedSpell = null;

    public SpellInventoryScreen() {
        this(0);
    }

    public SpellInventoryScreen(int initialTab) {
        super(Text.literal("Spell Inventory"));
        this.activeTab = Math.max(0, Math.min(1, initialTab));
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.guiWidth) / 2;
        this.guiTop = (this.height - this.guiHeight) / 2;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (this.client == null || this.client.player == null) return;

        // Draw Tabs at top
        drawTabs(context, mouseX, mouseY);

        // Draw GUI Background panel texture (176 x 166 on 256 x 256 sheet)
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                GUI_TEXTURE,
                guiLeft, guiTop,
                0.0F, 0.0F,
                guiWidth, guiHeight,
                256, 256
        );

        if (activeTab == 0) {
            // Title Text
            context.drawText(this.textRenderer, "SPELL INVENTORY", guiLeft + 12, guiTop + 10, 0xFFE0F8F5, false);

            var component = ModEntityComponents.SPELL_DATA.get(this.client.player);
            // Draw Known Spells Grid & Equipped Sidebar
            drawKnownSpellsGrid(context, component.getKnownSpells(), mouseX, mouseY);
            drawEquippedSidebar(context, component, mouseX, mouseY);
        } else {
            // Binding Vows Tab
            drawBindingVowsTab(context, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    private void drawTabs(DrawContext context, int mouseX, int mouseY) {
        int tabY = guiTop - 16;
        int tabH = 16;

        // Matching color palette from spell_gui.png (top color ~0xFF414265, border 0xFF488885)
        int tabActiveBg = 0xFF414265;
        int tabBorder = 0xFF488885;
        int tabInactiveBorder = 0xFF3A5858;

        // Spells Tab
        int tab0X = guiLeft + 8;
        int tab0W = 56;
        boolean tab0Active = (activeTab == 0);
        int tab0Bg = tab0Active ? tabActiveBg : (isHovering(tab0X, tabY, tab0W, tabH, mouseX, mouseY) ? 0xFF2D3048 : 0xFF202235);
        int tab0BorderCol = tab0Active ? tabBorder : tabInactiveBorder;
        int tab0TextCol = tab0Active ? 0xFFE0F8F5 : 0xFF8892B0;

        context.fill(tab0X, tabY, tab0X + tab0W, tabY + tabH, tab0Bg);
        context.drawBorder(tab0X, tabY, tab0W, tabH, tab0BorderCol);
        context.drawText(this.textRenderer, "Spells", tab0X + 11, tabY + 4, tab0TextCol, false);

        // Binding Vows Tab
        int tab1X = guiLeft + 66;
        int tab1W = 86;
        boolean tab1Active = (activeTab == 1);
        int tab1Bg = tab1Active ? tabActiveBg : (isHovering(tab1X, tabY, tab1W, tabH, mouseX, mouseY) ? 0xFF2D3048 : 0xFF202235);
        int tab1BorderCol = tab1Active ? tabBorder : tabInactiveBorder;
        int tab1TextCol = tab1Active ? 0xFFF9E2AF : 0xFF8892B0;

        context.fill(tab1X, tabY, tab1X + tab1W, tabY + tabH, tab1Bg);
        context.drawBorder(tab1X, tabY, tab1W, tabH, tab1BorderCol);
        context.drawText(this.textRenderer, "Binding Vows", tab1X + 9, tabY + 4, tab1TextCol, false);
    }

    private void drawBindingVowsTab(DrawContext context, int mouseX, int mouseY) {
        if (this.client == null || this.client.player == null) return;
        var player = this.client.player;
        BindingVowComponent vowComponent = ModEntityComponents.BINDING_VOWS.get(player);

        // Title and Subtitle
        context.drawText(this.textRenderer, "BINDING VOWS", guiLeft + 12, guiTop + 10, 0xFFF9E2AF, false);
        context.drawText(this.textRenderer, "Sacred Pacts & Restrictions", guiLeft + 12, guiTop + 21, 0xFF94A3B8, false);

        List<BindingVow> vows = new ArrayList<>(ModBindingVows.getAll());
        int cardX = guiLeft + 10;
        int cardY = guiTop + 33;
        int cardW = guiWidth - 20; // 156
        int cardH = 58;

        // Pending tooltip rendering (render after cards so it's always on top)
        Runnable pendingTooltip = null;

        for (int i = 0; i < vows.size(); i++) {
            BindingVow vow = vows.get(i);
            int y = cardY + (i * (cardH + 6));
            boolean isActive = vowComponent != null && vowComponent.hasVow(vow.getId());
            boolean canAccept = vow.canAccept(player);

            // Card background & border (tinted to complement background)
            int cardBg = isActive ? 0xD0182438 : 0xC0151825;
            int cardBorder = isActive ? 0xFF89B4FA : 0xFF384358;
            context.fill(cardX, y, cardX + cardW, y + cardH, cardBg);
            context.drawBorder(cardX, y, cardW, cardH, cardBorder);

            // Name
            context.drawText(this.textRenderer, vow.getName().getString(), cardX + 6, y + 5, 0xFFCDD6F4, false);

            // Status Badge
            if (isActive) {
                context.drawText(this.textRenderer, "[ACTIVE]", cardX + cardW - 48, y + 5, 0xFFA6E3A1, false);
            } else {
                context.drawText(this.textRenderer, "[SEALED]", cardX + cardW - 50, y + 5, 0xFF94A3B8, false);
            }

            // Sacrifice line
            String sacStr = "Sacrifice: " + vow.getSacrifice().getString();
            if (sacStr.length() > 27) sacStr = sacStr.substring(0, 25) + "..";
            context.drawText(this.textRenderer, sacStr, cardX + 6, y + 17, 0xFFF38BA8, false);

            // Gain line
            String gainStr = "Gain: " + vow.getGain().getString();
            if (gainStr.length() > 27) gainStr = gainStr.substring(0, 25) + "..";
            context.drawText(this.textRenderer, gainStr, cardX + 6, y + 28, 0xFFA6E3A1, false);

            // Action Button
            int btnW = 68;
            int btnH = 14;
            int btnX = cardX + cardW - btnW - 4;
            int btnY = y + cardH - btnH - 4;

            boolean hoveringBtn = isHovering(btnX, btnY, btnW, btnH, mouseX, mouseY);

            if (isActive) {
                int btnBg = hoveringBtn ? 0xFFE78284 : 0xFF582329;
                context.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnBg);
                context.drawBorder(btnX, btnY, btnW, btnH, 0xFFE78284);
                context.drawText(this.textRenderer, "Sever Pact", btnX + 7, btnY + 3, 0xFFFFFFFF, false);
            } else {
                if (canAccept) {
                    int btnBg = hoveringBtn ? 0xFF85C1DC : 0xFF1E3A4B;
                    context.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnBg);
                    context.drawBorder(btnX, btnY, btnW, btnH, 0xFF89DCEB);
                    context.drawText(this.textRenderer, "Pledge Vow", btnX + 5, btnY + 3, 0xFFFFFFFF, false);
                } else {
                    context.fill(btnX, btnY, btnX + btnW, btnY + btnH, 0xFF222436);
                    context.drawBorder(btnX, btnY, btnW, btnH, 0xFF45475A);
                    context.drawText(this.textRenderer, "Unavailable", btnX + 4, btnY + 3, 0xFF7F849C, false);
                }
            }

            // Mana cost label if not active and has cost
            float manaCost = vow.getActivationManaCost(player);
            if (!isActive && manaCost > 0) {
                context.drawText(this.textRenderer, (int) manaCost + " Mana", cardX + 6, y + 42, 0xFF89B4FA, false);
            }

            // Compact Tooltip handling
            if (hoveringBtn) {
                pendingTooltip = () -> {
                    List<Text> btnTooltip = new ArrayList<>();
                    if (isActive) {
                        btnTooltip.add(Text.literal("Sever Pact").formatted(Formatting.RED, Formatting.BOLD));
                        btnTooltip.add(Text.literal("Warning: Inflicts 15s Burnout").formatted(Formatting.GRAY));
                    } else if (canAccept) {
                        btnTooltip.add(Text.literal("Pledge Vow").formatted(Formatting.AQUA, Formatting.BOLD));
                        if (manaCost > 0) {
                            btnTooltip.add(Text.literal("Cost: " + (int) manaCost + " Mana").formatted(Formatting.GRAY));
                        }
                    } else {
                        btnTooltip.add(Text.literal("Unavailable").formatted(Formatting.RED, Formatting.BOLD));
                        btnTooltip.add(vow.getCannotAcceptReason(player).copy().formatted(Formatting.GRAY));
                    }
                    context.drawTooltip(this.textRenderer, btnTooltip, mouseX, mouseY);
                };
            } else if (isHovering(cardX, y, cardW, cardH, mouseX, mouseY)) {
                pendingTooltip = () -> {
                    List<OrderedText> tooltipLines = new ArrayList<>();
                    tooltipLines.add(vow.getName().copy().formatted(Formatting.GOLD, Formatting.BOLD).asOrderedText());
                    // Neatly wrapped to a compact width of 165px
                    tooltipLines.addAll(this.textRenderer.wrapLines(vow.getDescription(), 165));
                    context.drawOrderedTooltip(this.textRenderer, tooltipLines, mouseX, mouseY);
                };
            }
        }

        if (pendingTooltip != null) {
            pendingTooltip.run();
        }
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

            context.fill(x, y, x + slotSize, y + slotSize, 0x90101420);
            context.drawBorder(x, y, slotSize, slotSize, 0xFF384358);

            context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    spell.getIconTexture(),
                    x + 2, y + 2,
                    0.0F, 0.0F,
                    16, 16,
                    32, 32,
                    32, 32
            );

            if (this.selectedSpell == spell) {
                context.drawBorder(x - 1, y - 1, slotSize + 2, slotSize + 2, spell.getRarity().getColorHex());
            }

            if (isHovering(x, y, slotSize, slotSize, mouseX, mouseY)) {
                context.fill(x, y, x + slotSize, y + slotSize, 0x40FFFFFF);

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

            context.fill(sidebarX, y, sidebarX + slotSize, y + slotSize, 0x90101420);

            if (isInnateSlot) {
                context.drawBorder(sidebarX, y, slotSize, slotSize, 0xFFF9E2AF);
            } else {
                context.drawBorder(sidebarX, y, slotSize, slotSize, 0xFF384358);
            }

            if (equipped != null) {
                context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        equipped.getIconTexture(),
                        sidebarX + 4, y + 4,
                        0.0F, 0.0F,
                        16, 16,
                        32, 32,
                        32, 32
                );
            }

            if (isHovering(sidebarX, y, slotSize, slotSize, mouseX, mouseY)) {
                if (this.selectedSpell != null) {
                    if (this.selectedSpell.isInnateTechnique() && !isInnateSlot) {
                        context.fill(sidebarX, y, sidebarX + slotSize, y + slotSize, 0x60F38BA8);
                    } else {
                        context.fill(sidebarX, y, sidebarX + slotSize, y + slotSize, 0x60A6E3A1);
                    }
                } else {
                    context.fill(sidebarX, y, sidebarX + slotSize, y + slotSize, 0x40FFFFFF);
                }

                List<Text> tooltip = new ArrayList<>();
                if (isInnateSlot) {
                    if (equipped != null) {
                        tooltip.add(Text.literal("Innate Slot: ").append(equipped.getName().copy().formatted(equipped.getRarity().getFormatting())));
                        if (equipped.isInnateTechnique()) {
                            tooltip.add(Text.literal("✦ Innate Technique").formatted(Formatting.GOLD, Formatting.ITALIC));
                        }
                        tooltip.add(Text.literal(equipped.getRarity().getName() + " Spell").formatted(Formatting.GRAY));
                        tooltip.add(Text.literal("Right-click to unequip").formatted(Formatting.DARK_GRAY));
                    } else {
                        tooltip.add(Text.literal("Innate Technique Slot: Empty").formatted(Formatting.GOLD));
                        tooltip.add(Text.literal("Can hold any spell (Required for Innate Techniques)").formatted(Formatting.DARK_GRAY));
                    }
                } else {
                    if (equipped != null) {
                        tooltip.add(Text.literal("Slot " + (i + 1) + ": ").append(equipped.getName().copy().formatted(equipped.getRarity().getFormatting())));
                        tooltip.add(Text.literal(equipped.getRarity().getName() + " Spell").formatted(Formatting.GRAY));
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
            int tabY = guiTop - 16;
            int tabH = 16;

            // Check Tab clicks
            if (button == 0) {
                // Spells tab
                if (isHovering(guiLeft + 8, tabY, 56, tabH, (int) mouseX, (int) mouseY)) {
                    this.activeTab = 0;
                    return true;
                }
                // Binding Vows tab
                if (isHovering(guiLeft + 66, tabY, 86, tabH, (int) mouseX, (int) mouseY)) {
                    this.activeTab = 1;
                    return true;
                }
            }

            if (activeTab == 0) {
                var component = ModEntityComponents.SPELL_DATA.get(this.client.player);
                int sidebarX = guiLeft + 10;
                int sidebarY = guiTop + 30;
                int equipSlotSize = 24;

                // Left Click on Spells
                if (button == 0) {
                    List<Spell> knownSpells = component.getKnownSpells();

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

                    for (int i = 0; i < component.getMaxSpellSlots(); i++) {
                        int y = sidebarY + (i * (equipSlotSize + 4));

                        if (isHovering(sidebarX, y, equipSlotSize, equipSlotSize, (int) mouseX, (int) mouseY)) {
                            if (this.selectedSpell != null) {
                                if (this.selectedSpell.isInnateTechnique() && i != 0) {
                                    return true;
                                }
                                ClientPlayNetworking.send(new EquipSpellPayload(i, this.selectedSpell.getId().toString()));
                                this.selectedSpell = null;
                                return true;
                            }
                        }
                    }
                }

                // Right Click on Equipped Slot -> Unequip
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
            } else if (activeTab == 1) {
                // Binding Vows Tab clicks
                if (button == 0) {
                    List<BindingVow> vows = new ArrayList<>(ModBindingVows.getAll());
                    int cardX = guiLeft + 10;
                    int cardY = guiTop + 33;
                    int cardW = guiWidth - 20;
                    int cardH = 58;

                    for (int i = 0; i < vows.size(); i++) {
                        BindingVow vow = vows.get(i);
                        int y = cardY + (i * (cardH + 6));

                        int btnW = 68;
                        int btnH = 14;
                        int btnX = cardX + cardW - btnW - 4;
                        int btnY = y + cardH - btnH - 4;

                        if (isHovering(btnX, btnY, btnW, btnH, (int) mouseX, (int) mouseY)) {
                            ClientPlayNetworking.send(new ToggleBindingVowPayload(vow.getId().toString()));
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
        if (ModKeybindings.openSpellInventoryKey.matchesKey(keyCode, scanCode)
                || (this.client != null && this.client.options.inventoryKey.matchesKey(keyCode, scanCode))) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}

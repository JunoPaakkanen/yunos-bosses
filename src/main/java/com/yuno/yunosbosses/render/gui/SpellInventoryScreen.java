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

    // Binding Vows list layout & scroll state
    private static final int VOW_LIST_TOP_OFFSET = 24;
    private static final int VOW_LIST_BOTTOM_OFFSET = 158;
    private static final int VOW_CARD_HEIGHT = 30;
    private static final int VOW_CARD_GAP = 4;
    private float vowScrollOffset = 0.0F;

    // Confirmation Modal for Permanent Binding Vows
    private BindingVow pendingPermanentVow = null;

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

        // Render Confirmation Dialog modal overlay if active
        if (this.pendingPermanentVow != null) {
            drawPermanentConfirmationModal(context, mouseX, mouseY);
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

    private int getMaxVowScroll(int vowCount) {
        int listHeight = VOW_LIST_BOTTOM_OFFSET - VOW_LIST_TOP_OFFSET;
        int contentHeight = vowCount * VOW_CARD_HEIGHT + Math.max(0, vowCount - 1) * VOW_CARD_GAP;
        return Math.max(0, contentHeight - listHeight);
    }

    private String trimTextToWidth(String text, int maxWidth) {
        if (this.textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int available = Math.max(0, maxWidth - this.textRenderer.getWidth(ellipsis));
        return this.textRenderer.trimToWidth(text, available) + ellipsis;
    }

    private void drawBindingVowsTab(DrawContext context, int mouseX, int mouseY) {
        if (this.client == null || this.client.player == null) return;
        var player = this.client.player;
        BindingVowComponent vowComponent = ModEntityComponents.BINDING_VOWS.get(player);

        List<BindingVow> vows = new ArrayList<>(ModBindingVows.getAll());

        // Count active vows for a clean header summary
        int activeCount = 0;
        if (vowComponent != null) {
            for (BindingVow v : vows) {
                if (vowComponent.hasVow(v.getId())) activeCount++;
            }
        }

        // Title and right-aligned active counter
        context.drawText(this.textRenderer, "BINDING VOWS", guiLeft + 12, guiTop + 10, 0xFFF9E2AF, false);
        String countText = activeCount + "/" + vows.size() + " Active";
        int countWidth = this.textRenderer.getWidth(countText);
        context.drawText(this.textRenderer, countText, guiLeft + guiWidth - 10 - countWidth, guiTop + 10, 0xFF8892B0, false);

        int listTop = guiTop + VOW_LIST_TOP_OFFSET;
        int listBottom = guiTop + VOW_LIST_BOTTOM_OFFSET;
        int listHeight = listBottom - listTop;

        int maxScroll = getMaxVowScroll(vows.size());
        this.vowScrollOffset = Math.max(0.0F, Math.min(this.vowScrollOffset, maxScroll));
        int scrollY = Math.round(this.vowScrollOffset);

        int cardX = guiLeft + 10;
        int cardW = (maxScroll > 0) ? (guiWidth - 25) : (guiWidth - 20);
        int cardH = VOW_CARD_HEIGHT;

        // Pending tooltip rendering (render after scissor is disabled so it is never clipped)
        Runnable pendingTooltip = null;
        boolean mouseInListArea = isHovering(guiLeft + 8, listTop, guiWidth - 16, listHeight, mouseX, mouseY);

        context.enableScissor(guiLeft + 8, listTop, guiLeft + guiWidth - 8, listBottom);

        for (int i = 0; i < vows.size(); i++) {
            BindingVow vow = vows.get(i);
            int y = listTop + (i * (cardH + VOW_CARD_GAP)) - scrollY;

            // Skip cards completely outside the visible list region
            if (y + cardH < listTop || y > listBottom) {
                continue;
            }

            boolean isActive = vowComponent != null && vowComponent.hasVow(vow.getId());
            boolean canAccept = vow.canAccept(player);
            boolean isPermanent = vow.isPermanent();
            boolean hoveringCard = mouseInListArea && isHovering(cardX, y, cardW, cardH, mouseX, mouseY) && (pendingPermanentVow == null);

            // Card background & border
            int cardBg = isActive
                    ? (hoveringCard ? 0xE01E2D46 : 0xD0182438)
                    : (hoveringCard ? 0xD01C2032 : 0xC0151825);
            int cardBorder = isActive
                    ? (isPermanent ? 0xFFF38BA8 : 0xFF89B4FA)
                    : (isPermanent ? 0xFF5C3338 : 0xFF384358);
            context.fill(cardX, y, cardX + cardW, y + cardH, cardBg);
            context.drawBorder(cardX, y, cardW, cardH, cardBorder);

            // Left status accent strip (2px wide inside card border)
            int accentColor = isActive
                    ? (isPermanent ? 0xFFF38BA8 : 0xFFA6E3A1)
                    : (isPermanent ? 0xFF7A3E48 : 0xFF45526D);
            context.fill(cardX + 1, y + 1, cardX + 3, y + cardH - 1, accentColor);

            // Row 1: Vow Name (full width of card, trimmed cleanly if needed)
            String displayName = trimTextToWidth(vow.getName().getString(), cardW - 12);
            int nameColor = isActive ? 0xFFFFFFFF : 0xFFCDD6F4;
            context.drawText(this.textRenderer, displayName, cardX + 7, y + 4, nameColor, false);

            // Row 2 Left: Status Badge
            int statusY = y + 17;
            if (isActive) {
                if (isPermanent) {
                    context.drawText(this.textRenderer, "[BOUND]", cardX + 7, statusY, 0xFFF38BA8, false);
                } else {
                    context.drawText(this.textRenderer, "[ACTIVE]", cardX + 7, statusY, 0xFFA6E3A1, false);
                }
            } else {
                if (isPermanent) {
                    context.drawText(this.textRenderer, "[SEALED \u2022 PERM]", cardX + 7, statusY, 0xFFD27E8A, false);
                } else {
                    context.drawText(this.textRenderer, "[SEALED]", cardX + 7, statusY, 0xFF94A3B8, false);
                }
            }

            // Row 2 Right: Compact Action Button
            int btnW = 54;
            int btnH = 12;
            int btnX = cardX + cardW - btnW - 4;
            int btnY = y + cardH - btnH - 3;

            boolean hoveringBtn = mouseInListArea && isHovering(btnX, btnY, btnW, btnH, mouseX, mouseY) && (pendingPermanentVow == null);

            String btnLabel;
            int btnBg;
            int btnBorder;
            int btnTextCol;

            if (isActive) {
                if (isPermanent) {
                    btnBg = 0xFF281E24;
                    btnBorder = 0xFF4E2C33;
                    btnTextCol = 0xFFE78284;
                    btnLabel = "Locked";
                } else {
                    btnBg = hoveringBtn ? 0xFFE78284 : 0xFF582329;
                    btnBorder = 0xFFE78284;
                    btnTextCol = 0xFFFFFFFF;
                    btnLabel = "Sever";
                }
            } else if (canAccept) {
                if (isPermanent) {
                    btnBg = hoveringBtn ? 0xFFE78284 : 0xFF4A252D;
                    btnBorder = 0xFFE78284;
                    btnTextCol = 0xFFFFFFFF;
                    btnLabel = "Bind";
                } else {
                    btnBg = hoveringBtn ? 0xFF85C1DC : 0xFF1E3A4B;
                    btnBorder = 0xFF89DCEB;
                    btnTextCol = 0xFFFFFFFF;
                    btnLabel = "Pledge";
                }
            } else {
                btnBg = 0xFF222436;
                btnBorder = 0xFF45475A;
                btnTextCol = 0xFF7F849C;
                btnLabel = "Locked";
            }

            context.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnBg);
            context.drawBorder(btnX, btnY, btnW, btnH, btnBorder);
            int labelW = this.textRenderer.getWidth(btnLabel);
            context.drawText(this.textRenderer, btnLabel, btnX + (btnW - labelW) / 2, btnY + 2, btnTextCol, false);

            // Tooltip handling (suppressed when confirmation modal is up)
            float manaCost = vow.getActivationManaCost(player);
            if (pendingPermanentVow == null) {
                if (hoveringBtn) {
                    pendingTooltip = () -> {
                        List<Text> btnTooltip = new ArrayList<>();
                        if (isActive) {
                            if (isPermanent) {
                                btnTooltip.add(Text.literal("Permanent Vow").formatted(Formatting.RED, Formatting.BOLD));
                                btnTooltip.add(Text.literal("Cannot be deactivated manually!").formatted(Formatting.GRAY));
                            } else {
                                btnTooltip.add(Text.literal("Sever Pact").formatted(Formatting.RED, Formatting.BOLD));
                                btnTooltip.add(Text.literal("Warning: Inflicts 15s Burnout").formatted(Formatting.GRAY));
                            }
                        } else if (canAccept) {
                            if (isPermanent) {
                                btnTooltip.add(Text.literal("Pledge Permanent Vow").formatted(Formatting.RED, Formatting.BOLD));
                                btnTooltip.add(Text.literal("Warning: Permanent! Cannot be undone.").formatted(Formatting.YELLOW));
                            } else {
                                btnTooltip.add(Text.literal("Pledge Vow").formatted(Formatting.AQUA, Formatting.BOLD));
                                if (manaCost > 0) {
                                    btnTooltip.add(Text.literal("Cost: " + (int) manaCost + " Mana").formatted(Formatting.GRAY));
                                }
                            }
                        } else {
                            btnTooltip.add(Text.literal("Unavailable").formatted(Formatting.RED, Formatting.BOLD));
                            btnTooltip.add(vow.getCannotAcceptReason(player).copy().formatted(Formatting.GRAY));
                        }
                        context.drawTooltip(this.textRenderer, btnTooltip, mouseX, mouseY);
                    };
                } else if (hoveringCard) {
                    pendingTooltip = () -> {
                        List<OrderedText> tooltipLines = new ArrayList<>();
                        tooltipLines.add(vow.getName().copy().formatted(Formatting.GOLD, Formatting.BOLD).asOrderedText());
                        if (isPermanent) {
                            tooltipLines.add(Text.literal("PERMANENT VOW - CANNOT BE UNDONE").formatted(Formatting.RED, Formatting.BOLD).asOrderedText());
                        }
                        if (!isActive && manaCost > 0) {
                            tooltipLines.add(Text.literal("Activation Cost: " + (int) manaCost + " Mana").formatted(Formatting.AQUA).asOrderedText());
                        }
                        // Neatly wrapped to a compact width of 165px
                        tooltipLines.addAll(this.textRenderer.wrapLines(vow.getDescription(), 165));
                        context.drawOrderedTooltip(this.textRenderer, tooltipLines, mouseX, mouseY);
                    };
                }
            }
        }

        context.disableScissor();

        // Draw scrollbar if there are more vows than fit in the panel
        if (maxScroll > 0) {
            int barX = guiLeft + guiWidth - 12;
            int contentHeight = listHeight + maxScroll;
            int thumbH = Math.max(16, (int) ((float) listHeight * listHeight / contentHeight));
            int thumbY = listTop + (int) ((this.vowScrollOffset / maxScroll) * (listHeight - thumbH));

            context.fill(barX, listTop, barX + 3, listBottom, 0x80101420);
            context.fill(barX, thumbY, barX + 3, thumbY + thumbH, 0xFF488885);
        }

        if (pendingTooltip != null) {
            pendingTooltip.run();
        }
    }

    private void drawPermanentConfirmationModal(DrawContext context, int mouseX, int mouseY) {
        // Dim the entire background behind modal
        context.fill(0, 0, this.width, this.height, 0xB0000000);

        int modalW = 190;
        int modalH = 110;
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - modalH) / 2;

        // Modal window box
        context.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xF0181A26);
        context.drawBorder(modalX, modalY, modalW, modalH, 0xFFE78284);

        // Header warning
        context.drawCenteredTextWithShadow(this.textRenderer, "\u00a7c\u00a7lWARNING: PERMANENT VOW\u00a7r", modalX + modalW / 2, modalY + 10, 0xFFFFFFFF);

        // Explanatory text
        List<OrderedText> warningLines = this.textRenderer.wrapLines(
                Text.literal("Pledging \u00a76" + this.pendingPermanentVow.getName().getString() + "\u00a7r is \u00a7c\u00a7lPERMANENT\u00a7r!\n\nAre you sure you want to bind yourself to this vow?"),
                modalW - 20
        );
        int textY = modalY + 26;
        for (OrderedText line : warningLines) {
            context.drawText(this.textRenderer, line, modalX + 12, textY, 0xFFCDD6F4, false);
            textY += 10;
        }

        // Buttons: [ Confirm & Bind ] and [ Cancel ]
        int btnW = 76;
        int btnH = 16;
        int btnY = modalY + modalH - btnH - 10;
        int confirmX = modalX + 14;
        int cancelX = modalX + modalW - btnW - 14;

        boolean hoveringConfirm = isHovering(confirmX, btnY, btnW, btnH, mouseX, mouseY);
        boolean hoveringCancel = isHovering(cancelX, btnY, btnW, btnH, mouseX, mouseY);

        // Confirm Button
        int confirmBg = hoveringConfirm ? 0xFFE78284 : 0xFF582329;
        context.fill(confirmX, btnY, confirmX + btnW, btnY + btnH, confirmBg);
        context.drawBorder(confirmX, btnY, btnW, btnH, 0xFFE78284);
        context.drawCenteredTextWithShadow(this.textRenderer, "I am sure", confirmX + btnW / 2, btnY + 4, 0xFFFFFFFF);

        // Cancel Button
        int cancelBg = hoveringCancel ? 0xFF45475A : 0xFF2A2C3D;
        context.fill(cancelX, btnY, cancelX + btnW, btnY + btnH, cancelBg);
        context.drawBorder(cancelX, btnY, btnW, btnH, 0xFF585B70);
        context.drawCenteredTextWithShadow(this.textRenderer, "Cancel", cancelX + btnW / 2, btnY + 4, 0xFFCDD6F4);
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
                    tooltip.add(Text.literal("\u2726 Innate Technique").formatted(Formatting.GOLD, Formatting.ITALIC));
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
                            tooltip.add(Text.literal("\u2726 Innate Technique").formatted(Formatting.GOLD, Formatting.ITALIC));
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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.activeTab == 1 && this.pendingPermanentVow == null) {
            int maxScroll = getMaxVowScroll(ModBindingVows.getAll().size());
            if (maxScroll > 0) {
                int listTop = guiTop + VOW_LIST_TOP_OFFSET;
                int listHeight = VOW_LIST_BOTTOM_OFFSET - VOW_LIST_TOP_OFFSET;
                if (isHovering(guiLeft, listTop, guiWidth, listHeight, (int) mouseX, (int) mouseY)) {
                    this.vowScrollOffset = (float) Math.max(0.0, Math.min(maxScroll, this.vowScrollOffset - verticalAmount * 14.0));
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.client != null && this.client.player != null) {
            // If the permanent vow confirmation modal is open, consume all clicks inside/outside modal
            if (this.pendingPermanentVow != null) {
                if (button == 0) {
                    int modalW = 190;
                    int modalH = 110;
                    int modalX = (this.width - modalW) / 2;
                    int modalY = (this.height - modalH) / 2;

                    int btnW = 76;
                    int btnH = 16;
                    int btnY = modalY + modalH - btnH - 10;
                    int confirmX = modalX + 14;
                    int cancelX = modalX + modalW - btnW - 14;

                    // Confirm Click
                    if (isHovering(confirmX, btnY, btnW, btnH, (int) mouseX, (int) mouseY)) {
                        ClientPlayNetworking.send(new ToggleBindingVowPayload(this.pendingPermanentVow.getId().toString()));
                        this.pendingPermanentVow = null;
                        return true;
                    }

                    // Cancel Click
                    if (isHovering(cancelX, btnY, btnW, btnH, (int) mouseX, (int) mouseY)) {
                        this.pendingPermanentVow = null;
                        return true;
                    }
                }
                return true; // Block click through to background
            }

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
                    int listTop = guiTop + VOW_LIST_TOP_OFFSET;
                    int listBottom = guiTop + VOW_LIST_BOTTOM_OFFSET;
                    int listHeight = listBottom - listTop;

                    if (!isHovering(guiLeft + 8, listTop, guiWidth - 16, listHeight, (int) mouseX, (int) mouseY)) {
                        return super.mouseClicked(mouseX, mouseY, button);
                    }

                    var vowComponent = ModEntityComponents.BINDING_VOWS.get(this.client.player);
                    List<BindingVow> vows = new ArrayList<>(ModBindingVows.getAll());
                    int maxScroll = getMaxVowScroll(vows.size());
                    int scrollY = Math.round(this.vowScrollOffset);

                    int cardX = guiLeft + 10;
                    int cardW = (maxScroll > 0) ? (guiWidth - 25) : (guiWidth - 20);
                    int cardH = VOW_CARD_HEIGHT;

                    for (int i = 0; i < vows.size(); i++) {
                        BindingVow vow = vows.get(i);
                        int y = listTop + (i * (cardH + VOW_CARD_GAP)) - scrollY;

                        int btnW = 54;
                        int btnH = 12;
                        int btnX = cardX + cardW - btnW - 4;
                        int btnY = y + cardH - btnH - 3;

                        if (isHovering(btnX, btnY, btnW, btnH, (int) mouseX, (int) mouseY)) {
                            boolean isActive = vowComponent != null && vowComponent.hasVow(vow.getId());
                            if (isActive) {
                                if (vow.isPermanent()) {
                                    // Permanent vows cannot be severed through UI
                                    return true;
                                }
                                ClientPlayNetworking.send(new ToggleBindingVowPayload(vow.getId().toString()));
                                return true;
                            } else {
                                if (!vow.canAccept(this.client.player)) {
                                    return true;
                                }
                                if (vow.isPermanent()) {
                                    // Trigger confirmation prompt
                                    this.pendingPermanentVow = vow;
                                    return true;
                                }
                                ClientPlayNetworking.send(new ToggleBindingVowPayload(vow.getId().toString()));
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.pendingPermanentVow != null) {
            // Escape cancels confirmation modal
            if (keyCode == 256) { // GLFW_KEY_ESCAPE
                this.pendingPermanentVow = null;
                return true;
            }
        }

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

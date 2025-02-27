package com.direwolf20.buildinggadgets.client.screen;

import com.direwolf20.buildinggadgets.client.screen.components.GuiSliderInt;
import com.direwolf20.buildinggadgets.common.config.Config;
import com.direwolf20.buildinggadgets.common.items.GadgetDestruction;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import com.direwolf20.buildinggadgets.common.network.packets.PacketDestructionGUI;
import com.direwolf20.buildinggadgets.common.util.lang.GuiTranslation;
import com.direwolf20.buildinggadgets.common.util.lang.MessageTranslation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class DestructionGUI extends Screen {
    private final Set<GuiDestructionSlider> sliders = new HashSet<>();
    private GuiDestructionSlider left;
    private GuiDestructionSlider right;
    private GuiDestructionSlider up;
    private GuiDestructionSlider down;
    private GuiDestructionSlider depth;
    private Button confirm;

    private String sizeString = "";
    private boolean isValidSize = true;

    private final ItemStack destructionTool;

    public DestructionGUI(ItemStack tool) {
        super(new TextComponent("Destruction Gui?!?"));
        this.destructionTool = tool;
    }

    @Override
    public void init() {
        super.init();

        int x = width / 2;
        int y = height / 2;

        this.addRenderableWidget(confirm = new Button((x - 30) + 32, y + 65, 60, 20, new TranslatableComponent(GuiMod.getLangKeySingle("confirm")), b -> {
            if (Minecraft.getInstance().player == null) {
                return;
            }

            if (isWithinBounds()) {
                PacketHandler.sendToServer(new PacketDestructionGUI(left.getValueInt(), right.getValueInt(), up.getValueInt(), down.getValueInt(), depth.getValueInt()));
                this.onClose();
            }
            else
                Minecraft.getInstance().player.displayClientMessage(MessageTranslation.DESTRCUT_TOO_LARGE.componentTranslation(Config.GADGETS.GADGET_DESTRUCTION.destroySize.get()), true);
        }));

        this.addRenderableWidget(new Button((x - 30) - 32, y + 65, 60, 20, new TranslatableComponent(GuiMod.getLangKeySingle("cancel")), b -> onClose()));

        sliders.clear();
        sliders.add(depth   = new GuiDestructionSlider(x - (GuiDestructionSlider.width / 2), y - (GuiDestructionSlider.height / 2), GuiTranslation.SINGLE_DEPTH.format() + ":", GadgetDestruction.getToolValue(destructionTool, "depth")));
        sliders.add(right   = new GuiDestructionSlider(x + (GuiDestructionSlider.width + 5), y - (GuiDestructionSlider.height / 2), GuiTranslation.SINGLE_RIGHT.format() + ":", GadgetDestruction.getToolValue(destructionTool, "right")));
        sliders.add(left    = new GuiDestructionSlider(x - (GuiDestructionSlider.width * 2) - 5, y - (GuiDestructionSlider.height / 2), GuiTranslation.SINGLE_LEFT.format() + ":", GadgetDestruction.getToolValue(destructionTool, "left")));
        sliders.add(up      = new GuiDestructionSlider(x - (GuiDestructionSlider.width / 2), y - 35, GuiTranslation.SINGLE_UP.format() + ":", GadgetDestruction.getToolValue(destructionTool, "up")));
        sliders.add(down    = new GuiDestructionSlider(x - (GuiDestructionSlider.width / 2), y + 20, GuiTranslation.SINGLE_DOWN.format() + ":", GadgetDestruction.getToolValue(destructionTool, "down")));

        updateSizeString();
        updateIsValid();

        // Adds their buttons to the gui
        sliders.forEach(gui -> gui.getComponents().forEach(this::addRenderableWidget));
    }

    private boolean isWithinBounds() {
        int x = 1 + left.getValueInt() + right.getValueInt();
        int y = 1 + up.getValueInt() + down.getValueInt();
        int z = depth.getValueInt();
        int dim = Config.GADGETS.GADGET_DESTRUCTION.destroySize.get();

        return x <= (dim + 1) && y <= (dim + 1) && z <= dim;
    }

    private String getSizeString() {
        int x = 1 + left.getValueInt() + right.getValueInt(),
                y = 1 + up.getValueInt() + down.getValueInt(),
                z = depth.getValueInt();

        return String.format("%d x %d x %d",
                x,
                y,
                z
        );
    }

    private void updateIsValid() {
        this.isValidSize = isWithinBounds();
        if (!isValidSize && this.confirm.active) {
            this.confirm.setFGColor(0xFF2000);
            this.confirm.active = false;
        }

        if (isValidSize && !this.confirm.active) {
            this.confirm.clearFGColor();
            this.confirm.active = true;
        }
    }

    private void updateSizeString() {
        this.sizeString = getSizeString();
    }

    @Override
    public void render(@Nonnull PoseStack matrices, int mouseX, int mouseY, float partialTicks) {
        super.render(matrices, mouseX, mouseY, partialTicks);

        drawCenteredString(matrices, font, this.sizeString, width / 2, (height / 2) + 40, this.isValidSize ? 0x00FF00 : 0xFF2000);
        if (!this.isValidSize) {
            drawCenteredString(matrices, font, MessageTranslation.DESTRCUT_TOO_LARGE.format(Config.GADGETS.GADGET_DESTRUCTION.destroySize.get()), width / 2, (height / 2) + 50, 0xFF2000);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        for (GuiDestructionSlider slider : sliders) {
            slider.onRelease(mouseX, mouseY);
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // This is only done to reduce code dupe in this class.
    private class GuiDestructionSlider extends GuiSliderInt {
        public static final int width = 70;
        public static final int height = 14;

        private static final int min = 0;
        private static final int max = 16;


        GuiDestructionSlider(int x, int y, String prefix, int current) {
            super(
                    x, y, width, height, new TextComponent(String.format("%s ", prefix)), new TextComponent(""), min, max, current, false, true, Color.DARK_GRAY, null,
                    (slider, amount) -> {
                        slider.setValue(Mth.clamp(slider.getValueInt() + amount, min, max));
                        slider.updateSlider();
                    }
            );
        }

        @Override
        public void updateSlider() {
            super.updateSlider();
            DestructionGUI.this.updateSizeString();
            DestructionGUI.this.updateIsValid();
        }
    }
}
package com.direwolf20.buildinggadgets.client.gui.components;

import com.direwolf20.buildinggadgets.common.registry.objects.BGSound;
import com.direwolf20.buildinggadgets.common.util.ref.Reference;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.function.Predicate;

/**
 *
 */
public class GuiIconActionable extends Button {
    private Predicate<Boolean> action;
    private boolean selected;
    private boolean isSelectable;

    private Color selectedColor     = Color.GREEN;
    private Color deselectedColor   = new Color(255, 255, 255);
    private Color activeColor;

    private ResourceLocation selectedTexture;
    private ResourceLocation deselectedTexture;

    public GuiIconActionable(int x, int y, String texture, String message, boolean isSelectable, Predicate<Boolean> action) {
        super(x, y, 25, 25, message, (b) -> {});
        this.activeColor = deselectedColor;
        this.isSelectable = isSelectable;
        this.action = action;

        this.setSelected(action.test(false));

        // Set the selected and deselected textures.
        String assetLocation = "textures/gui/setting/%s.png";

        this.deselectedTexture = new ResourceLocation(Reference.MODID, String.format(assetLocation, texture));
        this.selectedTexture = !isSelectable ? this.deselectedTexture : new ResourceLocation(Reference.MODID, String.format(assetLocation, texture + "_colored"));
    }

    /**
     * If yo do not need to be able to select / toggle something then use this constructor as
     * you'll hit missing texture issues if you don't have an active (_selected) texture.
     */
    public GuiIconActionable(int x, int y, String texture, String message, Predicate<Boolean> action) {
        this(x, y, texture, message, false, action);
    }

    public void setFaded(boolean faded) {
        alpha = faded ? .6f : 1f;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        this.activeColor = selected ? selectedColor : deselectedColor;
    }

    @Override
    public void playDownSound(SoundHandler soundHandler) {
        soundHandler.play(SimpleSound.master(BGSound.BEEP.getSound(), selected ? .6F: 1F));
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX, mouseY);
        this.action.test(true);

        if( !this.isSelectable )
            return;

        this.setSelected(!this.selected);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTick) {
        if( !visible )
            return;

        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color4f(activeColor.getRed() / 255f, activeColor.getGreen() / 255f, activeColor.getBlue() / 255f, alpha);

        Minecraft.getInstance().getTextureManager().bindTexture(selected ? selectedTexture : deselectedTexture);
        blit(this.x, this.y, 0, 0, this.width, this.height, this.width, this.height);

        if( mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height )
            drawString(Minecraft.getInstance().fontRenderer, this.getMessage(), mouseX + 2, mouseY - 10, activeColor.getRGB());
    }
}

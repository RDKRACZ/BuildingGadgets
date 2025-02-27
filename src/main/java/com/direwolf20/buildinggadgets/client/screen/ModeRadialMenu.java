/**
 * This class was adapted from code written by Vazkii for the PSI mod: https://github.com/Vazkii/Psi
 * Psi is Open Source and distributed under the
 * Psi License: http://psi.vazkii.us/license.php
 */
package com.direwolf20.buildinggadgets.client.screen;

import com.direwolf20.buildinggadgets.client.KeyBindings;
import com.direwolf20.buildinggadgets.client.OurSounds;
import com.direwolf20.buildinggadgets.client.renderer.OurRenderTypes;
import com.direwolf20.buildinggadgets.client.screen.components.GuiIconActionable;
import com.direwolf20.buildinggadgets.client.screen.components.GuiSliderInt;
import com.direwolf20.buildinggadgets.common.config.Config;
import com.direwolf20.buildinggadgets.common.items.*;
import com.direwolf20.buildinggadgets.common.items.modes.BuildingModes;
import com.direwolf20.buildinggadgets.common.items.modes.ExchangingModes;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import com.direwolf20.buildinggadgets.common.network.packets.*;
import com.direwolf20.buildinggadgets.common.util.GadgetUtils;
import com.direwolf20.buildinggadgets.common.util.lang.GuiTranslation;
import com.direwolf20.buildinggadgets.common.util.lang.MessageTranslation;
import com.direwolf20.buildinggadgets.common.util.lang.RadialTranslation;
import com.direwolf20.buildinggadgets.common.util.lang.Styles;
import com.direwolf20.buildinggadgets.common.util.ref.Reference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeI18n;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ModeRadialMenu extends Screen {
    private static final ImmutableList<ResourceLocation> signsCopyPaste = ImmutableList.of(
        new ResourceLocation(Reference.MODID, "textures/gui/mode/copy.png"),
        new ResourceLocation(Reference.MODID, "textures/gui/mode/paste.png")
    );
    private final List<Button> conditionalButtons = new ArrayList<>();
    private int timeIn = 0;
    private int slotSelected = -1;
    private int segments;

    public ModeRadialMenu(ItemStack stack) {
        super(new TextComponent(""));

        if (stack.getItem() instanceof AbstractGadget) {
            this.setSocketable(stack);
        }
    }

    private static float mouseAngle(int x, int y, int mx, int my) {
        Vector2f baseVec = new Vector2f(1F, 0F);
        Vector2f mouseVec = new Vector2f(mx - x, my - y);

        float ang = (float) (Math.acos(baseVec.dot(mouseVec) / (baseVec.length() * mouseVec.length())) * (180F / Math.PI));
        return my < y
            ? 360F - ang
            : ang;
    }

    public void setSocketable(ItemStack stack) {
        if (stack.getItem() instanceof GadgetBuilding) {
            this.segments = BuildingModes.values().length;
        } else if (stack.getItem() instanceof GadgetExchanger) {
            this.segments = ExchangingModes.values().length;
        } else if (stack.getItem() instanceof GadgetCopyPaste) {
            this.segments = GadgetCopyPaste.ToolMode.values().length;
        }
    }

    @Override
    public void init() {
        this.conditionalButtons.clear();
        ItemStack tool = this.getGadget();
        boolean isDestruction = tool.getItem() instanceof GadgetDestruction;
        ScreenPosition right = isDestruction
            ? ScreenPosition.TOP
            : ScreenPosition.RIGHT;
        ScreenPosition left = isDestruction
            ? ScreenPosition.BOTTOM
            : ScreenPosition.LEFT;

        if (isDestruction) {
            addRenderableWidget(new PositionedIconActionable(RadialTranslation.DESTRUCTION_OVERLAY, "destroy_overlay", right, send -> {
                if (send)
                    PacketHandler.sendToServer(new PacketChangeRange());

                return GadgetDestruction.getOverlay(this.getGadget());
            }));

            addRenderableWidget(new PositionedIconActionable(RadialTranslation.FLUID_ONLY, "fluid_only", right, send -> {
                if (send)
                    PacketHandler.sendToServer(new PacketToggleFluidOnly());

                return GadgetDestruction.getIsFluidOnly(this.getGadget());
            }));
        } else {
            addRenderableWidget(new PositionedIconActionable(RadialTranslation.ROTATE, "rotate", left, false, send -> {
                if (send)
                    PacketHandler.sendToServer(new PacketRotateMirror(PacketRotateMirror.Operation.ROTATE));

                return false;
            }));
            addRenderableWidget(new PositionedIconActionable(RadialTranslation.MIRROR, "mirror", left, false, send -> {
                if (send)
                    PacketHandler.sendToServer(new PacketRotateMirror(PacketRotateMirror.Operation.MIRROR));

                return false;
            }));
        }
        if (!(tool.getItem() instanceof GadgetCopyPaste)) {
            if (!isDestruction || Config.GADGETS.GADGET_DESTRUCTION.nonFuzzyEnabled.get()) {
                Button button = new PositionedIconActionable(RadialTranslation.FUZZY, "fuzzy", right, send -> {
                    if (send) {
                        PacketHandler.sendToServer(new PacketToggleFuzzy());
                    }

                    return AbstractGadget.getFuzzy(this.getGadget());
                });
                addRenderableWidget(button);
                conditionalButtons.add(button);
            }
            if (!isDestruction) {
                Button button = new PositionedIconActionable(RadialTranslation.CONNECTED_SURFACE, "connected_area", right, send -> {
                    if (send) {
                        PacketHandler.sendToServer(new PacketToggleConnectedArea());
                    }

                    return AbstractGadget.getConnectedArea(this.getGadget());
                });
                addRenderableWidget(button);
                conditionalButtons.add(button);
            }
            if (!isDestruction) {
                int widthSlider = 82;
                GuiSliderInt sliderRange = new GuiSliderInt(width / 2 - widthSlider / 2, height / 2 + 72, widthSlider, 14, GuiTranslation.SINGLE_RANGE.componentTranslation().append(new TextComponent(": ")), new TextComponent(""), 1, Config.GADGETS.maxRange.get(),
                        GadgetUtils.getToolRange(tool), false, true, Color.DARK_GRAY, slider -> {
                    GuiSliderInt sliderI = (GuiSliderInt) slider;
                    this.sendRangeUpdate(sliderI.getValueInt());
                }, (slider, amount) -> {
                    int value = slider.getValueInt();
                    int valueNew = Mth.clamp(value + amount, 1, Config.GADGETS.maxRange.get());
                    sendRangeUpdate(valueNew);
                    slider.setValue(valueNew);
                    slider.updateSlider();
                }
                );
                sliderRange.precision = 1;
                sliderRange.getComponents().forEach(this::addRenderableWidget);
            }
        } else {
            // Copy Paste specific
            addRenderableWidget(new PositionedIconActionable(RadialTranslation.OPEN_GUI, "copypaste_opengui", right, send -> {
                if (!send)
                    return false;

                assert this.getMinecraft().player != null;

                getMinecraft().player.closeContainer();
                if (GadgetCopyPaste.getToolMode(tool) == GadgetCopyPaste.ToolMode.COPY)
                    getMinecraft().setScreen(new CopyGUI(tool));
                else
                    getMinecraft().setScreen(new PasteGUI(tool));
                return true;
            }));
            addRenderableWidget(new PositionedIconActionable(RadialTranslation.OPEN_MATERIAL_LIST, "copypaste_materiallist", right, send -> {
                if (!send)
                    return false;

                assert this.getMinecraft().player != null;

                getMinecraft().player.closeContainer();
                getMinecraft().setScreen(new MaterialListGUI(tool));
                return true;
            }));
        }
        addRenderableWidget(new PositionedIconActionable(RadialTranslation.RAYTRACE_FLUID, "raytrace_fluid", right, send -> {
            if (send)
                PacketHandler.sendToServer(new PacketToggleRayTraceFluid());

            return AbstractGadget.shouldRayTraceFluid(this.getGadget());
        }));
        if (tool.getItem() instanceof GadgetBuilding) {
            addRenderableWidget(new PositionedIconActionable(RadialTranslation.PLACE_ON_TOP, "building_place_atop", right, send -> {
                if (send)
                    PacketHandler.sendToServer(new PacketToggleBlockPlacement());

                return GadgetBuilding.shouldPlaceAtop(this.getGadget());
            }));
        }
        addRenderableWidget(new PositionedIconActionable(RadialTranslation.ANCHOR, "anchor", left, send -> {
            if (send)
                PacketHandler.sendToServer(new PacketAnchor());

            ItemStack stack = this.getGadget();
            if (stack.getItem() instanceof GadgetCopyPaste || stack.getItem() instanceof GadgetDestruction) {
                return ((AbstractGadget) stack.getItem()).getAnchor(stack) != null;
            }

            return GadgetUtils.getAnchor(stack).isPresent();
        }));

        if (!(tool.getItem() instanceof GadgetExchanger)) {
            addRenderableWidget(new PositionedIconActionable(RadialTranslation.UNDO, "undo", left, false, send -> {
                if (send)
                    PacketHandler.sendToServer(new PacketUndo());

                return false;
            }));
        }

        this.updateButtons(tool);
    }

    private void updateButtons(ItemStack tool) {
        int posRight = 0;
        int posLeft = 0;
        int dim = 24;
        int padding = 10;
        boolean isDestruction = tool.getItem() instanceof GadgetDestruction;
        ScreenPosition right = isDestruction ? ScreenPosition.BOTTOM : ScreenPosition.RIGHT;
        for (GuiEventListener widget : children()) {
            if (!(widget instanceof PositionedIconActionable))
                continue;

            PositionedIconActionable button = (PositionedIconActionable) widget;

            if (!button.visible) {
                continue;
            }
            int offset;
            boolean isRight = button.position == right;
            if (isRight) {
                posRight += dim + padding;
                offset = 70;
            } else {
                posLeft += dim + padding;
                offset = -70 - dim;
            }
            button.setWidth(dim);
            button.setHeight(dim);
            if (isDestruction)
                button.y = height / 2 + (isRight ? 10 : -button.getHeight() - 10);
            else
                button.x = width / 2 + offset;
        }
        posRight = resetPos(tool, padding, posRight);
        posLeft = resetPos(tool, padding, posLeft);
        for (GuiEventListener widget : children()) {
            if (!(widget instanceof PositionedIconActionable))
                continue;

            PositionedIconActionable button = (PositionedIconActionable) widget;
            if (!button.visible) {
                continue;
            }
            boolean isRight = button.position == right;
            int pos = isRight
                ? posRight
                : posLeft;
            if (isDestruction) {
                button.x = pos;
            } else {
                button.y = pos;
            }

            if (isRight) {
                posRight += dim + padding;
            } else {
                posLeft += dim + padding;
            }
        }
    }

    private int resetPos(ItemStack tool, int padding, int pos) {
        return tool.getItem() instanceof GadgetDestruction
            ? this.width / 2 - (pos - padding) / 2
            : this.height / 2 - (pos - padding) / 2;
    }

    private ItemStack getGadget() {
        assert this.getMinecraft().player != null;
        return AbstractGadget.getGadget(this.getMinecraft().player);
    }

    @Override
    public void render(PoseStack matrices, int mx, int my, float partialTicks) {
        float stime = 5F;
        float fract = Math.min(stime, this.timeIn + partialTicks) / stime;
        int x = this.width / 2;
        int y = this.height / 2;

        int radiusMin = 26;
        int radiusMax = 60;
        double dist = new Vec3(x, y, 0).distanceTo(new Vec3(mx, my, 0));
        boolean inRange = false;
        if (this.segments != 0) {
            inRange = dist > radiusMin && dist < radiusMax;
            for (GuiEventListener button : children()) {
                if (button instanceof PositionedIconActionable) {
                    ((PositionedIconActionable) button).setFaded(inRange);
                }
            }
        }

        // This triggers the animation on creation
        matrices.pushPose();
        matrices.translate((1 - fract) * x, (1 - fract) * y, 0);
        matrices.scale(fract, fract, fract);
        super.render(matrices, mx, my, partialTicks);
        matrices.popPose();

        if (this.segments == 0) {
            return;
        }

        float angle = mouseAngle(x, y, mx, my);

        float totalDeg = 0;
        float degPer = 360F / this.segments;

        List<NameDisplayData> nameData = new ArrayList<>();

        ItemStack tool = this.getGadget();
        if (tool.isEmpty()) {
            return;
        }

        this.slotSelected = -1;

        List<ResourceLocation> signs;
        int modeIndex;
        if (tool.getItem() instanceof GadgetBuilding) {
            modeIndex = GadgetBuilding.getToolMode(tool).ordinal();
            signs = Arrays.stream(BuildingModes.values()).map(e -> new ResourceLocation(Reference.MODID, e.getIcon())).collect(Collectors.toList());
        } else if (tool.getItem() instanceof GadgetExchanger) {
            modeIndex = GadgetExchanger.getToolMode(tool).ordinal();
            signs = Arrays.stream(ExchangingModes.values()).map(e -> new ResourceLocation(Reference.MODID, e.getIcon())).collect(Collectors.toList());
        } else {
            modeIndex = GadgetCopyPaste.getToolMode(tool).ordinal();
            signs = signsCopyPaste;
        }

        boolean shouldCenter = (this.segments + 2) % 4 == 0;
        int indexBottom = this.segments / 4;
        int indexTop = indexBottom + this.segments / 2;
        for (int seg = 0; seg < this.segments; seg++) {
            boolean mouseInSector = this.isCursorInSlice(angle, totalDeg, degPer, inRange);
            float radius = Math.max(0F, Math.min((this.timeIn + partialTicks - seg * 6F / this.segments) * 40F, radiusMax));

            float gs = 0.25F;
            if (seg % 2 == 0) {
                gs += 0.1F;
            }

            float r = gs;
            float g = gs + (seg == modeIndex
                ? 1F
                : 0.0F);
            float b = gs;
            float a = 0.4F;
            if (mouseInSector) {
                this.slotSelected = seg;
                r = g = b = 1F;
            }

            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            VertexConsumer buffer = bufferSource.getBuffer(OurRenderTypes.TRIANGLE_STRIP);

            for (float i = degPer; i >= 0; i--) {
                float rad = (float) ((i + totalDeg) / 180F * Math.PI);
                float xp = (float) (x + Math.cos(rad) * radius);
                float yp = (float) (y + Math.sin(rad) * radius);
                if ((int) i == (int) (degPer / 2))
                    nameData.add(new NameDisplayData((int) xp, (int) yp, mouseInSector, shouldCenter && (seg == indexBottom || seg == indexTop)));

                Matrix4f pose = matrices.last().pose();
                buffer.vertex(pose, (float) (x + Math.cos(rad) * radius / 2.3F), (float) (y + Math.sin(rad) * radius / 2.3F), 0).color(r, g, b, a).endVertex();
                buffer.vertex(xp, yp, 0).color(r, g, b, a).endVertex();
            }

            bufferSource.endBatch(OurRenderTypes.TRIANGLE_STRIP);
            totalDeg += degPer;
        }

        // This is the naming logic for the text that pops up
        for (int i = 0; i < nameData.size(); i++) {
            matrices.pushPose();
            NameDisplayData data = nameData.get(i);
            int xp = data.getX();
            int yp = data.getY();

            String name;
            if (tool.getItem() instanceof GadgetBuilding) {
                name = ForgeI18n.getPattern(BuildingModes.values()[i].getTranslationKey());
            } else if (tool.getItem() instanceof GadgetExchanger) {
                name = ForgeI18n.getPattern(ExchangingModes.values()[i].getTranslationKey());
            } else {
                name = GadgetCopyPaste.ToolMode.values()[i].getTranslation().format();
            }

            int xsp = xp - 4;
            int ysp = yp;
            int width = font.width(name);

            if (xsp < x) {
                xsp -= width - 8;
            }
            if (ysp < y) {
                ysp -= 9;
            }

            Color color = i == modeIndex ? Color.GREEN : Color.WHITE;
            if (data.isSelected())
                font.drawShadow(matrices, name, xsp + (data.isCentralized() ? width / 2f - 4 : 0), ysp, color.getRGB());

            double mod = 0.7;
            int xdp = (int) ((xp - x) * mod + x);
            int ydp = (int) ((yp - y) * mod + y);

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, 1);
            RenderSystem.setShaderTexture(0, signs.get(i));
            blit(matrices, xdp - 8, ydp - 8, 0, 0, 16, 16, 16, 16);

            matrices.popPose();
        }

        float s = 1.8F * fract;
        PoseStack stack = RenderSystem.getModelViewStack();
        stack.pushPose();
        stack.scale(s, s, s);
        matrices.popPose();
        stack.translate(x / s - (tool.getItem() instanceof GadgetCopyPaste ? 8 : 8.5), y / s - 8, 0);
        this.itemRenderer.renderAndDecorateItem(tool, 0, 0);
        stack.popPose();
    }

    private boolean isCursorInSlice(float angle, float totalDeg, float degPer, boolean inRange) {
        return inRange && angle > totalDeg && angle < totalDeg + degPer;
    }

    private void changeMode() {
        if (this.slotSelected >= 0) {
            Item gadget = this.getGadget().getItem();

            // This should logically never fail but implementing a way to ensure that would
            // be a pretty solid idea for the next guy to touch this code.
            String mode;
            if (gadget instanceof GadgetBuilding) {
                mode = ForgeI18n.getPattern(BuildingModes.values()[this.slotSelected].getTranslationKey());
            } else if (gadget instanceof GadgetExchanger) {
                mode = ForgeI18n.getPattern(ExchangingModes.values()[this.slotSelected].getTranslationKey());
            } else {
                mode = GadgetCopyPaste.ToolMode.values()[this.slotSelected].getTranslation().format();
            }

            assert getMinecraft().player != null;
            getMinecraft().player.displayClientMessage(MessageTranslation.MODE_SET.componentTranslation(mode).setStyle(Styles.AQUA), true);

            PacketHandler.sendToServer(new PacketToggleMode(this.slotSelected));
            OurSounds.BEEP.playSound();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        this.changeMode();
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void tick() {
        if (!InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), KeyBindings.menuSettings.getKey().getValue())) {
            onClose();
            changeMode();
        }

        ImmutableSet<KeyMapping> set = ImmutableSet.of(getMinecraft().options.keyUp, getMinecraft().options.keyLeft, getMinecraft().options.keyDown, getMinecraft().options.keyRight, getMinecraft().options.keyShift, getMinecraft().options.keySprint, getMinecraft().options.keyJump);
        for (KeyMapping k : set)
            KeyMapping.set(k.getKey(), k.isDown());

        this.timeIn++;
        ItemStack tool = this.getGadget();
        boolean builder = tool.getItem() instanceof GadgetBuilding;
        if (!builder && !(tool.getItem() instanceof GadgetExchanger)) {
            return;
        }

        boolean curent;
        boolean changed = false;
        for (int i = 0; i < this.conditionalButtons.size(); i++) {
            Button button = this.conditionalButtons.get(i);
            if (builder) {
                curent = GadgetBuilding.getToolMode(tool) == BuildingModes.SURFACE;
            } else {
                curent = i == 0 || GadgetExchanger.getToolMode(tool) == ExchangingModes.SURFACE;
            }

            if (button.visible != curent) {
                button.visible = curent;
                changed = true;
            }
        }
        if (changed) {
            this.updateButtons(tool);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void sendRangeUpdate(int valueNew) {
        if (valueNew != GadgetUtils.getToolRange(this.getGadget())) {
            PacketHandler.sendToServer(new PacketChangeRange(valueNew));
        }
    }

    public enum ScreenPosition {
        RIGHT, LEFT, BOTTOM, TOP
    }

    private static final class NameDisplayData {
        private final int x;
        private final int y;
        private final boolean selected;
        private final boolean centralize;

        private NameDisplayData(int x, int y, boolean selected, boolean centralize) {
            this.x = x;
            this.y = y;
            this.selected = selected;
            this.centralize = centralize;
        }

        private int getX() {
            return this.x;
        }

        private int getY() {
            return this.y;
        }

        private boolean isSelected() {
            return this.selected;
        }

        private boolean isCentralized() {
            return this.centralize;
        }
    }

    private static class PositionedIconActionable extends GuiIconActionable {
        private ScreenPosition position;

        PositionedIconActionable(RadialTranslation message, String icon, ScreenPosition position, boolean isSelectable, Predicate<Boolean> action) {
            super(0, 0, icon, message.componentTranslation(), isSelectable, action);

            this.position = position;
        }

        PositionedIconActionable(RadialTranslation message, String icon, ScreenPosition position, Predicate<Boolean> action) {
            this(message, icon, position, true, action);
        }
    }

    private static class Vector2f {
        public float x;
        public float y;

        public Vector2f(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public final float dot(Vector2f v1) {
            return (this.x * v1.x + this.y * v1.y);
        }

        public final float length() {
            return (float) Math.sqrt(this.x * this.x + this.y * this.y);
        }
    }
}

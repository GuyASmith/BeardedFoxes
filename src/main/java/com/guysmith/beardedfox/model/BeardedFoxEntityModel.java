package com.guysmith.beardedfox.model;

import com.google.common.collect.ImmutableList;
import com.guysmith.beardedfox.entity.BeardedFoxEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.FoxEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class BeardedFoxEntityModel extends EntityModel<BeardedFoxEntity> {
    // this code copied from mostly FoxEntityModel, with a hint of AnimalModel to cover my bases
        // reason it had to be duplicated and not inherited: FoxEntity.Type, the source of all my irritation
    private final boolean headScaled;
    private final float childHeadYOffset;
    private final float childHeadZOffset;
    private final float invertedChildHeadScale;
    private final float invertedChildBodyScale;
    private final float childBodyYOffset;

    public final ModelPart head;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart nose;
    private final ModelPart torso;
    private final ModelPart rightBackLeg;
    private final ModelPart leftBackLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart tail;
    private float legPitchModifier;

    public BeardedFoxEntityModel() {
        //super(true, 8.0F, 3.35F);
        this.headScaled = true;
        this.childHeadYOffset = 8.0f;
        this.childHeadZOffset = 3.35f;
        this.invertedChildHeadScale = 2.0f;
        this.invertedChildBodyScale = 2.0f;
        this.childBodyYOffset = 24.0f;

        this.textureWidth = 48;
        this.textureHeight = 32;
        this.head = new ModelPart(this, 1, 5);
        this.head.addCuboid(-3.0F, -2.0F, -5.0F, 8.0F, 6.0F, 6.0F);
        this.head.setPivot(-1.0F, 16.5F, -3.0F);
        this.rightEar = new ModelPart(this, 8, 1);
        this.rightEar.addCuboid(-3.0F, -4.0F, -4.0F, 2.0F, 2.0F, 1.0F);
        this.leftEar = new ModelPart(this, 15, 1);
        this.leftEar.addCuboid(3.0F, -4.0F, -4.0F, 2.0F, 2.0F, 1.0F);
        this.nose = new ModelPart(this, 6, 18);
        this.nose.addCuboid(-1.0F, 2.01F, -8.0F, 4.0F, 2.0F, 3.0F);
        this.head.addChild(this.rightEar);
        this.head.addChild(this.leftEar);
        this.head.addChild(this.nose);
        this.torso = new ModelPart(this, 24, 15);
        this.torso.addCuboid(-3.0F, 3.999F, -3.5F, 6.0F, 11.0F, 6.0F);
        this.torso.setPivot(0.0F, 16.0F, -6.0F);
        float f = 0.001F;
        this.rightBackLeg = new ModelPart(this, 13, 24);
        this.rightBackLeg.addCuboid(2.0F, 0.5F, -1.0F, 2.0F, 6.0F, 2.0F, 0.001F);
        this.rightBackLeg.setPivot(-5.0F, 17.5F, 7.0F);
        this.leftBackLeg = new ModelPart(this, 4, 24);
        this.leftBackLeg.addCuboid(2.0F, 0.5F, -1.0F, 2.0F, 6.0F, 2.0F, 0.001F);
        this.leftBackLeg.setPivot(-1.0F, 17.5F, 7.0F);
        this.rightFrontLeg = new ModelPart(this, 13, 24);
        this.rightFrontLeg.addCuboid(2.0F, 0.5F, -1.0F, 2.0F, 6.0F, 2.0F, 0.001F);
        this.rightFrontLeg.setPivot(-5.0F, 17.5F, 0.0F);
        this.leftFrontLeg = new ModelPart(this, 4, 24);
        this.leftFrontLeg.addCuboid(2.0F, 0.5F, -1.0F, 2.0F, 6.0F, 2.0F, 0.001F);
        this.leftFrontLeg.setPivot(-1.0F, 17.5F, 0.0F);
        this.tail = new ModelPart(this, 30, 0);
        this.tail.addCuboid(2.0F, 0.0F, -1.0F, 4.0F, 9.0F, 5.0F);
        this.tail.setPivot(-4.0F, 15.0F, -1.0F);
        this.torso.addChild(this.tail);
    }

    public void animateModel(BeardedFoxEntity foxEntity, float f, float g, float h) {
        this.torso.pitch = 1.5707964F;
        this.tail.pitch = -0.05235988F;
        this.rightBackLeg.pitch = MathHelper.cos(f * 0.6662F) * 1.4F * g;
        this.leftBackLeg.pitch = MathHelper.cos(f * 0.6662F + 3.1415927F) * 1.4F * g;
        this.rightFrontLeg.pitch = MathHelper.cos(f * 0.6662F + 3.1415927F) * 1.4F * g;
        this.leftFrontLeg.pitch = MathHelper.cos(f * 0.6662F) * 1.4F * g;
        this.head.setPivot(-1.0F, 16.5F, -3.0F);
        this.head.yaw = 0.0F;
        this.head.roll = foxEntity.getHeadRoll(h);
        this.rightBackLeg.visible = true;
        this.leftBackLeg.visible = true;
        this.rightFrontLeg.visible = true;
        this.leftFrontLeg.visible = true;
        this.torso.setPivot(0.0F, 16.0F, -6.0F);
        this.torso.roll = 0.0F;
        this.rightBackLeg.setPivot(-5.0F, 17.5F, 7.0F);
        this.leftBackLeg.setPivot(-1.0F, 17.5F, 7.0F);
        if (foxEntity.isInSneakingPose()) {
            this.torso.pitch = 1.6755161F;
            float i = foxEntity.getBodyRotationHeightOffset(h);
            this.torso.setPivot(0.0F, 16.0F + foxEntity.getBodyRotationHeightOffset(h), -6.0F);
            this.head.setPivot(-1.0F, 16.5F + i, -3.0F);
            this.head.yaw = 0.0F;
        } else if (foxEntity.isSleeping()) {
            this.torso.roll = -1.5707964F;
            this.torso.setPivot(0.0F, 21.0F, -6.0F);
            this.tail.pitch = -2.6179938F;
            if (this.child) {
                this.tail.pitch = -2.1816616F;
                this.torso.setPivot(0.0F, 21.0F, -2.0F);
            }

            this.head.setPivot(1.0F, 19.49F, -3.0F);
            this.head.pitch = 0.0F;
            this.head.yaw = -2.0943952F;
            this.head.roll = 0.0F;
            this.rightBackLeg.visible = false;
            this.leftBackLeg.visible = false;
            this.rightFrontLeg.visible = false;
            this.leftFrontLeg.visible = false;
        } else if (foxEntity.isSitting()) {
            this.torso.pitch = 0.5235988F;
            this.torso.setPivot(0.0F, 9.0F, -3.0F);
            this.tail.pitch = 0.7853982F;
            this.tail.setPivot(-4.0F, 15.0F, -2.0F);
            this.head.setPivot(-1.0F, 10.0F, -0.25F);
            this.head.pitch = 0.0F;
            this.head.yaw = 0.0F;
            if (this.child) {
                this.head.setPivot(-1.0F, 13.0F, -3.75F);
            }

            this.rightBackLeg.pitch = -1.3089969F;
            this.rightBackLeg.setPivot(-5.0F, 21.5F, 6.75F);
            this.leftBackLeg.pitch = -1.3089969F;
            this.leftBackLeg.setPivot(-1.0F, 21.5F, 6.75F);
            this.rightFrontLeg.pitch = -0.2617994F;
            this.leftFrontLeg.pitch = -0.2617994F;
        }

    }

    protected Iterable<ModelPart> getHeadParts() {
        return ImmutableList.of(this.head);
    }

    protected Iterable<ModelPart> getBodyParts() {
        return ImmutableList.of(this.torso, this.rightBackLeg, this.leftBackLeg, this.rightFrontLeg, this.leftFrontLeg);
    }

    public void setAngles(BeardedFoxEntity foxEntity, float f, float g, float h, float i, float j) {
        if (!foxEntity.isSleeping() && !foxEntity.isWalking() && !foxEntity.isInSneakingPose()) {
            this.head.pitch = j * 0.017453292F;
            this.head.yaw = i * 0.017453292F;
        }

        if (foxEntity.isSleeping()) {
            this.head.pitch = 0.0F;
            this.head.yaw = -2.0943952F;
            this.head.roll = MathHelper.cos(h * 0.027F) / 22.0F;
        }

        float l;
        if (foxEntity.isInSneakingPose()) {
            l = MathHelper.cos(h) * 0.01F;
            this.torso.yaw = l;
            this.rightBackLeg.roll = l;
            this.leftBackLeg.roll = l;
            this.rightFrontLeg.roll = l / 2.0F;
            this.leftFrontLeg.roll = l / 2.0F;
        }

        if (foxEntity.isWalking()) {
            l = 0.1F;
            this.legPitchModifier += 0.67F;
            this.rightBackLeg.pitch = MathHelper.cos(this.legPitchModifier * 0.4662F) * 0.1F;
            this.leftBackLeg.pitch = MathHelper.cos(this.legPitchModifier * 0.4662F + 3.1415927F) * 0.1F;
            this.rightFrontLeg.pitch = MathHelper.cos(this.legPitchModifier * 0.4662F + 3.1415927F) * 0.1F;
            this.leftFrontLeg.pitch = MathHelper.cos(this.legPitchModifier * 0.4662F) * 0.1F;
        }

    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        // this code copied from AnimalModel; thanks Yarn, Fabric, and Mojang teams
        if (this.child) {
            matrices.push();
            float g;
            if (this.headScaled) {
                g = 1.5F / this.invertedChildHeadScale;
                matrices.scale(g, g, g);
            }

            matrices.translate(0.0D, (double)(this.childHeadYOffset / 16.0F), (double)(this.childHeadZOffset / 16.0F));
            this.getHeadParts().forEach((modelPart) -> {
                modelPart.render(matrices, vertices, light, overlay, red, green, blue, alpha);
            });
            matrices.pop();
            matrices.push();
            g = 1.0F / this.invertedChildBodyScale;
            matrices.scale(g, g, g);
            matrices.translate(0.0D, (double)(this.childBodyYOffset / 16.0F), 0.0D);
            this.getBodyParts().forEach((modelPart) -> {
                modelPart.render(matrices, vertices, light, overlay, red, green, blue, alpha);
            });
            matrices.pop();
        } else {
            this.getHeadParts().forEach((modelPart) -> {
                modelPart.render(matrices, vertices, light, overlay, red, green, blue, alpha);
            });
            this.getBodyParts().forEach((modelPart) -> {
                modelPart.render(matrices, vertices, light, overlay, red, green, blue, alpha);
            });
        }
    }
}

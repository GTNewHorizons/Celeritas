package net.irisshaders.iris.uniforms;

import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import org.joml.Vector4f;

import static org.embeddedt.embeddium.api.compat.mc.MinecraftVersionShimService.MINECRAFT;

public class IrisExclusiveUniforms {

	public static void addIrisExclusiveUniforms(UniformHolder uniforms) {
		WorldInfoUniforms.addWorldInfoUniforms(uniforms);

		uniforms.uniform1i(UniformUpdateFrequency.PER_TICK, "currentColorSpace", () -> IrisVideoSettings.colorSpace.ordinal());

		//All Iris-exclusive uniforms (uniforms which do not exist in either OptiFine or ShadersMod) should be registered here.
		uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "thunderStrength", MINECRAFT::getThunderStrength);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerHealth", MINECRAFT::getCurrentHealth);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerHealth", MINECRAFT::getMaxHealth);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerHunger", MINECRAFT::getCurrentHunger);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerHunger", () -> 20);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerArmor", MINECRAFT::getCurrentArmor);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerArmor", () -> 50);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerAir", MINECRAFT::getCurrentAir);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerAir", MINECRAFT::getMaxAir);
		uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "firstPersonCamera", MINECRAFT::isFirstPersonCamera);
		uniforms.uniform1b(UniformUpdateFrequency.PER_TICK, "isSpectator", MINECRAFT::isSpectator);
		uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "eyePosition", MINECRAFT::getEyePosition);
		uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "cloudTime", CapturedRenderingState.INSTANCE::getCloudTime);
		uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "relativeEyePosition", () -> CameraUniforms.getUnshiftedCameraPosition().sub(MINECRAFT.getEyePosition()));
		uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "playerLookVector", MINECRAFT::getPlayerLookVector);
		uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "playerBodyVector", MINECRAFT::getPlayerBodyVector);
		uniforms.uniform4f(UniformUpdateFrequency.PER_TICK, "lightningBoltPosition", MINECRAFT::getLightningBoltPosition);
	}


	public static class WorldInfoUniforms {
		public static void addWorldInfoUniforms(UniformHolder uniforms) {
			// TODO: Use level.dimensionType() coordinates for 1.18!
			uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "bedrockLevel", MINECRAFT::getBedrockLevel);
			uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "cloudHeight", MINECRAFT::getCloudHeight);

			uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "heightLimit", MINECRAFT::getHeightLimit);
			uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "logicalHeightLimit", MINECRAFT::getLogicalHeightLimit);
			uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "hasCeiling", MINECRAFT::hasCeiling);
			uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "hasSkylight", MINECRAFT::hasSkyLight);
			uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "ambientLight", MINECRAFT::getAmbientLight);

		}
	}
}

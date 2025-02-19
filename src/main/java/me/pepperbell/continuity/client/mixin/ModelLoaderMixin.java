package me.pepperbell.continuity.client.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.mojang.datafixers.util.Pair;

import me.pepperbell.continuity.client.resource.CTMPropertiesLoader;
import me.pepperbell.continuity.client.resource.EmissiveSuffixLoader;
import me.pepperbell.continuity.client.resource.ModelWrappingHandler;
import me.pepperbell.continuity.client.resource.ResourcePackUtil;
import me.pepperbell.continuity.client.util.biome.BiomeHolderManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraftforge.client.model.geometry.GeometryLoaderManager;

@Mixin(ModelLoader.class)
public class ModelLoaderMixin {
	@Shadow
	@Final
	private Map<Identifier, UnbakedModel> unbakedModels;
	@Shadow
	@Final
	private Map<Identifier, UnbakedModel> modelsToBake;
	@Shadow
	@Final
	private Map<Identifier, Pair<SpriteAtlasTexture, SpriteAtlasTexture.Data>> spriteAtlasData;

	@Unique
	private BlockState continuity$currentBlockState;
	
	@Redirect(
	        method = "<init>(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/client/color/block/BlockColors;Lnet/minecraft/util/profiler/Profiler;I)V",
	        at = @At(
	            value = "INVOKE",
	            target = "Lnet/minecraftforge/client/model/geometry/GeometryLoaderManager;init()V"
	        )
	    )
	private void continuity$afterGeometryInit(ResourceManager resourceManager, BlockColors blockColors, Profiler profiler, int mipmap) {
		// TODO: move these to the very beginning of resource reload
		ResourcePackUtil.setup(resourceManager);
		BiomeHolderManager.clearCache();

		EmissiveSuffixLoader.load(resourceManager);
		CTMPropertiesLoader.clearAll();
		CTMPropertiesLoader.loadAll(resourceManager);
		
		GeometryLoaderManager.init();
    }

	@Inject(method = "m_119263_(Lnet/minecraft/block/BlockState;)V", at = @At("HEAD"))
	private void continuity$onAddBlockStateModel(BlockState state, CallbackInfo ci) {
		continuity$currentBlockState = state;
	}

	@Inject(method = "addModel(Lnet/minecraft/client/util/ModelIdentifier;)V", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
	private void continuity$afterAddModel(ModelIdentifier id, CallbackInfo ci, UnbakedModel model) {
		if (continuity$currentBlockState != null) {
			ModelWrappingHandler.onAddBlockStateModel(id, continuity$currentBlockState);
			continuity$currentBlockState = null;
		}
	}
	
	@Redirect(method = "<init>(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/client/color/block/BlockColors;Lnet/minecraft/util/profiler/Profiler;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", ordinal = 4))
	private void continuity$onFinishAddingModels(Profiler redirectedProfiler, String value, ResourceManager resourceManager, BlockColors blockColors, Profiler profiler, int mipmap) {
		ModelWrappingHandler.wrapCTMModels(unbakedModels, modelsToBake);
		redirectedProfiler.swap(value);
	}

	@Inject(method = "<init>(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/client/color/block/BlockColors;Lnet/minecraft/util/profiler/Profiler;I)V", at = @At("TAIL"))
	private void continuity$onTailInit(ResourceManager resourceManager, BlockColors blockColors, Profiler profiler, int mipmap, CallbackInfo ci) {
		ModelWrappingHandler.wrapEmissiveModels(spriteAtlasData, unbakedModels, modelsToBake);

		CTMPropertiesLoader.clearAll();

		// TODO: move these to the very end of resource reload
		ResourcePackUtil.clear();
		BiomeHolderManager.refreshHolders();
	}
}

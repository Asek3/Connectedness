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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.resource.ResourceManager;
import net.minecraft.state.StateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

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
	
	@Shadow
	@Final
	protected ResourceManager resourceManager;
	
	@Inject(method = "processLoading", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V", args = "ldc=missing_model", shift = At.Shift.BEFORE))
	private void continuity$afterStoreArgs(Profiler profiler, int mipmap, CallbackInfo ci) {
		// TODO: move these to the very beginning of resource reload
		ResourcePackUtil.setup(resourceManager);
		BiomeHolderManager.clearCache();

		EmissiveSuffixLoader.load(resourceManager);
		CTMPropertiesLoader.clearAll();
		CTMPropertiesLoader.loadAll(resourceManager);
	}
	
	@Redirect(method = "processLoading", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getStateManager()Lnet/minecraft/state/StateManager;"))
    private StateManager<Block, BlockState> redirectGetStateManager(Block block) {
        StateManager<Block, BlockState> stateManager = block.getStateManager();
        stateManager.getStates().forEach(state -> {
        	continuity$currentBlockState = state;
            ModelIdentifier modelId = BlockModels.getModelId(state);
            addModel(modelId);
        });
        return stateManager;
    }
	
	@Shadow
	private void addModel(ModelIdentifier modelId) {
	}

	@Inject(method = "addModel(Lnet/minecraft/client/util/ModelIdentifier;)V", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
	private void continuity$afterAddModel(ModelIdentifier id, CallbackInfo ci, UnbakedModel model) {
		if (continuity$currentBlockState != null) {
			ModelWrappingHandler.onAddBlockStateModel(id, continuity$currentBlockState);
			continuity$currentBlockState = null;
		}
	}
	
	@Inject(method = "processLoading", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=textures"))
	private void continuity$onFinishAddingModels(Profiler profiler, int mipmap, CallbackInfo ci) {
		ModelWrappingHandler.wrapCTMModels(unbakedModels, modelsToBake);
	}

	@Inject(method = "processLoading", at = @At("TAIL"))
	private void continuity$onTail(Profiler profiler, int mipmap, CallbackInfo ci) {
		ModelWrappingHandler.wrapEmissiveModels(spriteAtlasData, unbakedModels, modelsToBake);

		CTMPropertiesLoader.clearAll();

		// TODO: move these to the very end of resource reload
		ResourcePackUtil.clear();
		BiomeHolderManager.refreshHolders();
	}
}

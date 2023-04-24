package me.pepperbell.continuity.client.util;

import org.jetbrains.annotations.ApiStatus;

import me.pepperbell.continuity.client.ContinuityClient;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.impl.client.indigo.renderer.IndigoRenderer;
import net.fabricmc.fabric.impl.client.indigo.renderer.RenderMaterialImpl;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class RenderUtil {
	private static final BlockColors BLOCK_COLORS = MinecraftClient.getInstance().getBlockColors();
	private static final BakedModelManager MODEL_MANAGER = MinecraftClient.getInstance().getBakedModelManager();

	private static final ThreadLocal<MaterialFinder> MATERIAL_FINDER = ThreadLocal.withInitial(() -> RendererAccess.INSTANCE.getRenderer().materialFinder());

	private static final BlendModeGetter BLEND_MODE_GETTER = createBlendModeGetter();

	private static SpriteFinder blockAtlasSpriteFinder;

	private static BlendModeGetter createBlendModeGetter() {
		if (ModList.get().isLoaded("reforgium") && ModList.get().isLoaded("rubidium")) {
			return quad -> ((link.infra.indium.renderer.RenderMaterialImpl) quad.material()).blendMode(0);
		} else if (RendererAccess.INSTANCE.getRenderer() instanceof IndigoRenderer) {
			return quad -> ((RenderMaterialImpl) quad.material()).blendMode(0);
		}
		return quad -> BlendMode.DEFAULT;
	}

	public static int getTintColor(BlockState state, BlockRenderView blockView, BlockPos pos, int tintIndex) {
		if (state == null || tintIndex == -1) {
			return -1;
		}
		return 0xFF000000 | BLOCK_COLORS.getColor(state, blockView, pos, tintIndex);
	}

	public static MaterialFinder getMaterialFinder() {
		return MATERIAL_FINDER.get().clear();
	}

	public static BlendMode getBlendMode(QuadView quad) {
		return BLEND_MODE_GETTER.getBlendMode(quad);
	}

	public static SpriteFinder getSpriteFinder() {
		return blockAtlasSpriteFinder;
	}

	private interface BlendModeGetter {
		BlendMode getBlendMode(QuadView quad);
	}

	public static class ReloadListener implements SynchronousResourceReloader {
		public static final Identifier ID = ContinuityClient.asId("render_util");
		private static final ReloadListener INSTANCE = new ReloadListener();

		@ApiStatus.Internal
		public static void init() {
			FMLJavaModLoadingContext.get().getModEventBus().register(INSTANCE);
		}
		
		@SubscribeEvent
		public void addListener(RegisterClientReloadListenersEvent event) {
			event.registerReloadListener(this);
		}

		@Override
		public void reload(ResourceManager manager) {
			blockAtlasSpriteFinder = SpriteFinder.get(MODEL_MANAGER.getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
		}
	}
}

package me.pepperbell.continuity.impl.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.api.client.ProcessingDataKey;
import me.pepperbell.continuity.api.client.ProcessingDataKeyRegistry;
import net.minecraft.util.Identifier;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class ProcessingDataKeyRegistryImpl implements ProcessingDataKeyRegistry {
	public static final ProcessingDataKeyRegistryImpl INSTANCE = new ProcessingDataKeyRegistryImpl();

	private final Map<Identifier, ProcessingDataKey<?>> keyMap = new Object2ObjectOpenHashMap<>();
	private final List<ProcessingDataKey<?>> allResettable = new ObjectArrayList<>();
	private final List<ProcessingDataKey<?>> allResettableView = Collections.unmodifiableList(allResettable);

	private int registeredAmount = 0;
	private boolean frozen;

	@Override
	public <T> ProcessingDataKey<T> registerKey(Identifier id, Supplier<T> valueSupplier, Consumer<T> valueResetAction) {
		if (frozen) {
			throw new IllegalArgumentException("Cannot register processing data key for ID '" + id + "' to frozen registry");
		}
		ProcessingDataKey<?> oldKey = keyMap.get(id);
		if (oldKey != null) {
			throw new IllegalArgumentException("Cannot override processing data key registration for ID '" + id + "'");
		}
		ProcessingDataKeyImpl<T> key = new ProcessingDataKeyImpl<>(id, registeredAmount, valueSupplier, valueResetAction);
		keyMap.put(id, key);
		if (valueResetAction != null) {
			allResettable.add(key);
		}
		registeredAmount++;
		return key;
	}

	@Override
	@Nullable
	public ProcessingDataKey<?> getKey(Identifier id) {
		return keyMap.get(id);
	}

	@Override
	public int getRegisteredAmount() {
		return registeredAmount;
	}

	public void init() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
	}
	
	public void clientSetup(FMLClientSetupEvent event) {
		frozen = true;
	}

	public List<ProcessingDataKey<?>> getAllResettable() {
		return allResettableView;
	}
}

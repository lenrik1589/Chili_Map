package here.lenrik.chili_map.mixin.client;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.WorldSaveStorage;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftServer.class)
public interface SessionAccessor {
	@NotNull @Accessor
	WorldSaveStorage.Session getSession ();
}

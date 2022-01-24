package here.lenrik.chili_map.mixin.client;

import here.lenrik.chili_map.client.SessionAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements SessionAccessor {
	@Final @Shadow protected
	LevelStorage.Session session;

	@NotNull @Override public LevelStorage.Session getSession () {
		return session;
	}
}

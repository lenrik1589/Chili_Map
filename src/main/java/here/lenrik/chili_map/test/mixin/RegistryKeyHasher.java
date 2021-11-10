package here.lenrik.chili_map.test.mixin;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RegistryKey.class)
public class RegistryKeyHasher{
	@Shadow @Final private Identifier registry;

	@Shadow @Final private Identifier value;

	public int hashCode( ){
		return registry.hashCode() * 31 + value.hashCode();
	}
}

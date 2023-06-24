package here.lenrik.chili_map.mixin;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RegistryKey.class)
public class RegistryKeyHasher{
	@Shadow @Final private Identifier registry;

	@Shadow @Final private Identifier value;

	public int hashCode( ){
		return 31 * registry.hashCode() + value.hashCode();
	}
}

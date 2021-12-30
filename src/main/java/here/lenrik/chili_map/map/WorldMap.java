package here.lenrik.chili_map.map;

import here.lenrik.chili_map.misc.Misc;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;

public class WorldMap {
	private       Text                            worldName;
	private       Identifier                      id;
	private final HashMap<Misc.Vec2i, JRegionMap> regions = new HashMap<>();

	public JRegionMap getRegion (Vec3d pos) {
		return getRegion(Math.round(pos.x), Math.round(pos.z));
	}

	private JRegionMap getRegion (long x, long z) {
		Misc.Vec2i pos = Misc.mapPos(x, z, 0);
		return regions.compute(pos, (posVec2i, region) -> region != null? region : new JRegionMap(posVec2i));
	}
}

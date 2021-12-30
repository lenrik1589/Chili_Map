package here.lenrik.chili_map.map;

import here.lenrik.chili_map.misc.Misc.Vec2i;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.HashMap;

public class JRegionMap {
//	private HashMap<Vec3i, JAreaMap> areas = new HashMap<>();
	private Vec2i                    pos;

	public JRegionMap (Vec2i pos) {
		this.pos = pos;
	}

	public AreaMap getMap (Vec3d pos, int zoom) {
		return getMap(new Vec3i(pos.x, pos.z, zoom));
	}

	private AreaMap getMap (Vec3i zoomPos) {
		return null;
	}
}

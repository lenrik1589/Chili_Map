package here.lenrik.chili_map.to_remove;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapBannerMarker;
import net.minecraft.item.map.MapFrameMarker;
import net.minecraft.item.map.MapIcon;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.BlockView;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.dimension.DimensionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Deprecated
public class MapState extends PersistentState {
	private static final Logger LOGGER = LogManager.getLogger();
	/**
	 * The scaled center coordinate of the map state on the X axis.
	 * <p>
	 * Always {@code 0} for the client.
	 */
	public final int centerX;
	/**
	 * The scaled center coordinate of the map state on the Z axis.
	 * <p>
	 * Always {@code 0} for the client.
	 */
	public final int centerZ;
	public final RegistryKey<World> dimension;
	private final boolean showIcons;
	private final boolean unlimitedTracking;
	public final byte scale;
	public byte[] colors = new byte[16384];
	public final boolean locked;
	private final List<PlayerUpdateTracker> updateTrackers = Lists.newArrayList();
	private final Map<PlayerEntity, PlayerUpdateTracker> updateTrackersByPlayer = Maps.newHashMap();
	/**
	 * The banner markers to track in world.
	 * <p>
	 * Empty for the client.
	 */
	private final Map<String, MapBannerMarker> banners = Maps.newHashMap();
	final Map<String, MapIcon> icons = Maps.newLinkedHashMap();
	private final Map<String, MapFrameMarker> frames = Maps.newHashMap();
	private int iconCount;

	private MapState(int i, int j, byte b, boolean bl, boolean bl2, boolean bl3, RegistryKey<World> registryKey) {
		this.scale = b;
		this.centerX = i;
		this.centerZ = j;
		this.dimension = registryKey;
		this.showIcons = bl;
		this.unlimitedTracking = bl2;
		this.locked = bl3;
		this.markDirty();
	}

	/**
	 * Creates a new map state instance.
	 * 
	 * @param centerX the absolute center X-coordinate
	 * @param centerZ the absolute center Z-coordinate
	 */
	public static MapState of(double centerX, double centerZ, byte scale, boolean showIcons, boolean unlimitedTracking, RegistryKey<World> dimension) {
		int i = 128 * (1 << scale);
		int j = MathHelper.floor((centerX + 64.0D) / (double)i);
		int k = MathHelper.floor((centerZ + 64.0D) / (double)i);
		int l = j * i + i / 2 - 64;
		int m = k * i + i / 2 - 64;
		return new MapState(l, m, scale, showIcons, unlimitedTracking, false, dimension);
	}

	/**
	 * Creates a new map state instance for the client.
	 * <p>
	 * The client is not aware of the coordinates of the map state so its center coordinates will always be {@code (0, 0)}.
	 */
	public static MapState of(byte scale, boolean showIcons, RegistryKey<World> dimension) {
		return new MapState(0, 0, scale, false, false, showIcons, dimension);
	}

	public static MapState fromNbt(NbtCompound nbt) {
		DataResult<RegistryKey<World>> var10000 = DimensionType.worldFromDimensionNbt(new Dynamic<>(NbtOps.INSTANCE, nbt.get("dimension")));
		Logger var10001 = LOGGER;
		Objects.requireNonNull(var10001);
		RegistryKey<World> registryKey = var10000.resultOrPartial(var10001::error).orElseThrow(() -> new IllegalArgumentException("Invalid map dimension: " + nbt.get("dimension")));
		int i = nbt.getInt("xCenter");
		int j = nbt.getInt("zCenter");
		byte b = (byte)MathHelper.clamp(nbt.getByte("scale"), 0, 4);
		boolean bl = !nbt.contains("trackingPosition", 1) || nbt.getBoolean("trackingPosition");
		boolean bl2 = nbt.getBoolean("unlimitedTracking");
		boolean bl3 = nbt.getBoolean("locked");
		MapState mapState = new MapState(i, j, b, bl, bl2, bl3, registryKey);
		byte[] bs = nbt.getByteArray("colors");
		if (bs.length == 16384) {
			mapState.colors = bs;
		}

		NbtList nbtList = nbt.getList("banners", 10);

		for(int k = 0; k < nbtList.size(); ++k) {
			MapBannerMarker mapBannerMarker = MapBannerMarker.fromNbt(nbtList.getCompound(k));
			mapState.banners.put(mapBannerMarker.getKey(), mapBannerMarker);
			mapState.addIcon(mapBannerMarker.getIconType(), null, mapBannerMarker.getKey(), mapBannerMarker.getPos().getX(), mapBannerMarker.getPos().getZ(), 180.0D, mapBannerMarker.getName());
		}

		NbtList k = nbt.getList("frames", 10);

		for(int mapBannerMarker = 0; mapBannerMarker < k.size(); ++mapBannerMarker) {
			MapFrameMarker mapFrameMarker = MapFrameMarker.fromNbt(k.getCompound(mapBannerMarker));
			mapState.frames.put(mapFrameMarker.getKey(), mapFrameMarker);
			mapState.addIcon(MapIcon.Type.FRAME, null, "frame-" + mapFrameMarker.getEntityId(), mapFrameMarker.getPos().getX(), mapFrameMarker.getPos().getZ(), mapFrameMarker.getRotation(), null);
		}

		return mapState;
	}

	public NbtCompound writeNbt(NbtCompound nbt) {
		DataResult<NbtElement> var10000 = Identifier.CODEC.encodeStart(NbtOps.INSTANCE, this.dimension.getValue());
		Logger var10001 = LOGGER;
		Objects.requireNonNull(var10001);
		var10000.resultOrPartial(var10001::error).ifPresent((nbtElement) -> nbt.put("dimension", nbtElement));
		nbt.putInt("xCenter", this.centerX);
		nbt.putInt("zCenter", this.centerZ);
		nbt.putByte("scale", this.scale);
		nbt.putByteArray("colors", this.colors);
		nbt.putBoolean("trackingPosition", this.showIcons);
		nbt.putBoolean("unlimitedTracking", this.unlimitedTracking);
		nbt.putBoolean("locked", this.locked);
		NbtList nbtList = new NbtList();

		for(MapBannerMarker mapBannerMarker : this.banners.values()){
			nbtList.add(mapBannerMarker.getNbt());
		}

		nbt.put("banners", nbtList);
		NbtList nbtList2 = new NbtList();

		for(MapFrameMarker mapFrameMarker : this.frames.values()){
			nbtList2.add(mapFrameMarker.toNbt());
		}

		nbt.put("frames", nbtList2);
		return nbt;
	}

	public MapState copy() {
		MapState mapState = new MapState(this.centerX, this.centerZ, this.scale, this.showIcons, this.unlimitedTracking, true, this.dimension);
		mapState.banners.putAll(this.banners);
		mapState.icons.putAll(this.icons);
		mapState.iconCount = this.iconCount;
		System.arraycopy(this.colors, 0, mapState.colors, 0, this.colors.length);
		mapState.markDirty();
		return mapState;
	}

	/**
	 * Creates a new map state which is a zoomed out version of the current one.
	 * <p>
	 * The scale of the new map state is {@code currentScale + zoomOutScale} and clamped between {@code 0} and {@code 4}.
	 * <p>
	 * The colors are not copied, neither are the icons.
	 * 
	 * @param zoomOutScale the amount to add to the scale of the map
	 */
	public MapState zoomOut(int zoomOutScale) {
		return of(this.centerX, this.centerZ, (byte)MathHelper.clamp(this.scale + zoomOutScale, 0, 4), this.showIcons, this.unlimitedTracking, this.dimension);
	}

	public void update(PlayerEntity player, ItemStack stack) {
		if (!this.updateTrackersByPlayer.containsKey(player)) {
			PlayerUpdateTracker playerUpdateTracker = new PlayerUpdateTracker(player);
			this.updateTrackersByPlayer.put(player, playerUpdateTracker);
			this.updateTrackers.add(playerUpdateTracker);
		}

		if (!player.getInventory().contains(stack)) {
			this.removeIcon(player.getName().getString());
		}

		for(int playerUpdateTracker = 0; playerUpdateTracker < this.updateTrackers.size(); ++playerUpdateTracker) {
			PlayerUpdateTracker playerUpdateTracker2 = this.updateTrackers.get(playerUpdateTracker);
			String string = playerUpdateTracker2.player.getName().getString();
			if (!playerUpdateTracker2.player.isRemoved() && (playerUpdateTracker2.player.getInventory().contains(stack) || stack.isInFrame())) {
				if (!stack.isInFrame() && playerUpdateTracker2.player.world.getRegistryKey() == this.dimension && this.showIcons) {
					this.addIcon(MapIcon.Type.PLAYER, playerUpdateTracker2.player.world, string, playerUpdateTracker2.player.getX(), playerUpdateTracker2.player.getZ(), playerUpdateTracker2.player.getYaw(), null);
				}
			} else {
				this.updateTrackersByPlayer.remove(playerUpdateTracker2.player);
				this.updateTrackers.remove(playerUpdateTracker2);
				this.removeIcon(string);
			}
		}

		if (stack.isInFrame() && this.showIcons) {
			ItemFrameEntity playerUpdateTracker = stack.getFrame();
			BlockPos playerUpdateTracker2 = playerUpdateTracker.getDecorationBlockPos();
			MapFrameMarker string = this.frames.get(MapFrameMarker.getKey(playerUpdateTracker2));
			if (string != null && playerUpdateTracker.getId() != string.getEntityId() && this.frames.containsKey(string.getKey())) {
				this.removeIcon("frame-" + string.getEntityId());
			}

			MapFrameMarker mapFrameMarker = new MapFrameMarker(playerUpdateTracker2, playerUpdateTracker.getHorizontalFacing().getHorizontal() * 90, playerUpdateTracker.getId());
			this.addIcon(MapIcon.Type.FRAME, player.world, "frame-" + playerUpdateTracker.getId(), playerUpdateTracker2.getX(), playerUpdateTracker2.getZ(), playerUpdateTracker.getHorizontalFacing().getHorizontal() * 90, null);
			this.frames.put(mapFrameMarker.getKey(), mapFrameMarker);
		}

		NbtCompound playerUpdateTracker = stack.getNbt();
		if (playerUpdateTracker != null && playerUpdateTracker.contains("Decorations", 9)) {
			NbtList playerUpdateTracker2 = playerUpdateTracker.getList("Decorations", 10);

			for(int string = 0; string < playerUpdateTracker2.size(); ++string) {
				NbtCompound mapFrameMarker = playerUpdateTracker2.getCompound(string);
				if (!this.icons.containsKey(mapFrameMarker.getString("id"))) {
					this.addIcon(MapIcon.Type.byId(mapFrameMarker.getByte("type")), player.world, mapFrameMarker.getString("id"), mapFrameMarker.getDouble("x"), mapFrameMarker.getDouble("z"), mapFrameMarker.getDouble("rot"), null);
				}
			}
		}

	}

	private void removeIcon(String id) {
		MapIcon mapIcon = this.icons.remove(id);
		if (mapIcon != null && mapIcon.getType().method_37342()) {
			--this.iconCount;
		}

		this.markIconsDirty();
	}

	public static void addDecorationsNbt(ItemStack stack, BlockPos pos, String id, MapIcon.Type type) {
		NbtList nbtList;
		if (stack.hasNbt() && stack.getNbt().contains("Decorations", 9)) {
			nbtList = stack.getNbt().getList("Decorations", 10);
		} else {
			nbtList = new NbtList();
			stack.setSubNbt("Decorations", nbtList);
		}

		NbtCompound nbtCompound = new NbtCompound();
		nbtCompound.putByte("type", type.getId());
		nbtCompound.putString("id", id);
		nbtCompound.putDouble("x", pos.getX());
		nbtCompound.putDouble("z", pos.getZ());
		nbtCompound.putDouble("rot", 180.0D);
		nbtList.add(nbtCompound);
		if (type.hasTintColor()) {
			NbtCompound nbtCompound2 = stack.getOrCreateSubNbt("display");
			nbtCompound2.putInt("MapColor", type.getTintColor());
		}

	}

	private void addIcon(MapIcon.Type type, @Nullable WorldAccess world, String key, double x, double z, double rotation, @Nullable Text text) {
		int i = 1 << this.scale;
		float f = (float)(x - (double)this.centerX) / (float)i;
		float g = (float)(z - (double)this.centerZ) / (float)i;
		byte b = (byte)((int)((double)(f * 2.0F) + 0.5D));
		byte c = (byte)((int)((double)(g * 2.0F) + 0.5D));
		byte d;
		if (f >= -63.0F && g >= -63.0F && f <= 63.0F && g <= 63.0F) {
			rotation += rotation < 0.0D ? -8.0D : 8.0D;
			d = (byte)((int)(rotation * 16.0D / 360.0D));
			if (this.dimension == World.NETHER && world != null) {
				int k = (int)(world.getLevelProperties().getTimeOfDay() / 10L);
				d = (byte)(k * k * 34187121 + k * 121 >> 15 & 15);
			}
		} else {
			if (type != MapIcon.Type.PLAYER) {
				this.removeIcon(key);
				return;
			}

			if (Math.abs(f) < 320.0F && Math.abs(g) < 320.0F) {
				type = MapIcon.Type.PLAYER_OFF_MAP;
			} else {
				if (!this.unlimitedTracking) {
					this.removeIcon(key);
					return;
				}

				type = MapIcon.Type.PLAYER_OFF_LIMITS;
			}

			d = 0;
			if (f <= -63.0F) {
				b = -128;
			}

			if (g <= -63.0F) {
				c = -128;
			}

			if (f >= 63.0F) {
				b = 127;
			}

			if (g >= 63.0F) {
				c = 127;
			}
		}

		MapIcon k = new MapIcon(type, b, c, d, text);
		MapIcon mapIcon = this.icons.put(key, k);
		if (!k.equals(mapIcon)) {
			if (mapIcon != null && mapIcon.getType().method_37342()) {
				--this.iconCount;
			}

			if (type.method_37342()) {
				++this.iconCount;
			}

			this.markIconsDirty();
		}

	}

	@Nullable
	public Packet<?> getPlayerMarkerPacket(int id, PlayerEntity player) {
		PlayerUpdateTracker playerUpdateTracker = this.updateTrackersByPlayer.get(player);
		return playerUpdateTracker == null ? null : playerUpdateTracker.getPacket(id);
	}

	private void markDirty(int x, int z) {
		this.markDirty();

		for(PlayerUpdateTracker playerUpdateTracker : this.updateTrackers){
			playerUpdateTracker.markDirty(x, z);
		}

	}

	private void markIconsDirty() {
		this.markDirty();
		this.updateTrackers.forEach(PlayerUpdateTracker::markIconsDirty);
	}

	public PlayerUpdateTracker getPlayerSyncData(PlayerEntity player) {
		PlayerUpdateTracker playerUpdateTracker = this.updateTrackersByPlayer.get(player);
		if (playerUpdateTracker == null) {
			playerUpdateTracker = new PlayerUpdateTracker(player);
			this.updateTrackersByPlayer.put(player, playerUpdateTracker);
			this.updateTrackers.add(playerUpdateTracker);
		}

		return playerUpdateTracker;
	}

	public boolean addBanner(WorldAccess world, BlockPos pos) {
		double d = (double)pos.getX() + 0.5D;
		double e = (double)pos.getZ() + 0.5D;
		int i = 1 << this.scale;
		double f = (d - (double)this.centerX) / (double)i;
		double g = (e - (double)this.centerZ) / (double)i;
		if (f >= -63.0D && g >= -63.0D && f <= 63.0D && g <= 63.0D) {
			MapBannerMarker mapBannerMarker = MapBannerMarker.fromWorldBlock(world, pos);
			if (mapBannerMarker == null) {
				return false;
			}

			if (this.banners.remove(mapBannerMarker.getKey(), mapBannerMarker)) {
				this.removeIcon(mapBannerMarker.getKey());
				return true;
			}

			if (!this.method_37343(256)) {
				this.banners.put(mapBannerMarker.getKey(), mapBannerMarker);
				this.addIcon(mapBannerMarker.getIconType(), world, mapBannerMarker.getKey(), d, e, 180.0D, mapBannerMarker.getName());
				return true;
			}
		}

		return false;
	}

	public void removeBanner(BlockView world, int x, int z) {
		Iterator<MapBannerMarker> markerIterator = this.banners.values().iterator();

		while(markerIterator.hasNext()) {
			MapBannerMarker mapBannerMarker = markerIterator.next();
			if (mapBannerMarker.getPos().getX() == x && mapBannerMarker.getPos().getZ() == z) {
				MapBannerMarker mapBannerMarker2 = MapBannerMarker.fromWorldBlock(world, mapBannerMarker.getPos());
				if (!mapBannerMarker.equals(mapBannerMarker2)) {
					markerIterator.remove();
					this.removeIcon(mapBannerMarker.getKey());
				}
			}
		}

	}

	public Collection<MapBannerMarker> getBanners() {
		return this.banners.values();
	}

	public void removeFrame(BlockPos pos, int id) {
		this.removeIcon("frame-" + id);
		this.frames.remove(MapFrameMarker.getKey(pos));
	}

	/**
	 * Sets the color at the specified coordinates if the current color is different.
	 * 
	 * @return {@code true} if the color has been updated, else {@code false}
	 */
	public boolean putColor(int x, int z, byte color) {
		byte b = this.colors[x + z * 128];
		if (b != color) {
			this.setColor(x, z, color);
			return true;
		} else {
			return false;
		}
	}

	public void setColor(int x, int z, byte color) {
		this.colors[x + z * 128] = color;
		this.markDirty(x, z);
	}

	public boolean hasMonumentIcon() {
		Iterator var1 = this.icons.values().iterator();

		MapIcon mapIcon;
		do {
			if (!var1.hasNext()) {
				return false;
			}

			mapIcon = (MapIcon)var1.next();
		} while(mapIcon.getType() != MapIcon.Type.MANSION && mapIcon.getType() != MapIcon.Type.MONUMENT);

		return true;
	}

	public void replaceIcons(List<MapIcon> icons) {
		this.icons.clear();
		this.iconCount = 0;

		for(int i = 0; i < icons.size(); ++i) {
			MapIcon mapIcon = icons.get(i);
			this.icons.put("icon-" + i, mapIcon);
			if (mapIcon.getType().method_37342()) {
				++this.iconCount;
			}
		}

	}

	public Iterable<MapIcon> getIcons() {
		return this.icons.values();
	}

	public boolean method_37343(int i) {
		return this.iconCount >= i;
	}

	public class PlayerUpdateTracker {
		public final PlayerEntity player;
		private boolean dirty = true;
		private int startX;
		private int startZ;
		private int endX = 127;
		private int endZ = 127;
		private boolean iconsDirty = true;
		private int emptyPacketsRequested;
		public int field_131;

		PlayerUpdateTracker(PlayerEntity playerEntity) {
			this.player = playerEntity;
		}

		private UpdateData getMapUpdateData() {
			int i = this.startX;
			int j = this.startZ;
			int k = this.endX + 1 - this.startX;
			int l = this.endZ + 1 - this.startZ;
			byte[] bs = new byte[k * l];

			for(int m = 0; m < k; ++m) {
				for(int n = 0; n < l; ++n) {
					bs[m + n * k] = MapState.this.colors[i + m + (j + n) * 128];
				}
			}

			return new UpdateData(i, j, k, l, bs);
		}

		@Nullable
		Packet<?> getPacket(int mapId) {
			UpdateData updateData;
			if (this.dirty) {
				this.dirty = false;
				updateData = this.getMapUpdateData();
			} else {
				updateData = null;
			}

			Collection<MapIcon> collection;
			if (this.iconsDirty && this.emptyPacketsRequested++ % 5 == 0) {
				this.iconsDirty = false;
				collection = MapState.this.icons.values();
			} else {
				collection = null;
			}

			return collection == null && updateData == null ? null : new MapUpdateS2CPacket(mapId, MapState.this.scale, MapState.this.locked, collection, updateData);
		}

		void markDirty(int startX, int startZ) {
			if (this.dirty) {
				this.startX = Math.min(this.startX, startX);
				this.startZ = Math.min(this.startZ, startZ);
				this.endX = Math.max(this.endX, startX);
				this.endZ = Math.max(this.endZ, startZ);
			} else {
				this.dirty = true;
				this.startX = startX;
				this.startZ = startZ;
				this.endX = startX;
				this.endZ = startZ;
			}

		}

		private void markIconsDirty() {
			this.iconsDirty = true;
		}
	}

	public static class UpdateData extends net.minecraft.item.map.MapState.UpdateData{
		public final int startX;
		public final int startZ;
		public final int width;
		public final int height;
		public final byte[] colors;

		public UpdateData(int i, int j, int k, int l, byte[] bs) {
			super(i, j, k, l, bs);
			this.startX = i;
			this.startZ = j;
			this.width = k;
			this.height = l;
			this.colors = bs;
		}

		public void setColorsTo(MapState mapState) {
			for(int i = 0; i < this.width; ++i) {
				for(int j = 0; j < this.height; ++j) {
					mapState.setColor(this.startX + i, this.startZ + j, this.colors[i + j * this.width]);
				}
			}

		}
	}
}

package here.lenrik.chili_map.map;

import here.lenrik.chili_map.ChilliMap;
import here.lenrik.chili_map.client.ChilliMapClient;
import here.lenrik.chili_map.misc.Misc;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

import static here.lenrik.chili_map.misc.Misc.Vec2i;


public class MapContainer extends PersistentState implements AutoCloseable{
	private final Path savePath;
	public TextureManager textureManager;
	private long createdAt;
	private final MinecraftClient client;
	private final HashMap<Identifier, HashMap<Vec2i, MapRegion>> levels = new HashMap<>();
	private final HashMap<Identifier, HashMap<Misc.Vec2i, MapTexture>> mapTextures = new HashMap<>();
	private Text name;

	MapContainer(MinecraftClient client, Path path){
		textureManager = client.getTextureManager();
		this.savePath = path;
		this.client = client;
	}

	public static MapContainer loadRegions(Path mapsStoragePath, String name){
		MapContainer container = new MapContainer(MinecraftClient.getInstance(), mapsStoragePath);
		container.setName(name);
		container.restoreFromFile();
		return container;
	}

	private void restoreFromFile( ){
		try{
			NbtCompound readData = null;
			NbtCompound autoSaveData = null;
			try{
				readData = NbtIo.readCompressed(savePath.toFile());
			} catch(FileNotFoundException ignored) {
				ChilliMap.LOGGER.info("Could not find save file, checking autosave");
			}
			try{
				autoSaveData = NbtIo.readCompressed(savePath.resolveSibling("autosave_" + savePath.getFileName()).toFile());
			} catch(FileNotFoundException ignored) {
				ChilliMap.LOGGER.info("Could not find autosave");
			}
			if(readData == null && autoSaveData == null){
				createdAt = Instant.now().toEpochMilli();
			} else {
				if(readData == null){
					readData = autoSaveData.getCompound("data");
				} else if(autoSaveData != null){
					var savedAt = readData.getCompound("data").getLong("savedAt");
					var autoSavedAt = autoSaveData.getCompound("data").getLong("savedAt");
					if(savedAt < autoSavedAt){
						ChilliMap.LOGGER.info("loading autosave data as it appears newer than save");
						readData = autoSaveData.getCompound("data");
					} else {
						readData = readData.getCompound("data");
					}
				} else {
					readData = readData.getCompound("data");
				}
				createdAt = readData.getLong("createdAt");
				var levelsNbt = readData.getCompound("levels");
				for(String idKey : levelsNbt.getKeys()){
					var id = Identifier.tryParse(idKey);
					if(id == null) continue;
					var levelNbt = (NbtCompound) levelsNbt.get(idKey);
					var level = new HashMap<Vec2i, MapRegion>();
					for(String posKey : Objects.requireNonNull(levelNbt).getKeys()){
						var pos = new Vec2i(Integer.decode(posKey.split("_")[0]), Integer.decode(posKey.split("_")[1]));
						level.put(pos, MapRegion.fromNbt((NbtCompound) Objects.requireNonNull(levelNbt.get(posKey)), id));
					}
					levels.put(id, level);
				}
			}
		} catch(FileNotFoundException fnfe) {
			ChilliMap.LOGGER.info("No saves found for {}", name.asString());
		} catch(IOException e) {
			ChilliMap.LOGGER.error("Could not load data from {}", savePath, e);
		}
	}

	public MapTexture getMapTexture(Misc.Vec2i position){
		assert MinecraftClient.getInstance().world != null;
		return getMapTexture(MinecraftClient.getInstance().world.getRegistryKey().getValue(), position);
	}

	public MapTexture getMapTexture(Identifier dimension, Misc.Vec2i position){
		return getMapTexture(dimension, position, getRegion(dimension, position));
	}


	MapTexture getMapTexture(Identifier dimension, Misc.Vec2i position, MapRegion region){
		return this.mapTextures.compute(dimension, (dim, map) -> map == null? new HashMap<>() : map).compute(position, (pos, mapTexture) -> {
			if(mapTexture == null){
				return new MapTexture(this, pos, region);
			} else {
				mapTexture.setState(region);
				return mapTexture;
			}
		});
	}

	public void updateTexture(Misc.Vec2i pos, boolean force){
		if(force){
			ChilliMapClient.mapContainer.getMapTexture(pos).updateTexture();
		} else
			ChilliMapClient.mapContainer.getMapTexture(pos).setNeedsUpdate();
	}

	public MapRegion getRegion(Vec2i pos){
		assert MinecraftClient.getInstance().world != null;
		return getRegion(MinecraftClient.getInstance().world.getRegistryKey().getValue(), pos);
	}

	public MapRegion getRegion(Identifier dimension, Vec2i pos){
		return levels.compute(
				dimension,
				(p, l) -> l == null? new HashMap<>() : l
		).compute(
				pos,
				(p, r) -> r == null? new MapRegion((byte) 0, p.x() * 128, p.y() * 128, dimension) : r
		);
	}

	public void updateRegion(World world, Vec2i pos){
		var region = getRegion(world.getRegistryKey().getValue(), pos);
		region.updateColors(world, client.player);
	}

	public Set<Vec2i> getMapsPos(Identifier dimension){
		return levels.get(dimension).keySet();
	}

	private void setName(String name){

		this.name = Text.of(name);
	}

	public Text getName( ){
		return name;
	}

	public NbtCompound writeNbt(NbtCompound nbt){
		nbt.putLong("createdAt", createdAt);
		nbt.putLong("savedAt", Instant.now().toEpochMilli());
		var levelsNbt = new NbtCompound();
		for(Identifier id : levels.keySet()){
			var levelNbt = new NbtCompound();
			for(Vec2i pos : levels.get(id).keySet()){
				if(levels.get(id).get(pos).isEmpty()) continue;
				levelNbt.put(pos.x() + "_" + pos.y(), levels.get(id).get(pos).writeNbt(new NbtCompound()));
			}
			levelsNbt.put(id.toString(), levelNbt);
		}
		nbt.put("levels", levelsNbt);
		return nbt;
	}

	public void save( ){
		var preSave = isDirty();
		markDirty();
		savePath.getParent().toFile().mkdirs();
		ChilliMap.LOGGER.info("Saving map data for {}", name.asString());
		super.save(savePath.toFile());
		if(preSave) markDirty();
	}

	public void autoSave( ){
		var preSave = isDirty();
		markDirty();
		var path = savePath.resolveSibling("autosave_" + savePath.getFileName());
		path.getParent().toFile().mkdirs();
		super.save(path.toFile());
		if(preSave) markDirty();
	}

	public void close( ){
		save();
		reset();
	}

	public void reset( ){
		mapTextures.clear();
		levels.clear();
//		savePath = savePath.resolveSibling("");
	}
}

package here.lenrik.chili_map.map;

import com.google.common.collect.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.map.MapBannerMarker;
import net.minecraft.item.map.MapIcon;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.*;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;

public class MapRegion extends PersistentState{
	public final byte scale;
	public final int centerX;
	public final int centerZ;
	public byte[] colors = new byte[16384];
	private final Identifier dimension;
	final Map<String, MapIcon> icons = Maps.newLinkedHashMap();
	private final Map<String, MapBannerMarker> banners = Maps.newHashMap();
	private int iconCount = 0;
	private int updateCounter = 0;

	public MapRegion(byte scale, int centerX, int centerZ, Identifier dimension){
		this.dimension = dimension;
		this.scale = scale;
		this.centerX = centerX;
		this.centerZ = centerZ;
	}

	public boolean isPlayerInbound(AbstractClientPlayerEntity player){
		int radius = 64 * (1 << scale);
		return player.getBlockX() - centerX > -radius &&
				player.getBlockX() - centerX <= radius &&
				player.getBlockZ() - centerZ > -radius &&
				player.getBlockZ() - centerZ <= radius
				;
	}

	public void updateColors(World world, Entity entity){
		MinecraftClient.getInstance().getProfiler().push("chili_map_region_update");
		if(world.getRegistryKey().getValue().equals(dimension) && entity instanceof PlayerEntity){
			int scale = 1 << this.scale;
			int centerX = this.centerX;
			int centerZ = this.centerZ;
			int mapX = MathHelper.floor(entity.getX() - (double) centerX) / scale + 64;
			int mapZ = MathHelper.floor(entity.getZ() - (double) centerZ) / scale + 64;
			int loadRadus = 128 / scale;
			boolean hasLevelChange = false;
			if(world.getDimension().hasCeiling()){
				loadRadus /= 2;

				int prevH = Integer.MIN_VALUE;
				exitFor:
				for(int x = centerX - scale * 64; x < centerX + scale * 64; ++x){
					for(int y = centerZ - scale * 64; y < centerZ + scale * 64; ++y){
						int highest = world.getWorldChunk(new BlockPos(x, 0, y)).sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x & 15, y & 15) + 1;
						if(prevH != Integer.MIN_VALUE || prevH != highest){
							hasLevelChange = true;
							break exitFor;
						}
						prevH = highest;
					}
				}
			}

			++updateCounter;
			boolean prevUpdated = false;

			for(int pixelX = mapX - loadRadus + 1; pixelX < mapX + loadRadus; ++pixelX){
				if((pixelX & 15) == (updateCounter & 15) || prevUpdated /*|| true*/){
					prevUpdated = false;
					double previousPixelHeight = 0.0D;

					for(int pixelZ = mapZ - loadRadus - 1; pixelZ < mapZ + loadRadus; ++pixelZ){
						if(pixelX >= 0 && pixelZ >= -1 && pixelX < 128 && pixelZ < 128){
							int deltaX = pixelX - mapX;
							int deltaZ = pixelZ - mapZ;
							boolean isEdge = deltaX * deltaX + deltaZ * deltaZ > (loadRadus - 2) * (loadRadus - 2);
							int s = (centerX / scale + pixelX - 64) * scale;
							int t = (centerZ / scale + pixelZ - 64) * scale;
							Multiset<MapColor> multiset = LinkedHashMultiset.create();
							WorldChunk worldChunk = world.getWorldChunk(new BlockPos(s, 0, t));
							if(!worldChunk.isEmpty()){
								ChunkPos chunkPos = worldChunk.getPos();
								int xInChunk = s & 15;
								int zInChunk = t & 15;
								int topFluidCount = 0;
								double currentPixelHeight = 0.0D;
								if(world.getDimension().hasCeiling() && !hasLevelChange){
									int randomNumber = s + t * 231871;
									randomNumber = randomNumber * randomNumber * 31287121 + randomNumber * 11;
									if((randomNumber >> 20 & 1) == 0){
										multiset.add(Blocks.DIRT.getDefaultState().getMapColor(world, BlockPos.ORIGIN), 10);
									} else {
										multiset.add(Blocks.STONE.getDefaultState().getMapColor(world, BlockPos.ORIGIN), 100);
									}

									currentPixelHeight = 100.0D;
								} else {
									BlockPos.Mutable highestBP = new BlockPos.Mutable();
									BlockPos.Mutable mutable = new BlockPos.Mutable();

									for(int pixelDeltaX = 0; pixelDeltaX < scale; ++pixelDeltaX){
										for(int pixelDeltaZ = 0; pixelDeltaZ < scale; ++pixelDeltaZ){
											int highest = worldChunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, pixelDeltaX + xInChunk, pixelDeltaZ + zInChunk) + 1;
											BlockState blockState;
											if(highest <= world.getBottomY() + 1){
												blockState = Blocks.BEDROCK.getDefaultState();
											} else {
												do {
													--highest;
													highestBP.set(chunkPos.getStartX() + pixelDeltaX + xInChunk, highest, chunkPos.getStartZ() + pixelDeltaZ + zInChunk);
													blockState = worldChunk.getBlockState(highestBP);
												} while(blockState.getMapColor(world, highestBP) == MapColor.CLEAR && highest > world.getBottomY());

												if(highest > world.getBottomY() && !blockState.getFluidState().isEmpty()){
													int lowest = highest - 1;
													mutable.set(highestBP);

													BlockState blockState2;
													do {
														mutable.setY(lowest--);
														blockState2 = worldChunk.getBlockState(mutable);
														++topFluidCount;
													} while(lowest > world.getBottomY() && !blockState2.getFluidState().isEmpty());

													blockState = getFluidStateIfVisible(world, blockState, highestBP);
												}
											}

											this.removeBanner(world, chunkPos.getStartX() + pixelDeltaX + xInChunk, chunkPos.getStartZ() + pixelDeltaZ + zInChunk);
											currentPixelHeight += (double) highest / (double) (scale * scale);
											if(!world.getDimension().hasCeiling() || (world.getRegistryKey() == World.NETHER && !blockState.getBlock().equals(Blocks.BEDROCK))){
												multiset.add(blockState.getMapColor(world, highestBP));
											} else {
												int randomNumber = s + t * 231871;
												randomNumber = randomNumber * randomNumber * 31287121 + randomNumber * 11;
												if((randomNumber >> 20 & 1) == 0){
													multiset.add(Blocks.DIRT.getDefaultState().getMapColor(world, BlockPos.ORIGIN), 10);
												} else {
													multiset.add(Blocks.STONE.getDefaultState().getMapColor(world, BlockPos.ORIGIN), 100);
												}
											}
										}
									}
								}

								topFluidCount /= scale * scale;
								double depth_height = (currentPixelHeight - previousPixelHeight) * 4.0D / (double) (scale + 4) + ((double) (pixelX + pixelZ & 1) - 0.5D) * 0.4D;
								int shade = 1;
								if(depth_height > 0.6D){
									shade = 2;
								}

								if(depth_height < -0.6D){
									shade = 0;
								}

								MapColor color = Iterables.getFirst(Multisets.copyHighestCountFirst(multiset), MapColor.CLEAR);
								if(color == MapColor.WATER_BLUE){
									depth_height = (double) topFluidCount * 0.1D + (double) (pixelX + pixelZ & 1) * 0.2D;
									shade = 1;
									if(depth_height < 0.5D){
										shade = 2;
									}

									if(depth_height > 0.9D){
										shade = 0;
									}
								}

								previousPixelHeight = currentPixelHeight;
								if(pixelZ >= 0 && deltaX * deltaX + deltaZ * deltaZ < loadRadus * loadRadus && (!isEdge || (pixelX + pixelZ & 1) != 0)){
									prevUpdated |= this.putColor(pixelX, pixelZ, (byte) (color.id * 4 + shade));
								}
							}
						}
					}
				}
			}

		}
		MinecraftClient.getInstance().getProfiler().pop();
	}

	private boolean putColor(int pixelX, int pixelZ, byte color){
		byte current = this.colors[pixelX + pixelZ * 128];
		if(current != color){
			this.colors[pixelX + pixelZ * 128] = color;
			return true;
		} else {
			return false;
		}
	}

	private static BlockState getFluidStateIfVisible(World world, BlockState state, BlockPos pos){
		FluidState fluidState = state.getFluidState();
		return !fluidState.isEmpty() && !state.isSideSolidFullSquare(world, pos, Direction.UP)? fluidState.getBlockState() : state;
	}

	private void removeIcon(String id){
		MapIcon mapIcon = this.icons.remove(id);
		if(mapIcon != null && mapIcon.getType().method_37342()){
			--this.iconCount;
		}

//		this.markIconsDirty();
	}


	private void addIcon(MapIcon.Type type, @Nullable WorldAccess world, String key, double x, double z, double rotation, @Nullable Text text){
		int i = 1 << this.scale;
		float f = (float) (x - (double) this.centerX) / (float) i;
		float g = (float) (z - (double) this.centerZ) / (float) i;
		byte b = (byte) ((int) ((double) (f * 2.0F) + 0.5D));
		byte c = (byte) ((int) ((double) (g * 2.0F) + 0.5D));
		byte d;
		if(f >= -63.0F && g >= -63.0F && f <= 63.0F && g <= 63.0F){
			rotation += rotation < 0.0D? -8.0D : 8.0D;
			d = (byte) ((int) (rotation * 16.0D / 360.0D));
			if(this.dimension == World.NETHER.getValue() && world != null){
				int k = (int) (world.getLevelProperties().getTimeOfDay() / 10L);
				d = (byte) (k * k * 34187121 + k * 121 >> 15 & 15);
			}
		} else {
			if(type != MapIcon.Type.PLAYER){
				this.removeIcon(key);
				return;
			}

			if(Math.abs(f) < 320.0F && Math.abs(g) < 320.0F){
				type = MapIcon.Type.PLAYER_OFF_MAP;
			} /*else {
//				if (!this.unlimitedTracking) {
//					this.removeIcon(key);
//					return;
//				}

				type = MapIcon.Type.PLAYER_OFF_LIMITS;
			}*/

			d = 0;
			if(f <= -63.0F){
				b = -128;
			}

			if(g <= -63.0F){
				c = -128;
			}

			if(f >= 63.0F){
				b = 127;
			}

			if(g >= 63.0F){
				c = 127;
			}
		}

		MapIcon k = new MapIcon(type, b, c, d, text);
		MapIcon mapIcon = this.icons.put(key, k);
		if(!k.equals(mapIcon)){
			if(mapIcon != null && mapIcon.getType().method_37342()){
				--this.iconCount;
			}

			if(type.method_37342()){
				++this.iconCount;
			}

//			this.markIconsDirty();
		}

	}

	public void removeBanner(BlockView world, int x, int z){
		Iterator<MapBannerMarker> markerIterator = this.banners.values().iterator();

		while(markerIterator.hasNext()){
			MapBannerMarker mapBannerMarker = markerIterator.next();
			if(mapBannerMarker.getPos().getX() == x && mapBannerMarker.getPos().getZ() == z){
				MapBannerMarker mapBannerMarker2 = MapBannerMarker.fromWorldBlock(world, mapBannerMarker.getPos());
				if(!mapBannerMarker.equals(mapBannerMarker2)){
					markerIterator.remove();
					this.removeIcon(mapBannerMarker.getKey());
				}
			}
		}

	}

	public boolean addBanner(WorldAccess world, BlockPos pos){
		double blockXCenter = pos.getX() + 0.5;
		double blockZCenter = pos.getZ() + 0.5;
		int scale = 1 << this.scale;
		double pixelX = (blockXCenter - (double) this.centerX) / scale;
		double pixelZ = (blockZCenter - (double) this.centerZ) / scale;
		if(pixelX >= -63.0D && pixelZ >= -63.0D && pixelX <= 63.0D && pixelZ <= 63.0D){
			MapBannerMarker mapBannerMarker = MapBannerMarker.fromWorldBlock(world, pos);
			if(mapBannerMarker == null){
				return false;
			}

			if(this.banners.remove(mapBannerMarker.getKey(), mapBannerMarker)){
				this.removeIcon(mapBannerMarker.getKey());
				return true;
			}

			if(!this.isOverLimit(256)){
				this.banners.put(mapBannerMarker.getKey(), mapBannerMarker);
				this.addIcon(mapBannerMarker.getIconType(), world, mapBannerMarker.getKey(), blockXCenter, blockZCenter, 180.0D, mapBannerMarker.getName());
				return true;
			}
		}

		return false;
	}

	public boolean isOverLimit(int limit){
		return this.iconCount >= limit;
	}

	public static MapRegion fromNbt(NbtCompound nbt, Identifier dimension){
		int i = nbt.getInt("xCenter");
		int j = nbt.getInt("zCenter");
		byte b = (byte) MathHelper.clamp(nbt.getByte("scale"), 0, 4);
		MapRegion mapRegion = new MapRegion(b, i, j, dimension);
		byte[] bs = nbt.getByteArray("colors");
		if(bs.length == 16384){
			mapRegion.colors = bs;
		}

		NbtList nbtList = nbt.getList("banners", 10);

		for(int k = 0; k < nbtList.size(); ++k){
			MapBannerMarker mapBannerMarker = MapBannerMarker.fromNbt(nbtList.getCompound(k));
			mapRegion.banners.put(mapBannerMarker.getKey(), mapBannerMarker);
			mapRegion.addIcon(mapBannerMarker.getIconType(), null, mapBannerMarker.getKey(), mapBannerMarker.getPos().getX(), mapBannerMarker.getPos().getZ(), 180.0D, mapBannerMarker.getName());
		}
		return mapRegion;
	}

	public NbtCompound writeNbt(NbtCompound nbt){
		nbt.putInt("xCenter", this.centerX);
		nbt.putInt("zCenter", this.centerZ);
		nbt.putByte("scale", this.scale);
		nbt.putByteArray("colors", this.colors);
		NbtList nbtList = new NbtList();

		for(MapBannerMarker mapBannerMarker : this.banners.values()){
			nbtList.add(mapBannerMarker.getNbt());
		}

		nbt.put("banners", nbtList);
		return nbt;
	}

	public Iterable<MapIcon> getIcons( ){
		return icons.values();
	}

	public boolean isEmpty( ){
		byte sum = 0;
		for(var b : colors){
			sum |= b;
		}
		return sum == 0;
	}

	public Identifier getDimension( ){
		return dimension;
	}
}

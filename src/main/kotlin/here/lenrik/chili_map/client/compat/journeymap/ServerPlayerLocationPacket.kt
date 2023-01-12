package here.lenrik.chili_map.client.compat.journeymap

import here.lenrik.chili_map.ChiliMap
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import java.util.*

open class PlayerLocation(
	visible: Boolean, uniqueId: UUID,
	entityId: Int = 0,
	x: Double = .0,
	y: Double = .0,
	z: Double = .0,
	yaw: Double = .0,
	pitch: Double = .0,
) {
	var visible: Boolean
		private set
	var uniqueId: UUID
		private set
	var entityId: Int
		protected set
	var x: Double
		protected set
	var y: Double
		protected set
	var z: Double
		protected set
	var yaw: Double
		protected set
	var pitch: Double
		protected set

	init {
		this.visible = visible
		this.uniqueId = uniqueId
		this.entityId = entityId
		this.x = x
		this.y = y
		this.z = z
		this.yaw = yaw
		this.pitch = pitch
	}
}

class ServerPlayerLocationPacket(player: PlayerEntity, visible: Boolean) :
	PlayerLocation(visible, player.gameProfile.id) {
	init {
		if (visible) {
			entityId = player.id
			x = player.x
			y = player.y
			z = player.z
			yaw = player.yaw.toDouble()
			pitch = player.pitch.toDouble()
		}
	}

	companion object {
		val CHANNEL: Identifier = Identifier("journeymap", "player_loc")
		fun decode(buf: PacketByteBuf): PlayerLocation? {

			try {
				if (buf.readableBytes() > 1) {
					val uuid = buf.readUuid()
					return when (buf.readBoolean()) {
						true -> PlayerLocation(
							true, uuid, buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
							buf.readByte() * 360.0 / 256,
							buf.readByte() * 360.0 / 256,
						)

						else -> PlayerLocation(false, uuid)
					}
				}
			} catch (var3: Throwable) {
				ChiliMap.LOGGER.error("[toBytes]Failed to write message for player location request:$var3")
			}
			return null
		}
	}
}
// See LICENSE.SiFive for license details.

package freechips.rocketchip.subsystem

import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.prci.{ResetCrossingType, NoResetCrossing}
import freechips.rocketchip.tile._
import freechips.rocketchip.devices.debug.{HasPeripheryDebug}
import chisel3._

case class RocketCrossingParams(
  crossingType: ClockCrossingType = SynchronousCrossing(),
  master: TilePortParamsLike = TileMasterPortParams(),
  slave: TileSlavePortParams = TileSlavePortParams(),
  mmioBaseAddressPrefixWhere: TLBusWrapperLocation = CBUS,
  resetCrossingType: ResetCrossingType = NoResetCrossing(),
  forceSeparateClockReset: Boolean = false
) extends TileCrossingParamsLike

case class RocketTileAttachParams(
  tileParams: RocketTileParams,
  crossingParams: RocketCrossingParams
) extends CanAttachTile { type TileType = RocketTile }

trait HasRocketTiles extends HasTiles { this: BaseSubsystem =>
  val rocketTiles = tiles.collect { case r: RocketTile => r }

  def coreMonitorBundles = (rocketTiles map { t =>
    t.module.core.rocketImpl.coreMonitorBundle
  }).toList
  val dbg_portNexus = BundleBridgeNexusNode[freechips.rocketchip.tile.dbg_port]()
  val dbg_portNodes = rocketTiles.map(_.dbg_portNode)
  dbg_portNodes.foreach(dbg_portNexus := _)
}

class RocketSubsystem(implicit p: Parameters) extends BaseSubsystem with HasRocketTiles with HasPeripheryDebug {
  override lazy val module = new RocketSubsystemModuleImp(this)
}

class RocketSubsystemModuleImp[+L <: RocketSubsystem](_outer: L) extends BaseSubsystemModuleImp(_outer)
    with HasTilesModuleImp{
      val dbg_portIO = outer.dbg_portNexus.in.map(_._1)
      val dbg_port = IO(Output(UInt(8.W)))
      dbg_port := dbg_portIO(0).data
    }

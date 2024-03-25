
package rt

import spinal.core._
import spinal.lib.Counter
import spinal.lib.CounterFreeRun
import spinal.lib.GrayCounter

import math._


class PanoCoreWrapper extends Component {

    val io = new Bundle {
        val vo                  = out(VgaData())

        val led_green           = out(Bool)
    }


    val leds = new Area {
        val led_cntr = Reg(UInt(24 bits)) init(0)

        when(led_cntr === U(led_cntr.range -> true)){
            led_cntr := 0
        }
        .otherwise {
            led_cntr := led_cntr +1
        }

        io.led_green    := led_cntr.msb
    }

    val timings = VideoTimings()
    timings.h_active        := 640
    timings.h_fp            := 16
    timings.h_sync          := 96
    timings.h_bp            := 48
    timings.h_sync_positive := False
    timings.h_total_m1      := (timings.h_active + timings.h_fp + timings.h_sync + timings.h_bp -1).resize(timings.h_total_m1.getWidth)

    timings.v_active        := 480
    timings.v_fp            := 11
    timings.v_sync          := 2
    timings.v_bp            := 31
    timings.v_sync_positive := False
    timings.v_total_m1      := (timings.v_active + timings.v_fp + timings.v_sync + timings.v_bp -1).resize(timings.v_total_m1.getWidth)

    val vi_gen_pixel_out = PixelStream()

    val vi_gen = new VideoTimingGen()
    vi_gen.io.timings       <> timings
    vi_gen.io.pixel_out     <> vi_gen_pixel_out

    val width  = 640
    val height = 480

    val pix_x = Reg(SInt(12 bits))
    val pix_y = Reg(SInt(11 bits))

    when(vi_gen_pixel_out.vsync || (vi_gen_pixel_out.req && vi_gen_pixel_out.eof)){
        pix_x   := S(-width/2, 12 bits)
        pix_y   := S(height/2, 11 bits)
    }
    .elsewhen(vi_gen_pixel_out.req){
        when(vi_gen_pixel_out.eol){
            pix_x   := S(-width/2, 12 bits)
            pix_y   := pix_y - 1
        }
        .otherwise{
            pix_x   := pix_x + 1
        }
    }

    val u_pano_core = new PanoCore()
    u_pano_core.io.pix_x     <> pix_x
    u_pano_core.io.pix_y     <> pix_y
    u_pano_core.io.pixel_in  <> vi_gen_pixel_out

    val vo = new VideoOut()
    vo.io.timings := timings
    vo.io.pixel_in <> u_pano_core.io.pixel_out
    vo.io.vga_out <> io.vo
}



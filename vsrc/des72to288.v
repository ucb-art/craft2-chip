//==============================================================================
//  72 to 288 deserializer
//
//  Author      :  Jaeduk Han
//  Last update :  02/23/2017
//==============================================================================

//`default_nettype    none
`timescale          1ps/1ps

module des1to4(
    input  wire                 clk,
    input  wire                 in,
    input  wire     [ 1:0]      phi, //phase
    output reg      [ 3:0]      out
);
//------------------------------------------------------------------------------
// 1:4 deserializer module
//------------------------------------------------------------------------------
// preout[0] = in when phi==00
// preout[1] = in when phi==01
// preout[2] = in when phi==10
// preout[3] = in when phi==11
// out = preout when phi==00
reg                 [3:0]       preout;
wire                [3:0]       preout_next;
assign preout_next[0] = preout[1];
assign preout_next[1] = preout[2];
assign preout_next[2] = preout[3];
assign preout_next[3] = in;  

always @(posedge clk) begin
    preout <= preout_next;
    if (phi == 2'b00) begin
        out <= preout; //align output
    end
end
endmodule


module des9to36(
    input  wire                 clk,
    input  wire     [ 8:0]      in,
    input  wire     [ 1:0]      phi, //phase
    output wire     [ 8:0]      out0,
    output wire     [ 8:0]      out1,
    output wire     [ 8:0]      out2,
    output wire     [ 8:0]      out3
);
//------------------------------------------------------------------------------
// 9:36 deserializer module
//------------------------------------------------------------------------------
// dout0 = in when phi==00
// dout1 = in when phi==01
// dout2 = in when phi==10
// dout3 = in when phi==11

des1to4 des8(
    .clk                        (clk                                    ),
    .in                         (in[8]                                  ),
    .phi                        (phi                                    ),
    .out                        ({out3[8], out2[8], out1[8], out0[8]}   )
);
des1to4 des7(
    .clk                        (clk                                    ),
    .in                         (in[7]                                  ),
    .phi                        (phi                                    ),
    .out                        ({out3[7], out2[7], out1[7], out0[7]}   )
);
des1to4 des6(
    .clk                        (clk                                    ),
    .in                         (in[6]                                  ),
    .phi                        (phi                                    ),
    .out                        ({out3[6], out2[6], out1[6], out0[6]}   )
);
des1to4 des5(
    .clk                        (clk                                    ),
    .in                         (in[5]                                  ),
    .phi                        (phi                                    ),
    .out                        ({out3[5], out2[5], out1[5], out0[5]}   )
);
des1to4 des4(
    .clk                        (clk                                    ),
    .in                         (in[4]                                  ),
    .phi                        (phi                                    ),
    .out                        ({out3[4], out2[4], out1[4], out0[4]}   )
);
des1to4 des3(
    .clk                        (clk                                    ),
    .in                         (in[3]                                  ),
    .phi                        (phi                                    ),
    .out                        ({out3[3], out2[3], out1[3], out0[3]}   )
);
des1to4 des2(
    .clk                        (clk                                    ),
    .in                         (in[2]                                  ),
    .phi                        (phi                                    ),
    .out                        ({out3[2], out2[2], out1[2], out0[2]}   )
);
des1to4 des1(
    .clk                        (clk                                    ),
    .in                         (in[1]                                  ),
    .phi                        (phi                                    ),
    .out                        ({out3[1], out2[1], out1[1], out0[1]}   )
);
des1to4 des0(
    .clk                        (clk                                    ),
    .in                         (in[0]                                  ),
    .phi                        (phi                                    ),
    .out                        ({out3[0], out2[0], out1[0], out0[0]}   )
);
endmodule


module des72to288(
    input  wire                 clk,
    input  wire                 rst,
    input  wire     [ 1:0]      phi_init, //set to 2'b0 or connect it to external
    input  wire     [ 8:0]      in_0,
    input  wire     [ 8:0]      in_1,
    input  wire     [ 8:0]      in_2,
    input  wire     [ 8:0]      in_3,
    input  wire     [ 8:0]      in_4,
    input  wire     [ 8:0]      in_5,
    input  wire     [ 8:0]      in_6,
    input  wire     [ 8:0]      in_7,
    output reg                  clkout_data, //divided-by-4 clock, synchronized to out (for fifo in)
    output reg                  clkout_dsp,  //divided-by-4 clock for dsp (need some buffering in synthesis)
    output wire     [ 8:0]      out_0,
    output wire     [ 8:0]      out_1,
    output wire     [ 8:0]      out_2,
    output wire     [ 8:0]      out_3,
    output wire     [ 8:0]      out_4,
    output wire     [ 8:0]      out_5,
    output wire     [ 8:0]      out_6,
    output wire     [ 8:0]      out_7,
    output wire     [ 8:0]      out_8,
    output wire     [ 8:0]      out_9,
    output wire     [ 8:0]      out_10,
    output wire     [ 8:0]      out_11,
    output wire     [ 8:0]      out_12,
    output wire     [ 8:0]      out_13,
    output wire     [ 8:0]      out_14,
    output wire     [ 8:0]      out_15,
    output wire     [ 8:0]      out_16,
    output wire     [ 8:0]      out_17,
    output wire     [ 8:0]      out_18,
    output wire     [ 8:0]      out_19,
    output wire     [ 8:0]      out_20,
    output wire     [ 8:0]      out_21,
    output wire     [ 8:0]      out_22,
    output wire     [ 8:0]      out_23,
    output wire     [ 8:0]      out_24,
    output wire     [ 8:0]      out_25,
    output wire     [ 8:0]      out_26,
    output wire     [ 8:0]      out_27,
    output wire     [ 8:0]      out_28,
    output wire     [ 8:0]      out_29,
    output wire     [ 8:0]      out_30,
    output wire     [ 8:0]      out_31
);
//------------------------------------------------------------------------------
// 72:288 deserializer module
//------------------------------------------------------------------------------
// clkout_dsp : divided-by-4 clock
// out_0 = in_0 when phi==00
// out_1 = in_1 when phi==00
// out_2 = in_2 when phi==00
// out_3 = in_3 when phi==00
// out_4 = in_4 when phi==00
// out_5 = in_5 when phi==00
// out_6 = in_6 when phi==00
// out_7 = in_7 when phi==00
// out_8 = in_0 when phi==01
// out_9 = in_1 when phi==01
// out_10 = in_2 when phi==01
// out_11 = in_3 when phi==01
// out_12 = in_4 when phi==01
// out_13 = in_5 when phi==01
// out_14 = in_6 when phi==01
// out_15 = in_7 when phi==01
// out_16 = in_0 when phi==10
// out_17 = in_1 when phi==10
// out_18 = in_2 when phi==10
// out_19 = in_3 when phi==10
// out_20 = in_4 when phi==10
// out_21 = in_5 when phi==10
// out_22 = in_6 when phi==10
// out_23 = in_7 when phi==10
// out_24 = in_0 when phi==11
// out_25 = in_1 when phi==11
// out_26 = in_2 when phi==11
// out_27 = in_3 when phi==11
// out_28 = in_4 when phi==11
// out_29 = in_5 when phi==11
// out_30 = in_6 when phi==11
// out_31 = in_7 when phi==11

wire                [1:0]       phi_next;
// wire                            clkout_dsp_next;
reg                 [1:0]       phi;

assign phi_next = phi + 2'b01; //counter
// assign clkout_dsp_next = !phi[1]; //divide-by-4

always @(posedge clk, posedge rst) begin //asynchronous reset
    if (rst == 1'b1) begin
        phi <= phi_init;
        clkout_dsp <= !phi_init[1]; 
        clkout_data <= !phi_init[1]; 
    end else begin
        phi <= phi_next;
        clkout_data = (phi == 2'b10 || phi == 2'b11) ? 1 : 0;
        clkout_dsp =  (phi == 2'b00 || phi == 2'b01) ? 1 : 0;
        //clkout_dsp <= clkout_dsp_next;
        //clkout_data <= clkout_dsp_next;
    end
end
//des array
des9to36 des0(
    .clk                        (clk                                    ),
    .in                         (in_0                                   ),
    .phi                        (phi                                    ),
    .out0                       (out_0                                  ),
    .out1                       (out_8                                  ),
    .out2                       (out_16                                 ),
    .out3                       (out_24                                 )
);
des9to36 des1(
    .clk                        (clk                                    ),
    .in                         (in_1                                   ),
    .phi                        (phi                                    ),
    .out0                       (out_1                                  ),
    .out1                       (out_9                                  ),
    .out2                       (out_17                                 ),
    .out3                       (out_25                                 )
);
des9to36 des2(
    .clk                        (clk                                    ),
    .in                         (in_2                                   ),
    .phi                        (phi                                    ),
    .out0                       (out_2                                  ),
    .out1                       (out_10                                 ),
    .out2                       (out_18                                 ),
    .out3                       (out_26                                 )
);
des9to36 des3(
    .clk                        (clk                                    ),
    .in                         (in_3                                   ),
    .phi                        (phi                                    ),
    .out0                       (out_3                                  ),
    .out1                       (out_11                                 ),
    .out2                       (out_19                                 ),
    .out3                       (out_27                                 )
);
des9to36 des4(
    .clk                        (clk                                    ),
    .in                         (in_4                                   ),
    .phi                        (phi                                    ),
    .out0                       (out_4                                  ),
    .out1                       (out_12                                 ),
    .out2                       (out_20                                 ),
    .out3                       (out_28                                 )
);
des9to36 des5(
    .clk                        (clk                                    ),
    .in                         (in_5                                   ),
    .phi                        (phi                                    ),
    .out0                       (out_5                                  ),
    .out1                       (out_13                                 ),
    .out2                       (out_21                                 ),
    .out3                       (out_29                                 )
);
des9to36 des6(
    .clk                        (clk                                    ),
    .in                         (in_6                                   ),
    .phi                        (phi                                    ),
    .out0                       (out_6                                  ),
    .out1                       (out_14                                 ),
    .out2                       (out_22                                 ),
    .out3                       (out_30                                 )
);
des9to36 des7(
    .clk                        (clk                                    ),
    .in                         (in_7                                   ),
    .phi                        (phi                                    ),
    .out0                       (out_7                                  ),
    .out1                       (out_15                                 ),
    .out2                       (out_23                                 ),
    .out3                       (out_31                                 )
);
endmodule


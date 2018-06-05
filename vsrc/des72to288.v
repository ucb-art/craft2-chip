//==============================================================================
//  72 to 288 deserializer
//
//  Author      :  Jaeduk Han
//  Last update :  02/23/2017
//==============================================================================

//`default_nettype    none
`timescale          1ps/1ps


module des1to8(
    input  wire                 clk,
    input  wire                 in,
    input  wire     [ 2:0]      phi, //phase
    output reg      [ 7:0]      out
);
//------------------------------------------------------------------------------
// 1:8 deserializer module
//------------------------------------------------------------------------------
// preout[0] = in when phi==000
// preout[1] = in when phi==001
// preout[2] = in when phi==010
// preout[3] = in when phi==011
// preout[4] = in when phi==100
// preout[5] = in when phi==101
// preout[6] = in when phi==110
// preout[7] = in when phi==111
// out = preout when phi==00
reg                 [7:0]       preout;
wire                [7:0]       preout_next;
assign preout_next[0] = preout[1];
assign preout_next[1] = preout[2];
assign preout_next[2] = preout[3];
assign preout_next[3] = preout[4];
assign preout_next[4] = preout[5];
assign preout_next[5] = preout[6];
assign preout_next[6] = preout[7];
assign preout_next[7] = in;  

always @(posedge clk) begin
    preout <= preout_next;
    if (phi == 2'b00) begin
        out <= preout; //align output
    end
end
endmodule

module des9to72(
    input  wire                 clk,
    input  wire     [ 8:0]      in,
    input  wire     [ 2:0]      phi, //phase
    output wire     [ 8:0]      out0,
    output wire     [ 8:0]      out1,
    output wire     [ 8:0]      out2,
    output wire     [ 8:0]      out3,
    output wire     [ 8:0]      out4,
    output wire     [ 8:0]      out5,
    output wire     [ 8:0]      out6,
    output wire     [ 8:0]      out7
);
//------------------------------------------------------------------------------
// 9:72 deserializer module
//------------------------------------------------------------------------------
// dout0 = in when phi==000
// dout1 = in when phi==001
// dout2 = in when phi==010
// dout3 = in when phi==011
// dout0 = in when phi==100
// dout1 = in when phi==101
// dout2 = in when phi==110
// dout3 = in when phi==111

des1to8 des8(
    .clk                        (clk                                    ),
    .in                         (in[8]                                  ),
    .phi                        (phi                                    ),
    .out                        ({out7[8], out6[8], out5[8], out4[8], out3[8], out2[8], out1[8], out0[8]}   )
);
des1to8 des7(
    .clk                        (clk                                    ),
    .in                         (in[7]                                  ),
    .phi                        (phi                                    ),
    .out                        ({out7[7], out6[7], out5[7], out4[7], out3[7], out2[7], out1[7], out0[7]}   )
);
des1to8 des6(
    .clk                        (clk                                    ),
    .in                         (in[6]                                  ),
    .phi                        (phi                                    ),
    .out                        ({out7[6], out6[6], out5[6], out4[6], out3[6], out2[6], out1[6], out0[6]}   )
);
des1to8 des5(
    .clk                        (clk                                    ),
    .in                         (in[5]                                  ),
    .phi                        (phi                                    ),
    .out                        ({out7[5], out6[5], out5[5], out4[5], out3[5], out2[5], out1[5], out0[5]}   )
);
des1to8 des4(
    .clk                        (clk                                    ),
    .in                         (in[4]                                  ),
    .phi                        (phi                                    ),
    .out                        ({out7[4], out6[4], out5[4], out4[4], out3[4], out2[4], out1[4], out0[4]}   )
);
des1to8 des3(
    .clk                        (clk                                    ),
    .in                         (in[3]                                  ),
    .phi                        (phi                                    ),
    .out                        ({out7[3], out6[3], out5[3], out4[3], out3[3], out2[3], out1[3], out0[3]}   )
);
des1to8 des2(
    .clk                        (clk                                    ),
    .in                         (in[2]                                  ),
    .phi                        (phi                                    ),
    .out                        ({out7[2], out6[2], out5[2], out4[2], out3[2], out2[2], out1[2], out0[2]}   )
);
des1to8 des1(
    .clk                        (clk                                    ),
    .in                         (in[1]                                  ),
    .phi                        (phi                                    ),
    .out                        ({out7[1], out6[1], out5[1], out4[1], out3[1], out2[1], out1[1], out0[1]}   )
);
des1to8 des0(
    .clk                        (clk                                    ),
    .in                         (in[0]                                  ),
    .phi                        (phi                                    ),
    .out                        ({out7[0], out6[0], out5[0], out4[0], out3[0], out2[0], out1[0], out0[0]}   )
);
endmodule


module des72to576(
    input  wire                 clk,
    input  wire                 rst,
    input  wire     [ 2:0]      phi_init, //set to 2'b0 or connect it to external
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
    output wire     [ 8:0]      out_31,
    output wire     [ 8:0]      out_32,
    output wire     [ 8:0]      out_33,
    output wire     [ 8:0]      out_34,
    output wire     [ 8:0]      out_35,
    output wire     [ 8:0]      out_36,
    output wire     [ 8:0]      out_37,
    output wire     [ 8:0]      out_38,
    output wire     [ 8:0]      out_39,
    output wire     [ 8:0]      out_40,
    output wire     [ 8:0]      out_41,
    output wire     [ 8:0]      out_42,
    output wire     [ 8:0]      out_43,
    output wire     [ 8:0]      out_44,
    output wire     [ 8:0]      out_45,
    output wire     [ 8:0]      out_46,
    output wire     [ 8:0]      out_47,
    output wire     [ 8:0]      out_48,
    output wire     [ 8:0]      out_49,
    output wire     [ 8:0]      out_50,
    output wire     [ 8:0]      out_51,
    output wire     [ 8:0]      out_52,
    output wire     [ 8:0]      out_53,
    output wire     [ 8:0]      out_54,
    output wire     [ 8:0]      out_55,
    output wire     [ 8:0]      out_56,
    output wire     [ 8:0]      out_57,
    output wire     [ 8:0]      out_58,
    output wire     [ 8:0]      out_59,
    output wire     [ 8:0]      out_60,
    output wire     [ 8:0]      out_61,
    output wire     [ 8:0]      out_62,
    output wire     [ 8:0]      out_63
);

wire        [2:0]               phi_next;
reg         [2:0]               phi;

assign phi_next = phi + 3'b001; //counter

always @(posedge clk, posedge rst) begin //asynchronous reset
    if (rst == 1'b1) begin
        phi <= phi_init;
        clkout_dsp <= !phi_init[2]; 
        clkout_data <= !phi_init[2]; 
    end else begin
        phi <= phi_next;
        clkout_data <= (phi == 3'b100 || phi == 3'b101 || phi == 3'b110 || phi == 3'b111) ? 1 : 0;
        clkout_dsp <=  (phi == 3'b000 || phi == 3'b001 || phi == 3'b010 || phi == 3'b011) ? 1 : 0;
    end
end

des9to72 des0(
  .clk(clk),
  .in(in_0),
  .phi(phi),
  .out0(out_0),
  .out1(out_8),
  .out2(out_16),
  .out3(out_24),
  .out4(out_32),
  .out5(out_40),
  .out6(out_48),
  .out7(out_56)
);
des9to72 des1(
  .clk(clk),
  .in(in_1),
  .phi(phi),
  .out0(out_1),
  .out1(out_9),
  .out2(out_17),
  .out3(out_25),
  .out4(out_33),
  .out5(out_41),
  .out6(out_49),
  .out7(out_57)
);
des9to72 des2(
  .clk(clk),
  .in(in_2),
  .phi(phi),
  .out0(out_2),
  .out1(out_10),
  .out2(out_18),
  .out3(out_26),
  .out4(out_34),
  .out5(out_42),
  .out6(out_50),
  .out7(out_58)
);
des9to72 des3(
  .clk(clk),
  .in(in_3),
  .phi(phi),
  .out0(out_3),
  .out1(out_11),
  .out2(out_19),
  .out3(out_27),
  .out4(out_35),
  .out5(out_43),
  .out6(out_51),
  .out7(out_59)
);
des9to72 des4(
  .clk(clk),
  .in(in_4),
  .phi(phi),
  .out0(out_4),
  .out1(out_12),
  .out2(out_20),
  .out3(out_28),
  .out4(out_36),
  .out5(out_44),
  .out6(out_52),
  .out7(out_60)
);
des9to72 des5(
  .clk(clk),
  .in(in_5),
  .phi(phi),
  .out0(out_5),
  .out1(out_13),
  .out2(out_21),
  .out3(out_29),
  .out4(out_37),
  .out5(out_45),
  .out6(out_53),
  .out7(out_61)
);
des9to72 des6(
  .clk(clk),
  .in(in_6),
  .phi(phi),
  .out0(out_6),
  .out1(out_14),
  .out2(out_22),
  .out3(out_30),
  .out4(out_38),
  .out5(out_46),
  .out6(out_54),
  .out7(out_62)
);
des9to72 des7(
  .clk(clk),
  .in(in_7),
  .phi(phi),
  .out0(out_7),
  .out1(out_15),
  .out2(out_23),
  .out3(out_31),
  .out4(out_39),
  .out5(out_47),
  .out6(out_55),
  .out7(out_63)
);


endmodule

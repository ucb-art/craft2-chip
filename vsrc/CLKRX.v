
module CLK_RX_amp_buf (
  input VIP,
  input VIN,
  output VOBUF
); 

assign VOBUF = VIP & !VIN;

endmodule

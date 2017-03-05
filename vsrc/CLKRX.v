
module CLKRX (
  input VIP,
  input VIN,
  output VOBUF
); 

assign VOBUF = VIP & !VIN;

endmodule

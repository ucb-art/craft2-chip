module BBFFromInt(
    input  [63:0] in,
    output reg [63:0] out
);
  assign out = $realtobits($itor(in));
endmodule

// WARNING! May cause overflow!
module BBFToInt(
    input  [63:0] in,
    output reg [63:0] out
);
  assign out = $rtoi($bitstoreal(in));
endmodule

module BBFAdd(
    input  [63:0] in1,
    input  [63:0] in2,
    output reg [63:0] out
);
  assign out = $realtobits($bitstoreal(in1) + $bitstoreal(in2));
endmodule

module BBFSubtract(
    input  [63:0] in1,
    input  [63:0] in2,
    output [63:0] out
);
  assign out = $realtobits($bitstoreal(in1) - $bitstoreal(in2));
endmodule

module BBFMultiply(
    input  [63:0] in1,
    input  [63:0] in2,
    output [63:0] out
);
  assign out = $realtobits($bitstoreal(in1) * $bitstoreal(in2));
endmodule

module BBFDivide(
    input  [63:0] in1,
    input  [63:0] in2,
    output [63:0] out
);
  assign out = $realtobits($bitstoreal(in1) / $bitstoreal(in2));
endmodule

module BBFGreaterThan(
    input  [63:0] in1,
    input  [63:0] in2,
    output out
);
  assign out = $bitstoreal(in1) > $bitstoreal(in2);
endmodule

module BBFGreaterThanEquals(
    input  [63:0] in1,
    input  [63:0] in2,
    output out
);
  assign out = $bitstoreal(in1) >= $bitstoreal(in2);
endmodule

module BBFLessThan(
    input  [63:0] in1,
    input  [63:0] in2,
    output out
);
  assign out = $bitstoreal(in1) < $bitstoreal(in2);
endmodule

module BBFLessThanEquals(
    input  [63:0] in1,
    input  [63:0] in2,
    output out
);
  assign out = $bitstoreal(in1) <= $bitstoreal(in2);
endmodule

module BBFEquals(
    input  [63:0] in1,
    input  [63:0] in2,
    output out
);
  assign out = $bitstoreal(in1) == $bitstoreal(in2);
endmodule

module BBFNotEquals(
    input  [63:0] in1,
    input  [63:0] in2,
    output out
);
  assign out = $bitstoreal(in1) != $bitstoreal(in2);
endmodule

`timescale {{timeUnitPs}}ps / {{timePrecisionFs}}fs
`define CLK_PERIOD {{clkMul}}
`define HALF_CLK_PERIOD {{clkHalfMul}}
`define RESET_TIME {{resetTime}}
`define INIT_TIME  {{initTime}}

`define expect(nodeName, nodeVal, expVal, cycle, message) \
    if (nodeVal !== expVal) begin \
    $display("\t ASSERTION ON %s FAILED @ CYCLE = %d, 0x%h != EXPECTED 0x%h\n", nodeName,cycle,nodeVal,expVal); \
    $display(message); \
    $stop; \
end

module {{moduleName}}(
    // Inputs of DUT, outputs to this testbench
    {{#each inputs}}output {{signed}}[{{width}}-1:0] {{name}},
    {{/each}}
    // Outputs of DUT, inputs to this testbench
    {{#each outputs}}input {{signed}}[{{width}}-1:0] {{name}},
    {{/each}}
    output finish
);

    integer cycle = 0;
    reg clock     = 1;
    reg reset     = 1;
    
    {{#each inputs}}reg {{signed}}[{{width}}-1:0] reg_{{name}} = 0;
    {{/each}}

    {{#each inputs}}assign {{name}} = reg_{{name}};
    {{/each}}

    always #`HALF_CLK_PERIOD clock = ~clock;

    initial begin
        #`RESET_TIME
        forever #`CLK_PERIOD cycle = cycle + 1;
    end

    initial begin
        #`INIT_TIME reset = 0;
    {{#each statements}}
        {{#if step}}// step {{step_n}}
        #({{step_n}}*`CLK_PERIOD){{/if}}{{#if reset}}// reset {{reset_n}}
        reset = 1;
        #({{reset_n}}*`CLK_PERIOD)
        reset = 0;{{/if}}{{#if poke}}// poke {{poke_name}} {{poke_value}}
        reg_{{poke_name}} = {{poke_value}};{{/if}}{{#if expect}}// expect {{expect_name}} {{expect_value}} {{expect_message}}
        `expect("{{expect_name}}", {{expect_name}}, {{expect_value}}, cycle, "{{expect_message}}"){{/if}}{{/each}}

        // finish = 1;
        {{#if end_on_finish}}
        #`CLK_PERIOD $display("\t **Ran through all test vectors**"); $finish;
        {{/if}}
    end
endmodule


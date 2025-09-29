package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.api.Sinstruction;
import semulator.logic.execution.ExecutionContext;
import semulator.logic.execution.ExecutionContextImpl;
import semulator.logic.execution.ProgramExecutorImpl;
import semulator.logic.expansion.ExpansionIdAllocator;
import semulator.logic.functionInput.FunctionInput;
import semulator.logic.functions.Function;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.program.Sprogram;
import semulator.logic.variable.Variable;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;

public class QuoteInstruction extends AbstractInstruction implements FunctionInput {

    public QuoteInstruction(Variable variable,Function mainFunction) {
        super(InstructionData.QUOTE, variable);
        this.mainFunction = mainFunction;

    }

    public QuoteInstruction(Variable variable, Function mainFunction, Label label) {
        super(InstructionData.QUOTE, variable, label);
        this.mainFunction = mainFunction;
    }

    public QuoteInstruction(Variable variable, Function mainFunction, Sinstruction parentInstruction) {
        super(InstructionData.QUOTE, variable, parentInstruction);
        this.mainFunction = mainFunction;
    }

    private Function mainFunction;
    private List<FunctionInput> functionInputs =  new ArrayList<FunctionInput>();

    public void addFunctionInput(FunctionInput functionInput)
    {
        functionInputs.add(functionInput);
    }

    @Override
    public Label execute(ExecutionContext context) {

        List<Long> inputs = new ArrayList<>(functionInputs.size());
        for (FunctionInput in : functionInputs) {
            inputs.add(in.getValue(context)); // סדר נשמר
        }

        Long[] input = inputs.toArray(new Long[0]);
        long y = new ProgramExecutorImpl(mainFunction.getInstructionExecuteProgram()).run(input);

        context.updateVariable(getVariable(), y);
        return FixedLabel.EMPTY;
    }

    @Override
    public String toDisplayString() {
        String dst = getVariable().getRepresentation().toLowerCase();
        String fname = mainFunction.getUsherName();
        String args = functionInputs.stream()
                .map(FunctionInput::toDisplayString)
                .collect(joining(","));
        return String.format("%s <- %s(%s)", dst, fname, args);
    }

    @Override
    public void InitializeIProgramInstruction(ExpansionIdAllocator ex) {


    }

    @Override
    public Long getValue(ExecutionContext context) {
        List<Long> inputs = new ArrayList<>(functionInputs.size());
        for (FunctionInput in : functionInputs) {
            inputs.add(in.getValue(context));
        }

        Long[] input = inputs.toArray(new Long[0]);
        return new ProgramExecutorImpl(mainFunction.getInstructionExecuteProgram()).run(input);
    }

    @Override
    public Sprogram getInstructionProgram() {
        return mainFunction.getInstructionProgram();
    }

    @Override
    public int cycles() {
        return instructionData.cycles() + this.mainFunction.cycles();
    }

    @Override
    public int degree() {return instructionData.degree();}
}

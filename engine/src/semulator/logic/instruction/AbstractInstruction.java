package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.api.Sinstruction;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.program.Sprogram;
import semulator.logic.program.SprogramImpl;
import semulator.logic.variable.Variable;

public abstract class AbstractInstruction implements Sinstruction {

    private final InstructionData instructionData;
    private final Label label;
    private final Variable variable;
    private Sprogram expandProgram;
    private Sinstruction parentInstruction;

    public AbstractInstruction(InstructionData instructionData, Variable variable)
    {
     this(instructionData, variable, FixedLabel.EMPTY);
    }
    public AbstractInstruction(InstructionData instructionData, Variable variable, Label label)
    {
        this(instructionData, variable, label,null);
    }

    public AbstractInstruction(InstructionData instructionData, Variable variable, Label label,Sinstruction parentInstruction )
    {
        this.instructionData = instructionData;
        this.label = label;
        this.variable = variable;
        this.expandProgram = new SprogramImpl("expand");
        this.parentInstruction = parentInstruction;


    }

    @Override
    public String getName() {
        return instructionData.getName();
    }

    @Override
    public int cycles() {
        return instructionData.cycles();
    }

    @Override
    public Label getLabel() {
        return  label;
    }

    @Override
    public Variable getVariable() {
        return  variable;
    }

    @Override
    public String isBasic() {
        return instructionData.isBasic();
    }

    @Override
    public Sprogram getExpandProgram() {
        return expandProgram;
    }

    @Override
    public Sinstruction getparentInstruction(){ return parentInstruction;}


}

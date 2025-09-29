package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.api.Sinstruction;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.program.Sprogram;
import semulator.logic.program.SprogramImpl;
import semulator.logic.variable.Variable;

public abstract class AbstractInstruction implements Sinstruction {

    protected final InstructionData instructionData;
    private int instructionNumber;
    private final Label label;
    private final Variable variable;
    private Sinstruction parentInstruction;
    protected Sprogram instructionProgram;
    protected Label jumpTo;
    protected Variable secondaryVariable;
    protected long constValue;

    public AbstractInstruction(InstructionData instructionData, Variable variable)
    {
     this(instructionData, variable, FixedLabel.EMPTY);
    }
    public AbstractInstruction(InstructionData instructionData, Variable variable, Label label)
    {
        this(instructionData, variable,null, label);
    }
    public AbstractInstruction(InstructionData instructionData, Variable variable, Sinstruction parentInstruction)
    {
        this(instructionData, variable,parentInstruction, FixedLabel.EMPTY);
    }

    public AbstractInstruction(InstructionData instructionData, Variable variable, Sinstruction parentInstruction, Label label )
    {
        this.instructionData = instructionData;
        this.label = label;
        this.variable = variable;
        this.instructionProgram = new SprogramImpl("expand");
        this.parentInstruction = parentInstruction;
        this.jumpTo = null;
        this.secondaryVariable = null;
        this.constValue = 0L;

    }
    @Override
    public long getConstValue(){return constValue;}

    @Override
    public Label getJumpLabel()
    {
        return this.jumpTo;
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
    public int degree() {return instructionData.degree();}

    @Override
    public Label getLabel() {
        return  label;
    }

    @Override
    public Variable getVariable() {
        return  variable;
    }

    @Override
    public Variable getSecondaryVariable() {return secondaryVariable;}

    @Override
    public String isBasic() {
        return instructionData.isBasic();
    }

    @Override
    public Sprogram getInstructionProgram() {
        return instructionProgram;
    }

    @Override
    public Sinstruction getParentInstruction(){ return parentInstruction;}

    @Override
    public void setInstructionNumber(int instructionNumber) {
        this.instructionNumber = instructionNumber;
    }

    @Override
    public int getInstructionNumber() {
        return instructionNumber;
    }

}

package semulator.logic.program;

import semulator.logic.api.Sinstruction;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.label.LabelImp;
import semulator.logic.variable.VariableType;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class SprogramImpl implements Sprogram {

    private final String name;
    private final List<Sinstruction> instructions;

    public SprogramImpl(String name) {
        this.name = name;
        this.instructions = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addInstruction(Sinstruction instruction) {
        instructions.add(instruction);
    }

    @Override
    public List<Sinstruction> getInstructions() {
        return instructions;
    }

    @Override
    public int findInstructionIndexByLabel(Label label)
    {
        for (int i = 0; i < instructions.size(); i++)
        {
            Label currentLabel = instructions.get(i).getLabel();

            if (label instanceof FixedLabel && currentLabel instanceof FixedLabel)
            {
                if (label.equals(currentLabel))
                {
                    return i;
                }
            }
            else if (label instanceof LabelImp && currentLabel instanceof LabelImp)
            {
                if (label.getLabelRepresentation().equals(currentLabel.getLabelRepresentation()))
                {
                    return i;
                }
            }
            else
            {
                throw new NoSuchElementException("No such label found");
            }
        }
        return -1;
    }





    @Override
    public boolean validate() {
        ///  ??
        return false;
    }

    @Override
    public int calculateMaxDegree() {
        ///  ??
        return 0;
    }

    @Override
    public int calculateCycle() {
        ///  ??
        return 0;
    }

    @Override
    public StringBuilder getInputVariable() {
        StringBuilder inputVariable = new StringBuilder();
        for  (int i = 0; i < instructions.size(); i++)
        {
            if(instructions.get(i).getVariable() instanceof VariableType && instructions.get(i).getVariable().equals(VariableType.INPUT))
             inputVariable.append(instructions.get(i).getVariable().getRepresentation());
             inputVariable.append(" , ");
        }
        return inputVariable;
    }
}

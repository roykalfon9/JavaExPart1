package semulator.logic.variable;

import semulator.logic.execution.ExecutionContext;
import semulator.logic.execution.ExecutionContextImpl;
import semulator.logic.functionInput.FunctionInput;

import java.util.Map;
import java.util.Objects;

public class VariableImpl implements Variable, FunctionInput {

    private VariableType type;
    private final int number;

    public VariableImpl(VariableType type, int number) {
        this.type = type;
        this.number = number;
    }

    @Override
    public VariableType getType() {
        return type;
    }
    @Override
    public String getRepresentation() {
        return type.getVariableRepresentation(number);
    }
    @Override
    public int getNumber() { return number; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VariableImpl)) return false;
        VariableImpl that = (VariableImpl) o;
        return number == that.number && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, number);
    }

    @Override
    public String toString() {
        return getRepresentation();
    }

    @Override
    public Long getValue(ExecutionContext context) {
        return context.getVariablevalue(this);
    }

    @Override
    public String toDisplay() {
        return this.getRepresentation();
    }

    @Override
    public void addVar(Map<Variable, Long> programVariableState) {
        programVariableState.put(this,0L);
    }
}

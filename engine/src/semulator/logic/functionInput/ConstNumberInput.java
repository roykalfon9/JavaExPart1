package semulator.logic.functionInput;

import semulator.logic.execution.ExecutionContext;
import semulator.logic.execution.ExecutionContextImpl;
import semulator.logic.variable.Variable;

import java.util.Map;

public class ConstNumberInput implements FunctionInput {

    Long value;

    public ConstNumberInput(Long value){
        this.value = value;
    }

    @Override
    public Long getValue(ExecutionContext context) {
        return value;
    }

    @Override
    public String toDisplay() {
        return Long.toString(value);
    }

    @Override
    public void addVar(Map<Variable, Long> programVariableState) {
        return;
    }
}

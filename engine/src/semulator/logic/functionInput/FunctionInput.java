package semulator.logic.functionInput;

import semulator.logic.execution.ExecutionContext;
import semulator.logic.variable.Variable;

import java.util.Map;

public interface FunctionInput {
    Long getValue(ExecutionContext context);

    String toDisplay();

    void addVar(Map<Variable, Long> programVariableState);

}

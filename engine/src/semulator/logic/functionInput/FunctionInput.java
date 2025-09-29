package semulator.logic.functionInput;

import semulator.logic.execution.ExecutionContext;

public interface FunctionInput {
    Long getValue(ExecutionContext context);

    String toDisplayString();
}

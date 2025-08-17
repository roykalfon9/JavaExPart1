package semulator.logic.api;

import semulator.logic.execution.ExecutionContext;
import semulator.logic.label.Label;
import semulator.logic.variable.Variable;

public interface SInstruction {

String getName();
Label execute(ExecutionContext context);
int cycles();
Label getLabel();
Variable getVariable();
}

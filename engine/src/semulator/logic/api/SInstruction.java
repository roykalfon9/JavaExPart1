package semulator.logic.api;

import semulator.logic.label.Label;
import semulator.logic.variable.Variable;

public interface SInstruction {

String getName();
void execute();
int cycles();
Label getLabel();
Variable getVariable();
}

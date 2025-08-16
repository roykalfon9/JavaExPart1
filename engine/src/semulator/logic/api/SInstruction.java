package semulator.logic.api;

import semulator.logic.label.Label;

public interface SInstruction {

String getName();
void execute();
int cycles();
Label getLabel();
}

package semulator.logic.api;

import semulator.logic.execution.ExecutionContext;
import semulator.logic.label.Label;
import semulator.logic.program.Sprogram;
import semulator.logic.variable.Variable;

public interface Sinstruction {

String getName();
Label execute(ExecutionContext context);
int cycles();
Label getLabel();
Variable getVariable();
String isBasic();
Sprogram getInstructionProgram();
Sinstruction getParentInstruction();
void setInstructionNumber(int instructionNumber);
int getInstructionNumber();
String toDisplayString();
Label getJumpLabel();
}

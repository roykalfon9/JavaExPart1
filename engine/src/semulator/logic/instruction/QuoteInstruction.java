package semulator.logic.instruction;

import semulator.logic.api.InstructionData;
import semulator.logic.api.Sinstruction;
import semulator.logic.execution.ExecutionContext;
import semulator.logic.execution.ProgramExecutorImpl;
import semulator.logic.expansion.ExpansionIdAllocator;
import semulator.logic.functionInput.ConstNumberInput;
import semulator.logic.functionInput.FunctionInput;
import semulator.logic.functions.Function;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.label.LabelImp;
import semulator.logic.program.Sprogram;
import semulator.logic.program.SprogramImpl;
import semulator.logic.variable.Variable;
import semulator.logic.variable.VariableImpl;
import semulator.logic.variable.VariableType;
import semulator.logic.xml.schema.SFunction;

import java.util.*;

import static java.util.stream.Collectors.joining;

public class QuoteInstruction extends AbstractInstruction implements FunctionInput {

    private final Function mainFunction;
    private final List<FunctionInput> functionInputs = new ArrayList<>();

    public QuoteInstruction(Variable variable, Function mainFunction) {
        super(InstructionData.QUOTE, variable);
        this.mainFunction = mainFunction;
    }

    public QuoteInstruction(Variable variable, Function mainFunction, Label label) {
        super(InstructionData.QUOTE, variable, label);
        this.mainFunction = mainFunction;
    }

    public QuoteInstruction(Variable variable, Function mainFunction, Sinstruction parentInstruction) {
        super(InstructionData.QUOTE, variable, parentInstruction);
        this.mainFunction = mainFunction;
    }

    public QuoteInstruction(Variable variable, Function mainFunction, Sinstruction parentInstruction, Label label) {
        super(InstructionData.QUOTE, variable, parentInstruction, label);
        this.mainFunction = mainFunction;
    }

    public void addFunctionInput(FunctionInput in) {
        functionInputs.add(in);
    }

    public Function getMainFunction() {
        return mainFunction;
    }

    public List<FunctionInput> getFunctionInputs() {
        return Collections.unmodifiableList(functionInputs);
    }

    // -------- run-time --------
    @Override
    public Label execute(ExecutionContext context) {
        List<Long> inputs = new ArrayList<>(functionInputs.size());
        for (FunctionInput in : functionInputs) inputs.add(in.getValue(context));
        long y = new ProgramExecutorImpl(mainFunction.getInstructionExecuteProgram())
                .run(inputs.toArray(new Long[0]));
        context.updateVariable(getVariable(), y);
        return FixedLabel.EMPTY;
    }

    // -------- display --------
    @Override
    public String toDisplayString() {
        String lbl = getLabel().getLabelRepresentation();
        String dst = getVariable().getRepresentation().toLowerCase(Locale.ROOT);
        return String.format("[%-5s]  %s <- %s", lbl, dst, toDisplay());
    }

    @Override
    public String toDisplay() {
        String fname = mainFunction.getUsherName();
        if (functionInputs.isEmpty()) return String.format("(%s)", fname);
        String args = functionInputs.stream().map(FunctionInput::toDisplay).collect(joining(", "));
        return String.format("(%s , %s)", fname, args);
    }

    @Override
    public void InitializeIProgramInstruction(ExpansionIdAllocator ex) {
        this.instructionProgram = new SprogramImpl("expand-quote:" + mainFunction.getName());

        Label outerLbl = this.getLabel();
        if (outerLbl != FixedLabel.EMPTY) {
            Variable anchor = new VariableImpl(VariableType.WORK, ex.getWorkVariableNumber());
            this.instructionProgram.addInstruction(new NeutralInstruction(anchor, this, outerLbl));
        }

        Label retLbl = new LabelImp(ex.getLabelNumber());

        List<Variable> wArgs = new ArrayList<>(functionInputs.size());
        for (int i = 0; i < functionInputs.size(); i++) {
            wArgs.add(new VariableImpl(VariableType.WORK, ex.getWorkVariableNumber()));
        }
        Variable wRet = new VariableImpl(VariableType.WORK, ex.getWorkVariableNumber());

        for (int i = 0; i < functionInputs.size(); i++) {
            FunctionInput in = functionInputs.get(i);
            Variable wi = wArgs.get(i);
            Sinstruction preload;

            if (in instanceof QuoteInstruction qi) {
                // ציטוט מקונן — בלי לייבל חיצוני
                QuoteInstruction nested = new QuoteInstruction(wi, qi.getMainFunction(), this);
                for (FunctionInput fi : qi.getFunctionInputs()) nested.addFunctionInput(fi);
                preload = nested;

            } else if (in instanceof Variable v) {
                preload = new AssigmentInstruction(wi, v, this);

            } else if (in instanceof ConstNumberInput cni) {
                long val = cni.getValue(null);
                preload = new ConstantAssignmentInstruction(wi, val, this);

            } else {
                String s = (in.toDisplay() == null ? "" : in.toDisplay()).trim().toUpperCase(Locale.ROOT);
                Sinstruction made = null;

                if ("Y".equals(s)) {
                    Variable parsed = new VariableImpl(VariableType.RESULT, 1);
                    made = new AssigmentInstruction(wi, parsed, this);

                } else if (!s.isEmpty()) {
                    char c = s.charAt(0);
                    String digits = (s.length() > 1) ? s.substring(1) : "";
                    boolean onlyDigits = !digits.isEmpty() && digits.chars().allMatch(Character::isDigit);

                    if ((c == 'X' || c == 'Y' || c == 'W' || c == 'Z') && onlyDigits) {
                        int num = Integer.parseInt(digits);
                        VariableType t = (c == 'X') ? VariableType.INPUT
                                : (c == 'Y') ? VariableType.RESULT
                                : VariableType.WORK;
                        Variable parsed = new VariableImpl(t, num);
                        made = new AssigmentInstruction(wi, parsed, this);

                    } else if (s.chars().allMatch(Character::isDigit)) {
                        long val = Long.parseLong(s);
                        made = new ConstantAssignmentInstruction(wi, val, this);
                    }
                }

                if (made == null) {
                    throw new IllegalStateException(
                            "Unsupported FunctionInput in QUOTE prolog: "
                                    + in.getClass().getName() + " ('" + s + "')");
                }
                preload = made;
            }

            this.instructionProgram.addInstruction(preload);
        }

        Map<Integer, Variable> xMap = new HashMap<>();   // Xn -> wArgs[n-1]
        for (int i = 0; i < wArgs.size(); i++) xMap.put(i + 1, wArgs.get(i));
        Map<Integer, Variable> wMap = new HashMap<>();   // Wk פנימי -> WORK חדש
        Map<Integer, Label>    lMap = new HashMap<>();   // L# פנימי -> L# חדש

        for (Sinstruction src : mainFunction.getInstructionExecuteProgram().getInstructions()) {
            Sinstruction cloned = cloneWithRemap(src, xMap, wRet, wMap, lMap, ex, retLbl);
            this.instructionProgram.addInstruction(cloned);
        }

        this.instructionProgram.addInstruction(new NeutralInstruction(wRet, this, retLbl));

        this.instructionProgram.addInstruction(new AssigmentInstruction(this.getVariable(), wRet, this));
    }


    private Variable remapVar(Variable v,
                              Map<Integer, Variable> xMap,
                              Variable wRet,
                              Map<Integer, Variable> wMap,
                              ExpansionIdAllocator ex) {
        if (v == null) return null;
        switch (v.getType()) {
            case INPUT: {
                Variable repl = xMap.get(v.getNumber());
                if (repl == null) repl = new VariableImpl(VariableType.WORK, ex.getWorkVariableNumber());
                return repl;
            }
            case RESULT:
                return wRet;
            case WORK:
                return wMap.computeIfAbsent(
                        v.getNumber(),
                        k -> new VariableImpl(VariableType.WORK, ex.getWorkVariableNumber())
                );
            default:
                return v;
        }
    }

    private Label remapJumpOrLabel(Label l, Map<Integer, Label> lMap,
                                   ExpansionIdAllocator ex, Label retLbl) {
        if (l == null) return null;
        if (l == FixedLabel.EMPTY) return FixedLabel.EMPTY;
        if (l == FixedLabel.EXIT)  return retLbl; // map EXIT->retLbl
        return lMap.computeIfAbsent(l.getNumber(), k -> new LabelImp(ex.getLabelNumber()));
    }

    private Sinstruction cloneWithRemap(Sinstruction ins,
                                        Map<Integer, Variable> xMap,
                                        Variable wRet,
                                        Map<Integer, Variable> wMap,
                                        Map<Integer, Label> lMap,
                                        ExpansionIdAllocator ex,
                                        Label retLbl) {
        Variable v1 = remapVar(ins.getVariable(),         xMap, wRet, wMap, ex);
        Variable v2 = remapVar(ins.getSecondaryVariable(),xMap, wRet, wMap, ex);
        Label    L  = remapJumpOrLabel(ins.getLabel(),     lMap, ex, retLbl);
        Label    J  = remapJumpOrLabel(ins.getJumpLabel(), lMap, ex, retLbl);

        switch (ins.getName()) {
            case "INCREASE":            return (L == FixedLabel.EMPTY) ? new IncreaseInstruction(v1, this)              : new IncreaseInstruction(v1, this, L);
            case "DECREASE":            return (L == FixedLabel.EMPTY) ? new DecreaseInstruction(v1, this)              : new DecreaseInstruction(v1, this, L);
            case "ZERO_VARIABLE":       return (L == FixedLabel.EMPTY) ? new ZeroVariableInstruction(v1, this)          : new ZeroVariableInstruction(v1, this, L);
            case "NEUTRAL":             return (L == FixedLabel.EMPTY) ? new NeutralInstruction(v1, this)               : new NeutralInstruction(v1, this, L);
            case "ASSIGNMENT":          return (L == FixedLabel.EMPTY) ? new AssigmentInstruction(v1, v2, this)         : new AssigmentInstruction(v1, v2, this, L);
            case "CONSTANT_ASSIGNMENT": return (L == FixedLabel.EMPTY) ? new ConstantAssignmentInstruction(v1, ins.getConstValue(), this)
                    : new ConstantAssignmentInstruction(v1, ins.getConstValue(), this, L);
            case "GOTO_LABEL":          return (L == FixedLabel.EMPTY) ? new GoToLabelInstruction(v1, J, this)          : new GoToLabelInstruction(v1, J, this, L);
            case "JUMP_ZERO":           return (L == FixedLabel.EMPTY) ? new JumpZeroInstruction(v1, J, this)           : new JumpZeroInstruction(v1, J, this, L);
            case "JUMP_NOT_ZERO":       return (L == FixedLabel.EMPTY) ? new JumpNotZeroInstruction(v1, J, this)        : new JumpNotZeroInstruction(v1, J, this, L);
            case "JUMP_EQUAL_CONSTANT": return (L == FixedLabel.EMPTY) ? new JumpEqualConstantInstruction(v1, J, ins.getConstValue(), this)
                    : new JumpEqualConstantInstruction(v1, J, ins.getConstValue(), this, L);
            case "JUMP_EQUAL_VARIABLE": return (L == FixedLabel.EMPTY) ? new JumpEqualVariableInstruction(v1, J, v2, this)
                    : new JumpEqualVariableInstruction(v1, J, v2, this, L);

            case "QUOTE": {
                if (!(ins instanceof QuoteInstruction qsrc))
                    throw new UnsupportedOperationException("QUOTE instance expected");
                QuoteInstruction q = (L == FixedLabel.EMPTY)
                        ? new QuoteInstruction(v1, qsrc.getMainFunction(), this)
                        : new QuoteInstruction(v1, qsrc.getMainFunction(), this, L);
                for (FunctionInput fi : qsrc.getFunctionInputs()) {
                    q.addFunctionInput(remapInput(fi, xMap, wRet, wMap, lMap, ex));
                }
                return q;
            }

            default:
                throw new UnsupportedOperationException("Unsupported instruction: " + ins.getName());
        }
    }

    private FunctionInput remapInput(FunctionInput fi,
                                     Map<Integer, Variable> xMap,
                                     Variable wRet,
                                     Map<Integer, Variable> wMap,
                                     Map<Integer, Label> lMap,
                                     ExpansionIdAllocator ex) {
        if (fi instanceof Variable v) {
            Variable rv = remapVar(v, xMap, wRet, wMap, ex);
            return new FunctionInput() {
                @Override public Long getValue(ExecutionContext ctx) { return ctx.getVariablevalue(rv); }
                @Override public String toDisplay() { return rv.getRepresentation().toLowerCase(Locale.ROOT); }
                @Override public void addVar(Map<Variable, Long> m) { m.putIfAbsent(rv, 0L); }
            };
        }
        if (fi instanceof QuoteInstruction qi) {
            QuoteInstruction nested = new QuoteInstruction(
                    new VariableImpl(VariableType.WORK, ex.getWorkVariableNumber()),
                    qi.getMainFunction(),
                    this
            );
            for (FunctionInput inner : qi.getFunctionInputs()) {
                nested.addFunctionInput(remapInput(inner, xMap, wRet, wMap, lMap, ex));
            }
            return nested;
        }
        return fi;
    }



    @Override
    public Long getValue(ExecutionContext context) {
        List<Long> inputs = new ArrayList<>(functionInputs.size());
        for (FunctionInput in : functionInputs) inputs.add(in.getValue(context));
        return new ProgramExecutorImpl(mainFunction.getInstructionExecuteProgram())
                .run(inputs.toArray(new Long[0]));
    }

    @Override
    public void addVar(Map<Variable, Long> programVariableState) {
        for (FunctionInput in : functionInputs) in.addVar(programVariableState);
    }

    // -------- misc --------
    public int getMaxVariableNumber() {
        int max = 0;
        for (FunctionInput in : functionInputs) {
            if (in instanceof QuoteInstruction qi) {
                max = Math.max(max, qi.getMaxVariableNumber());
            } else if (in instanceof Variable l && l.getType() == VariableType.WORK) {
                max = Math.max(max, l.getNumber());
            }
        }
        return max;
    }

    @Override
    public Sprogram getInstructionProgram() {
        return this.instructionProgram;
    }

    @Override
    public int cycles() {
        return instructionData.cycles() + this.mainFunction.cycles();
    }

    @Override
    public int degree() {
        return 1 + computeProgramMaxDegree(mainFunction.getInstructionExecuteProgram());
    }

    private static int computeProgramMaxDegree(Sprogram prog) {
        int max = 0;
        for (Sinstruction ins : prog.getInstructions()) {
            int d = ins.degree();
            if (d > max) max = d;
        }
        return max + 1;
    }

    public Sprogram getMainFunctionProgram() {
        return mainFunction.getInstructionExecuteProgram();
    }
}

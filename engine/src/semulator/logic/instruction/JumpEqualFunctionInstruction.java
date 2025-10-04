package semulator.logic.instruction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import semulator.logic.api.InstructionData;
import semulator.logic.api.Sinstruction;
import semulator.logic.execution.ExecutionContext;
import semulator.logic.execution.ProgramExecutorImpl;
import semulator.logic.expansion.ExpansionIdAllocator;
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

import static java.util.stream.Collectors.joining;

public class JumpEqualFunctionInstruction extends AbstractInstruction {

    private final Function mainFunction;
    private final List<FunctionInput> functionInputs = new ArrayList<>();

    public JumpEqualFunctionInstruction(Variable variable, Label jumpTo, Function mainFunction) {
        super(InstructionData.JUMP_EQUAL_FUNCTION, variable);
        this.jumpTo = jumpTo;
        this.mainFunction = mainFunction;
    }

    public JumpEqualFunctionInstruction(Variable variable, Label jumpTo,  Function mainFunction, Label label) {
        super(InstructionData.JUMP_EQUAL_FUNCTION, variable, label);
        this.jumpTo = jumpTo;
        this.mainFunction = mainFunction;
    }

    public JumpEqualFunctionInstruction(Variable variable, Label jumpTo,  Function mainFunction, Sinstruction parentInstruction) {
        super(InstructionData.JUMP_EQUAL_FUNCTION, variable, parentInstruction);
        this.jumpTo = jumpTo;
        this.mainFunction = mainFunction;
    }

    public JumpEqualFunctionInstruction(Variable variable, Label jumpTo,  Function mainFunction, Sinstruction parentInstruction, Label label) {
        super(InstructionData.JUMP_EQUAL_FUNCTION, variable, parentInstruction, label);
        this.jumpTo = jumpTo;
        this.mainFunction = mainFunction;
    }

    // API לקלטים
    public void addFunctionInput(FunctionInput in) {
        functionInputs.add(in);
    }
    public List<FunctionInput> getFunctionInputs() {
        return functionInputs;
    }

    @Override
    public Label execute(ExecutionContext context) {
        if (mainFunction.getInstructionExecuteProgram() == null) {
            throw new IllegalStateException("Function has no execute program.");
        }
        List<Long> inputs = new ArrayList<>(functionInputs.size());

        for (FunctionInput in : functionInputs) {
            inputs.add(in.getValue(context));
        }
        long y = new ProgramExecutorImpl(mainFunction.getInstructionExecuteProgram())
                .run(inputs.toArray(new Long[0]));

        Long Var = context.getVariablevalue(this.getVariable());

        if (y == Var)
            return jumpTo;
        else
            return FixedLabel.EMPTY;
    }

    @Override
    public String toDisplayString() {
        String lbl = getLabel().getLabelRepresentation();
        String dst = getVariable().getRepresentation().toLowerCase();
        String jmp = jumpTo.getLabelRepresentation();

        return String.format("[%-5s] IF %s = %s GOTO %s",
                lbl,
                dst,
                toDisplay(),
                jmp);
    }

    @Override
    public void InitializeIProgramInstruction(ExpansionIdAllocator ex) {

        this.instructionProgram = new SprogramImpl("expand-jeqf:" + mainFunction.getName());

        Label firstLbl = (this.getLabel() != FixedLabel.EMPTY)
                ? new LabelImp(ex.getLabelNumber())
                : FixedLabel.EMPTY;

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
                QuoteInstruction nested = (firstLbl == FixedLabel.EMPTY)
                        ? new QuoteInstruction(wi, qi.getMainFunction(), this)
                        : new QuoteInstruction(wi, qi.getMainFunction(), this, firstLbl);
                for (FunctionInput fi : qi.getFunctionInputs()) nested.addFunctionInput(fi);
                preload = nested;
                firstLbl = FixedLabel.EMPTY;

            } else if (in instanceof Variable v) {
                preload = (firstLbl == FixedLabel.EMPTY)
                        ? new AssigmentInstruction(wi, v, this)
                        : new AssigmentInstruction(wi, v, this, firstLbl);
                firstLbl = FixedLabel.EMPTY;

            } else if (in instanceof semulator.logic.functionInput.ConstNumberInput cni) {
                long val = cni.getValue(null);
                preload = (firstLbl == FixedLabel.EMPTY)
                        ? new ConstantAssignmentInstruction(wi, val, this)
                        : new ConstantAssignmentInstruction(wi, val, this, firstLbl);
                firstLbl = FixedLabel.EMPTY;

            } else {
                throw new IllegalStateException("Unsupported FunctionInput in JEQF prolog: " + in.getClass().getName());
            }

            this.instructionProgram.addInstruction(preload);
        }

        Label Lret = new LabelImp(ex.getLabelNumber());

        Map<Integer, Variable> xMap = new HashMap<>(); // Xn -> wArgs[n-1]
        for (int i = 0; i < wArgs.size(); i++) xMap.put(i + 1, wArgs.get(i));
        Map<Integer, Variable> wMap = new HashMap<>(); // WORK פנימיים -> WORK חדשים
        Map<Integer, Label>    lMap = new HashMap<>(); // L# פנימיים -> L# חדשים

        for (Sinstruction src : mainFunction.getInstructionExecuteProgram().getInstructions()) {
            Sinstruction cloned = cloneWithRemap(src, xMap, wRet, wMap, lMap, Lret, ex);
            this.instructionProgram.addInstruction(cloned);
        }

        this.instructionProgram.addInstruction(new NeutralInstruction(wRet, this, Lret));

        this.instructionProgram.addInstruction(
                new JumpEqualVariableInstruction(
                        this.getVariable(),
                        jumpTo,
                        wRet,
                        this
                )
        );
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
                if (repl == null) {
                    repl = new VariableImpl(VariableType.WORK, ex.getWorkVariableNumber());
                }
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

    private Label remapLabel(Label l, Map<Integer, Label> lMap, Label Lret, ExpansionIdAllocator ex) {
        if (l == null) return null;
        if (l == FixedLabel.EMPTY) return FixedLabel.EMPTY;
        if (l == FixedLabel.EXIT)  return Lret; // כל EXIT פנימי יהפוך לעוגן המקומי
        return lMap.computeIfAbsent(l.getNumber(), k -> new LabelImp(ex.getLabelNumber()));
    }

    private Sinstruction cloneWithRemap(Sinstruction ins,
                                        Map<Integer, Variable> xMap,
                                        Variable wRet,
                                        Map<Integer, Variable> wMap,
                                        Map<Integer, Label> lMap,
                                        Label Lret,
                                        ExpansionIdAllocator ex) {
        Variable v1 = remapVar(ins.getVariable(),         xMap, wRet, wMap, ex);
        Variable v2 = remapVar(ins.getSecondaryVariable(),xMap, wRet, wMap, ex);
        Label    L  = remapLabel(ins.getLabel(),          lMap, Lret, ex);
        Label    J  = remapLabel(ins.getJumpLabel(),      lMap, Lret, ex);

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
                    q.addFunctionInput(remapInput(fi, xMap, wRet, wMap, lMap, Lret, ex));
                }
                return q;
            }

            case "JUMP_EQUAL_FUNCTION": {
                if (!(ins instanceof JumpEqualFunctionInstruction jsrc))
                    throw new UnsupportedOperationException("JEQF instance expected");

                JumpEqualFunctionInstruction j = (L == FixedLabel.EMPTY)
                        ? new JumpEqualFunctionInstruction(v1, J, jsrc.mainFunction, this)
                        : new JumpEqualFunctionInstruction(v1, J, jsrc.mainFunction, this, L);

                for (FunctionInput fi : jsrc.getFunctionInputs()) {
                    j.addFunctionInput(remapInput(fi, xMap, wRet, wMap, lMap, Lret, ex));
                }
                return j;
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
                                     Label Lret,
                                     ExpansionIdAllocator ex) {
        if (fi instanceof Variable v) {
            Variable rv = remapVar(v, xMap, wRet, wMap, ex);
            return new FunctionInput() {
                @Override public Long getValue(ExecutionContext ctx) { return ctx.getVariablevalue(rv); }
                @Override public String toDisplay() { return rv.getRepresentation().toLowerCase(); }
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
                nested.addFunctionInput(remapInput(inner, xMap, wRet, wMap, lMap, Lret, ex));
            }
            return nested;
        }

        return fi;
    }

    public String toDisplay() {
        String fname = mainFunction.getUsherName();
        if (functionInputs.isEmpty()) return String.format("(%s)", fname);
        String args = functionInputs.stream().map(FunctionInput::toDisplay).collect(joining(", "));
        return String.format("(%s , %s)", fname, args);
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

    public Function getMainFunction() {
        return mainFunction;
    }


}

package semulator.logic.label;

public class LabelImp implements Label {

    private final String label;

    public LabelImp(int number) {
        label = "L" + number;
    }

    public String getLabelRepresentation() {
        return label;
    }
}

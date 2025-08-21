package semulator.logic.label;

public class LabelImp implements Label {

    private final String label;

    public LabelImp(int number) {
        label = "L" + number;
    }

    public String getLabelRepresentation() {
        return label;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LabelImp)) return false;
        LabelImp other = (LabelImp) obj;
        return this.label.equals(other.label);
    }

    @Override
    public int hashCode() {
        return label.hashCode();
    }

    @Override
    public String toString() {
        return label;
    }

}

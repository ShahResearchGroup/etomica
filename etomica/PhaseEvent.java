package etomica;

/**
 * Event that conveys some happening with respect to a phase or the things it contains.
 *
 * @see PhaseListener
 * @see DisplayPhaseListener
 */
public class PhaseEvent extends SimulationEvent {
    
    protected Phase phase;
    protected Atom atom;
    protected Space.Vector point;
    protected Type type;
    
    public PhaseEvent(Object source) {
        this(source, null);
    }
    public PhaseEvent(Object source, Type t) {
        super(source);
        type = t;
    }
    
    public void setType(Type t) {type = t;}
    public Type type() {return type;}
    
    public final PhaseEvent setPhase(Phase p) {phase = p; return this;}
    public final Phase phase() {return phase;}
    
    public final PhaseEvent setPoint(Space.Vector p) {point = p; return this;}
    public Space.Vector point() {return point;}
    
    public final PhaseEvent setAtom(Atom a) {atom = a; return this;}
    public Atom atom() {return atom;}
    
    public static class Type extends Constants.TypedConstant {
        private Type(String label) {super(label);}
        public static final Type[] CHOICES = new Type[] {
            new Type("Point selected"),
            new Type("Atom added"),
            new Type("Atom removed"),
            new Type("Atom selected"),
            new Type("Atom released")};
        public final Constants.TypedConstant[] choices() {return CHOICES;}
    }
    public static final Type POINT_SELECTED = Type.CHOICES[0];
    public static final Type ATOM_ADDED = Type.CHOICES[1];
    public static final Type ATOM_REMOVED = Type.CHOICES[2];
    public static final Type ATOM_SELECTED = Type.CHOICES[3];
    public static final Type ATOM_RELEASED = Type.CHOICES[4];
}//end of PhaseEvent
    
package etomica;

/**
 * Builds an atom group that comprises a set of differently formed atoms or atomgroups.
 * Each child atom is constructed by a different atom factory, which are set as an
 * array of atomfactories given in the constructor.
 *
 * @author David Kofke
 */
public class AtomFactoryHetero extends AtomFactory {
    
    private AtomFactory[] childFactory;
    private final AtomType.Group groupType = new AtomType.Group(this);
    
    /**
     * @param factory array of atom factories, each of which makes a different child.
     */
    public AtomFactoryHetero(Simulation sim, AtomFactory[] factory) {
        this(sim, factory, new ConfigurationLinear(sim));
    }
    /**
     * @param factory the factory that makes each of the identical children.
     * @param atoms the number of identical children per group (default is 1).
     * @param config the configuration applied to each group that is built (default is Linear).
     */
    public AtomFactoryHetero(Simulation sim, AtomFactory[] factory, 
                            Configuration config) {
        this(sim, factory, config, sim.getIteratorFactory().simpleSequencerFactory());
    }
    /**
     * @param factory the factory that makes each of the identical children.
     * @param atoms the number of identical children per group (default is 1).
     * @param config the configuration applied to each group that is built (default is Linear).
     * @param sequencerFactory the factory making sequencers used in the groups made by this factory (default is simple sequencer).
     */
    public AtomFactoryHetero(Simulation sim, AtomFactory[] factory, 
                            Configuration config, AtomSequencer.Factory sequencerFactory) {
        super(sim, sequencerFactory);
        childFactory = factory;
        configuration = config;
        //set up fields of Group type
        //children are forced (for now) to use simple sequencer
        groupType.childSequencerClass = sim.iteratorFactory.simpleSequencerClass();
        for(int i=0; i<factory.length; i++) {
            groupType.childrenAreGroups = factory[i].isGroupFactory();
            if(groupType.childrenAreGroups) break;
        }
    }
    
    public boolean isGroupFactory() {return true;}
    
    /**
     * Constructs a new group.
     */
    public Atom build(Atom group) {
        for(int i=0; i<childFactory.length; i++) {
            childFactory[i].build((AtomTreeNodeGroup)group.node);//builds child atom with group as parent
        }
        bondInitializer.makeBonds(group);
        configuration.initializeCoordinates(group);
        return group;
    }
    
    /**
     * Returns the array of subfactories that produces each of the identical atoms
     * in the group made by this factory.
     */
    public AtomFactory[] childFactory() {return childFactory;}
    
    public boolean vetoAddition(Atom a) {return true;} 
        
/*    public void renew(Atom a) {//need an exception in the case a is unrenewable
        if(a.type != groupType) return;  //throw exception
        configuration.initializeCoordinates((AtomGroup)a);
    }       
*/        
}//end of AtomFactoryHomo
    

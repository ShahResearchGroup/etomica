package etomica;

public final class IntegratorVerlet extends IntegratorMD implements EtomicaElement {

    public String getVersion() {return "IntegratorVerlet:01.07.05/"+IntegratorMD.VERSION;}

    AtomIterator atomIterator;
    
    public final PotentialCalculationForceSum forceSum;
    private final IteratorDirective allAtoms = new IteratorDirective();
    
    Space.Vector work;
                
    public IntegratorVerlet() {
        this(Simulation.instance);
    }
    public IntegratorVerlet(final Simulation sim) {
        super(sim);
        forceSum = new PotentialCalculationForceSum(sim.space());
        work = sim.space().makeVector();
        setTimeStep(etomica.units.LennardJones.Time.UNIT.toSim(2.0));
    }

    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Molecular dynamics using basic Verlet algorithm");
        return info;
    }

	/**
	 * Overrides superclass method to instantiate iterators when iteratorFactory in phase is changed.
	 * Called by Integrator.addPhase and Integrator.iteratorFactorObserver.
	 */
	protected void makeIterators(IteratorFactory factory) {
        atomIterator = factory.makeAtomIterator();
    }
    
        
  private double t2;
  public final void setTimeStep(double t) {
    super.setTimeStep(t);
    t2 = timeStep*timeStep;
  }
          
//--------------------------------------------------------------
// steps all particles across time interval tStep

    public void doStep() {

        //Compute forces on each atom
        atomIterator.reset();
        while(atomIterator.hasNext()) {   //zero forces on all atoms
            ((Agent)atomIterator.next().ia).force.E(0.0);
        }
        potential.calculate(allAtoms, forceSum);

        //take step
        atomIterator.reset();
        while(atomIterator.hasNext()) {
            Atom a = atomIterator.next();
            Agent agent = (Agent)a.ia;
            Space.Vector r = a.coord.position();
            work.E(r);
            r.PE(agent.rMrLast);
            agent.force.TE(a.coord.rm()*t2);
            r.PE(agent.force);
            agent.rMrLast.E(r);
            agent.rMrLast.ME(work);
        }
    }//end of doStep
    

    protected void doReset() {
        atomIterator.reset();
        potential.set(firstPhase);//assumes only one phase
        while(atomIterator.hasNext()) {
            Atom a = atomIterator.next();
            Agent agent = (Agent)a.ia;
            agent.rMrLast.Ea1Tv1(-timeStep*a.coord.rm(),a.coord.momentum());
        }
    }
              
//--------------------------------------------------------------

    public final Integrator.Agent makeAgent(Atom a) {
        return new Agent(parentSimulation(),a);
    }
            
    public final static class Agent implements Integrator.Agent.Forcible {  //need public so to use with instanceof
        public Atom atom;
        public Space.Vector force;
        public Space.Vector rMrLast;  //r - rLast

        public Agent(Simulation sim, Atom a) {
            atom = a;
            force = sim.space().makeVector();
            rMrLast = sim.space().makeVector();
        }
        
        public Space.Vector force() {return force;}
    }//end of Agent
    
/*    public static void main(String[] args) {
        
	    IntegratorVerlet integrator = new IntegratorVerlet();
	    SpeciesSpheres species = new SpeciesSpheres();
	    Phase phase = new Phase();
	    P2LennardJones potential = new P2LennardJones();
	    Controller controller = new Controller();
	    DisplayPhase display = new DisplayPhase();
	    IntegratorMD.Timer timer = integrator.new Timer(integrator.chronoMeter());
	    timer.setUpdateInterval(10);

		MeterEnergy energy = new MeterEnergy();
		energy.setPhase(phase);
		energy.setHistorying(true);
		energy.setActive(true);		
		energy.getHistory().setNValues(500);		
		DisplayPlot plot = new DisplayPlot();
		plot.setLabel("Energy");
		plot.setDataSource(energy.getHistory());
		
		integrator.setSleepPeriod(2);
		
		Simulation.instance.elementCoordinator.go();
        Simulation.makeAndDisplayFrame(Simulation.instance);

    }//end of main 
    */
}//end of IntegratorVerlet


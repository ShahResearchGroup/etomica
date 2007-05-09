package etomica.normalmode;

import Jama.Matrix;
import etomica.action.activity.ActivityIntegrate;
import etomica.config.ConfigurationLatticeSimple;
import etomica.integrator.IntegratorMD;
import etomica.lattice.BravaisLattice;
import etomica.lattice.crystal.Primitive;
import etomica.lattice.crystal.PrimitiveCubic;
import etomica.lattice.crystal.PrimitiveFcc;
import etomica.phase.Phase;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;
import etomica.space.Boundary;
import etomica.space.BoundaryDeformableLattice;
import etomica.space.BoundaryRectangularPeriodic;
import etomica.space.Space;
import etomica.species.SpeciesSpheresMono;

/**
 * Simulation class of hard spheres in 1D or 3D that calculates the dq/dx
 * Jacobian.
 */
public class SimCalcJ extends Simulation {

    public SimCalcJ(Space space, int numAtoms) {
        super(space, true, new PotentialMaster(space));

        defaults.makeLJDefaults();
        defaults.atomSize = 1.0;

        SpeciesSpheresMono species = new SpeciesSpheresMono(this);
        getSpeciesManager().addSpecies(species);

        phase = new Phase(this);
        addPhase(phase);
        phase.getAgent(species).setNMolecules(numAtoms);

        Primitive primitive;
        if (space.D() == 1) {
            primitive = new PrimitiveCubic(space, 1);
            bdry = new BoundaryRectangularPeriodic(space, getRandom(), numAtoms);
        }
        else {
            primitive = new PrimitiveFcc(space, 1);
            int n = (int)Math.round(Math.pow(numAtoms, 1.0/3.0));
            bdry = new BoundaryDeformableLattice(primitive, getRandom(), new int[]{n,n,n});
        }
        phase.setBoundary(bdry);

        lattice = new BravaisLattice(primitive);
        config = new ConfigurationLatticeSimple(lattice);

        config.initializeCoordinates(phase);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        // defaults
        int D = 1;
        int nA = 4;

        // parse arguments
        if (args.length > 0) {
            nA = Integer.parseInt(args[0]);
        }

        System.out.println("Calculating "
                + (D == 1 ? "1D" : (D == 3 ? "FCC" : "2D hexagonal"))
                + " hard sphere Jacobian for N="+nA);

        // construct simulation
        SimCalcJ sim = new SimCalcJ(Space.getInstance(D), nA);

        Primitive primitive = sim.lattice.getPrimitive();

        // set up normal-mode meter
        WaveVectorFactory waveVectorFactory;
        if (D == 1) {
            waveVectorFactory = new WaveVectorFactory1D();
        } else if (D == 2) {
            waveVectorFactory = null;
        } else {
            waveVectorFactory = new WaveVectorFactorySimple(primitive);
        }
        CalcJacobian meterJacobian = new CalcJacobian(D);
        meterJacobian.setWaveVectorFactory(waveVectorFactory);
        meterJacobian.setPhase(sim.phase);

        double[][] jac = meterJacobian.getJacobian();
        
        if (jac.length < 100) {
            for (int i=0; i<jac.length; i++) {
                for (int j=0; j<jac[0].length; j++) {
                    System.out.print(jac[i][j]+"  ");
                }
                System.out.println();
            }
        }
        
        Matrix m = new Matrix(jac);
        double d = Math.abs(m.det());
        System.out.println("dx/dq det = "+1/d+" (log2 det = "+Math.log(1.0/d*Math.sqrt(nA))/Math.log(2)+")");
    }

    private static final long serialVersionUID = 1L;
    public IntegratorMD integrator;
    public ActivityIntegrate activityIntegrate;
    public Phase phase;
    public Boundary bdry;
    public BravaisLattice lattice;
    public ConfigurationLatticeSimple config;
}
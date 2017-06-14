package etomica.simulation;

import etomica.data.meter.MeterPressureTensor;
import etomica.space3d.Space3D;
import etomica.surfacetension.LJMC;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Fork(1)
public class BenchSimSurfacetensionLJMC {

    @Param({"1000", "20000"})
    private int numAtoms;

    private LJMC sim;
    private MeterPressureTensor meter;

    @Setup(Level.Iteration)
    public void setUp() {
        sim = new LJMC(Space3D.getInstance(), numAtoms, 1.1, 6);

        meter = new MeterPressureTensor(sim.potentialMaster, sim.space);
        meter.setBox(sim.box);
        meter.setTemperature(1.1);

    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Warmup(time = 1, iterations = 5)
    @Measurement(time = 1, timeUnit = TimeUnit.SECONDS)
    public void surfacetension() {
        sim.integrator.doStep();
    }
}
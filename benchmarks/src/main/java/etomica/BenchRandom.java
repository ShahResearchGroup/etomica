package etomica;

import etomica.util.random.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1)
public class BenchRandom {
    @Param({"mersennetwister", "xoroshiro", "java"})
    private String type;

    private IRandom random;

    @Setup(Level.Trial)
    public void setup() {
        switch (type) {
            case "mersennetwister":
                random = new RandomMersenneTwister(RandomNumberGeneratorUnix.getRandSeedArray());
                break;
            case "xoroshiro":
                random = new XoRoShiRo128PlusRandom();
                break;
            case "java":
                random = new RandomNumberGenerator();
        }
    }

    @Benchmark
    public double benchRandom() {
        double sum = 0;
        for (int i = 0; i < 100; i++) {
            sum += random.nextDouble();
        }
        return sum;
    }

    public static void main(String[] args) throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(BenchRandom.class.getSimpleName())
                .build();

        new Runner(opts).run();
    }
}

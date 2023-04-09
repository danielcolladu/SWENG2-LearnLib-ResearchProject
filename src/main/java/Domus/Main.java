package Domus;

import Domus.DatasetUtils.CustomGson;
import Domus.DatasetUtils.DataserClass.Dataset;
import Domus.DatasetUtils.DomusRecord;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.learnlib.algorithms.lstar.dfa.ClassicLStarDFA;
import de.learnlib.algorithms.lstar.dfa.ClassicLStarDFABuilder;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.datastructure.observationtable.writer.ObservationTableASCIIWriter;
import de.learnlib.oracle.equivalence.SampleSetEQOracle;
import de.learnlib.util.Experiment;
import de.learnlib.util.statistics.SimpleProfiler;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.serialization.dot.GraphDOT;
import net.automatalib.visualization.Visualization;

import java.io.*;

public class Main {
    public static void main(String[] args) throws IOException {
        // setting up dataset
        Dataset datasetSeries1 = readJson("./DatasetSeries1.json");
        Dataset datasetSeries2 = readJson("./DatasetSeries2.json");
        int nUsers = 6;
        int nDays = 5;

        // test driver
        DomusTestDriver testDriver = new DomusTestDriver(nUsers, nDays, datasetSeries2, datasetSeries1);

        // membership oracle
        MembershipOracle<DomusRecord, Boolean> mOracle = new DomusOracle(testDriver);

        // equivalence oracle
        // EquivalenceOracle.DFAEquivalenceOracle<DomusRecord> eqOracle = new DFAWMethodEQOracle<>(mOracle, 100);
        SampleSetEQOracle<DomusRecord, Boolean> eqOracle = new SampleSetEQOracle<>(false);
        for (int u = 0; u < nUsers; u++) {
            for (int d = 0; d < nDays; d++) {
                eqOracle.addAll(mOracle, new DomusWord(datasetSeries2.getUsers().get(u).get(d).getPreTea()));
                eqOracle.addAll(mOracle, new DomusWord(datasetSeries2.getUsers().get(u).get(d).getDuringTea()));
                eqOracle.addAll(mOracle, new DomusWord(datasetSeries2.getUsers().get(u).get(d).getPostTea()));
            }
        }

        // l star algorithm
        ClassicLStarDFA<DomusRecord> lStarDFA = new ClassicLStarDFABuilder<DomusRecord>()
                .withAlphabet(DomusTestDriver.SIGMA)
                .withOracle(mOracle)
                .create();

        // experiment
        Experiment.DFAExperiment<DomusRecord> experiment = new Experiment.DFAExperiment<>(lStarDFA, eqOracle, DomusTestDriver.SIGMA);

        // turn on time profiling
        experiment.setProfile(true);

        // enable logging of models
        experiment.setLogModels(true);

        // run experiment
        experiment.run();

        // get learned model
        DFA<?, DomusRecord> result = experiment.getFinalHypothesis();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        // save result to Json, result is a compactDFA
        try (FileWriter writer = new FileWriter("./DomusDFA.json")) {
            gson.toJson(result, writer);
        } catch (IOException e) {
           e.printStackTrace();
        }

        // report results
        System.out.println("-------------------------------------------------------");

        // profiling
        System.out.println(SimpleProfiler.getResults());

        // learning statistics
        System.out.println(experiment.getRounds().getSummary());
        System.out.println();
        new ObservationTableASCIIWriter<>().write(lStarDFA.getObservationTable(), System.out);

        // model statistics
        System.out.println("States: " + result.size());
        System.out.println("Sigma: " + DomusTestDriver.SIGMA.size());

        // show model
        System.out.println();
        System.out.println("Model: ");
        GraphDOT.write(result, DomusTestDriver.SIGMA, System.out); // may throw IOException!

        Visualization.visualize(result, DomusTestDriver.SIGMA);

        System.out.println("-------------------------------------------------------");
    }

    private static Dataset readJson(String path) throws FileNotFoundException {
        Gson g = CustomGson.getCustomGson();
        Reader reader = new FileReader(path);
        Dataset d = g.fromJson(reader,Dataset.class);
        System.out.println("Read " + path);
        return d;
    }
}

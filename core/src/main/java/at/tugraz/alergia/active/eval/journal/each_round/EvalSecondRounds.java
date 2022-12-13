/*******************************************************************************
 * prob-black-reach
 * Copyright (C) 2017 TU Graz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package at.tugraz.alergia.active.eval.journal.each_round;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;

import at.tugraz.alergia.active.ActiveTestingStrategyInference;
import at.tugraz.alergia.active.Experiment;
import at.tugraz.alergia.active.adapter.Adapter;
import at.tugraz.alergia.active.adapter.prism_matrix_export.MatrixExportAdapter;
import at.tugraz.alergia.active.eval.Config;
import at.tugraz.alergia.active.eval.gridworld.Direction;
import at.tugraz.alergia.active.strategy.UniformSelectionTestStrategy;
import at.tugraz.alergia.active.strategy.adversary.AdversaryBasedTestStrategy;
import at.tugraz.alergia.data.InputOutputStep;
import at.tugraz.alergia.data.InputSymbol;

public class EvalSecondRounds {
	public static long[] seeds = {
			1000l, 2000l, 3000l, 4000l, 5000l, 6000l, 7000l, 8000l, 9000l, 10000l
			// 11000l, 12000l, 13000l, 14000l, 15000l, 16000l, 17000l, 18000l, 19000l, 110000l 
	};
	private static String logFileName = null;

	public static final int STEP_BOUND = 15;
	
	public static final int ROUNDS = 150;
	public static final int BATCHSIZE = 500;


	public static final double STOP_PROBABILITY = 0.5;

	public static final Predicate<List<InputOutputStep>> alternativeStoppingCriterion = (
			List<InputOutputStep> trace) -> 
	trace.get(trace.size()-1).getOutput().stringRepresentation().equals("goal");

	public static ActiveTestingStrategyInference incremental(Adapter adapter, InputSymbol[] inputs,
			String prismLocation) {
		double probRandomSample = 0.75;
		double probRandomSampleChangeFactor = 0.975;
		int nrRounds = ROUNDS;
		int batchSize = BATCHSIZE;
		long initialSeed = 0;

		AdversaryBasedTestStrategy testStrategy = new AdversaryBasedTestStrategy(adapter, STEP_BOUND, batchSize,
				initialSeed, STOP_PROBABILITY, inputs, probRandomSample, probRandomSampleChangeFactor, prismLocation);
		testStrategy.setUsePrism(false);
		Set<InputSymbol> inputSet = new HashSet<>(Arrays.asList(inputs));
		testStrategy.setInputs(inputSet);
		
		testStrategy.setAlternativeStoppingCriterion(alternativeStoppingCriterion);
		ActiveTestingStrategyInference inferrer = new ActiveTestingStrategyInference(nrRounds, testStrategy);
		inferrer.setUseAdaptiveEpsilon(false);
		return inferrer;
	}

	public static void main(String[] args) throws Exception {

		String prismLocation = Config.prismLocation();
		InputSymbol[] inputs = Direction.asInputSymbols();

		Adapter adapter = new MatrixExportAdapter("core/src/main/resources/gridworld/journal/second");

		String propertiesFile = "core/src/main/resources/gridworld/journal/second.props";

		int property = 1;
		String path = "log_journal/eval_each_round/log_second_gridworld_prop1_500";
		logFileName = path + ".log";
		List<List<Double>> allEvaluations = new ArrayList<>();
		for (long seed : seeds) {
			System.out.println("SEED: " + seed);
			ActiveTestingStrategyInference inferrer = incremental(adapter, inputs, prismLocation);
			inferrer.setMaxNrRounds(500);
			inferrer.setEvalEachRound(true);
			adapter.init(seed);
			inferrer.getStrategy().setSeed(seed);
			inferrer.getStrategy().init(propertiesFile, property);
			inferrer.run();
			List<Double> evaluations = inferrer.getEvaluations();
			allEvaluations.add(evaluations);

			printToFile(allEvaluations);
		}
		printToFile(allEvaluations);
		createMeanGraph(allEvaluations);
	}

	private static void createMeanGraph(List<List<Double>> allEvaluations) {
		List<Double> means = new ArrayList<>();
		for (int round = 0; round < 500; round++) {
			double meanForRound = 0.0;
			for (List<Double> evals : allEvaluations) {
				Double evalForRound = evals.get(round);
				meanForRound += evalForRound / allEvaluations.size();
			}
			means.add(meanForRound);
		}
		System.out.println(means.stream().map(Object::toString).collect(Collectors.joining(";")));
	}

	private static void printToFile(List<List<Double>> allEvaluations) throws IOException {
		File file = new File(logFileName);
		if (!file.getParentFile().exists())
			file.getParentFile().mkdirs();
		try (FileWriter fw = new FileWriter(file)) {
			for (List<Double> evals : allEvaluations) {
				String logString = evals.stream().map(Object::toString).collect(Collectors.joining(","));
				fw.write(logString);
				fw.write(System.lineSeparator());
			}
		}

	}

}

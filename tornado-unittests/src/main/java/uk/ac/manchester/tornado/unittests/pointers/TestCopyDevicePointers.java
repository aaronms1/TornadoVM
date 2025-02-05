/*
 * Copyright (c) 2025, APT Group, Department of Computer Science,
 * The University of Manchester.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.ac.manchester.tornado.unittests.pointers;

import static org.junit.Assert.assertEquals;

import java.util.stream.IntStream;

import org.junit.Test;

import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;
import uk.ac.manchester.tornado.api.exceptions.TornadoExecutionPlanException;
import uk.ac.manchester.tornado.api.types.arrays.FloatArray;
import uk.ac.manchester.tornado.unittests.common.TornadoTestBase;

/**
 * How to run?
 * <p>
 * <code>
 * tornado-test -pbc --threadInfo -V uk.ac.manchester.tornado.unittests.pointers.TestCopyDevicePointers
 * </code>
 * </p>
 */
public class TestCopyDevicePointers extends TornadoTestBase {

    public static void iterativeUpdate(FloatArray array) {
        for (@Parallel int i = 0; i < array.getSize(); i++) {
            array.set(i, array.get(i) + 1.0f);
        }
    }

    public static void iterativeUpdate2D(FloatArray array, int m, int n) {
        for (@Parallel int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                array.set(i * n + j, i);
            }
        }
    }

    public static void compute(FloatArray array) {
        for (@Parallel int i = 0; i < array.getSize(); i++) {
            array.set(i, array.get(i) * 2);
        }
    }

    public static void computeRow(FloatArray array) {
        for (@Parallel int i = 0; i < array.getSize(); i++) {
            array.set(i, array.get(i) + 0.1f);
        }
    }

    @Test
    public void testCopyDevicePointers() throws TornadoExecutionPlanException {
        final int size = 32;

        FloatArray array = new FloatArray(size);
        array.init(0.5f);

        // We will have a task graph which needs to be executed multiple times on the
        // hardware accelerator
        TaskGraph taskGraph1 = new TaskGraph("s0") //
                .task("s0", TestCopyDevicePointers::iterativeUpdate, array) //
                .transferToHost(DataTransferMode.UNDER_DEMAND, array);

        FloatArray copyArray = new FloatArray(size);

        // Then we will have a second task-graph for which we will need to pass data from
        // another task-graph. The idea is to copy device pointers to avoid sync with the host
        // back and forth.
        TaskGraph taskGraph2 = new TaskGraph("s1") //
                .transferToDevice(DataTransferMode.UNDER_DEMAND, copyArray)   // Copy-In should be under demand
                .task("s1", TestCopyDevicePointers::compute, copyArray) //
                .transferToHost(DataTransferMode.EVERY_EXECUTION, copyArray);

        try (TornadoExecutionPlan executionPlan = new TornadoExecutionPlan(taskGraph1.snapshot(), taskGraph2.snapshot())) {
            // execute the first graph

            // run for a few iterations
            for (int i = 0; i < 3; i++) {
                executionPlan.withGraph(0).execute();
            }

            // perform a copy
            int offset = 0;
            int fromGraphIndex = 0;
            int toGraphIndex = 1;
            executionPlan.copyPointerFromGraphToGraph(copyArray, array, offset, fromGraphIndex, toGraphIndex);

            // Execute the second graph with updated pointers
            executionPlan.withGraph(1).execute();

            // Check result
            IntStream.range(0, copyArray.getSize()).forEach(i -> assertEquals(7.0f, copyArray.get(i), 0.01f));
        }
    }

    @Test
    public void testCopyDevicePointersMatrix() throws TornadoExecutionPlanException {
        final int size = 32;

        FloatArray matrix = new FloatArray(size * size);

        // We will have a task graph which needs to be executed multiple times on the
        // hardware accelerator
        TaskGraph taskGraph1 = new TaskGraph("s0") //
                .task("s0", TestCopyDevicePointers::iterativeUpdate2D, matrix, size, size) //
                .transferToHost(DataTransferMode.UNDER_DEMAND, matrix);

        FloatArray row = new FloatArray(size);

        // Then we will have a second task-graph for which we will need to pass data from
        // another task-graph. The idea is to copy device pointers to avoid sync with the host
        // back and forth.
        TaskGraph taskGraph2 = new TaskGraph("s1") //
                .transferToDevice(DataTransferMode.UNDER_DEMAND, row)   // Copy-In should be under demand
                .task("s1", TestCopyDevicePointers::computeRow, row) //
                .transferToHost(DataTransferMode.EVERY_EXECUTION, row);

        try (TornadoExecutionPlan executionPlan = new TornadoExecutionPlan(taskGraph1.snapshot(), taskGraph2.snapshot())) {

            executionPlan.withGraph(0).execute();

            int offset = 15 * size;
            int fromGraphIndex = 0;
            int toGraphIndex = 1;
            executionPlan.copyPointerFromGraphToGraph(row, matrix, offset, fromGraphIndex, toGraphIndex);

            // Execute the second graph with updated pointers
            executionPlan.withGraph(1).execute();

            IntStream.range(0, row.getSize()).forEach(i -> assertEquals(15.1f, row.get(i), 0.01f));
        }
    }
}

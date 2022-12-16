/*
 * Copyright (c) 2021, 2022, APT Group, Department of Computer Science,
 * The University of Manchester.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.ac.manchester.tornado.unittests.foundation;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;

import org.junit.Test;

import uk.ac.manchester.tornado.api.ImmutableTaskGraph;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutor;
import uk.ac.manchester.tornado.api.TornadoExecutorPlan;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;
import uk.ac.manchester.tornado.unittests.common.TornadoTestBase;

/**
 * <p>
 * How to test?
 * </p>
 * <code>
 *     tornado-test -V uk.ac.manchester.tornado.unittests.foundation.MultipleRuns
 * </code>
 */
public class MultipleRuns extends TornadoTestBase {

    @Test
    public void multipleRuns() {

        final int numElements = 512;
        int[] a = new int[numElements];

        final int iterations = 50;

        int[] expectedResult = new int[numElements];
        Arrays.fill(expectedResult, iterations * 50);

        TaskGraph taskGraph = new TaskGraph("s0") //
                .transferToDevice(DataTransferMode.EVERY_EXECUTION, a) //
                .task("t0", TestKernels::addValue, a) //
                .transferToHost(a);

        ImmutableTaskGraph immutableTaskGraph = taskGraph.snapshot();
        TornadoExecutorPlan executor = new TornadoExecutor(immutableTaskGraph).build();

        for (int i = 0; i < iterations; i++) {
            executor.execute();
        }
        assertArrayEquals(expectedResult, a);
    }
}

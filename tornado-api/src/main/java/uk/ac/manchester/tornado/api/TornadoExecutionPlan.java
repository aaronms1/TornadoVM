/*
 * Copyright (c) 2013-2024, APT Group, Department of Computer Science,
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
package uk.ac.manchester.tornado.api;

import java.util.concurrent.atomic.AtomicLong;

import uk.ac.manchester.tornado.api.common.TornadoDevice;
import uk.ac.manchester.tornado.api.enums.ProfilerMode;
import uk.ac.manchester.tornado.api.enums.TornadoVMBackendType;
import uk.ac.manchester.tornado.api.exceptions.TornadoExecutionPlanException;
import uk.ac.manchester.tornado.api.plantype.ExecutionPlanType;
import uk.ac.manchester.tornado.api.plantype.OffConcurrentDevices;
import uk.ac.manchester.tornado.api.plantype.OffMemoryLimit;
import uk.ac.manchester.tornado.api.plantype.OffPrintKernel;
import uk.ac.manchester.tornado.api.plantype.OffProfiler;
import uk.ac.manchester.tornado.api.plantype.OffThreadInfo;
import uk.ac.manchester.tornado.api.plantype.WithWarmUp;
import uk.ac.manchester.tornado.api.plantype.WithBatch;
import uk.ac.manchester.tornado.api.plantype.WithClearProfiles;
import uk.ac.manchester.tornado.api.plantype.WithCompilerFlags;
import uk.ac.manchester.tornado.api.plantype.WithConcurrentDevices;
import uk.ac.manchester.tornado.api.plantype.WithDefaultScheduler;
import uk.ac.manchester.tornado.api.plantype.WithDevicePlan;
import uk.ac.manchester.tornado.api.plantype.WithDynamicReconfiguration;
import uk.ac.manchester.tornado.api.plantype.WithFreeDeviceMemory;
import uk.ac.manchester.tornado.api.plantype.WithGridScheduler;
import uk.ac.manchester.tornado.api.plantype.WithMemoryLimit;
import uk.ac.manchester.tornado.api.plantype.WithPrintKernel;
import uk.ac.manchester.tornado.api.plantype.WithProfiler;
import uk.ac.manchester.tornado.api.plantype.WithResetDevice;
import uk.ac.manchester.tornado.api.plantype.WithThreadInfo;
import uk.ac.manchester.tornado.api.runtime.ExecutorFrame;
import uk.ac.manchester.tornado.api.runtime.TornadoRuntimeProvider;

/**
 * Class to create and optimize execution plans for running a set of
 * immutable tasks-graphs on modern hardware. An executor plan contains an
 * executor object, which in turn, contains a set of immutable task-graphs.
 * All actions applied to the execution plan affect to all the immutable
 * graphs associated with it.
 *
 * @since v0.15
 */
public sealed class TornadoExecutionPlan implements AutoCloseable permits ExecutionPlanType {

    /**
     * Method to obtain the default device in TornadoVM. The default one corresponds
     * to the device assigned to the driver (backend) with index 0 and device 0.
     */
    public static TornadoDevice DEFAULT_DEVICE = TornadoRuntimeProvider.getTornadoRuntime().getDefaultDevice();

    private static final AtomicLong globalExecutionPlanCounter = new AtomicLong(0);

    /**
     * The TornadoVM executor is a list of chain of actions to be performed.
     * Each action can enable/disable runtime features, influence in the compiler,
     * influence the code optimization, adapt runtime parameters, etc.
     */
    protected TornadoExecutor tornadoExecutor;

    protected ExecutorFrame executionFrame;

    protected TornadoExecutionPlan childLink;
    protected TornadoExecutionPlan parentLink;

    /**
     * Create an Execution Plan: Object to create and optimize an execution plan for
     * running a set of immutable tasks-graphs. An executor plan contains an
     * executor object, which in turn, contains a set of immutable task-graphs. All
     * actions applied to the execution plan affect to all the immutable graphs
     * associated with it.
     *
     * @param immutableTaskGraphs
     *     {@link ImmutableTaskGraph}
     */
    public TornadoExecutionPlan(ImmutableTaskGraph... immutableTaskGraphs) {
        this.tornadoExecutor = new TornadoExecutor(immutableTaskGraphs);
        final long id = globalExecutionPlanCounter.incrementAndGet();
        executionFrame = new ExecutorFrame(id);
    }

    /**
     * Method to obtain a specific device using the driver index (backend index) and
     * device index.
     *
     * @param driverIndex
     *     Integer value that identifies the backend to be used.
     * @param deviceIndex
     *     Integer value that identifies the device within the backend to be
     *     used.
     * @return {@link TornadoDevice}
     *
     */
    public static TornadoDevice getDevice(int driverIndex, int deviceIndex) {
        return TornadoRuntimeProvider.getTornadoRuntime().getBackend(driverIndex).getDevice(deviceIndex);
    }

    /**
     * Method to return the total number of execution plans instantiated in a single JVM instance.
     *
     * @since 1.0.2
     * 
     * @return int
     */
    public static int getTotalPlans() {
        return globalExecutionPlanCounter.intValue();
    }

    /**
     * Return a data structure that contains all drivers and devices that the TornadoVM Runtime can access.
     * 
     * @return {@link TornadoDeviceMap}
     */
    public static TornadoDeviceMap getTornadoDeviceMap() {
        return new TornadoDeviceMap();
    }

    /**
     * Execute an execution plan. It returns a {@link TornadoExecutionPlan} for
     * further build different optimization after the execution as well as obtain
     * the profiler results.
     *
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionResult execute() {
        tornadoExecutor.execute(executionFrame);
        TornadoProfilerResult profilerResult = new TornadoProfilerResult(tornadoExecutor);
        return new TornadoExecutionResult(profilerResult);
    }

    /**
     * It invokes the JIT compiler for all immutable tasks-graphs associated to an
     * executor.
     *
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan withWarmUp() {
        tornadoExecutor.warmup(executionFrame);
        return new WithWarmUp(this);
    }

    /**
     * It selects a specific device for all immutable tasks graphs associated to an
     * executor.
     *
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan withDevice(TornadoDevice device) {
        tornadoExecutor.setDevice(device);
        return new WithDevicePlan(this);
    }

    /**
     * Print all operations enabled/disabled from the Execution Plan.
     * 
     * @since 1.0.8
     */
    public void printTraceExecutionPlan() {
        System.out.println(childLink);
    }

    /**
     * Returns a string with all the operations enabled/disabled from the
     * Execution Plan.
     *
     * @since 1.0.8
     */
    public String getTraceExecutionPlan() {
        return childLink.toString();
    }

    @Override
    public String toString() {
        return "Root";
    }

    /**
     * It selects a specific device for one particular task of the task-graph.
     *
     * @param taskName
     *     The task-name is identified by the task-graph name followed by a dot (".") and
     *     the task name. For example: "graph.task1".
     * @param device
     *     The device is an instance of a {@link TornadoDevice}
     *
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan withDevice(String taskName, TornadoDevice device) {
        tornadoExecutor.setDevice(taskName, device);
        return new WithDevicePlan(this);
    }

    /**
     * It enables multiple tasks in a task graph to run concurrently on the same
     * or different devices. Note that the TornadoVM runtime does not check for
     * data dependencies across tasks when using this API call. Thus, it is
     * the responsibility of the programmer to provide tasks with no data dependencies
     * when invoking the method {@link TornadoExecutionPlan#withConcurrentDevices}.
     *
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan withConcurrentDevices() {
        tornadoExecutor.withConcurrentDevices();
        return new WithConcurrentDevices(this);
    }

    /**
     * It disables multiple tasks in a task graph to run concurrently on the same
     * or different devices.
     *
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan withoutConcurrentDevices() {
        tornadoExecutor.withoutConcurrentDevices();
        return new OffConcurrentDevices(this);
    }

    /**
     * It obtains the device for a specific immutable task-graph. Note that,
     * ideally, different task immutable task-graph could be executed on different
     * devices.
     *
     * @param immutableTaskGraphIndex
     *     Index of a specific immutable task-graph
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoDevice getDevice(int immutableTaskGraphIndex) {
        return tornadoExecutor.getDevice(immutableTaskGraphIndex);
    }

    /**
     * Mark all device buffers that correspond to the current execution plan as free
     * in order for the TornadoVM runtime system to reuse those buffers and avoid
     * continuous device memory deallocation and allocation.
     *
     * <p>
     * Note that, in this context, "free device memory" means the TornadoVM runtime
     * system marks device buffers to be reusable, thus, for the runtime system,
     * device buffers are no longer linked to the current execution plan.
     * </p>
     *
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan freeDeviceMemory() {
        tornadoExecutor.freeDeviceMemory();
        return new WithFreeDeviceMemory(this);
    }

    /**
     * Use a {@link GridScheduler} for thread dispatch. The same GridScheduler will
     * be applied to all tasks within the executor. Note that the grid-scheduler API
     * can specify all workers for each task-graph.
     *
     * @param gridScheduler
     *     {@link GridScheduler}
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan withGridScheduler(GridScheduler gridScheduler) {
        tornadoExecutor.withGridScheduler(gridScheduler);
        return new WithGridScheduler(this);
    }

    /**
     * Notify the TornadoVM runtime system to utilize the default thread scheduler.
     *
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan withDefaultScheduler() {
        tornadoExecutor.withDefaultScheduler();
        return new WithDefaultScheduler(this);
    }

    /**
     * Use the TornadoVM dynamic reconfiguration (akka live task migration) across
     * visible devices.
     *
     * @param policy
     *     {@link Policy}
     * @param mode
     *     {@link DRMode}
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan withDynamicReconfiguration(Policy policy, DRMode mode) {
        executionFrame.withPolicy(policy).withMode(mode);
        return new WithDynamicReconfiguration(this);
    }

    /**
     * Enable batch processing. TornadoVM will split the iteration space in smaller
     * batches (with batch size specified by the user). This is used mainly when
     * users want to execute big data applications that do not fit on the device's
     * global memory.
     *
     * @param batchSize
     *     String in the format a number + "MB" Example "512MB".
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan withBatch(String batchSize) {
        tornadoExecutor.withBatch(batchSize);
        return new WithBatch(this);
    }

    /**
     * Enables the profiler. The profiler includes options to query device kernel
     * time, data transfers and compilation at different stages (JIT, driver
     * compilation, Graal, etc.).
     *
     * @param profilerMode
     *     {@link ProfilerMode}
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan withProfiler(ProfilerMode profilerMode) {
        executionFrame.withProfilerOn(profilerMode);
        return new WithProfiler(this);
    }

    /**
     * Disables the profiler if previous execution plan had the profiler enabled.
     *
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan withoutProfiler() {
        executionFrame.withProfilerOff();
        return new OffProfiler(this);
    }

    /**
     * This method sets a limit to the amount of memory used on the target
     * hardware accelerator. The TornadoVM runtime will check that the
     * current instance of the {@link TornadoExecutionPlan} does not exceed
     * the limit that was specified.
     *
     * @param memoryLimit
     *     Specify the limit in a string format. E.g., "1GB", "512MB".
     *
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan withMemoryLimit(String memoryLimit) {
        tornadoExecutor.withMemoryLimit(memoryLimit);
        return new WithMemoryLimit(this);
    }

    /**
     * It disables the memory limit for the current instance of an
     * {@link TornadoExecutionPlan}. This is the default action.
     * If the memory limit is not set, then the maximum memory to use
     * is set to the maximum buffer allocation (e.g., 1/4 of the total
     * capacity using the OpenCL backend), or the maximum memory available
     * on the target device.
     *
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan withoutMemoryLimit() {
        tornadoExecutor.withoutMemoryLimit();
        return new OffMemoryLimit(this);
    }

    /**
     * Reset the execution context for the current execution plan. The TornadoVM
     * runtime system will clean the code cache and all events associated with the
     * current execution. It resets the internal GPU/FPGA/CPU execution context to
     * its default values.
     *
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan resetDevice() {
        tornadoExecutor.resetDevice();
        return new WithResetDevice(this);
    }

    /**
     * Obtains the ID that was assigned to the execution plan.
     */
    public long getId() {
        return executionFrame.getExecutionPlanId();
    }

    /**
     * Obtains the total number of execution plans instantiated in a TornadoVM application.
     */
    public long getGlobalExecutionPlansCounter() {
        return globalExecutionPlanCounter.get();
    }

    /**
     * Clean all events associated with previous executions.
     *
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan clearProfiles() {
        tornadoExecutor.clearProfiles();
        return new WithClearProfiles(this);
    }

    /**
     * Enable printing of the Thread-Block Deployment for the generated kernels.
     *
     * @since 1.0.2
     * 
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan withThreadInfo() {
        tornadoExecutor.withThreadInfo();
        return new WithThreadInfo(this);
    }

    /**
     * Disable printing of the Thread-Block Deployment for the generated kernels.
     *
     * @since 1.0.2
     * 
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan withoutThreadInfo() {
        tornadoExecutor.withoutThreadInfo();
        return new OffThreadInfo(this);
    }

    /**
     * Enable printing of the generated kernels for each task in a task-graph.
     *
     * @since 1.0.2
     * 
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan withPrintKernel() {
        tornadoExecutor.withPrintKernel();
        return new WithPrintKernel(this);
    }

    /**
     * Disable printing of the generated kernels for each task in a task-graph.
     * 
     * @since 1.0.2
     *
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan withoutPrintKernel() {
        tornadoExecutor.withoutPrintKernel();
        return new OffPrintKernel(this);
    }

    /**
     * Set compiler flags for each backend.
     * 
     * @param backend
     *     {@link TornadoVMBackendType}
     * @param compilerFlags
     *     {@link String}
     * @since 1.0.7
     * @return {@link TornadoExecutionPlan}
     */
    public TornadoExecutionPlan withCompilerFlags(TornadoVMBackendType backend, String compilerFlags) {
        tornadoExecutor.withCompilerFlags(backend, compilerFlags);
        return new WithCompilerFlags(this);
    }

    /**
     * @since 1.0.4
     * 
     * @throws {@link
     *     TornadoExecutionPlanException}
     */
    @Override
    public void close() throws TornadoExecutionPlanException {
        tornadoExecutor.freeDeviceMemory();
    }

    public TornadoExecutor getTornadoExecutor() {
        return this.tornadoExecutor;
    }

    /**
     * It returns the current memory usage on the device in bytes.
     * 
     * @return long
     *     Number of bytes used.
     */
    public long getCurrentDeviceMemoryUsage() {
        return tornadoExecutor.getCurrentDeviceMemoryUsage();
    }

    public ExecutorFrame getExecutionFrame() {
        return executionFrame;
    }

    public void updateChildFromRoot(TornadoExecutionPlan childNode) {
        assert childNode != null;
        TornadoExecutionPlan rootNode = childNode;
        TornadoExecutionPlan iterator = childNode;

        // Traverse the list until we find the root node
        while (iterator != null) {
            rootNode = iterator;
            iterator = iterator.parentLink;
        }

        // Set the child of the root node to the new node
        rootNode.childLink = childNode;
    }
}

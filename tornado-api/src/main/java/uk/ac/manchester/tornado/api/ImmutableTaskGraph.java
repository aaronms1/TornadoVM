/*
 * This file is part of Tornado: A heterogeneous programming framework:
 * https://github.com/beehive-lab/tornadovm
 *
 * Copyright (c) 2022, APT Group, Department of Computer Science,
 * The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GNU Classpath is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * GNU Classpath is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GNU Classpath; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 *
 */
package uk.ac.manchester.tornado.api;

import uk.ac.manchester.tornado.api.common.TornadoDevice;
import uk.ac.manchester.tornado.api.exceptions.TornadoRuntimeException;
import uk.ac.manchester.tornado.api.profiler.ProfileInterface;

public class ImmutableTaskGraph implements ProfileInterface {

    /**
     * The idea is a task-graph is encapsulated in this class and all actions over a
     * task graph are coded from this class. For instance, execution.
     *
     * <p>
     * This class should not allow a task-graph to add/remove tasks or data from the
     * graph itself. To do so, we should transform a graph from immutable to mutable
     * state.
     * </p>
     */
    private final TaskGraph taskGraph;

    ImmutableTaskGraph(TaskGraph taskGraph) {
        this.taskGraph = taskGraph;
    }

    public void execute() {
        this.taskGraph.execute();
    }

    public void execute(GridScheduler gridScheduler) {
        taskGraph.execute(gridScheduler);
    }

    public void executeWithDynamicReconfiguration(Policy policy) {
        switch (policy) {
            case SYNC_PERFORMANCE:
                taskGraph.executeWithProfilerSequential(DynamicReconfigurationPolicy.PERFORMANCE);
                break;
            case SYNC_END_2_END:
                taskGraph.executeWithProfilerSequential(DynamicReconfigurationPolicy.END_2_END);
                break;
            case SYNC_LATENCY:
                taskGraph.executeWithProfilerSequential(DynamicReconfigurationPolicy.LATENCY);
                break;
            case ASYNC_PERFORMANCE:
                taskGraph.executeWithProfiler(DynamicReconfigurationPolicy.PERFORMANCE);
                break;
            case ASYNC_END_2_END:
                taskGraph.executeWithProfiler(DynamicReconfigurationPolicy.END_2_END);
                break;
            case ASYNC_LATENCY:
                taskGraph.executeWithProfiler(DynamicReconfigurationPolicy.LATENCY);
                break;
            default:
                throw new TornadoRuntimeException("[ERROR] Dynamic Reconfiguration Policy [" + policy.name() + "] not implemented yet");
        }
    }

    public void warmup() {
        taskGraph.warmup();
    }

    public void setDevice(TornadoDevice device) {
        taskGraph.setDevice(device);
    }

    public void lockObjectsInMemory(Object... objects) {
        taskGraph.lockObjectsInMemory(objects);
    }

    public void unlockObjectFromMemory(Object[] objects) {
        taskGraph.unlockObjectFromMemory(objects);
    }

    public void syncObject(Object... objects) {
        taskGraph.syncObjects(objects);
    }

    public void syncField(Object object) {
        taskGraph.syncField(object);
    }

    public void replaceParameter(Object oldParameter, Object newParameter) {
        taskGraph.replaceParameter(oldParameter, newParameter);
    }

    @Override
    public long getTotalTime() {
        return taskGraph.getTotalTime();
    }

    @Override
    public long getCompileTime() {
        return taskGraph.getCompileTime();
    }

    @Override
    public long getTornadoCompilerTime() {
        return taskGraph.getTornadoCompilerTime();
    }

    @Override
    public long getDriverInstallTime() {
        return taskGraph.getDriverInstallTime();
    }

    @Override
    public long getDataTransfersTime() {
        return taskGraph.getDataTransfersTime();
    }

    @Override
    public long getWriteTime() {
        return taskGraph.getWriteTime();
    }

    @Override
    public long getReadTime() {
        return taskGraph.getReadTime();
    }

    @Override
    public long getDataTransferDispatchTime() {
        return taskGraph.getDataTransferDispatchTime();
    }

    @Override
    public long getKernelDispatchTime() {
        return taskGraph.getKernelDispatchTime();
    }

    @Override
    public long getDeviceWriteTime() {
        return taskGraph.getDeviceWriteTime();
    }

    @Override
    public long getDeviceKernelTime() {
        return taskGraph.getDeviceKernelTime();
    }

    @Override
    public long getDeviceReadTime() {
        return taskGraph.getDeviceReadTime();
    }

    @Override
    public String getProfileLog() {
        return taskGraph.getProfileLog();
    }

    public boolean isFinished() {
        return taskGraph.isFinished();
    }

    public void dumpProfiles() {
        taskGraph.dumpProfiles();
    }

    public void resetDevices() {
        taskGraph.getDevice().reset();
    }

    public void clearProfiles() {
        taskGraph.clearProfiles();
    }

    public void useDefaultScheduler(boolean useDefaultScheduler) {
        taskGraph.useDefaultThreadScheduler(useDefaultScheduler);
    }

    public void withBatch(String batchSize) {
        taskGraph.batch(batchSize);
    }
}

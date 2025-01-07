/*
 * This file is part of Tornado: A heterogeneous programming framework:
 * https://github.com/beehive-lab/tornadovm
 *
 * Copyright (c) 2021, 2024, APT Group, Department of Computer Science,
 * School of Engineering, The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package uk.ac.manchester.tornado.drivers.spirv;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import uk.ac.manchester.tornado.api.exceptions.TornadoRuntimeException;
import uk.ac.manchester.tornado.runtime.common.TornadoOptions;

/**
 * Class for Calling JNI methods that can dispatch SPIR-V code.
 * code.
 * <p>
 * There are currently two ways:
 * <p>
 * - Via the OpenCL Runtime (OpenCL >= 2.1)
 * <p>
 * - Via the Level-Zero API.
 * <p>
 */
public class SPIRVRuntimeImpl {

    private final String ERROR_PLATFORM_NOT_IMPLEMENTED = "SPIR-V Runtime Implementation not supported: " + TornadoOptions.SPIRV_DEFAULT_RUNTIME + " \nUse \"opencl\" or \"levelzero\"";

    private List<SPIRVPlatform> platforms;
    private static SPIRVRuntimeImpl instance;

    public static SPIRVRuntimeImpl getInstance() {
        if (instance == null) {
            instance = new SPIRVRuntimeImpl();
        }
        return instance;
    }

    private SPIRVRuntimeImpl() {
        init();
    }

    private SPIRVDispatcher instantiateDispatcher(String runtimeName) {
        SPIRVDispatcher dispatcher;
        System.out.println("Creating Dispatcher for " + runtimeName);
        if (runtimeName.equalsIgnoreCase("opencl")) {
            dispatcher = new SPIRVOpenCLDriver();
        } else if (runtimeName.equalsIgnoreCase("levelzero")) {
            dispatcher = new SPIRVLevelZeroDriver();
        } else {
            throw new TornadoRuntimeException(ERROR_PLATFORM_NOT_IMPLEMENTED);
        }
        return dispatcher;
    }

    private synchronized void init() {
        if (platforms == null) {
            List<SPIRVDispatcher> dispatchers = new ArrayList<>();
            dispatchers.add(instantiateDispatcher(TornadoOptions.SPIRV_DEFAULT_RUNTIME));

            String[] listOfRuntimes = TornadoOptions.SPIRV_INSTALLED_RUNTIMES.split(",");
            if (listOfRuntimes.length > 1) {
                // We need to install the second runtime
                for (String runtime : listOfRuntimes) {
                    if (!runtime.equals(TornadoOptions.SPIRV_DEFAULT_RUNTIME)) {
                        dispatchers.add( instantiateDispatcher(runtime));
                    }
                }
            }

            platforms = new ArrayList<>();
            for (SPIRVDispatcher dispatcher : dispatchers) {
                IntStream.range(0, dispatcher.getNumPlatforms()).forEach(platformIndex -> platforms.add(dispatcher.getPlatform(platformIndex)));
            }
        }
    }

    public int getNumPlatforms() {
        return platforms.size();
    }

    public SPIRVPlatform getPlatform(int platformIndex) {
        return platforms.get(platformIndex);
    }
}

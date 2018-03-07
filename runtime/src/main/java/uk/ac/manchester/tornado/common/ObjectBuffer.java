/*
 * This file is part of Tornado: A heterogeneous programming framework: 
 * https://github.com/beehive-lab/tornado
 *
 * Copyright (c) 2013-2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Authors: James Clarkson
 *
 */
package uk.ac.manchester.tornado.common;

import uk.ac.manchester.tornado.common.exceptions.TornadoOutOfMemoryException;

public interface ObjectBuffer {

    public long toBuffer();

    public long getBufferOffset();

    public long toAbsoluteAddress();

    public long toRelativeAddress();

    public void read(Object ref);

    public void read(Object ref, int[] events, boolean useDeps);

    public void write(Object ref);

    public int enqueueRead(Object ref, int[] events, boolean useDeps);

    public int enqueueWrite(Object ref, int[] events, boolean useDeps);

    public void allocate(Object ref) throws TornadoOutOfMemoryException;

    public int getAlignment();

    public boolean isValid();

    public void invalidate();

    public void printHeapTrace();

    public long size();

}

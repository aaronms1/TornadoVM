/*
 * This file is part of Tornado: A heterogeneous programming framework: 
 * https://github.com/beehive-lab/tornadovm
 *
 * Copyright (c) 2013-2019, APT Group, School of Computer Science,
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
package uk.ac.manchester.tornado.api.utils;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;

public class TornadoUtilities {

    public static final int ONE_GIGABYTE = 1 * 1024 * 1024 * 1024;
    public static final int ONE_MEGABYTE = 1 * 1024 * 1024;
    public static final int ONE_KILOBYTE = 1 * 1024;

    public static long parseSize(String size) {
        if (size.endsWith("B")) {
            int index = size.indexOf("B");
            final String prefixes = "KMGTPE";
            if (prefixes.contains(size.substring(index - 1, index))) {
                final int prefix = prefixes.indexOf(size.charAt(index - 1));
                final long base = 1024;
                final long unit = (long) Math.pow(base, prefix + 1);
                return Long.parseLong(size.substring(0, index - 1)) * unit;

            } else {
                return Long.parseLong(size.substring(0, index - 1));
            }
        } else {
            return Long.parseLong(size);
        }
    }

    /**
     * Conversion from byte sizes into human readable format<br>
     * Based on code from http://stackoverflow.com/questions/3758606/how-to-convert
     * -byte-size-into-human-readable-format-in-java
     * <p>
     *
     * @param bytes
     *            number of bytes
     * @param si
     *            units (true if 1000, false if 1024).
     *
     * @return humanReadableByteCount
     */
    public static String humanReadableByteCount(long bytes, boolean si) {
        final int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        final int exp = (int) (Math.log(bytes) / Math.log(unit));
        final String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");

        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static String humanReadableFreq(int freq) {
        final int unit = 1000;
        if (freq < unit) {
            return freq + " MHz";
        }
        final int exp = (int) (Math.log(freq) / Math.log(unit));
        final char pre = "GT".charAt(exp - 1);
        return String.format("%.1f %sHz", freq / Math.pow(unit, exp), pre);
    }

    public static final String formatBytes(final long bytes) {
        String out = "";

        if (bytes >= ONE_GIGABYTE) {
            out = String.format("%.2f GB", ((double) bytes / (double) ONE_GIGABYTE));
        } else if (bytes >= ONE_MEGABYTE) {
            out = String.format("%.2f MB", ((double) bytes / (double) ONE_MEGABYTE));
        } else if (bytes >= ONE_KILOBYTE) {
            out = String.format("%.2f KB", ((double) bytes / (double) ONE_KILOBYTE));
        } else {
            out = String.format("%d B", bytes);
        }
        return out;
    }

    public static final String formatBytesPerSecond(final double bytes) {
        String out = "";

        if (bytes >= ONE_GIGABYTE) {
            out = String.format("%.2f GB/s", (bytes / ONE_GIGABYTE));
        } else if (bytes >= ONE_MEGABYTE) {
            out = String.format("%.2f MB/s", (bytes / ONE_MEGABYTE));
        } else if (bytes >= ONE_KILOBYTE) {
            out = String.format("%.2f KB/s", (bytes / ONE_KILOBYTE));
        } else {
            out = String.format("%f B/s", bytes);
        }
        return out;
    }

    /**
     * Returns true if object is a boxed type
     *
     * @param obj
     *            Object
     *
     * @return boolean
     */
    public static final boolean isBoxedPrimitive(final Object obj) {
        boolean isBox = false;

        if (obj instanceof Boolean) {
            isBox = true;
        } else if (obj instanceof Byte) {
            isBox = true;
        } else if (obj instanceof Character) {
            isBox = true;
        } else if (obj instanceof Short) {
            isBox = true;
        } else if (obj instanceof Integer) {
            isBox = true;
        } else if (obj instanceof Long) {
            isBox = true;
        } else if (obj instanceof Float) {
            isBox = true;
        } else if (obj instanceof Double) {
            isBox = true;
        }

        return isBox;
    }

    /**
     * Returns true if object is a boxed type
     *
     * @param clazz
     *            Class
     *
     * @return boolean
     */
    public static final boolean isBoxedPrimitiveClass(final Class<?> clazz) {
        boolean isBox = false;

        if (clazz == Boolean.class) {
            isBox = true;
        } else if (clazz == Byte.class) {
            isBox = true;
        } else if (clazz == Character.class) {
            isBox = true;
        } else if (clazz == Short.class) {
            isBox = true;
        } else if (clazz == Integer.class) {
            isBox = true;
        } else if (clazz == Long.class) {
            isBox = true;
        } else if (clazz == Float.class) {
            isBox = true;
        } else if (clazz == Double.class) {
            isBox = true;
        }

        return isBox;
    }

    /**
     * Returns true if object is a boxed type
     *
     * @param clazz
     *            Class
     *
     * @return {@link Class<?>}
     */
    public static final Class<?> toUnboxedPrimitiveClass(final Class<?> clazz) {
        Class<?> result = null;

        if (clazz == Boolean.class) {
            result = boolean.class;
        } else if (clazz == Byte.class) {
            result = byte.class;
        } else if (clazz == Character.class) {
            result = char.class;
        } else if (clazz == Short.class) {
            result = short.class;
        } else if (clazz == Integer.class) {
            result = int.class;
        } else if (clazz == Long.class) {
            result = long.class;
        } else if (clazz == Float.class) {
            result = float.class;
        } else if (clazz == Double.class) {
            result = double.class;
        }

        return result != null ? result : clazz;
    }

    /**
     * determines whether a given array is composed of primitives or objects
     *
     * @param type
     *            type to check
     *
     * @return true if the array is composed of a primitive type
     */
    public static final boolean isPrimitiveArray(final Class<?> type) {
        Class<?> componentType = type.getComponentType();
        while (componentType.isArray()) {
            componentType = componentType.getComponentType();
        }
        return componentType.isPrimitive() || isBoxedPrimitive(componentType);
    }

    public static void printBuffer(final ByteBuffer buffer) {

        System.out.printf("buffer : position=%d, remaining=%d, capacity=%d, limit=%d\n", buffer.position(), buffer.remaining(), buffer.capacity(), buffer.limit());
        System.out.printf("array  : length=%d, offset=%d\n", buffer.array().length, buffer.arrayOffset());
        System.out.printf("%-8s: ", "Index");
        for (int i = 0; i < 8; i++) {
            System.out.printf("%-8d ", i * 4);
        }
        System.out.println();
        System.out.println();

        System.out.printf("");
        for (int i = 0; i < buffer.remaining(); i += 32) {
            System.out.printf("%-8d: ", i);
            for (int j = 0; j < 32; j += 4) {
                for (int k = 0; k < 4; k++) {
                    if ((i + j + k) < buffer.remaining()) {
                        final byte b = buffer.get(i + j + k);
                        System.out.printf("%02x", b);
                    } else {
                        System.out.printf("%2s", "..");
                    }
                }
                System.out.printf(" ");
            }
            System.out.println();
        }
        System.out.println();
    }

    public static void printBuffer(final ByteBuffer buffer, final int start, final int len) {

        System.out.printf("Index : ");
        for (int i = 0; i < 5; i++) {
            System.out.printf(" %8d", i);
        }
        System.out.println();

        System.out.printf("Buffer: ");
        for (int i = 0; i < len; i++) {
            System.out.printf(" %2X", buffer.get(start + i));
        }
        System.out.println();
    }

    public static double elapsedTimeInSeconds(long start, long end) {
        final long duration = end - start;
        return duration * 1e-9;
    }

    public static String formatArray(Object object) {
        String result;
        if (object.getClass().isArray()) {
            int len = Array.getLength(object);
            result = String.format("%s[%d]", object.getClass().getComponentType().getName(), len);
        } else {
            result = object.toString();
        }
        return result;
    }

    public static boolean isPrimitive(Class<?> type) {
        if (type.isPrimitive()) {
            return true;
        } else {
            return isBoxedPrimitive(type);
        }
    }

    private TornadoUtilities() {

    }
}

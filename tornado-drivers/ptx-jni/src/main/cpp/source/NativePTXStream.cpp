/*
* MIT License
 *
 * Copyright (c) 2025, APT Group, Department of Computer Science,
 * The University of Manchester.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
#include <jni.h>
#include <cuda.h>

#include <iostream>
#include "NativePTXStream.h"
#include "ptx_log.h"
/*
* Class:     uk_ac_manchester_tornado_drivers_ptx_nstream_NativePTXStream
 * Method:    copyDevicePointer
 * Signature: (JJJI)J
 */
 JNIEXPORT jlong JNICALL Java_uk_ac_manchester_tornado_drivers_ptx_nstream_NativePTXStream_copyDevicePointer
  (JNIEnv * env, jclass klass, jlong destDeviceBufferPtr, jlong srcDeviceBufferPtr, jlong offset, jint sizeOfType) {
   std::cout << "[JNI] NativePTXStream_copyDevicePointer: destDeviceBufferPtr = " << destDeviceBufferPtr << std::endl;
   return srcDeviceBufferPtr + (offset * sizeOfType);
 }
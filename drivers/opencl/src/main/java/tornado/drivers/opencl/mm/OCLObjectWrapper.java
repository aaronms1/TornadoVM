package tornado.drivers.opencl.mm;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import tornado.api.Event;
import tornado.api.Payload;
import tornado.api.Vector;
import tornado.common.ObjectBuffer;
import tornado.common.RuntimeUtilities;
import tornado.common.Tornado;
import tornado.common.exceptions.TornadoOutOfMemoryException;
import tornado.drivers.opencl.OCLDeviceContext;
import tornado.runtime.EmptyEvent;
import tornado.runtime.TornadoRuntime;
import tornado.runtime.api.TaskUtils;

import com.oracle.graal.api.meta.PrimitiveConstant;
import com.oracle.graal.hotspot.HotSpotGraalRuntimeProvider;
import com.oracle.graal.hotspot.meta.HotSpotResolvedJavaField;
import com.oracle.graal.hotspot.meta.HotSpotResolvedJavaType;

public class OCLObjectWrapper implements ObjectBuffer<Object> {

    private final boolean vectorObject;
    private int vectorStorageIndex;
    private long bufferOffset;
    private long bytes;
    private ByteBuffer buffer;
    private HotSpotResolvedJavaType resolvedType;
    private HotSpotResolvedJavaField[] fields;
    private FieldBuffer<?>[] wrappedFields;

    private final Class<?> type;

    private int hubOffset;
    private int fieldsOffset;

    private final OCLDeviceContext deviceContext;
    private boolean valid;
    private boolean isFinal;

    private final HotSpotGraalRuntimeProvider runtime;

    public OCLObjectWrapper(final OCLDeviceContext device, Object object) {
        this.type = object.getClass();
        this.deviceContext = device;

        valid = false;
        isFinal = true;

        runtime = TornadoRuntime.getVMRuntimeProvider();
        hubOffset = runtime.getConfig().hubOffset;
        fieldsOffset = runtime.getConfig().instanceKlassFieldsOffset;

        resolvedType = (HotSpotResolvedJavaType) TornadoRuntime
                .getVMProviders().getMetaAccess()
                .lookupJavaType(object.getClass());

        if (resolvedType.getAnnotation(Vector.class) != null)
            vectorObject = true;
        else
            vectorObject = false;

        vectorStorageIndex = -1;

        fields = (HotSpotResolvedJavaField[]) resolvedType
                .getInstanceFields(true);
        sortFieldsByOffset();

        wrappedFields = new FieldBuffer<?>[fields.length];

        int index = 0;
        // calculate object size
        bytes = (fields.length > 0) ? fields[0].offset() : 32;
        for (HotSpotResolvedJavaField field : fields) {
            final Field reflectedField = getField(type, field.getName());
            final Class<?> type = reflectedField.getType();
            final boolean isFinal = Modifier.isFinal(reflectedField
                    .getModifiers());

            if (vectorObject) {
                if (field.getAnnotation(Payload.class) != null)
                    vectorStorageIndex = index;
            }

            if (Tornado.DEBUG)
                Tornado.trace("field: name=%s, kind=%s, offset=%d",
                        field.getName(), type.getName(), field.offset());
            bytes = field.offset();
            bytes += (field.getKind().isObject()) ? 8 : field.getKind()
                    .getByteCount();

            ObjectBuffer<?> wrappedField = null;
            if (type.isArray()) {
                if (type == int[].class) {
                    wrappedField = new OCLIntArrayWrapper(device, isFinal);
                } else if (type == float[].class) {
                    wrappedField = new OCLFloatArrayWrapper(device, isFinal);

                } else if (type == double[].class) {
                    wrappedField = new OCLDoubleArrayWrapper(device, isFinal);

                } else if (type == long[].class) {
                    wrappedField = new OCLLongArrayWrapper(device, isFinal);

                } else if (type == short[].class) {
                    wrappedField = new OCLShortArrayWrapper(device, isFinal);
                } else if (type == byte[].class) {
                    wrappedField = new OCLByteArrayWrapper(device, isFinal);
                } else {
                    Tornado.warn("cannot wrap field: array type=%s",
                            type.getName());

                }
            } else if (field.getKind().isObject()) {
                try {
                    wrappedField = new OCLObjectWrapper(device,
                            reflectedField.get(object));
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                this.isFinal &= isFinal;
            }

            if (wrappedField != null) {
                wrappedFields[index] = new FieldBuffer(reflectedField,
                        wrappedField);
            }
            index++;
        }

        if (Tornado.DEBUG)
            Tornado.trace("object: type=%s, size=%s", resolvedType.getName(),
                    RuntimeUtilities.humanReadableByteCount(bytes, true));

    }

    @Override
    public void allocate(Object ref) throws TornadoOutOfMemoryException {
        if (Tornado.DEBUG)
            Tornado.debug("object: object=0x%x, class=%s, size=%s",
                    ref.hashCode(), ref.getClass().getName(),
                    RuntimeUtilities.humanReadableByteCount(bytes, true));
        buffer = ByteBuffer.allocate((int) bytes);
        buffer.order(deviceContext.getByteOrder());
        bufferOffset = deviceContext.getMemoryManager().tryAllocate(bytes,
                getAlignment());

        if (Tornado.DEBUG)
            Tornado.debug("object: object=0x%x @ 0x%x (0x%x)", ref.hashCode(),
                    toAbsoluteAddress(), toRelativeAddress());
        for (FieldBuffer<?> buffer : wrappedFields)
            if (buffer != null)
                buffer.allocate(ref);
    }

    private Field getField(Class<?> type, String name) {
        Field result = null;
        try {
            result = type.getDeclaredField(name);
            result.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException e) {
            if (type.getSuperclass() != null)
                result = getField(type.getSuperclass(), name);
            else {
                Tornado.fatal("unable to get field");
                e.printStackTrace();
            }
        }
        return result;
    }

    private void writeFieldToBuffer(int index, Field field, Object obj) {
        Class<?> fieldType = field.getType();
        if (fieldType.isPrimitive()) {
            try {
                PrimitiveSerialiser.put(buffer, field.get(obj));
            } catch (IllegalArgumentException | IllegalAccessException e) {
                Tornado.fatal("unable to write primitive to buffer");
            }
        } else {
            if (wrappedFields[index] != null) {
                buffer.putLong(wrappedFields[index].toAbsoluteAddress());
            } else {
                Tornado.fatal("unimplemented - field type %s",
                        fieldType.getName());
            }
        }
    }

    private void readFieldFromBuffer(int index, Field field, Object obj) {
        Class<?> fieldType = field.getType();
        if (fieldType.isPrimitive()) {
            try {
                if (fieldType == int.class) {
                    field.setInt(obj, buffer.getInt());
                } else if (fieldType == long.class) {
                    field.setLong(obj, buffer.getLong());
                } else if (fieldType == short.class) {
                    field.setShort(obj, buffer.getShort());
                } else if (fieldType == byte.class) {
                    field.set(obj, buffer.get());
                } else if (fieldType == float.class) {
                    field.setFloat(obj, buffer.getFloat());
                } else if (fieldType == double.class) {
                    field.setDouble(obj, buffer.getDouble());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } else {
            if (wrappedFields[index] != null) {
                buffer.getLong();
            } else {
                Tornado.fatal("unimplemented - field type %s",
                        fieldType.getName());
            }
        }
    }

    private void sortFieldsByOffset() {
        for (int i = 0; i < fields.length; i++) {
            for (int j = 0; j < fields.length; j++) {
                if (fields[i].offset() < fields[j].offset()) {
                    final HotSpotResolvedJavaField tmp = fields[j];
                    fields[j] = fields[i];
                    fields[i] = tmp;
                }
            }
        }

    }

    private void serialise(Object object) {
        buffer.rewind();

        buffer.position(hubOffset);
        if (Tornado.DEBUG)
            Tornado.trace("object: hub offset=%d, value=0x%x", hubOffset,
                    ((PrimitiveConstant) resolvedType.getObjectHub()).asLong());
        buffer.putLong(((PrimitiveConstant) resolvedType.getObjectHub())
                .asLong());

        if (fields.length > 0) {
            buffer.position(fields[0].offset());

            for (int i = 0; i < fields.length; i++) {
                HotSpotResolvedJavaField field = fields[i];
                Field f = getField(type, field.getName());
                if (Tornado.DEBUG)
                    Tornado.trace("writing field: name=%s, offset=%d",
                            field.getName(), field.offset());

                buffer.position(field.offset());
                writeFieldToBuffer(i, f, object);
            }

        }
        // dump();
    }

    private void deserialise(Object object) {
        buffer.rewind();

        if (fields.length > 0) {
            buffer.position(fields[0].offset());

            for (int i = 0; i < fields.length; i++) {
                HotSpotResolvedJavaField field = fields[i];
                Field f = getField(type, field.getName());
                f.setAccessible(true);
                if (Tornado.DEBUG)
                    Tornado.trace("reading field: name=%s, offset=%d",
                            field.getName(), field.offset());
                readFieldFromBuffer(i, f, object);
            }
        }
    }

    @Override
    public long toBuffer() {
        return deviceContext.getMemoryManager().toBuffer();
    }

    @Override
    public long getBufferOffset() {
        return bufferOffset;
    }

    @Override
    public void write(Object object) {
        if (vectorObject) {
            final FieldBuffer<?> fieldBuffer = wrappedFields[vectorStorageIndex];
            fieldBuffer.write(object);
        } else {
            if (!valid) {
                serialise(object);
                deviceContext.writeBuffer(toBuffer(), bufferOffset, bytes,
                        buffer.array(), null);
            }
            for (int i = 0; i < fields.length; i++) {
                if (wrappedFields[i] != null)
                    wrappedFields[i].write(object);
            }
        }
        valid = true;
    }

    @Override
    public void read(Object object) {
        if (vectorObject) {
            final FieldBuffer<?> fieldBuffer = wrappedFields[vectorStorageIndex];
            fieldBuffer.read(object);
        } else {
            buffer.position(buffer.capacity());
            deviceContext.readBuffer(toBuffer(), bufferOffset, bytes,
                    buffer.array(), null);
            for (int i = 0; i < fields.length; i++) {
                if (wrappedFields[i] != null)
                    wrappedFields[i].read(object);
            }
            deserialise(object);
        }
    }

    @Override
    public long toAbsoluteAddress() {
        return (vectorObject) ? getVectorAddress(false) : deviceContext
                .getMemoryManager().toAbsoluteDeviceAddress(bufferOffset);
    }

    private long getVectorAddress(boolean relative) {
        final HotSpotResolvedJavaField resolvedField = fields[vectorStorageIndex];
        final FieldBuffer<?> fieldBuffer = wrappedFields[vectorStorageIndex];
        final long address = (relative) ? fieldBuffer.toRelativeAddress()
                : fieldBuffer.toAbsoluteAddress();

        final long arrayBaseOffset = runtime.getArrayBaseOffset(resolvedField
                .getKind());
        return address + arrayBaseOffset;
    }

    @Override
    public long toRelativeAddress() {
        return (vectorObject) ? getVectorAddress(true) : bufferOffset;
    }

    public void clear() {
        buffer.rewind();
        while (buffer.hasRemaining())
            buffer.put((byte) 0);
        buffer.rewind();
    }

    public void dump() {
        dump(8);
    }

    protected void dump(int width) {
        System.out
                .printf("Buffer  : capacity = %s, in use = %s, device = %s \n",
                        RuntimeUtilities.humanReadableByteCount(bytes, true),
                        RuntimeUtilities.humanReadableByteCount(
                                buffer.position(), true), deviceContext
                                .getDevice().getName());
        for (int i = 0; i < buffer.position(); i += width) {
            System.out.printf("[0x%04x]: ", i);
            for (int j = 0; j < Math.min(buffer.capacity() - i, width); j++) {
                if (j % 2 == 0)
                    System.out.printf(" ");
                if (j < buffer.position() - i)
                    System.out.printf("%02x", buffer.get(i + j));
                else
                    System.out.printf("..");
            }
            System.out.println();
        }
    }

    @Override
    public Event enqueueRead(Object ref) {
        final List<Event> waitEvents = new ArrayList<Event>(0);
        return enqueueReadAfterAll(ref, waitEvents);
    }

    @Override
    public Event enqueueReadAfter(Object ref, Event event) {
        final List<Event> waitEvents = new ArrayList<Event>(1);
        waitEvents.add(event);
        return enqueueReadAfterAll(ref, waitEvents);
    }

    @Override
    public Event enqueueReadAfterAll(Object ref, List<Event> events) {
        // System.out.println("enqueue read after all...");
        TaskUtils.waitForEvents(events);

        if (vectorObject) {
            final FieldBuffer<?> fieldBuffer = wrappedFields[vectorStorageIndex];
            return fieldBuffer.enqueueReadAfterAll(ref, events);
        } else {
            final List<Event> waitEvents = new ArrayList<Event>();
            for (FieldBuffer<?> fb : wrappedFields) {
                if (fb != null) {
                    waitEvents.add(fb.enqueueReadAfterAll(ref, events));
                }
            }

            if (!isFinal) {
                waitEvents.add(deviceContext.enqueueReadBuffer(toBuffer(),
                        bufferOffset, bytes, buffer.array(), events));
                // TODO this needs to run asynchronously
                deserialise(ref);
            }

            return (waitEvents.size() > 1) ? deviceContext
                    .enqueueMarker(waitEvents) : waitEvents.get(0);
        }
    }

    @Override
    public Event enqueueWrite(Object ref) {
        final List<Event> waitEvents = new ArrayList<Event>(0);
        return enqueueWriteAfterAll(ref, waitEvents);
    }

    @Override
    public Event enqueueWriteAfter(Object ref, Event event) {
        final List<Event> waitEvents = new ArrayList<Event>(1);
        waitEvents.add(event);
        return enqueueWriteAfterAll(ref, waitEvents);
    }

    @Override
    public Event enqueueWriteAfterAll(Object ref, List<Event> events) {
        // System.out.println("enqueue write after all...");
        TaskUtils.waitForEvents(events);
        final List<Event> waitEvents = new ArrayList<Event>();

        if (vectorObject) {
            final FieldBuffer<?> fieldBuffer = wrappedFields[vectorStorageIndex];
            if (!valid) {
                valid = true;
                return fieldBuffer.enqueueWriteAfterAll(ref, events);
            } else {
                return new EmptyEvent();
            }

        } else {
            // TODO this needs to run asynchronously
            if (!valid || (valid && !isFinal)) {
                serialise(ref);

                waitEvents.add(deviceContext.enqueueWriteBuffer(toBuffer(),
                        bufferOffset, bytes, buffer.array(), events));
                valid = true;
            }

            for (FieldBuffer<?> fb : wrappedFields) {
                if (fb != null) {
                    // System.out.printf("field: write %s\n",fb.getFieldName());
                    waitEvents.add(fb.enqueueWriteAfterAll(ref, events));
                }
            }

            return (waitEvents.size() > 1) ? deviceContext
                    .enqueueMarker(waitEvents) : waitEvents.get(0);
        }
    }

    @Override
    public Event enqueueZeroMemory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getAlignment() {
        return 16;
    }

    public FieldBuffer<?> getField(String name) {
        int index = 0;
        for (HotSpotResolvedJavaField field : fields) {
            if (field.getName().equalsIgnoreCase(name)) {
                return wrappedFields[index];
            }
            break;
        }
        return null;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public void invalidate() {
        valid = false;

    }

    public String toString() {
        return String.format("object wrapper: type=%s, fields=%d, valid=%s\n",
                resolvedType.getName(), wrappedFields.length, valid);
    }

    @Override
    public void printHeapTrace() {
        System.out.printf("0x%x:\ttype=%s, num fields=%d (%d)\n",
                toAbsoluteAddress(), type.getName(), fields.length,
                wrappedFields.length);
        for (FieldBuffer<?> fb : wrappedFields)
            if (fb != null)
                System.out.printf("\t0x%x\tname=%s\n", fb.toAbsoluteAddress(),
                        fb.getFieldName());
    }

    @Override
    public long size() {
        return bytes;
    }

}
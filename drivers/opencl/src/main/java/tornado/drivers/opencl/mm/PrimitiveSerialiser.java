package tornado.drivers.opencl.mm;

import java.nio.ByteBuffer;
import tornado.common.Tornado;

public class PrimitiveSerialiser {

	
	private static final void align(ByteBuffer buffer, int align){
		while(buffer.position() % align != 0)
			buffer.put((byte) 0);
	}
	
	public static final void put(ByteBuffer buffer, Object value){
		put(buffer,value,0);
	}
	
	public static final void put(ByteBuffer buffer, Object value, int alignment){
		if(value instanceof Integer){
			buffer.putInt((int) value);
		} else if(value instanceof Long){
			buffer.putLong((long) value);
		} else if (value instanceof Short){
			buffer.putShort((short) value);
		} else if (value instanceof Float){
			buffer.putFloat((float) value);
		} else if (value instanceof Double){
			buffer.putDouble((double) value);
		} else {
			Tornado.warn("unable to serialise: %s (%s)",value,value.getClass().getName());
		}
		
		if(alignment != 0)
			align(buffer,alignment);
	}
	
}
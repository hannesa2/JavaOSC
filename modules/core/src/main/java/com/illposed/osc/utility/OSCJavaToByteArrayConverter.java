/*
 * Copyright (C) 2003-2006, C. Ramakrishnan / Illposed Software.
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc.utility;

import com.illposed.osc.OSCImpulse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Collection;

/**
 * OSCJavaToByteArrayConverter is a helper class that translates
 * from Java types to their byte stream representations according to
 * the OSC spec.
 *
 * The implementation is based on
 * <a href=" http://www.emergent.de/">Markus Gaelli</a> and
 * Iannis Zannos' OSC implementation in Squeak.
 *
 * This version includes bug fixes and improvements from
 * Martin Kaltenbrunner and Alex Potsides.
 *
 * @author Chandrasekhar Ramakrishnan
 * @author Martin Kaltenbrunner
 * @author Alex Potsides
 */
public class OSCJavaToByteArrayConverter {

	private static final byte[] SINGLE_BYTE_PADDING = new byte[] { 0, 0, 0 };

	private final ByteArrayOutputStream stream;
	/** Used to encode message addresses and string parameters. */
	private Charset charset;
	private final byte[] intBytes;
	private final byte[] longintBytes;

	public OSCJavaToByteArrayConverter() {

		this.stream = new ByteArrayOutputStream();
		this.charset = Charset.defaultCharset();
		this.intBytes = new byte[4];
		this.longintBytes = new byte[8];
	}

	/**
	 * Returns the character set used to encode message addresses
	 * and string parameters.
	 */
	public Charset getCharset() {
		return charset;
	}

	/**
	 * Sets the character set used to encode message addresses
	 * and string parameters.
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	/**
	 * Line up the Big end of the bytes to a 4 byte boundary.
	 * @return byte[]
	 * @param bytes byte[]
	 */
	private byte[] alignBigEndToFourByteBoundry(byte[] bytes) {
		int mod = bytes.length % 4;
		// if the remainder == 0 then return the bytes otherwise pad the bytes
		// to lineup correctly
		if (mod == 0) {
			return bytes;
		}
		int pad = 4 - mod;
		byte[] newBytes = new byte[pad + bytes.length];
//		for (int i = 0; i < pad; i++)
//			newBytes[i] = 0;
//		for (int i = 0; i < bytes.length; i++)
//			newBytes[pad + i] = bytes[i];
		System.arraycopy(bytes, 0, newBytes, pad, bytes.length);
		return newBytes;
	}

	/**
	 * Pad the stream to have a size divisible by 4.
	 */
	public void appendNullCharToAlignStream() {
		int mod = stream.size() % 4;
		int pad = 4 - mod;
		for (int i = 0; i < pad; i++) {
			stream.write(0);
		}
	}

	/**
	 * Convert the contents of the output stream to a byte array.
	 * @return the byte array containing the byte stream
	 */
	public byte[] toByteArray() {
		return stream.toByteArray();
	}

	/**
	 * Write bytes into the byte stream.
	 * @param bytes  bytes to be written
	 */
	public void write(byte[] bytes) {
		writeUnderHandler(bytes);
	}

	/**
	 * Write an integer into the byte stream.
	 * @param i the integer to be written
	 */
	public void write(int i) {
		writeInteger32ToByteArray(i);
	}

	/**
	 * Write a float into the byte stream.
	 * @param f floating point number to be written
	 */
	public void write(Float f) {
		writeInteger32ToByteArray(Float.floatToIntBits(f));
	}

	/**
	 * Write a double into the byte stream (8 bytes).
	 * @param d double precision floating point number to be written
	 */
	public void write(Double d) {
		writeInteger64ToByteArray(Double.doubleToRawLongBits(d));
	}

	/**
	 * @param i the integer to be written
	 */
	public void write(Integer i) {
		writeInteger32ToByteArray(i);
	}

	/**
	 * @param i the integer to be written
	 */
	public void write(BigInteger i) {
		writeInteger64ToByteArray(i.longValue());
	}

	/**
	 * Write a string into the byte stream.
	 * @param aString the string to be written
	 */
	public void write(String aString) {
/*
		XXX to be revised ...
		int stringLength = aString.length();
			// this is a deprecated method -- should use get char and convert
			// the chars to bytes
//		aString.getBytes(0, stringLength, stringBytes, 0);
		aString.getChars(0, stringLength, stringChars, 0);
			// pad out to align on 4 byte boundry
		int mod = stringLength % 4;
		int pad = 4 - mod;
		for (int i = 0; i < pad; i++)
			stringChars[stringLength++] = 0;
		// convert the chars into bytes and write them out
		for (int i = 0; i < stringLength; i++) {
			stringBytes[i] = (byte) (stringChars[i] & 0x00FF);
		}
		stream.write(stringBytes, 0, stringLength);
*/
		byte[] stringBytes = aString.getBytes(charset);

		// pad out to align on 4 byte boundry
		int mod = aString.length() % 4;
		int pad = 4 - mod;

		byte[] newBytes = new byte[pad + stringBytes.length];
		System.arraycopy(stringBytes, 0, newBytes, 0, stringBytes.length);

		try {
			stream.write(newBytes);
		} catch (IOException e) {
			throw new RuntimeException("You're screwed:"
					+ " IOException writing to a ByteArrayOutputStream", e);
		}
	}

	/**
	 * Write a char into the byte stream, and ensure it is 4 byte aligned again.
	 * @param c the character to be written
	 */
	public void write(Character c) {
		stream.write((char) c);
		writeUnderHandler(SINGLE_BYTE_PADDING);
	}

	/**
	 * Write a char into the byte stream.
	 * CAUTION, this does not ensure 4 byte alignment (it actually breaks it)!
	 * @param c the character to be written
	 */
	public void write(char c) {
		stream.write(c);
	}

	/**
	 * Write an object into the byte stream.
	 * @param anObject one of Float, String, Integer, BigInteger, or array of
	 *   these.
	 */
	public void write(Object anObject) {
		// Can't do switch on class
		if (anObject instanceof Collection) {
			Collection<Object> theArray = (Collection<Object>) anObject;
			for (Object entry : theArray) {
				write(entry);
			}
		} else if (anObject instanceof Float) {
			write((Float) anObject);
		} else if (anObject instanceof Double) {
			write((Double) anObject);
		} else if (anObject instanceof String) {
			write((String) anObject);
		} else if (anObject instanceof Character) {
			write((Character) anObject);
		} else if (anObject instanceof Integer) {
			write((Integer) anObject);
		} else if (anObject instanceof BigInteger) {
			write((BigInteger) anObject);
		} else if (anObject == null) {
			throw new RuntimeException("Can not write null");
		} else {
			throw new RuntimeException("Unknown object type: " + anObject.getClass());
		}
	}

	/**
	 * Write the OSC specification type tag for the type a certain Java type
	 * converts to.
	 * @param c Class of a Java object in the arguments
	 */
	public void writeType(Class c) {

		// A big ol' else-if chain -- what's polymorphism mean, again?
		// I really wish I could extend the base classes!
		if (Integer.class.equals(c)) {
			stream.write('i');
		} else if (java.math.BigInteger.class.equals(c)) {
			stream.write('h');
		} else if (Float.class.equals(c)) {
			stream.write('f');
		} else if (Double.class.equals(c)) {
			stream.write('d');
		} else if (String.class.equals(c)) {
			stream.write('s');
		} else if (Character.class.equals(c)) {
			stream.write('c');
		} else if (OSCImpulse.class.equals(c)) {
			stream.write('I');
		}
	}

	/**
	 * Write the types for an array element in the arguments.
	 * @param array array of base Objects
	 */
	public void writeTypesArray(Collection<Object> array) {
		// A big ol' case statement in a for loop -- what's polymorphism mean,
		// again?
		// I really wish I could extend the base classes!

		for (Object element : array) {
			if (Boolean.TRUE.equals(element)) {
				// Create a way to deal with Boolean type objects
				stream.write('T');
			} else if (Boolean.FALSE.equals(element)) {
				stream.write('F');
			} else {
				// this is an object -- write the type for the class
				writeType(element.getClass());
			}
		}
	}

	/**
	 * Write types for the arguments.
	 * @param types  the arguments to an OSCMessage
	 */
	public void writeTypes(Collection<Object> types) {

		for (Object type : types) {
			if (null == type) {
				stream.write('N');
			} else if (type instanceof Collection) {
				// If the array at i is a type of array, write a '['.
				// This is used for nested arguments.
				stream.write('[');
				// fill the [] with the SuperCollider types corresponding to
				// the object (e.g., Object of type String needs -s).
				writeTypesArray((Collection<Object>) type);
				// close the array
				stream.write(']');
			} else if (Boolean.TRUE.equals(type)) {
				stream.write('T');
			} else if (Boolean.FALSE.equals(type)) {
				stream.write('F');
			} else {
				// go through the array and write the superCollider types as shown
				// in the above method.
				// The classes derived here are used as the arg to the above method.
				writeType(type.getClass());
			}
		}
		// align the stream with padded bytes
		appendNullCharToAlignStream();
	}

	/**
	 * Write bytes to the stream, catching IOExceptions and converting them to
	 * RuntimeExceptions.
	 * @param bytes byte[]
	 */
	private void writeUnderHandler(byte[] bytes) {

		try {
			stream.write(alignBigEndToFourByteBoundry(bytes));
		} catch (IOException e) {
			throw new RuntimeException("You're screwed:"
					+ " IOException writing to a ByteArrayOutputStream");
		}
	}

	/**
	 * Write a 32 bit integer to the byte array without allocating memory.
	 * @param value a 32 bit integer.
	 */
	private void writeInteger32ToByteArray(int value) {
		//byte[] intBytes = new byte[4];
		//I allocated the this buffer globally so the GC has less work

		intBytes[3] = (byte)value; value >>>= 8;
		intBytes[2] = (byte)value; value >>>= 8;
		intBytes[1] = (byte)value; value >>>= 8;
		intBytes[0] = (byte)value;

		try {
			stream.write(intBytes);
		} catch (IOException ex) {
			throw new RuntimeException("You're screwed:"
					+ " IOException writing to a ByteArrayOutputStream", ex);
		}
	}

	/**
	 * Write a 64 bit integer to the byte array without allocating memory.
	 * @param value a 64 bit integer.
	 */
	private void writeInteger64ToByteArray(long value) {
		longintBytes[7] = (byte)value; value >>>= 8;
		longintBytes[6] = (byte)value; value >>>= 8;
		longintBytes[5] = (byte)value; value >>>= 8;
		longintBytes[4] = (byte)value; value >>>= 8;
		longintBytes[3] = (byte)value; value >>>= 8;
		longintBytes[2] = (byte)value; value >>>= 8;
		longintBytes[1] = (byte)value; value >>>= 8;
		longintBytes[0] = (byte)value;

		try {
			stream.write(longintBytes);
		} catch (IOException ex) {
			throw new RuntimeException("You're screwed:"
					+ " IOException writing to a ByteArrayOutputStream", ex);
		}
	}
}

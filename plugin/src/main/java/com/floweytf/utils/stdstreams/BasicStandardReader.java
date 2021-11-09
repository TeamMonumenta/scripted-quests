package com.floweytf.utils.stdstreams;
/*
 * These are utilities for serialization between forge & bukkit
 * See original project here: https://github.com/FloweyTheFlower420/mappings-utils
 * Author: Flowey
 * License: GPL v3
 */

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class BasicStandardReader<T> implements IStandardByteReader {
	public interface ByteReader<T> {
		int read(T instance, byte[] buf) throws IOException;
	}

	private final T mInstance;
	private final ByteReader<T> mReader;

	public BasicStandardReader(T inst, ByteReader<T> rd) {
		mInstance = inst;
		mReader = rd;
	}

	public long readLong() throws IOException {
		byte[] bytes = new byte[8];
		mReader.read(mInstance, bytes);
		return ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).getLong();
	}

	public int readInt() throws IOException {
		byte[] bytes = new byte[4];
		mReader.read(mInstance, bytes);
		return ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
	}

	public short readShort() throws IOException {
		byte[] bytes = new byte[2];
		mReader.read(mInstance, bytes);
		return ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
	}

	public byte readByte() throws IOException {
		byte[] bytes = new byte[1];
		mReader.read(mInstance, bytes);
		return bytes[0];
	}

	public String readString() throws IOException {
		int size = readInt();
		byte[] bytes = new byte[size];
		mReader.read(mInstance, bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	public float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}

	public double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
	}
}

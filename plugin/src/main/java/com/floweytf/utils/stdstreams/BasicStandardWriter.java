package com.floweytf.utils.stdstreams;

/*
 * These are utilities for serialization between forge & bukkit
 * See original project here: https://github.com/FloweyTheFlower420/mappings-utils
 * Author: Flowey
 * License: GPL v3
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class BasicStandardWriter<T> implements IStandardByteWriter {
	public interface ByteWriter<T> {
		void write(T instance, byte[] buf) throws IOException;
	}

	private final T mInstance;
	private final ByteWriter<T> mWriter;

	public BasicStandardWriter(T inst, ByteWriter<T> wr) {
		mInstance = inst;
		mWriter = wr;
	}

	@Override
	public void write(byte b) throws IOException {
		mWriter.write(mInstance, new byte[] {b});
	}

	@Override
	public void write(short s) throws IOException {
		mWriter.write(mInstance, new byte[] {
			(byte) s,
			(byte) ((s >> 8) & 0xFF)
		});
	}

	@Override
	public void write(int i) throws IOException {
		mWriter.write(mInstance, new byte[] {
			(byte) i,
			(byte) ((i >> 8) & 0xFF),
			(byte) ((i >> 16) & 0xFF),
			(byte) ((i >> 24) & 0xFF)
		});
	}

	@Override
	public void write(long l) throws IOException {
		mWriter.write(mInstance, new byte[]{
			(byte) l,
			(byte) (l >> 8),
			(byte) (l >> 16),
			(byte) (l >> 24),
			(byte) (l >> 32),
			(byte) (l >> 40),
			(byte) (l >> 48),
			(byte) (l >> 56),
		});
	}

	@Override
	public void write(String s) throws IOException {
		write(s.length());
		byte[] ba = s.getBytes(StandardCharsets.UTF_8);
		mWriter.write(mInstance, ba);
	}

	@Override
	public void write(float f) throws IOException {
		write(Float.floatToRawIntBits(f));
	}

	@Override
	public void write(double f) throws IOException {
		write(Double.doubleToRawLongBits(f));
	}
}

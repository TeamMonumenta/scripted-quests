package com.floweytf.utils.stdstreams;
/*
 * These are utilities for serialization between forge & bukkit
 * See original project here: https://github.com/FloweyTheFlower420/mappings-utils
 * Author: Flowey
 * License: GPL v3
 */

import java.io.IOException;

public interface IStandardByteReader {
	/**
	 * Reads 8 bytes from the stream, then uses it to create one long. The bytes are converted as little endian.
	 * @return The long read
	 * @throws IOException Underlying IO error
	 */
	long readLong() throws IOException;

	/**
	 * Reads 4 bytes from the stream, then uses it to create one integer. The bytes are converted as little endian.
	 * @return The integer read
	 * @throws IOException Underlying IO error
	 */
	int readInt() throws IOException;

	/**
	 * Reads 2 bytes from the stream, then uses it to create one short. The bytes are converted as little endian.
	 * @return The short read
	 * @throws IOException Underlying IO error
	 */
	short readShort() throws IOException;

	/**
	 * reads one byte
	 * @return The byte read
	 * @throws IOException Underlying IO error
	 */
	byte readByte() throws IOException;

	/**
	 * Reads one integer from the stream (n), then reads n bytes from the stream.
	 * The bytes are then converted to string using UTF8
	 * @return The string read
	 * @throws IOException Underlying IO error
	 */
	String readString() throws IOException;

	/**
	 * Reads 4 bytes from the stream, and uses it to create a float
	 * @return The float read
	 * @throws IOException Underlying IO error
	 */
	float readFloat() throws IOException;

	/**
	 * Reads 8 bytes from the stream, and uses it to create a double
	 * @return The double read
	 * @throws IOException Underlying IO error
	 */
	double readDouble() throws IOException;
}

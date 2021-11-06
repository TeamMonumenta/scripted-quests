package com.floweytf.utils.stdstreams;
/*
 * These are utilities for serialization between forge & bukkit
 * See original project here: https://github.com/FloweyTheFlower420/mappings-utils
 * Author: Flowey
 * License: GPL v3
 */

import java.io.IOException;

public interface IStandardByteWriter {
    /**
     * Writes one byte
     * @param b Byte to write
     * @throws IOException Underlying IO error
     */
    void write(byte b) throws IOException;

    /**
     * Writes the short, as little endian
     * @param s Short to write
     * @throws IOException Underlying IO error
     */
    void write(short s) throws IOException;

    /**
     * Writes the int, as little endian
     * @param i Int to write
     * @throws IOException Underlying IO error
     */
    void write(int i) throws IOException;

    /**
     * Writes the long, as little endian
     * @param l Long to write
     * @throws IOException Underlying IO error
     */
    void write(long l) throws IOException;

    /**
     * Writes s.length() as integer, then the rest of the buffer
     * @param s String to write
     * @throws IOException Underlying IO error
     */
    void write(String s) throws IOException;

    /**
     * Writes the float, encoded as int
     * @param f Float to write
     * @throws IOException Underlying IO error
     */
    void write(float f) throws IOException;

    /**
     * Writes the double, encoded as long
     * @param f Double to write
     * @throws IOException Underlying IO error
     */
    void write(double f) throws IOException;
}

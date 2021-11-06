package com.floweytf.utils.stdstreams;
/*
 * These are utilities for serialization between forge & bukkit
 * See original project here: https://github.com/FloweyTheFlower420/mappings-utils
 * Author: Flowey
 * License: GPL v3
 */

import java.io.InputStream;

public class StandardByteReader extends BasicStandardReader<InputStream> {
    public StandardByteReader(InputStream is) {
        super(is, InputStream::read);
    }
}

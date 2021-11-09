package com.floweytf.utils.stdstreams;
/*
 * These are utilities for serialization between forge & bukkit
 * See original project here: https://github.com/FloweyTheFlower420/mappings-utils
 * Author: Flowey
 * License: GPL v3
 */

import java.io.OutputStream;

public class StandardByteWriter extends BasicStandardWriter<OutputStream> {
	public StandardByteWriter(OutputStream os) {
		super(os, (OutputStream::write));
	}
}

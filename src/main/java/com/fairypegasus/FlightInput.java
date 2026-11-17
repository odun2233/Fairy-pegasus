package com.fairypegasus;

/** Holds the up/down key state of the local player. Written by the client, read by the pegasus while riding. */
public final class FlightInput {
	/** 1 = fly up, -1 = fly down, 0 = hover */
	public static volatile int vertical = 0;

	private FlightInput() {}
}

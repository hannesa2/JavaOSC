// SPDX-FileCopyrightText: 2015 C. Ramakrishnan / Illposed Software
// SPDX-FileCopyrightText: 2021-2024 Robin Vobruba <hoijui.quaero@gmail.com>
//
// SPDX-License-Identifier: BSD-3-Clause

package com.illposed.osc.argument.handler.javase;

import com.illposed.osc.BufferBytesReceiver;
import com.illposed.osc.argument.ArgumentHandler;
import com.illposed.osc.OSCParseException;
import com.illposed.osc.OSCSerializeException;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import com.illposed.osc.argument.OSCColor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AwtColorArgumentHandlerTest {

	private final Logger log = LoggerFactory.getLogger(AwtColorArgumentHandlerTest.class);

	public static final Color[] DEFAULT_COLORS = new Color[] {
		Color.BLACK,
		Color.BLUE,
		Color.CYAN,
		Color.DARK_GRAY,
		Color.GRAY,
		Color.GREEN,
		Color.LIGHT_GRAY,
		Color.MAGENTA,
		Color.ORANGE,
		Color.PINK,
		Color.RED,
		Color.WHITE,
		Color.YELLOW};


	private static <T> T reparse(final ArgumentHandler<T> type, final int bufferSize, final T orig)
			throws OSCSerializeException, OSCParseException
	{
		// serialize
		final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		final BufferBytesReceiver bytesReceiver = new BufferBytesReceiver(buffer);
		type.serialize(bytesReceiver, orig);
		final ByteBuffer reparsableBuffer = (ByteBuffer) buffer.flip();

		// re-parse
		return type.parse(reparsableBuffer);
	}

	private static Color reparse(final Color orig)
			throws OSCSerializeException, OSCParseException
	{
		return reparse(AwtColorArgumentHandler.INSTANCE, OSCColor.NUM_CONTENT_BYTES, orig);
	}

	@Test
	public void testReparseDefaultColors() throws Exception {

		for (final Color orig : DEFAULT_COLORS) {
			Assertions.assertEquals(orig, reparse(orig));
		}
	}

	/**
	 * Adds random alpha values between 0 and 255 to the default colors,
	 * and then tries to re-parse them.
	 * @throws Exception on re-parse failure
	 */
	@Test
	@SuppressWarnings("SpellCheckingInspection")
	public void testReparseDefaultColorsAlphaed() throws Exception {

		final long alphaRandomSeed = new Random().nextLong();
		log.debug("{}#testReparseDefaultColorsAlphaed:alphaRandomSeed: {}",
				AwtColorArgumentHandlerTest.class.getSimpleName(), alphaRandomSeed);
		final Random alphaRandom = new Random(alphaRandomSeed);
		final Color[] alphaedDefaultColors = Arrays.copyOf(DEFAULT_COLORS, DEFAULT_COLORS.length);
		for (int tci = 0; tci < alphaedDefaultColors.length; tci++) {
			final Color orig = alphaedDefaultColors[tci];
			final int alpha = alphaRandom.nextInt(256);
			final Color alphaed = new Color(orig.getRed(), orig.getGreen(), orig.getBlue(), alpha);
			alphaedDefaultColors[tci] = alphaed;
		}

		for (final Color origColor : alphaedDefaultColors) {
			Assertions.assertEquals(origColor, reparse(origColor));
		}
	}
}

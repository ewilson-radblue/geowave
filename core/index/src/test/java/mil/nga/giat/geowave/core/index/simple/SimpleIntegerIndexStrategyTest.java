package mil.nga.giat.geowave.core.index.simple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import mil.nga.giat.geowave.core.index.ByteArrayId;
import mil.nga.giat.geowave.core.index.ByteArrayRange;
import mil.nga.giat.geowave.core.index.NumericIndexStrategy;
import mil.nga.giat.geowave.core.index.lexicoder.Lexicoders;
import mil.nga.giat.geowave.core.index.sfc.data.BasicNumericDataset;
import mil.nga.giat.geowave.core.index.sfc.data.MultiDimensionalNumericData;
import mil.nga.giat.geowave.core.index.sfc.data.NumericData;
import mil.nga.giat.geowave.core.index.sfc.data.NumericRange;
import mil.nga.giat.geowave.core.index.sfc.data.NumericValue;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.primitives.UnsignedBytes;

public class SimpleIntegerIndexStrategyTest
{

	private static final NumericIndexStrategy STRATEGY = new SimpleIntegerIndexStrategy();

	private static MultiDimensionalNumericData getIndexedRange(
			final long value ) {
		return getIndexedRange(
				value,
				value);
	}

	private static MultiDimensionalNumericData getIndexedRange(
			final long min,
			final long max ) {
		NumericData[] dataPerDimension;
		if (min == max) {
			dataPerDimension = new NumericData[] {
				new NumericValue(
						min)
			};
		}
		else {
			dataPerDimension = new NumericData[] {
				new NumericRange(
						min,
						max)
			};
		}
		return new BasicNumericDataset(
				dataPerDimension);
	}

	private static byte[] getByteArray(
			final long value ) {
		final MultiDimensionalNumericData indexedRange = getIndexedRange(value);
		final List<ByteArrayId> insertionIds = STRATEGY.getInsertionIds(indexedRange);
		final ByteArrayId insertionId = insertionIds.get(0);
		return insertionId.getBytes();
	}

	@Test
	public void testGetQueryRangesPoint() {
		final MultiDimensionalNumericData indexedRange = getIndexedRange(10l);
		final List<ByteArrayRange> ranges = STRATEGY.getQueryRanges(indexedRange);
		Assert.assertEquals(
				ranges.size(),
				1);
		final ByteArrayRange range = ranges.get(0);
		final ByteArrayId start = range.getStart();
		final ByteArrayId end = range.getEnd();
		Assert.assertTrue(Arrays.equals(
				start.getBytes(),
				end.getBytes()));
		Assert.assertEquals(
				10L,
				(long) Lexicoders.LONG.fromByteArray(start.getBytes()));
	}

	@Test
	public void testGetQueryRangesRange() {
		final long startValue = 10;
		final long endValue = 15;
		final MultiDimensionalNumericData indexedRange = getIndexedRange(
				startValue,
				endValue);
		final List<ByteArrayRange> ranges = STRATEGY.getQueryRanges(indexedRange);
		Assert.assertEquals(
				ranges.size(),
				1);
		final ByteArrayRange range = ranges.get(0);
		final ByteArrayId start = range.getStart();
		final ByteArrayId end = range.getEnd();
		Assert.assertEquals(
				(long) Lexicoders.LONG.fromByteArray(start.getBytes()),
				startValue);
		Assert.assertEquals(
				(long) Lexicoders.LONG.fromByteArray(end.getBytes()),
				endValue);
	}

	/**
	 * Check that lexicographical sorting of the byte arrays yields the same
	 * sort order as sorting the values
	 */
	@Test
	public void testRangeSortOrder() {
		final List<Long> values = Arrays.asList(
				10L,
				0L,
				15L,
				-27895L,
				8740982L,
				257430L,
				82L);
		final List<byte[]> byteArrays = new ArrayList<>(
				values.size());
		for (final long value : values) {
			final byte[] bytes = getByteArray(value);
			byteArrays.add(bytes);
		}
		Collections.sort(values);
		Collections.sort(
				byteArrays,
				UnsignedBytes.lexicographicalComparator());
		final List<Long> convertedValues = new ArrayList<>(
				values.size());
		for (final byte[] bytes : byteArrays) {
			convertedValues.add(Lexicoders.LONG.fromByteArray(bytes));
		}
		Assert.assertTrue(values.equals(convertedValues));
	}

	@Test
	public void testGetInsertionIdsPoint() {
		final long pointValue = 875926;
		final MultiDimensionalNumericData indexedData = getIndexedRange(pointValue);
		final List<ByteArrayId> insertionIds = STRATEGY.getInsertionIds(indexedData);
		Assert.assertEquals(
				insertionIds.size(),
				1);
		final ByteArrayId insertionId = insertionIds.get(0);
		Assert.assertEquals(
				(long) Lexicoders.LONG.fromByteArray(insertionId.getBytes()),
				pointValue);
	}

	@Test
	public void testGetInsertionIdsRange() {
		final long startValue = 875926;
		final long endValue = 875926 + 15;
		final MultiDimensionalNumericData indexedData = getIndexedRange(
				startValue,
				endValue);
		final List<ByteArrayId> insertionIds = STRATEGY.getInsertionIds(indexedData);
		Assert.assertEquals(
				insertionIds.size(),
				(int) ((endValue - startValue) + 1));
		int i = 0;
		for (final ByteArrayId insertionId : insertionIds) {
			Assert.assertEquals(
					(long) Lexicoders.LONG.fromByteArray(insertionId.getBytes()),
					startValue + i++);
		}
	}
}

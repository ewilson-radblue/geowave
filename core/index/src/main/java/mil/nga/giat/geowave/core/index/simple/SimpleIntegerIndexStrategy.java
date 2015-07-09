package mil.nga.giat.geowave.core.index.simple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mil.nga.giat.geowave.core.index.ByteArrayId;
import mil.nga.giat.geowave.core.index.ByteArrayRange;
import mil.nga.giat.geowave.core.index.NumericIndexStrategy;
import mil.nga.giat.geowave.core.index.StringUtils;
import mil.nga.giat.geowave.core.index.dimension.BasicDimensionDefinition;
import mil.nga.giat.geowave.core.index.dimension.NumericDimensionDefinition;
import mil.nga.giat.geowave.core.index.sfc.data.BasicNumericDataset;
import mil.nga.giat.geowave.core.index.sfc.data.MultiDimensionalNumericData;
import mil.nga.giat.geowave.core.index.sfc.data.NumericData;
import mil.nga.giat.geowave.core.index.sfc.data.NumericValue;

import com.google.common.primitives.Longs;

/**
 * A simple 1-dimensional NumericIndexStrategy that represents an index of
 * unsigned longs. The range of acceptable values is from 0 to 2^64. The
 * strategy doesn't use any binning. The ids are simply the byte[] of the value.
 * This index strategy will not perform well for inserting ranges because there
 * will be too much replication of data.
 *
 */
public class SimpleIntegerIndexStrategy implements
		NumericIndexStrategy
{

	/**
	 * Range can't include negative numbers because two's complement would
	 * destroy the sort order of the byte arrays. Lexicographical order wouldn't
	 * correspond to the order of the values.
	 */
	private static final NumericDimensionDefinition[] DEFINITIONS = new NumericDimensionDefinition[] {
		new BasicDimensionDefinition(
				0,
				Long.MAX_VALUE)
	};

	@Override
	public byte[] toBinary() {
		return new byte[] {};
	}

	@Override
	public void fromBinary(
			final byte[] bytes ) {}

	@Override
	public List<ByteArrayRange> getQueryRanges(
			final MultiDimensionalNumericData indexedRange ) {
		return getQueryRanges(
				indexedRange,
				-1);
	}

	/**
	 * Always returns a single range since this is a 1-dimensional index. The
	 * sort-order of the bytes is the same as the sort order of values, so an
	 * indexedRange can be represented by a single contiguous ByteArrayRange.
	 * {@inheritDoc}
	 */
	@Override
	public List<ByteArrayRange> getQueryRanges(
			final MultiDimensionalNumericData indexedRange,
			final int maxEstimatedRangeDecomposition ) {
		final long min = (long) indexedRange.getMinValuesPerDimension()[0];
		final ByteArrayId start = new ByteArrayId(
				Longs.toByteArray(min));
		final long max = (long) indexedRange.getMaxValuesPerDimension()[0];
		final ByteArrayId end = new ByteArrayId(
				Longs.toByteArray(max));
		final ByteArrayRange range = new ByteArrayRange(
				start,
				end);
		return Collections.singletonList(range);
	}

	@Override
	public List<ByteArrayId> getInsertionIds(
			final MultiDimensionalNumericData indexedData ) {
		return getInsertionIds(
				indexedData,
				-1);
	}

	/**
	 * Returns all of the insertion ids for the range. Since this index strategy
	 * doensn't use binning, it will return the ByteArrayId of every value in
	 * the range (i.e. if you are storing a range using this index strategy,
	 * your data will be replicated for every integer value in the range).
	 *
	 * {@inheritDoc}
	 */
	@Override
	public List<ByteArrayId> getInsertionIds(
			final MultiDimensionalNumericData indexedData,
			final int maxEstimatedDuplicateIds ) {
		final long min = (long) indexedData.getMinValuesPerDimension()[0];
		final long max = (long) indexedData.getMaxValuesPerDimension()[0];
		final List<ByteArrayId> insertionIds = new ArrayList<>(
				(int) (max - min) + 1);
		for (long i = min; i <= max; i++) {
			insertionIds.add(new ByteArrayId(
					Longs.toByteArray(i)));
		}
		return insertionIds;
	}

	/**
	 * Returns the range for the insertionId. This is always a single value.
	 *
	 * {@inheritDoc}
	 */
	@Override
	public MultiDimensionalNumericData getRangeForId(
			final ByteArrayId insertionId ) {
		final long value = Longs.fromByteArray(insertionId.getBytes());
		final NumericData[] dataPerDimension = new NumericData[] {
			new NumericValue(
					value)
		};
		return new BasicNumericDataset(
				dataPerDimension);
	}

	@Override
	public long[] getCoordinatesPerDimension(
			final ByteArrayId insertionId ) {
		return new long[] {
			Longs.fromByteArray(insertionId.getBytes())
		};
	}

	@Override
	public NumericDimensionDefinition[] getOrderedDimensionDefinitions() {
		return DEFINITIONS;
	}

	@Override
	public String getId() {
		return StringUtils.intToString(hashCode());
	}

	@Override
	public double[] getHighestPrecisionIdRangePerDimension() {
		return new double[] {
			Long.MAX_VALUE
		};
	}

}

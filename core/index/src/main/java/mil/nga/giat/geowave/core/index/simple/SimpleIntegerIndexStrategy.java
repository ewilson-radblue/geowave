package mil.nga.giat.geowave.core.index.simple;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import mil.nga.giat.geowave.core.index.ByteArrayId;
import mil.nga.giat.geowave.core.index.ByteArrayRange;
import mil.nga.giat.geowave.core.index.NumericIndexStrategy;
import mil.nga.giat.geowave.core.index.StringUtils;
import mil.nga.giat.geowave.core.index.dimension.BasicDimensionDefinition;
import mil.nga.giat.geowave.core.index.dimension.NumericDimensionDefinition;
import mil.nga.giat.geowave.core.index.lexicoder.IntegerLexicoder;
import mil.nga.giat.geowave.core.index.lexicoder.Lexicoders;
import mil.nga.giat.geowave.core.index.lexicoder.LongLexicoder;
import mil.nga.giat.geowave.core.index.lexicoder.NumberLexicoder;
import mil.nga.giat.geowave.core.index.lexicoder.ShortLexicoder;
import mil.nga.giat.geowave.core.index.sfc.data.BasicNumericDataset;
import mil.nga.giat.geowave.core.index.sfc.data.MultiDimensionalNumericData;
import mil.nga.giat.geowave.core.index.sfc.data.NumericData;
import mil.nga.giat.geowave.core.index.sfc.data.NumericValue;

/**
 * A simple 1-dimensional NumericIndexStrategy that represents an index of
 * signed integer values (currently supports 16 bit, 32 bit, and 64 bit
 * integers). The strategy doesn't use any binning. The ids are simply the
 * byte[] of the value. This index strategy will not perform well for inserting
 * ranges because there will be too much replication of data.
 *
 */
public class SimpleIntegerIndexStrategy implements
		NumericIndexStrategy
{

	private NumberLexicoder<? extends Number> lexicoder;
	private NumericDimensionDefinition[] definitions;

	public SimpleIntegerIndexStrategy() {
		init(Lexicoders.LONG);
	}

	private SimpleIntegerIndexStrategy(
			final NumberLexicoder<? extends Number> lexicoder ) {
		init(lexicoder);
	}

	private void init(
			final NumberLexicoder<? extends Number> lexicoder ) {
		this.lexicoder = lexicoder;
		if (!(lexicoder instanceof ShortLexicoder) && !(lexicoder instanceof IntegerLexicoder) && !(lexicoder instanceof LongLexicoder)) {
			throw new UnsupportedOperationException(
					"SimpleIntegerIndexStrategy only supports Short, Integer, Long");
		}
		definitions = new NumericDimensionDefinition[] {
			new BasicDimensionDefinition(
					lexicoder.getMinimumValue().doubleValue(),
					lexicoder.getMaximumValue().doubleValue())
		};
	}

	public static SimpleIntegerIndexStrategy get16BitIntegerIndexStrategy() {
		return new SimpleIntegerIndexStrategy(
				Lexicoders.SHORT);
	}

	public static SimpleIntegerIndexStrategy get32BitIntegerIndexStrategy() {
		return new SimpleIntegerIndexStrategy(
				Lexicoders.INT);
	}

	public static SimpleIntegerIndexStrategy get64BitIntegerIndexStrategy() {
		return new SimpleIntegerIndexStrategy(
				Lexicoders.LONG);
	}

	@Override
	public byte[] toBinary() {
		if (lexicoder instanceof ShortLexicoder) {
			return new byte[] {
				0x00
			};
		}
		else if (lexicoder instanceof IntegerLexicoder) {
			return new byte[] {
				0x01
			};
		}
		else if (lexicoder instanceof LongLexicoder) {
			return new byte[] {
				0x02
			};
		}
		else {
			throw new UnsupportedOperationException(
					"SimpleIntegerIndexStrategy only supports Short, Integer, Long");
		}
	}

	@Override
	public void fromBinary(
			final byte[] bytes ) {
		checkArgument(
				(bytes != null) && (bytes.length == 1),
				"invalid byte array to deserialize");
		switch (bytes[0]) {
			case 0x00:
				init(Lexicoders.SHORT);
				break;
			case 0x01:
				init(Lexicoders.INT);
				break;
			case 0x02:
				init(Lexicoders.LONG);
				break;
			default:
				throw new UnsupportedOperationException(
						"SimpleIntegerIndexStrategy only supports Short, Integer, Long");
		}
	}

	private byte[] encodeInteger(
			final long value ) {
		return lexicoder.toByteArray(value);
	}

	private long decodeInteger(
			final byte[] bytes ) {
		return Long.class.cast(lexicoder.fromByteArray(bytes));
	}

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
				encodeInteger(min));
		final long max = (long) indexedRange.getMaxValuesPerDimension()[0];
		final ByteArrayId end = new ByteArrayId(
				encodeInteger(max));
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
					encodeInteger(i)));
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
		final long value = decodeInteger(insertionId.getBytes());
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
			decodeInteger(insertionId.getBytes())
		};
	}

	@Override
	public NumericDimensionDefinition[] getOrderedDimensionDefinitions() {
		return definitions;
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

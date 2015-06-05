package mil.nga.giat.geowave.vector.stats;

import java.nio.ByteBuffer;

import mil.nga.giat.geowave.index.ByteArrayId;
import mil.nga.giat.geowave.index.Mergeable;
import mil.nga.giat.geowave.store.DataStoreEntryInfo;
import mil.nga.giat.geowave.store.adapter.statistics.AbstractDataStatistics;
import mil.nga.giat.geowave.store.adapter.statistics.DataStatistics;

import org.opengis.feature.simple.SimpleFeature;

import com.clearspring.analytics.stream.frequency.CountMinSketch;

public class FeatureCountMinSketchStatistics extends
		AbstractDataStatistics<SimpleFeature> implements
		FeatureStatistic
{
	public static final String STATS_TYPE = "ATT_SKETCH";
	private CountMinSketch sketch = new CountMinSketch(
			0.001,
			0.98,
			7364181);

	protected FeatureCountMinSketchStatistics() {
		super();
	}

	public FeatureCountMinSketchStatistics(
			final ByteArrayId dataAdapterId,
			final String fieldName ) {
		super(
				dataAdapterId,
				composeId(
						STATS_TYPE,
						fieldName));
	}

	public static final ByteArrayId composeId(
			final String fieldName ) {
		return composeId(
				STATS_TYPE,
				fieldName);
	}

	@Override
	public String getFieldName() {
		return decomposeNameFromId(getStatisticsId());
	}

	@Override
	public DataStatistics<SimpleFeature> duplicate() {
		return new FeatureCountMinSketchStatistics(
				dataAdapterId,
				getFieldName());
	}

	public long totalSampleSize() {
		return sketch.size();
	}

	public long count(
			String item ) {
		return sketch.estimateCount(item);
	}

	@Override
	public void merge(
			final Mergeable mergeable ) {
		if (mergeable instanceof FeatureCountMinSketchStatistics) {
			try {
				sketch = CountMinSketch.merge(
						sketch,
						((FeatureCountMinSketchStatistics) mergeable).sketch);
			}
			catch (Exception e) {
				throw new RuntimeException(
						"Unable to merge sketches",
						e);
			}
		}

	}

	@Override
	public byte[] toBinary() {
		byte[] data = CountMinSketch.serialize(sketch);
		final ByteBuffer buffer = super.binaryBuffer(4 + data.length);
		buffer.putInt(data.length);
		buffer.put(data);
		return buffer.array();
	}

	@Override
	public void fromBinary(
			final byte[] bytes ) {
		final ByteBuffer buffer = super.binaryBuffer(bytes);
		final byte[] data = new byte[buffer.getInt()];
		buffer.get(data);
		sketch = CountMinSketch.deserialize(data);
	}

	@Override
	public void entryIngested(
			final DataStoreEntryInfo entryInfo,
			final SimpleFeature entry ) {
		final Object o = entry.getAttribute(getFieldName());
		if (o == null) {
			return;
		}
		sketch.add(
				o.toString(),
				1);
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(
				"sketch[adapter=").append(
				super.getDataAdapterId().getString());
		buffer.append(
				", field=").append(
				getFieldName());
		buffer.append(
				", size=").append(
				sketch.size());
		buffer.append("]");
		return buffer.toString();
	}

}
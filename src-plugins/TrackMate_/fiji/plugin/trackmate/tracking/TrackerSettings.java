package fiji.plugin.trackmate.tracking;

import java.util.HashMap;
import java.util.Map;

import fiji.plugin.trackmate.SpotFeature;

public class TrackerSettings {
	
	private static final double 	DEFAULT_LINKING_DISTANCE_CUTOFF 		= 15.0;
	private static final HashMap<SpotFeature, Double> DEFAULT_LINKING_FEATURE_CUTOFFS = new HashMap<SpotFeature, Double>();
	
	private static final boolean 	DEFAULT_ALLOW_GAP_CLOSING 				= true;
	private static final double 	DEFAULT_GAP_CLOSING_TIME_CUTOFF 		= 4;
	private static final double 	DEFAULT_GAP_CLOSING_DISTANCE_CUTOFF 	= 15.0;
	private static final HashMap<SpotFeature, Double> DEFAULT_GAP_CLOSING_FEATURE_CUTOFFS = new HashMap<SpotFeature, Double>();
	static {
		DEFAULT_GAP_CLOSING_FEATURE_CUTOFFS.put(SpotFeature.MEAN_INTENSITY, 4d);
	}
	
	private static final boolean 	DEFAULT_ALLOW_MERGING 					= false;
	private static final double 	DEFAULT_MERGING_TIME_CUTOFF 			= 1;
	private static final double 	DEFAULT_MERGING_DISTANCE_CUTOFF 		= 15.0;
	private static final HashMap<SpotFeature, Double> DEFAULT_MERGING_FEATURE_CUTOFFS = new HashMap<SpotFeature, Double>();
	static {
		DEFAULT_MERGING_FEATURE_CUTOFFS.put(SpotFeature.MEAN_INTENSITY, 4d);
	}

	private static final boolean 	DEFAULT_ALLOW_SPLITTING 				= false;
	private static final double 	DEFAULT_SPLITTING_TIME_CUTOFF 			= 1;
	private static final double 	DEFAULT_SPLITTING_DISTANCE_CUTOFF 		= 15.0;
	private static final HashMap<SpotFeature, Double> DEFAULT_SPLITTING_FEATURE_CUTOFFS = new HashMap<SpotFeature, Double>();
	static {
		DEFAULT_SPLITTING_FEATURE_CUTOFFS.put(SpotFeature.MEAN_INTENSITY, 4d);
	}

	private static final double 	DEFAULT_ALTERNATIVE_OBJECT_LINKING_COST_FACTOR = 1.05d;
	private static final double 	DEFAULT_CUTOFF_PERCENTILE 				= 0.9d;
	
	/** The tracker type this settings object is for. This field is here just for reference. */
	public TrackerType trackerType = TrackerType.SIMPLE_LAP_TRACKER;
	
	/** Max time difference over which particle linking is allowed.	 */
	public double linkingDistanceCutOff 		= DEFAULT_LINKING_DISTANCE_CUTOFF;
	/** Feature difference cutoffs for linking. */
	public Map<SpotFeature, Double> linkingFeatureCutoffs = DEFAULT_LINKING_FEATURE_CUTOFFS; 
	
	/** Allow track segment gap closing? */
	public boolean allowGapClosing 				= DEFAULT_ALLOW_GAP_CLOSING;
	/** Max time difference over which segment gap closing is allowed.	 */
	public double gapClosingTimeCutoff 			= DEFAULT_GAP_CLOSING_TIME_CUTOFF;
	/** Max distance over which segment gap closing is allowed. */
	public double gapClosingDistanceCutoff 		= DEFAULT_GAP_CLOSING_DISTANCE_CUTOFF;
	/** Feature difference cutoffs for gap closing. */
	public Map<SpotFeature, Double> gapClosingFeatureCutoffs = DEFAULT_GAP_CLOSING_FEATURE_CUTOFFS; 

	/** Allow track segment merging? */
	public boolean allowMerging 				= DEFAULT_ALLOW_MERGING;
	/** Max time difference over which segment gap closing is allowed.	 */
	public double mergingTimeCutoff 			= DEFAULT_MERGING_TIME_CUTOFF;
	/** Max distance over which segment gap closing is allowed. */
	public double mergingDistanceCutoff 		= DEFAULT_MERGING_DISTANCE_CUTOFF;
	/** Feature difference cutoffs for merging. */
	public Map<SpotFeature, Double> mergingFeatureCutoffs = DEFAULT_MERGING_FEATURE_CUTOFFS; 

	/** Allow track segment splitting? */
	public boolean allowSplitting				= DEFAULT_ALLOW_SPLITTING;
	/** Max time difference over which segment splitting is allowed.	 */
	public double splittingTimeCutoff 			= DEFAULT_SPLITTING_TIME_CUTOFF;
	/** Max distance over which segment splitting is allowed. */
	public double splittingDistanceCutoff 		= DEFAULT_SPLITTING_DISTANCE_CUTOFF;
	/** Feature difference cutoffs for splitting. */
	public Map<SpotFeature, Double> splittingFeatureCutoffs = DEFAULT_SPLITTING_FEATURE_CUTOFFS; 

	/** The factor used to create d and b in the paper, the alternative costs to linking objects. */
	public double alternativeObjectLinkingCostFactor = DEFAULT_ALTERNATIVE_OBJECT_LINKING_COST_FACTOR;
	/** The percentile used to calculate d and b cutoffs in the paper. */
	public double cutoffPercentile 				= DEFAULT_CUTOFF_PERCENTILE;
	
	/** Value used to block assignments when physically meaningless. */
	public double blockingValue 				= Double.MAX_VALUE;
	
	public String timeUnits 					= "frames";
	public String spaceUnits 					= "pixels";
	

	/*
	 * METHODS
	 */
	
	@Override
	public String toString() {
		String 	str = "Tracker: "+ trackerType.toString()+'\n';
		
		str += "  Linking conditions:\n";
		str += String.format("    - distance cutoff: %.1f\n", linkingDistanceCutOff);
		str += echoFeatureCuttofs(linkingFeatureCutoffs);
		
		if (allowGapClosing) {
			str += "  Gap-closing conditions:\n";
			str += String.format("    - distance cutoff: %.1f\n", gapClosingDistanceCutoff);
			str += String.format("    - max frame interval: %.1f\n", gapClosingTimeCutoff);
			str += echoFeatureCuttofs(gapClosingFeatureCutoffs);
		} else {
			str += "  Gap-closing not allowed.\n";
		}
		
		if (allowSplitting) {
			str += "  Track splitting conditions:\n";
			str += String.format("    - distance cutoff: %.1f\n", splittingDistanceCutoff);
			str += String.format("    - max frame interval: %.1f\n", splittingTimeCutoff);
			str += echoFeatureCuttofs(splittingFeatureCutoffs);
		} else {
			str += "  Track splitting not allowed.\n";
		}
		
		if (allowMerging) {
			str += "  Track merging conditions:\n";
			str += String.format("    - distance cutoff: %.1f\n", mergingDistanceCutoff);
			str += String.format("    - max frame interval: %.1f\n", mergingTimeCutoff);
			str += echoFeatureCuttofs(mergingFeatureCutoffs);
		} else {
			str += "  Track merging not allowed.\n";
		}
		
		return str;
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private static String echoFeatureCuttofs(final Map<SpotFeature, Double> featureCutoffs) {
		String str = "";
		if (featureCutoffs.isEmpty()) 
			str += "    - no feature condidions\n";
		else {
			str += "    - with feature conditions:\n";
			for (SpotFeature feature : featureCutoffs.keySet()) {
				str += "      - "+feature.toString() + ": ratio = " + String.format("%.1f", featureCutoffs.get(feature)) + '\n';
			}
		}
		return str;
		
	}


}

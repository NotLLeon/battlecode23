package v3CappedArc;

public class Constants {
    // weights for explore
    static int TOTAL_WEIGHT_DIRECTIONS = 13;
    static int HIGH_WEIGHT_DIRECTION = 6;
    static int MID_WEIGHT_DIRECTION = 2;

    // stop sampling direction
    static int MAX_DIRECTION_SEARCH_ATTEMPTS = 32;

    // indices for comms
    static int IDX_NUM_HQS = 63;
    static int NUM_WELLS_STORED;

    static int NUM_TRACKED_LOCATIONS = 3;

    static int MOVES_TO_TRACK_LOCATION = 8;
}

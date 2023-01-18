package v4_1;

public class Constants {
    // weights for explore
    static int TOTAL_WEIGHT_DIRECTIONS = 13;
    static int HIGH_WEIGHT_DIRECTION = 6;
    static int MID_WEIGHT_DIRECTION = 2;


    // limits for explore

    static int NUM_TRACKED_LOCATIONS = 3;

    static int MOVES_TO_TRACK_LOCATION = 8;

    // stop sampling direction
    static int MAX_DIRECTION_SEARCH_ATTEMPTS = 32;

    // indices for comms
    static int IDX_NUM_HQS = 63;
    static int IDX_NUM_MANA_WELLS = 61;
    static int IDX_NUM_AD_WELLS = 62;
    static int IDX_NUM_ISLANDS = 60;

    //Temporary
    static int MAX_WELLS_STORED=20;
    static int MAX_AD_WELLS_STORED = 10;
    static int MAX_MANA_WELLS_STORED = 10;
    static int MAX_HQS_STORED=4;
    static int MAX_ISLANDS_STORED=20;

}

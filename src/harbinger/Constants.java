package harbinger;

public class Constants {
    // weights for explore
    static int TOTAL_WEIGHT_DIRECTIONS = 13;
    static int HIGH_WEIGHT_DIRECTION = 6;
    static int MID_WEIGHT_DIRECTION = 2;

    // stop sampling direction
    static int MAX_DIRECTION_SEARCH_ATTEMPTS = 32;

    // indices for comms
    static int IDX_NUM_HQS = 63;
    static int IDX_NUM_WELLS=62;
    static int IDX_NUM_ISLANDS=61;

    //Temporary
    static int MAX_WELLS_STORED=20;
    static int MAX_HQS_STORED=4;
    static int MAX_ISLANDS_STORED=20;
    static int NUM_WELLS_STORED=0;
    static int NUM_ISLANDS_STORED=0;
}

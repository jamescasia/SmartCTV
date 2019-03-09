package aetherapps.com.smartstreamer;

/**
 * Created by James on 09/03/2019.
 */

public class DetectedImage {

    public String timeTaken;
    public String storageLink;
    public String taker;

    public DetectedImage(String timeTaken, String storageLink, String taker){

        this.timeTaken = timeTaken;
        this.storageLink = storageLink;
        this.taker = taker;
    }
}

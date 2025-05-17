package application;

public class RecognitionResult {
    private final int personId;
    private final String fullName;
    private final boolean clockIn;
    private final double confidence;
    private final byte[] snapshot;

    public RecognitionResult(int personId, String fullName, boolean clockIn, double confidence, byte[] snapshot) {
        this.personId   = personId;
        this.fullName   = fullName;
        this.clockIn    = clockIn;
        this.confidence = confidence;
        this.snapshot   = snapshot;
    }

    public int getPersonId()    { return personId; }
    public String getFullName() { return fullName; }
    public boolean isClockIn()  { return clockIn; }
    public double getConfidence(){ return confidence; }
    public byte[] getSnapshot() { return snapshot; }
}

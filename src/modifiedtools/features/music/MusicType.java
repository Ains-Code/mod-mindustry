package modifiedtools.features.music;

public enum MusicType {
    AMBIENT,
    DARK,
    BOSS;

    public String getKey() {
        return "modifiedtools.music." + name().toLowerCase();
    }
}

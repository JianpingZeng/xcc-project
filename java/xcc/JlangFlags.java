package xcc;

public interface JlangFlags
{
    int DriverOption = (1 << 4);
    int LinkerInput = (1 << 5);
    int NoArgumentUnused = (1 << 6);
    int Unsupported = (1 << 7);
    int CoreOption = (1 << 8);
    int CLOption = (1 << 9);
    int CC1Option = (1 << 10);
    int NoDriverOption = (1 << 11);
    int HelpHidden = (1 << 12);
    int NoForward = (1 << 13);
    int RenderJoined = (1 << 14);
    int RenderAsInput = (1 << 15);
}

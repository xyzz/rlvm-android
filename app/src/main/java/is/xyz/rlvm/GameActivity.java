package is.xyz.rlvm;


import org.libsdl.app.SDLActivity;

public class GameActivity extends SDLActivity {
    @Override
    protected String[] getLibraries() {
        return new String[] {
                "SDL2",
                "game"
        };
    }
}

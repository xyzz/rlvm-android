package is.xyz.rlvm;


import android.util.Log;

import org.libsdl.app.SDLActivity;

public class GameActivity extends SDLActivity {
    @Override
    protected String[] getLibraries() {
        return new String[] {
                "SDL2",
                "game"
        };
    }

    @Override
    public void onDestroy() {
        finish();
        // Just kill ourselves since running again results in a GL error
        android.os.Process.killProcess(android.os.Process.myPid());
        super.onDestroy();
    }
}

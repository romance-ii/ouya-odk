/*
 * Copyright (C) 2012 OUYA, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tv.ouya.sample.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import tv.ouya.console.api.OuyaController;

import static tv.ouya.sample.game.R.id;
import static tv.ouya.sample.game.R.layout;

public class GameActivity extends Activity {
    private Player[] players;

    static public Boolean pauseInput = new Boolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(layout.game);
        Button quitGame = (Button) findViewById(id.quit_button);
        quitGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        players = new Player[4];
        for(int i = 0; i < 4; ++i) {
            players[i] = new Player(this, i);

            // Show ships for controllers that are currently connected
            OuyaController ouyaController = OuyaController.getControllerByPlayer(i);
            if (ouyaController != null) {
                findOrCreatePlayer(ouyaController.getDeviceId());
            }
        }

        switch (Options.getInstance().getLevel()) {
            case FREEDOM:
                break;
            case ALLEYWAY:
                new Wall(GameRenderer.BOARD_WIDTH * 0.25f, GameRenderer.BOARD_HEIGHT * 0.25f,
                         GameRenderer.BOARD_WIDTH * 0.75f, GameRenderer.BOARD_HEIGHT * 0.25f);
                new Wall(GameRenderer.BOARD_WIDTH * 0.25f, GameRenderer.BOARD_HEIGHT * 0.50f,
                         GameRenderer.BOARD_WIDTH * 0.75f, GameRenderer.BOARD_HEIGHT * 0.50f);
                new Wall(GameRenderer.BOARD_WIDTH * 0.25f, GameRenderer.BOARD_HEIGHT * 0.75f,
                         GameRenderer.BOARD_WIDTH * 0.75f, GameRenderer.BOARD_HEIGHT * 0.75f);
                break;
            case BOXY:
                final float c_numPieces = 10;
                float wallWidth = GameRenderer.BOARD_WIDTH / c_numPieces;
                float wallHeight = GameRenderer.BOARD_HEIGHT / c_numPieces;
                float wallXOfs = wallWidth * 0.5f;
                float wallYOfs = wallHeight * 0.5f;
                for (int i = 1; i < c_numPieces; i += 2) {
                    for (int j = 1; j < c_numPieces; j += 2) {
                        if (i != c_numPieces - 1) {
                            new Wall(wallXOfs + wallWidth * i, wallHeight * j,
                                     wallXOfs + wallWidth * (i+1), wallHeight * j);
                        }
                        if (j != c_numPieces - 1) {
                            new Wall(wallWidth * i, wallYOfs + wallHeight * j,
                                     wallWidth * i, wallYOfs + wallHeight * (j+1));
                        }
                    }
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // hide the mouse cursor
        OuyaController.showCursor(false);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // show the mouse cursor
        OuyaController.showCursor(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        synchronized (pauseInput) {
            handled = OuyaController.onKeyDown(keyCode, event);
            findOrCreatePlayer(event.getDeviceId());
        }

        if (keyCode == OuyaController.BUTTON_A) {
            finish();
        }

        return handled || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean handled = false;
        synchronized (pauseInput) {
            handled = OuyaController.onKeyUp(keyCode, event);
        }
        return handled || super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        boolean handled = false;
        synchronized (pauseInput) {
            handled = OuyaController.onGenericMotionEvent(event);
        }

        OuyaController c = OuyaController.getControllerByDeviceId(event.getDeviceId());
        if (c != null) {
            if (Player.isStickNotCentered(
                    c.getAxisValue(OuyaController.AXIS_LS_X),
                    c.getAxisValue(OuyaController.AXIS_LS_Y))) {
                // Create the player if necessary
                Player p = findOrCreatePlayer(event.getDeviceId());
            }
        }

        return handled || super.onGenericMotionEvent(event);
    }

    private Player findOrCreatePlayer(int deviceId) {
        int playerNum = OuyaController.getPlayerNumByDeviceId(deviceId);
        if (playerNum < 0) {
            return null;
        }

        if (players[playerNum].isValid()) {
            return players[playerNum];
        }

        players[playerNum].init();
        return players[playerNum];
    }

}

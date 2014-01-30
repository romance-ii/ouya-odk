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

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.media.MediaPlayer;
import android.util.Log;
import tv.ouya.console.api.OuyaController;

import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Player extends RenderObject {
    private int playerNum = -1;
    private boolean isVisible = false;
    private boolean isDead = false;
    private long lastShotTime = 0;

    private PointF shootDir;
    private float forwardAmount;

    static final private int[] c_playerColors = {
            Color.WHITE,
            Color.BLUE,
            Color.YELLOW,
            Color.GREEN
    };
    static final private int c_deadColor = Color.DKGRAY;
    static final private float c_playerRadius = 0.5f;
    static final private float c_timeBetweenShots = 0.1f;

    static MediaPlayer s_sfx[] = null;

    public Player(Context context, int playerNum) {
        super(c_playerRadius);
        this.playerNum = playerNum;
        shootDir = new PointF();

        if (s_sfx == null) {
            try {
                s_sfx = new MediaPlayer[3];
                s_sfx[0] = MediaPlayer.create(context, R.raw.sound1);
                s_sfx[1] = MediaPlayer.create(context, R.raw.sound2);
                s_sfx[2] = MediaPlayer.create(context, R.raw.sound3);
            }
            catch (Exception e) {
            }
        }

        setCollisionListener(new CollisionListener() {
            @Override
            public void onCollide(PointF prev, RenderObject me, RenderObject other) {
                if (other instanceof Wall) {
                    Wall wall = (Wall) other;

                    translation = wall.slideAgainst(prev, translation, getRadius());
                }
            }
        });
    }

    public void init() {
        isVisible = true;

        // Pick a random starting location
        translation.x = (float) (Math.random() * (GameRenderer.BOARD_WIDTH - 1.0f) + 1.0f);
        translation.y = (float) (Math.random() * (GameRenderer.BOARD_HEIGHT - 1.0f) + 1.0f);
        rotation = (float) (Math.random() * 360.0f);
    }

    public boolean isValid() {
        return isVisible;
    }

    public void shoot(float dirX, float dirY) {
        shootDir.set(dirX, dirY);
    }

    public void die() {
        isDead = true;
    }

    @Override
    protected void initModel() {
        final short[] _indicesArray = {0, 1, 2, 1, 3, 2};

        // float has 4 bytes
        ByteBuffer vbb = ByteBuffer.allocateDirect(_indicesArray.length * 3 * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();

        // short has 2 bytes
        ByteBuffer ibb = ByteBuffer.allocateDirect(_indicesArray.length * 2);
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asShortBuffer();

        final float[] coords = {
                -0.5f, -0.5f, 0.0f, // 0
                 0.0f, -0.2f, 0.0f, // 1
                 0.0f,  0.1f, 0.0f, // 2
                 0.5f, -0.5f, 0.0f, // 3
        };

        vertexBuffer.put(coords);
        indexBuffer.put(_indicesArray);

        vertexBuffer.position(0);
        indexBuffer.position(0);
    }

    final float c_forwardSpeed = 0.1f;

    static private float stickMag(float axisX, float axisY) {
        float stickMag = (float) Math.sqrt(axisX * axisX + axisY * axisY);
        return stickMag;
    }

    static public boolean isStickNotCentered(float axisX, float axisY) {
        float stickMag = stickMag(axisX, axisY);
        return (stickMag >= OuyaController.STICK_DEADZONE);
    }

    private void checkPlaySound(OuyaController c) {
        if (c.buttonChangedThisFrame(OuyaController.BUTTON_Y)) {
            Log.d("BUTTON", "State changed");
        }
        if (c.buttonPressedThisFrame(OuyaController.BUTTON_Y)) {
            int sfxIdx = 0;
            int rnd = (int)(Math.random() * 10);
            if (rnd > 5) {
                sfxIdx = 0;
            } else if (rnd > 0) {
                sfxIdx = 1;
            } else {
                sfxIdx = 2;
            }
            s_sfx[sfxIdx].start();
        }
    }

    private void getForwardAmountFromController(OuyaController c) {
        float axisX = c.getAxisValue(OuyaController.AXIS_LS_X);
        axisX = Math.min(axisX, 1.0f);
        float axisY = c.getAxisValue(OuyaController.AXIS_LS_Y);
        axisY = Math.min(axisY, 1.0f);
        if (isStickNotCentered(axisX, axisY)) {
            float stickMag = stickMag(axisX, axisY);
            float desiredDir = (float) Math.toDegrees( Math.atan2(-axisX, axisY) );
            setRotate(desiredDir);
            forwardAmount = stickMag * c_forwardSpeed;
        } else {
            forwardAmount = 0.0f;
        }
    }

    private void getShootDirFromController(OuyaController c) {
        float axisX = c.getAxisValue(OuyaController.AXIS_RS_X);
        axisX = Math.min(axisX, 1.0f);
        float axisY = c.getAxisValue(OuyaController.AXIS_RS_Y);
        axisY = Math.min(axisY, 1.0f);
        if (isStickNotCentered(axisX, axisY)) {
            float stickMag = stickMag(axisX, axisY);
            // normalize the direction vec
            shootDir.x = axisX / stickMag;
            shootDir.y = axisY / stickMag;
        } else {
            shootDir.set(0.0f, 0.0f);

            // Stick isn't pressed, check the buttons
            if (c.getButton(OuyaController.BUTTON_O)
                   || c.getButton(OuyaController.BUTTON_U)) {
                PointF fwdVec = getForwardVector();
                shootDir = fwdVec;
            }
        }
    }

    @Override
    protected void update() {
        if (!isValid()) {
            return;
        }

        OuyaController c = OuyaController.getControllerByPlayer(playerNum);
        if (c == null) {
            return;
        }

        super.update();

        checkPlaySound(c);
        getForwardAmountFromController(c);
        getShootDirFromController(c);

        if (!isDead && forwardAmount != 0.0f) {
            goForward(forwardAmount);
        }

        if (shootDir.x != 0.0f || shootDir.y != 0.0f) {
            long currentTime = System.currentTimeMillis();
            float timeSinceLastShot = (currentTime - lastShotTime) / 1000.0f;
            if (timeSinceLastShot > c_timeBetweenShots) {
                lastShotTime = currentTime;
                float desiredDir = (float) Math.toDegrees( Math.atan2(-shootDir.x, shootDir.y) );

                final float c_bulletDistance = 0.0f;
                Bullet b = new Bullet(this, translation.x + shootDir.x * c_bulletDistance, translation.y + shootDir.y * c_bulletDistance, desiredDir);
            }
        }
    }

    @Override
    protected void doRender(GL10 gl) {
        if (!isValid()) {
            return;
        }

        int color = isDead ? c_deadColor : c_playerColors[playerNum];
        setColor(gl, color);
        super.doRender(gl);
    }

    @Override
    public boolean doesCollide(RenderObject other) {
        if (other instanceof Player) {
            return false;
        }
        return super.doesCollide(other);
    }
}

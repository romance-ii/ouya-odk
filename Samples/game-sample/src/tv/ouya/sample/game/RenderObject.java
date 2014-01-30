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

import android.graphics.Color;
import android.graphics.PointF;

import javax.microedition.khronos.opengles.GL10;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public abstract class RenderObject {
    protected ShortBuffer indexBuffer;
    protected FloatBuffer vertexBuffer;

    protected float rotation = 0.0f;    // rotation about the Z-axis
    protected PointF translation;

    protected float radius = 1.0f;

    protected CollisionListener collisionListener;

    public interface CollisionListener {
        // Return false if the move should fail
        public void onCollide(PointF prev, RenderObject me, RenderObject other);
    }

    public RenderObject(float radius) {
        this.radius = radius;
        translation = new PointF();
        initModel();
        GameRenderer.s_instance.addRenderObject(this);
    }

    public void setCollisionListener(CollisionListener collisionListener) {
        this.collisionListener = collisionListener;
    }

    public void setRotate(float degrees) {
        float delta =  degrees - rotation;
        rotate(delta);
    }

    public void rotate(float degreeDelta) {
        rotation += degreeDelta;
        rotation %= 360.0f;
    }

    public PointF getForwardVector() {
        float fwdX = (float) Math.sin(Math.toRadians(-rotation));
        float fwdY = (float) Math.cos(Math.toRadians(-rotation));
        return new PointF(fwdX, fwdY);
    }

    public void goForward(float amount) {
        final PointF prev = new PointF(translation.x, translation.y);

        PointF newPos = getForwardVector();

        translation.x += newPos.x * amount;
        translation.y += newPos.y * amount;

        if (translation.x < 0.0f) translation.x += GameRenderer.BOARD_WIDTH;
        if (translation.x > 10.0f) translation.x %= GameRenderer.BOARD_WIDTH;
        if (translation.y < 0.0f) translation.y += GameRenderer.BOARD_HEIGHT;
        if (translation.y > 10.0f) translation.y %= GameRenderer.BOARD_HEIGHT;

        if (collisionListener != null) {
            final RenderObject collidingObject = GameRenderer.s_instance.getCollidingObject(this);
            if (collidingObject != null) {
                collisionListener.onCollide(prev, this, collidingObject);
            }
        }
    }

    protected abstract void initModel();

    protected void update() {
    }

    protected void setColor(GL10 gl, int color) {
        gl.glColor4f(
                Color.red(color) / 255.0f,
                Color.green(color) / 255.0f,
                Color.blue(color) / 255.0f,
                0.75f);
    }

    protected void doRender(GL10 gl) {
        gl.glPushMatrix();

        gl.glTranslatef(translation.x, translation.y, 5.0f);
        gl.glRotatef(rotation, 0.0f, 0.0f, 1.0f);

        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
        gl.glDrawElements(GL10.GL_TRIANGLES, indexBuffer.limit(), GL10.GL_UNSIGNED_SHORT, indexBuffer);

        gl.glPopMatrix();
    }

    public float getRadius() {
        return radius;
    }

    public boolean doesCollide(RenderObject other) {
        float deltaX = translation.x - other.translation.x;
        float deltaY = translation.y - other.translation.y;
        float distSq = deltaX * deltaX + deltaY * deltaY;
        float radiiSq = getRadius() + other.getRadius();
        radiiSq *= radiiSq;
        if (distSq <= radiiSq) {
            return true;
        }
        return false;
    }
}

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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Wall extends RenderObject {

    static private final int c_wallColor = Color.YELLOW;

    float[] vertices;
    PointF normal;

    public Wall(float x1, float y1, float x2, float y2) {
        super(0.1f);

        vertices = new float[6];
        vertices[0] = x1;
        vertices[1] = y1;
        vertices[2] = 0.0f;
        vertices[3] = x2;
        vertices[4] = y2;
        vertices[5] = 0.0f;

        normal = new PointF(
            vertices[4] - vertices[1],
            vertices[3] - vertices[0]);
        float normMag = normal.length();
        normal.x /= normMag;
        normal.y /= normMag;

        initModel();
    }

    @Override
    protected void initModel() {
        if (vertices == null) {
            return;
        }

        // float has 4 bytes
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
    }

    @Override
    protected void doRender(GL10 gl) {
        setColor(gl, c_wallColor);
        // NOT calling super.doRender here

        gl.glLineWidth(5.0f);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
        gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, 2);
    }

    @Override
    public boolean doesCollide(RenderObject other) {
        PointF proj = projectOntoWall(other.translation, true);
        float deltaX = proj.x - other.translation.x;
        float deltaY = proj.y - other.translation.y;
        float distanceSq = (deltaX * deltaX + deltaY * deltaY);

        float radiiSq = getRadius() + other.getRadius();
        radiiSq *= radiiSq;

        if (distanceSq <= radiiSq) {
            return true;
        }
        return false;
    }

    public PointF slideAgainst(PointF prevPoint, PointF newPoint, float radius) {
        PointF prevProjectedPoint = projectOntoWall(prevPoint, false);
        float prevDistanceToWallX = prevPoint.x - prevProjectedPoint.x;
        float prevDistanceToWallY = prevPoint.y - prevProjectedPoint.y;
        float prevDistanceToWall = (float) Math.sqrt(prevDistanceToWallX * prevDistanceToWallX + prevDistanceToWallY * prevDistanceToWallY);
        if (prevDistanceToWall < radius + getRadius()) {
            prevDistanceToWall = radius + getRadius();
        }

        PointF newProjectedPoint = projectOntoWall(newPoint, false);

        float toPrevX = prevPoint.x - vertices[0];
        float toPrevY = prevPoint.y - vertices[1];
        float dotPrev = (normal.x * toPrevX + normal.y * toPrevY);
        if (dotPrev < 0.0f) {
            normal.negate();
        }

        float slideX = newProjectedPoint.x + normal.x * prevDistanceToWall;
        float slideY = newProjectedPoint.y + normal.y * prevDistanceToWall;

        return new PointF(slideX, slideY);
    }

    private PointF projectOntoWall(PointF pos, boolean clampToSegment) {
        PointF other = new PointF(pos.x - vertices[0], pos.y - vertices[1]);
        PointF me = new PointF(vertices[3] - vertices[0], vertices[4] - vertices[1]);

        float otherLen = other.length();
        float myLen = me.length();

        float dotP = (me.x * other.x + me.y * other.y);
        dotP /= (myLen * otherLen);

        float newLen = dotP * otherLen;
        if (clampToSegment) {
            if (newLen < 0.0f) {
                newLen = 0.0f;
            } else if (newLen > myLen) {
                newLen = myLen;
            }
        }

        float projX = me.x / myLen * newLen + vertices[0];
        float projY = me.y / myLen * newLen + vertices[1];

        return new PointF(projX, projY);
    }
}

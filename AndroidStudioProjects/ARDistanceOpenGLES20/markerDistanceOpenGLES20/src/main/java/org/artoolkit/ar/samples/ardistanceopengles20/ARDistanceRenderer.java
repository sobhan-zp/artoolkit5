/*
 *  ARActivity.java
 *  ARToolKit5
 *
 *  This file is part of ARToolKit.
 *
 *  ARToolKit is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ARToolKit is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with ARToolKit.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  As a special exception, the copyright holders of this library give you
 *  permission to link this library with independent modules to produce an
 *  executable, regardless of the license terms of these independent modules, and to
 *  copy and distribute the resulting executable under terms of your choice,
 *  provided that you also meet, for each linked independent module, the terms and
 *  conditions of the license of that module. An independent module is a module
 *  which is neither derived from nor based on this library. If you modify this
 *  library, you may extend this exception to your version of the library, but you
 *  are not obligated to do so. If you do not wish to do so, delete this exception
 *  statement from your version.
 *
 *  Copyright 2015 Daqri, LLC.
 *  Copyright 2011-2015 ARToolworks, Inc.
 *
 *  Author(s): Julian Looser, Philip Lamb
 *
 */
package org.artoolkit.ar.samples.ardistanceopengles20;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import org.artoolkit.ar.base.ARToolKit;
import org.artoolkit.ar.base.rendering.Line;
import org.artoolkit.ar.base.rendering.gles20.ARDrawableOpenGLES20;
import org.artoolkit.ar.base.rendering.gles20.ARRendererGLES20;
import org.artoolkit.ar.base.rendering.gles20.BaseVertexShader;
import org.artoolkit.ar.base.rendering.gles20.LineGLES20;
import org.artoolkit.ar.base.rendering.gles20.ShaderProgram;
import org.artoolkit.ar.samples.ardistanceopengles20.shader.MarkerDistanceFragmentShader;
import org.artoolkit.ar.samples.ardistanceopengles20.shader.MarkerDistanceShaderProgram;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import gltext3des20.GLText;

/**
 * Created by Thorsten Bux on 25.01.2016.
 */
public class ARDistanceRenderer extends ARRendererGLES20{

    private static final String TAG = "ARDistanceRenderer";
    private final Context context;
    private ARToolKit arToolKit;
    private int markerId2;
    private int markerId1;
    private AbstractCollection<Integer> markerArray = new ArrayList<>();
    ARDrawableOpenGLES20 line;
    private GLText glText;

    public ARDistanceRenderer(Context context) {
        this.context = context;
    }

    @Override
    public boolean configureARScene() {

        arToolKit = ARToolKit.getInstance();
        markerId2 = arToolKit.addMarker("single;Data/cat.patt;80");
        if (markerId2 < 0) {
            Log.e(TAG, "Unable to load marker 2");
            return false;
        }
        markerId1 = arToolKit.addMarker("single;Data/minion.patt;80");
        if (markerId1 < 0) {
            Log.e(TAG, "Unable to load marker 1");
            return false;
        }

        markerArray.add(markerId1);
        markerArray.add(markerId2);
        arToolKit.setBorderSize(0.1f);
        Log.i(TAG, "Border size: " + arToolKit.getBorderSize());

        return true;
    }

    @Override
    public void draw() {
        super.draw();

        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glFrontFace(GLES20.GL_CW);

        Map<Integer, float[]> transformationMatrixPerVisibleMarker = storeTransformationMatrixPerVisibleMarker();

        if(transformationMatrixPerVisibleMarker.size() > 1 ){

            float[] positionMarker2 = arToolKit.retrievePosition(markerId1, markerId2);

            //Draw line from referenceMarker to another marker
            //In relation to the second marker the referenceMarker is on position 0/0/0
            float[] basePosition = {0f, 0f, 0f, 1f};

            if(positionMarker2 != null) {
                ((Line) line).setStart(basePosition);
                ((Line) line).setEnd(positionMarker2);
                float[] color = {0.38f, 0.757f, 0.761f,1};
                ((Line) line).setColor(color);

                line.draw(arToolKit.getProjectionMatrix(), transformationMatrixPerVisibleMarker.get(markerId1));


                // TEST: render the entire font texture
                //glText.drawTexture(100 / 2, 100 / 2, arToolKit.getProjectionMatrix());            // Draw the Entire Texture

                // TEST: render some strings with the font
                glText.begin(1.0f, 0.0f, 0.0f, 1.0f, arToolKit.getProjectionMatrix());         // Begin Text Rendering (Set Color WHITE)
                glText.draw("1", transformationMatrixPerVisibleMarker.get(markerId1));
                //glText.drawC("Test String 3D!", 0f, 0f, 0f, 0, -30, 0);
//		glText.drawC( "Test String :)", 0, 0, 0 );          // Draw Test String
                //glText.draw( "Diagonal 1", 40, 40, 40);                // Draw Test String
                //glText.draw( "Column 1", 100, 100, 90);              // Draw Test String
                glText.end();                                   // End Text Rendering

                //glText.begin( 0.0f, 0.0f, 1.0f, 1.0f, arToolKit.getProjectionMatrix() );         // Begin Text Rendering (Set Color BLUE)
                //glText.draw( "More Lines...", 50, 200 );        // Draw Test String
                //glText.draw( "The End.", 50, 200 + glText.getCharHeight(), 180);  // Draw Test String
                //glText.end();                                   // End Text Rendering
            }
        }
    }

    //Shader calls should be within a GL thread that is onSurfaceChanged(), onSurfaceCreated() or onDrawFrame()
    //As the cube instantiates the shader during constructor call we need to do create the cube here.
    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        super.onSurfaceCreated(unused, config);

        int lineWidth = 3;
        ShaderProgram shaderProgram = new MarkerDistanceShaderProgram(new BaseVertexShader(),new MarkerDistanceFragmentShader(),lineWidth);

        line = new LineGLES20(lineWidth);
        line.setShaderProgram(shaderProgram);

        // Create the GLText
        glText = new GLText(context.getAssets());

        // Load the font from file (set size + padding), creates the texture
        // NOTE: after a successful call to this the font is ready for rendering!
        glText.load("Roboto-Regular.ttf", 14, 2, 2);  // Create Font (Height: 14 Pixels / X+Y Padding 2 Pixels)

        // enable texture + alpha blending
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private Map<Integer, float[]> storeTransformationMatrixPerVisibleMarker() {
        Map<Integer, float[]> transformationArray = new HashMap<>();

        for (int markerId : markerArray) {
            if (arToolKit.queryMarkerVisible(markerId)) {

                float[] transformation = arToolKit.queryMarkerTransformation(markerId);

                if (transformation != null) {
                    transformationArray.put(markerId, transformation);
                    Log.d(TAG, "Found Marker " + markerId + " with transformation " + Arrays.toString(transformation));
                }
            } else {
                transformationArray.remove(markerId);
            }

        }
        return transformationArray;
    }
}

package org.polyfrost.polydium.client;

import java.nio.FloatBuffer;
import net.minecraft.client.render.pipeline.RenderTarget;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.DefaultVertexFormat;
import net.minecraft.client.render.vertex.Tesselator;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

final class HudTiles {
    private static final int FIRST_STEP = 8;
    private static final int SECOND_STEP = 4;
    private static final int TILE = FIRST_STEP * SECOND_STEP;

    private static final String REDUCE_VERTEX = "#version 120\n"
            + "void main() { gl_Position = gl_Vertex; }\n";
    private static final String COMPOSITE_VERTEX = "#version 120\n"
            + "uniform sampler2D mask;\n"
            + "uniform vec2 tiles;\n"
            + "uniform vec2 scale;\n"
            + "void main() {\n"
            + "    vec2 uv = min(gl_Vertex.xy * scale, 1.0);\n"
            + "    gl_TexCoord[0] = vec4(uv, 0.0, 1.0);\n"
            + "    bool used = texture2DLod(mask, (gl_Vertex.zw + 0.5) / tiles, 0.0).a > 0.0;\n"
            + "    gl_Position = used ? vec4(uv * 2.0 - 1.0, 0.0, 1.0) : vec4(-2.0, -2.0, 0.0, 1.0);\n"
            + "}\n";
    private static final String COMPOSITE_FRAGMENT = "#version 120\n"
            + "uniform sampler2D hud;\n"
            + "void main() { gl_FragColor = texture2D(hud, gl_TexCoord[0].xy); }\n";

    private static boolean failed;
    private static int firstProgram;
    private static int firstTexel;
    private static int secondProgram;
    private static int secondTexel;
    private static int compositeProgram;
    private static int compositeTiles;
    private static int compositeScale;
    private static RenderTarget partial;
    private static RenderTarget mask;
    private static int grid;
    private static int columns;
    private static int rows;
    private static int width;
    private static int height;

    private HudTiles() {
    }

    static boolean update(RenderTarget hud) {
        if (failed || !init()) return false;
        if (hud.width != width || hud.height != height) {
            resize(hud.width, hud.height);
        }

        GlStateManager.activeTexture(GL13.GL_TEXTURE0);
        boolean blend = GlStateManager.BLEND.state.enabled;
        boolean alphaTest = GlStateManager.ALPHA_TEST.state.enabled;
        GlStateManager.disableBlend();
        GlStateManager.disableAlphaTest();
        reduce(hud, partial, firstProgram, firstTexel);
        reduce(partial, mask, secondProgram, secondTexel);
        GL20.glUseProgram(0);
        if (blend) GlStateManager.enableBlend();
        if (alphaTest) GlStateManager.enableAlphaTest();
        return true;
    }

    private static void reduce(RenderTarget source, RenderTarget destination, int program, int texel) {
        destination.bindWrite(true);
        GlStateManager.bindTexture(source.colorTextureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL20.glUseProgram(program);
        GL20.glUniform2f(texel, 1.0F / source.width, 1.0F / source.height);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION);
        buffer.vertex(-1.0, -1.0, 0.0).nextVertex();
        buffer.vertex(1.0, -1.0, 0.0).nextVertex();
        buffer.vertex(1.0, 1.0, 0.0).nextVertex();
        buffer.vertex(-1.0, 1.0, 0.0).nextVertex();
        tesselator.end();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    static void draw(RenderTarget hud) {
        GlStateManager.activeTexture(GL13.GL_TEXTURE2);
        GlStateManager.bindTexture(mask.colorTextureId);
        GlStateManager.activeTexture(GL13.GL_TEXTURE0);
        GlStateManager.bindTexture(hud.colorTextureId);
        GL20.glUseProgram(compositeProgram);
        GL20.glUniform2f(compositeTiles, columns, rows);
        GL20.glUniform2f(compositeScale, (float) TILE / width, (float) TILE / height);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, grid);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(4, GL11.GL_FLOAT, 0, 0L);
        GL11.glDrawArrays(GL11.GL_QUADS, 0, columns * rows * 4);
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL20.glUseProgram(0);
    }

    private static boolean init() {
        if (firstProgram != 0) return true;
        try {
            if (!GLContext.getCapabilities().OpenGL21 || GL11.glGetInteger(GL20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS) <= 0) {
                throw new IllegalStateException("vertex texture fetch is not supported");
            }
            firstProgram = link(REDUCE_VERTEX, reduceFragment(FIRST_STEP));
            firstTexel = GL20.glGetUniformLocation(firstProgram, "texel");
            secondProgram = link(REDUCE_VERTEX, reduceFragment(SECOND_STEP));
            secondTexel = GL20.glGetUniformLocation(secondProgram, "texel");
            compositeProgram = link(COMPOSITE_VERTEX, COMPOSITE_FRAGMENT);
            GL20.glUseProgram(compositeProgram);
            GL20.glUniform1i(GL20.glGetUniformLocation(compositeProgram, "hud"), 0);
            GL20.glUniform1i(GL20.glGetUniformLocation(compositeProgram, "mask"), 2);
            compositeTiles = GL20.glGetUniformLocation(compositeProgram, "tiles");
            compositeScale = GL20.glGetUniformLocation(compositeProgram, "scale");
            GL20.glUseProgram(0);
            grid = GL15.glGenBuffers();
            return true;
        } catch (RuntimeException e) {
            GL20.glUseProgram(0);
            LogManager.getLogger("Polydium").error("Could not set up tiled HUD compositing, compositing the full screen", e);
            failed = true;
            return false;
        }
    }

    private static String reduceFragment(int step) {
        return "#version 120\n"
                + "uniform sampler2D source;\n"
                + "uniform vec2 texel;\n"
                + "void main() {\n"
                + "    vec2 base = floor(gl_FragCoord.xy) * " + step + ".0 + 1.0;\n"
                + "    float alpha = 0.0;\n"
                + "    for (int y = 0; y < " + step + "; y += 2) {\n"
                + "        for (int x = 0; x < " + step + "; x += 2) {\n"
                + "            vec4 color = texture2D(source, (base + vec2(x, y)) * texel);\n"
                + "            alpha = max(alpha, max(max(color.r, color.g), max(color.b, color.a)));\n"
                + "        }\n"
                + "    }\n"
                + "    gl_FragColor = vec4(alpha > 0.0 ? 1.0 : 0.0);\n"
                + "}\n";
    }

    private static int link(String vertexSource, String fragmentSource) {
        int program = GL20.glCreateProgram();
        int vertex = compile(GL20.GL_VERTEX_SHADER, vertexSource);
        int fragment = compile(GL20.GL_FRAGMENT_SHADER, fragmentSource);
        GL20.glAttachShader(program, vertex);
        GL20.glAttachShader(program, fragment);
        GL20.glLinkProgram(program);
        GL20.glDetachShader(program, vertex);
        GL20.glDetachShader(program, fragment);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new IllegalStateException(GL20.glGetProgramInfoLog(program, 4096));
        }
        return program;
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new IllegalStateException(GL20.glGetShaderInfoLog(shader, 4096));
        }
        return shader;
    }

    private static void resize(int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;
        columns = (width + TILE - 1) / TILE;
        rows = (height + TILE - 1) / TILE;
        int partialWidth = (width + FIRST_STEP - 1) / FIRST_STEP;
        int partialHeight = (height + FIRST_STEP - 1) / FIRST_STEP;
        if (mask == null) {
            partial = new RenderTarget(partialWidth, partialHeight, false);
            mask = new RenderTarget(columns, rows, false);
        } else {
            partial.resize(partialWidth, partialHeight);
            mask.resize(columns, rows);
        }

        FloatBuffer vertices = BufferUtils.createFloatBuffer(columns * rows * 16);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                vertices.put(column).put(row).put(column).put(row);
                vertices.put(column + 1).put(row).put(column).put(row);
                vertices.put(column + 1).put(row + 1).put(column).put(row);
                vertices.put(column).put(row + 1).put(column).put(row);
            }
        }
        vertices.flip();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, grid);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }
}

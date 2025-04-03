import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class TrueTypeFont {
    private Font font;
    private Map<Character, Integer> charTextures = new HashMap<>();
    private Map<Character, Integer> charWidths = new HashMap<>();
    private int fontHeight;

    public TrueTypeFont(Font font, boolean antiAlias) {
        this.font = font;
        buildFont(antiAlias);
    }

    private void buildFont(boolean antiAlias) {
        BufferedImage img = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setFont(font);
        if (antiAlias) g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        FontMetrics fm = g.getFontMetrics();

        fontHeight = fm.getHeight();

        for (char c = 32; c < 127; c++) {
            int charWidth = fm.charWidth(c);
            BufferedImage charImage = new BufferedImage(charWidth, fontHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D cg = charImage.createGraphics();
            cg.setFont(font);
            if (antiAlias) cg.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            cg.drawString(String.valueOf(c), 0, fm.getAscent());

            int[] pixels = charImage.getRGB(0, 0, charWidth, fontHeight, null, 0, charWidth);
            ByteBuffer buffer = BufferUtils.createByteBuffer(charWidth * fontHeight * 4);
            for (int y = 0; y < fontHeight; y++) {
                for (int x = 0; x < charWidth; x++) {
                    int pixel = pixels[y * charWidth + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                    buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                    buffer.put((byte) (pixel & 0xFF));         // Blue
                    buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
                }
            }
            buffer.flip();

            int texId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, charWidth, fontHeight, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

            charTextures.put(c, texId);
            charWidths.put(c, charWidth);
        }
    }

    public void drawString(float x, float y, String text) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor3f(1f, 1f, 1f); // Set text color to white

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            Integer texId = charTextures.get(c);
            Integer w = charWidths.get(c);
            if (texId == null || w == null) continue;

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex2f(x, y);
            GL11.glTexCoord2f(1, 0);
            GL11.glVertex2f(x + w, y);
            GL11.glTexCoord2f(1, 1);
            GL11.glVertex2f(x + w, y + fontHeight);
            GL11.glTexCoord2f(0, 1);
            GL11.glVertex2f(x, y + fontHeight);
            GL11.glEnd();

            x += w;
        }

        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }
    
    public int getCharWidth(char c) {
        Integer w = charWidths.get(c);
        return (w != null) ? w : 0;
    }
    
    public int getHeight() {
        return fontHeight;
    }
}

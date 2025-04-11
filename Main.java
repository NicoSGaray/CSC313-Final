import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import static org.lwjgl.glfw.GLFW.*;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class Main {

    private long window;
    private int width = 800;
    private int height = 600;
    private Terrain terrain;

    public int beanCount = 3;
    private List<BeanCharacter> beans;
    private int currBean = 0;
    private boolean tabPressed = false;

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        init();
        loop();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    private void init() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        window = GLFW.glfwCreateWindow(width, height, "Bean Simulation", 0, 0);
        if (window == 0) {
            throw new IllegalStateException("Failed to create the GLFW window");
        }
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        setPerspectiveProjection(45.0f, (float) 800 / (float) 600, 0.1f, 100.0f);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        initLighting();

        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);

        FloatBuffer lightPosition = BufferUtils.createFloatBuffer(4).put(new float[] { 0.0f, 10.0f, 10.0f, 1.0f });
        lightPosition.flip();
        GL11.glLightfv(GL11.GL_LIGHT0, GL11.GL_POSITION, lightPosition);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);

        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        beans = new ArrayList<>();
        for (int i = 0; i < beanCount; i++) {
            BeanCharacter bean = new BeanCharacter();
            bean.setPosition((float) (Math.random() * 30), 0.0f, (float) (Math.random() * 30));
            beans.add(bean);
        }

        terrain = new Terrain("terrain.obj");
    }

    private void loop() {
        while (!GLFW.glfwWindowShouldClose(window)) {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glLoadIdentity();

            updateBeanMovement();

            BeanCharacter activeBean = beans.get(currBean);
            updateCamera(activeBean);

            terrain.render();
            for (int i = 0; i < beans.size(); i++) {
                BeanCharacter bean = beans.get(i);
                bean.update(terrain);
                bean.render(terrain, i);
            }

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }


    public void initLighting() {
        // Enable lighting and the first light
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_LIGHT0);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);

        // Set the light position
        FloatBuffer lightPosition = BufferUtils.createFloatBuffer(4).put(new float[] { 0.0f, 10.0f, 10.0f, 1.0f });
        lightPosition.flip();
        GL11.glLightfv(GL11.GL_LIGHT0, GL11.GL_POSITION, lightPosition);

        // Set the brighter ambient, diffuse, and specular light
        FloatBuffer ambientLight = BufferUtils.createFloatBuffer(4).put(new float[] { 0.4f, 0.4f, 0.4f, 1.0f });
        ambientLight.flip();
        GL11.glLightfv(GL11.GL_LIGHT0, GL11.GL_AMBIENT, ambientLight);

        FloatBuffer diffuseLight = BufferUtils.createFloatBuffer(4).put(new float[] { 1.0f, 1.0f, 1.0f, 1.0f });
        diffuseLight.flip();
        GL11.glLightfv(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, diffuseLight);

        FloatBuffer specularLight = BufferUtils.createFloatBuffer(4).put(new float[] { 1.0f, 1.0f, 1.0f, 1.0f });
        // Increase specular highlight
        specularLight.flip();
        GL11.glLightfv(GL11.GL_LIGHT0, GL11.GL_SPECULAR, specularLight);

        // Enable color material to allow vertex colors with lighting
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);

        // Set material properties
        FloatBuffer materialAmbient = BufferUtils.createFloatBuffer(4).put(new float[] { 0.6f, 0.6f, 0.6f, 1.0f });
        materialAmbient.flip();
        GL11.glMaterialfv(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT, materialAmbient);

        FloatBuffer materialDiffuse = BufferUtils.createFloatBuffer(4).put(new float[] { 0.8f, 0.8f, 0.8f, 1.0f });
        // Brighter diffuse reflection
        materialDiffuse.flip();
        GL11.glMaterialfv(GL11.GL_FRONT_AND_BACK, GL11.GL_DIFFUSE, materialDiffuse);

        FloatBuffer materialSpecular = BufferUtils.createFloatBuffer(4).put(new float[] { 1.0f, 1.0f, 1.0f, 1.0f });
        // Specular highlight
        materialSpecular.flip();
        GL11.glMaterialfv(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, materialSpecular);

        GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 50.0f);
        // Set shininess (higher = more specular reflection)

        // Set global ambient light
        FloatBuffer globalAmbient = BufferUtils.createFloatBuffer(4).put(new float[] { 0.5f, 0.5f, 0.5f, 1.0f });
        globalAmbient.flip();
        GL11.glLightModelfv(GL11.GL_LIGHT_MODEL_AMBIENT, globalAmbient);
    }

    private void setPerspectiveProjection(float fov, float aspect, float zNear, float zFar) {
        float ymax = (float) (zNear * Math.tan(Math.toRadians(fov) / 2.0));
        float xmax = ymax * aspect;

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glFrustum(-xmax, xmax, -ymax, ymax, zNear, zFar);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    public void setupCamera() {
        // Position the camera behind the car, following it
        GL11.glTranslatef(0, -5, -20); // Adjust this for better view
        GL11.glRotatef(20, 1, 0, 0); // slight downward angle
    }

    private float lerp(float start, float end, float alpha) {
        return start + alpha * (end - start);
    }

    private float cameraX = 0;
    private float cameraY = 5;
    private float cameraZ = 10;


    private void updateCamera(BeanCharacter bean) {
      float cameraDistance = 8.0f;
      float cameraHeight = 4.0f;

      float targetCameraX = bean.getX() - (float) (Math.sin(Math.toRadians(bean.getAngle())) * cameraDistance);
      float targetCameraZ = bean.getZ() - (float) (Math.cos(Math.toRadians(bean.getAngle())) * cameraDistance);
      float targetCameraY = bean.getY() + cameraHeight;

      float alpha = 0.1f;
      cameraX = lerp(cameraX, targetCameraX, alpha);
      cameraY = lerp(cameraY, targetCameraY, alpha);
      cameraZ = lerp(cameraZ, targetCameraZ, alpha);

      GL11.glLoadIdentity();
      GL11.glRotatef((float) Math.toDegrees(Math.atan2(bean.getX() - cameraX, bean.getZ() - cameraZ)), 0, 1, 0);
      GL11.glTranslatef(-cameraX, -cameraY, -cameraZ);
  }

  private void updateBeanMovement() {
      if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_TAB) == GLFW.GLFW_PRESS) {
          if (!tabPressed) {
              currBean = (currBean + 1) % beans.size();
              tabPressed = true;
          }
      } else {
          tabPressed = false;
      }

      BeanCharacter activeBean = beans.get(currBean);
      if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
          activeBean.moveForward();
      }
      if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
          activeBean.moveBackward();
      }
      if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
          activeBean.turnLeft();
      }
      if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
          activeBean.turnRight();
      }
      if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS) {
          activeBean.shove();
      }
  }

  public static class BeanCharacter {
      private float x = 0, y = 0, z = 0;
      private float angle = 0;
      private float speed = 0;
      private float maxSpeed = 0.15f;
      private float acceleration = 0.02f;
      private float friction = 0.92f;
      private float turnSpeed = 3.0f;
      private float shoveForce = 0.4f;
      private float shoveCooldown = 0;

      public float getX() { return x; }
      public float getY() { return y; }
      public float getZ() { return z; }
      public float getAngle() { return angle; }

      public void setPosition(float x, float y, float z) {
          this.x = x;
          this.y = y;
          this.z = z;
      }

      public void moveForward() {
          if (speed < maxSpeed) speed += acceleration;
      }

      public void moveBackward() {
          if (speed > -maxSpeed/2) speed -= acceleration/2;
      }

      public void turnLeft() {
          angle += turnSpeed;
      }

      public void turnRight() {
          angle -= turnSpeed;
      }

      public void shove() {
          if (shoveCooldown <= 0) {
              speed += shoveForce;
              shoveCooldown = 1.0f;
          }
      }

      public void update(Terrain terrain) {
          x += speed * Math.sin(Math.toRadians(angle));
          z += speed * Math.cos(Math.toRadians(angle));
          y = terrain.getTerrainHeightAt(x, z) + 0.5f;
          
          speed *= friction;
          if (shoveCooldown > 0) shoveCooldown -= 0.02f;
      }

      public void render(Terrain terrain, int beanNumber) {
          GL11.glPushMatrix();
          GL11.glTranslatef(x, y, z);
          GL11.glRotatef(angle, 0, 1, 0);

          // Bean body
          GL11.glColor3f(0.9f, 0.7f, 0.1f);
          if (beanNumber % 3 == 0) {
              GL11.glColor3f(0.8f, 0.2f, 0.2f);
          } else if (beanNumber % 3 == 1) {
              GL11.glColor3f(0.2f, 0.8f, 0.2f);
          } else {
              GL11.glColor3f(0.2f, 0.2f, 0.8f);
          }

          // Main body
          GL11.glPushMatrix();
          GL11.glScalef(0.8f, 1.2f, 0.8f);
          drawSphere(0.5f);
          GL11.glPopMatrix();

          // Eyes
          GL11.glPushMatrix();
          GL11.glColor3f(1, 1, 1);
          GL11.glTranslatef(0.2f, 0.4f, 0.4f);
          drawSphere(0.15f);
          GL11.glTranslatef(-0.4f, 0, 0);
          drawSphere(0.15f);
          GL11.glPopMatrix();

          // Arms
          GL11.glPushMatrix();
          GL11.glColor3f(0.9f, 0.7f, 0.1f);
          GL11.glRotatef(30, 0, 0, 1);
          GL11.glTranslatef(0.6f, -0.2f, 0);
          GL11.glScalef(0.8f, 0.3f, 0.3f);
          drawSphere(0.3f);
          GL11.glPopMatrix();

          GL11.glPushMatrix();
          GL11.glRotatef(-30, 0, 0, 1);
          GL11.glTranslatef(-0.6f, -0.2f, 0);
          GL11.glScalef(0.8f, 0.3f, 0.3f);
          drawSphere(0.3f);
          GL11.glPopMatrix();

          GL11.glPopMatrix();
      }

      private void drawSphere(float radius) {
          GL11.glShadeModel(GL11.GL_SMOOTH);
          GL11.glBegin(GL11.GL_QUAD_STRIP);
          for (int i = 0; i <= 360; i += 10) {
              float theta = (float) Math.toRadians(i);
              float nextTheta = (float) Math.toRadians(i + 10);
              
              for (int j = 0; j <= 180; j += 10) {
                  float phi = (float) Math.toRadians(j);
                  float x = (float) (radius * Math.cos(theta) * Math.sin(phi));
                  float y = (float) (radius * Math.cos(phi));
                  float z = (float) (radius * Math.sin(theta) * Math.sin(phi));
                  GL11.glNormal3f(x, y, z);
                  GL11.glVertex3f(x, y, z);
                  
                  x = (float) (radius * Math.cos(nextTheta) * Math.sin(phi));
                  z = (float) (radius * Math.sin(nextTheta) * Math.sin(phi));
                  GL11.glNormal3f(x, y, z);
                  GL11.glVertex3f(x, y, z);
              }
          }
          GL11.glEnd();
      }
  }
    public static class OBJLoader {
        public Model loadModel(String fileName) throws IOException {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line;
            List<float[]> vertices = new ArrayList<>();
            List<float[]> normals = new ArrayList<>();
            List<int[]> faces = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\\s");
                if (tokens[0].equals("v")) {
                    float[] vertex = { Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]),
                            Float.parseFloat(tokens[3]) };
                    vertices.add(vertex);
                } else if (tokens[0].equals("vn")) {
                    float[] normal = { Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]),
                            Float.parseFloat(tokens[3]) };
                    normals.add(normal);
                } else if (tokens[0].equals("f")) {
                    int[] face = { Integer.parseInt(tokens[1].split("/")[0]) - 1,
                            Integer.parseInt(tokens[2].split("/")[0]) - 1,
                            Integer.parseInt(tokens[3].split("/")[0]) - 1 };
                    faces.add(face);
                }
            }

            float[] verticesArray = new float[vertices.size() * 3];
            float[] normalsArray = new float[normals.size() * 3];
            int[] indicesArray = new int[faces.size() * 3];

            int vertexIndex = 0;
            for (float[] vertex : vertices) {
                verticesArray[vertexIndex++] = vertex[0];
                verticesArray[vertexIndex++] = vertex[1];
                verticesArray[vertexIndex++] = vertex[2];
            }

            int normalIndex = 0;
            for (float[] normal : normals) {
                normalsArray[normalIndex++] = normal[0];
                normalsArray[normalIndex++] = normal[1];
                normalsArray[normalIndex++] = normal[2];
            }

            int faceIndex = 0;
            for (int[] face : faces) {
                indicesArray[faceIndex++] = face[0];
                indicesArray[faceIndex++] = face[1];
                indicesArray[faceIndex++] = face[2];
            }

            reader.close();
            System.out.println(
                    "Model loaded successfully with " + vertices.size() + " vertices and " + faces.size()
                            + " faces and "
                            + normals.size() + " normals.");
            return new Model(verticesArray, normalsArray, indicesArray);
        }
    }

    public static class Model {
        private float[] vertices;
        private float[] normals;
        private int[] indices;

        public Model(float[] vertices, float[] normals, int[] indices) {
            this.vertices = vertices;
            this.normals = normals;
            this.indices = indices;
        }

        public float[] getVertices() {
            return vertices;
        }

        public float[] getNormals() {
            return normals;
        }

        public int[] getIndices() {
            return indices;
        }
    }

    public static class Terrain {
        private Model model;

        public Terrain(String objFilePath) {
            try {
                OBJLoader objLoader = new OBJLoader();
                this.model = objLoader.loadModel(objFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void render() {
            GL11.glColor3f(0.3f, 0.8f, 0.3f); // Lighter green for the terrain
            GL11.glShadeModel(GL11.GL_SMOOTH); // Smooth shading for better Phong effect

            // Adjust terrain material properties to make it brighter
            FloatBuffer terrainAmbient = BufferUtils.createFloatBuffer(4).put(new float[] { 0.6f, 0.8f, 0.6f, 1.0f });
            // Higher ambient light reflection
            FloatBuffer terrainDiffuse = BufferUtils.createFloatBuffer(4).put(new float[] { 0.7f, 0.9f, 0.7f, 1.0f });
            // Higher diffuse light reflection for visibility
            FloatBuffer terrainSpecular = BufferUtils.createFloatBuffer(4).put(new float[] { 0.2f, 0.2f, 0.2f, 1.0f });
            // Light specular highlight for subtle shine

            terrainAmbient.flip();
            terrainDiffuse.flip();
            terrainSpecular.flip();

            GL11.glMaterialfv(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT, terrainAmbient);
            GL11.glMaterialfv(GL11.GL_FRONT_AND_BACK, GL11.GL_DIFFUSE, terrainDiffuse);
            GL11.glMaterialfv(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, terrainSpecular);
            GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 10.0f); // Lower shininess for a more matte look

            float[] vertices = model.getVertices();
            float[] normals = model.getNormals();
            int[] indices = model.getIndices();

            GL11.glBegin(GL11.GL_TRIANGLES);
            for (int i = 0; i < indices.length; i += 3) {
                int vIndex1 = indices[i] * 3;
                int vIndex2 = indices[i + 1] * 3;
                int vIndex3 = indices[i + 2] * 3;
                GL11.glNormal3f(normals[vIndex1], normals[vIndex1 + 1], normals[vIndex1 + 2]);
                GL11.glVertex3f(vertices[vIndex1], vertices[vIndex1 + 1], vertices[vIndex1 + 2]);

                GL11.glNormal3f(normals[vIndex2], normals[vIndex2 + 1], normals[vIndex2 + 2]);
                GL11.glVertex3f(vertices[vIndex2], vertices[vIndex2 + 1], vertices[vIndex2 + 2]);

                GL11.glNormal3f(normals[vIndex3], normals[vIndex3 + 1], normals[vIndex3 + 2]);
                GL11.glVertex3f(vertices[vIndex3], vertices[vIndex3 + 1], vertices[vIndex3 + 2]);
            }
            GL11.glEnd();
        }

        public float getTerrainHeightAt(float x, float z) {
            float[] vertices = model.getVertices(); // Get the terrain vertices
            int[] indices = model.getIndices(); // Get the triangle indexes

            // Iterate therough all triangles in the terrain mesh
            for (int i = 0; i < indices.length; i += 3) {

                // Get the vertices of the triangle
                int vIndex1 = indices[i] * 3;
                int vIndex2 = indices[i + 1] * 3;
                int vIndex3 = indices[i + 2] * 3;

                // Vertices of the traingle
                float v1X = vertices[vIndex1];
                float v1Y = vertices[vIndex1 + 1]; // the height at vertex 1
                float v1Z = vertices[vIndex1 + 2];

                float v2X = vertices[vIndex2];
                float v2Y = vertices[vIndex2 + 1]; // he height at vertex 2
                float v2Z = vertices[vIndex2 + 2];

                float v3X = vertices[vIndex3];
                float v3Y = vertices[vIndex3 + 1]; // he height at vertex 3
                float v3Z = vertices[vIndex3 + 2];

                // Check if the point (x, z) is in the triangle
                if (isPointInTriangle(x, z, v1X, v1Z, v2X, v2Z, v3X, v3Z)) {
                    // If the point is in th etriangle, calculate the height using barycentric
                    // interpolation
                    return interpolateHeight(x, z, v1X, v1Y, v1Z, v2X, v2Y, v2Z, v3X, v3Y, v3Z);
                }

            }

            // If no triangle was found, return 0 as a default
            return 0.0f;
        }

        private boolean isPointInTriangle(float px, float pz, float v1X, float v1Z, float v2X, float v2Z, float v3X,
                float v3Z) {
            float d1 = sign(px, pz, v1X, v1Z, v2X, v2Z);
            float d2 = sign(px, pz, v2X, v2Z, v3X, v3Z);
            float d3 = sign(px, pz, v3X, v3Z, v1X, v1Z);

            boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
            boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);

            return !(hasNeg && hasPos); // Point is inside the triangle if all signs are the same
        }

        private float sign(float px, float pz, float v1X, float v1Z, float v2X, float v2Z) {
            return (px - v2X) * (v1Z - v2Z) - (v1X - v2X) * (pz - v2Z);
        }

        private float interpolateHeight(float x, float z, float v1X, float v1Y, float v1Z, float v2X, float v2Y,
                float v2Z,
                float v3X, float v3Y, float v3Z) {
            // Calculate the areas needed for barycentric interpolation
            float areaTotal = triangleArea(v1X, v1Z, v2X, v2Z, v3X, v3Z);
            float area1 = triangleArea(x, z, v2X, v2Z, v3X, v3Z);
            float area2 = triangleArea(x, z, v3X, v3Z, v1X, v1Z);
            float area3 = triangleArea(x, z, v1X, v1Z, v2X, v2Z);

            // Calculate the barycentric weights
            float weight1 = area1 / areaTotal;
            float weight2 = area2 / areaTotal;
            float weight3 = area3 / areaTotal;

            // Interpolate the height using the weights
            return weight1 * v1Y + weight2 * v2Y + weight3 * v3Y;
        }

        private float triangleArea(float x1, float z1, float x2, float z2, float x3, float z3) {
            return Math.abs((x1 * (z2 - z3) + x2 * (z3 - z1) + x3 * (z1 - z2)) / 2.0f);
        }
    }
}
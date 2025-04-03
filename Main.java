/*
javac -classpath ".;C:\Program Files\lwjgl-release-3.3.6-custom\*" Main.java TrueTypeFont.java
java -classpath ".;\Program Files\lwjgl-release-3.3.6-custom\*" Main
*/

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import java.awt.Font;

enum GameState {
  MENU,
  PLAYING
}

enum MenuState {
  MAIN_MENU,
  HOSTING,
  JOINING
}

public class Main {
  private long window;
  private int width = 800;
  private int height = 600;
  private Car car;
  private Terrain terrain;
  private List<Car> cars = new ArrayList<>();
  private int activeCarIndex = 0;

  private MenuState menuState = MenuState.MAIN_MENU;
  private String hostIp = "Detecting...";
  private List<String> playerList = new ArrayList<>();
  private String typedIp = "";
  private boolean typingActive = false;
  private ServerThread serverThread;

  public static void main(String[] args) {
    new Main().run();
  }

  private GameState gameState = GameState.MENU;

  public void run() {
    init();
    loop();
    GLFW.glfwDestroyWindow(window);
    GLFW.glfwTerminate();
  }

  private TrueTypeFont font;

  private void init() {
    if (!GLFW.glfwInit()) {
      throw new IllegalStateException("Unable to initialize GLEW");
    }

    window = GLFW.glfwCreateWindow(width, height, "Car Simulation", 0, 0);
    if (window == 0) {
      throw new RuntimeException("Failed to create the GLEW window");
    }

    GLFW.glfwMakeContextCurrent(window);
    GL.createCapabilities();

    GLFW.glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
      if (menuState == MenuState.JOINING && action == GLFW.GLFW_PRESS) {
          if (key == GLFW.GLFW_KEY_ENTER) {
            System.out.println("Joining server at " + typedIp + "...");
            new ClientThread(typedIp).start();
            typedIp = ""; // Clear for next time
            menuState = MenuState.MAIN_MENU; // Optional: return to menu after joining
          } else if (key == GLFW.GLFW_KEY_BACKSPACE && typedIp.length() > 0) {
            typedIp = typedIp.substring(0, typedIp.length() - 1);
          } else {
              // Limit to numbers, dots
              if ((key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) || key == GLFW.GLFW_KEY_PERIOD) {
                char c = (char) key;
                if (mods == GLFW.GLFW_MOD_SHIFT && key == GLFW.GLFW_KEY_PERIOD) c = '.'; // handle period
                else if (key == GLFW.GLFW_KEY_PERIOD) c = '.';
                else c = (char) ('0' + (key - GLFW.GLFW_KEY_0));
                typedIp += c;
              }
            }
        }
      });
  
    Font awtFont = new Font("Arial", Font.BOLD, 24);
    font = new TrueTypeFont(awtFont, true);

    GL11.glMatrixMode(GL11.GL_PROJECTION);
    GL11.glLoadIdentity();
    setPerspectiveProjection(45.0f, (float) 800 / 600, 0.1f, 100.0f);
    GL11.glMatrixMode(GL11.GL_MODELVIEW);

    initLighting();

    GL11.glEnable(GL11.GL_COLOR_MATERIAL);
    GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);

    // Define light properties
    FloatBuffer lightPosition = BufferUtils.createFloatBuffer(4).put(new float[] { 0.0f, 10.0f, 10.0f, 1.0f });
    lightPosition.flip();
    GL11.glLightfv(GL11.GL_LIGHT0, GL11.GL_POSITION, lightPosition);

    GL11.glEnable(GL11.GL_DEPTH_TEST);
    GL11.glDepthFunc(GL11.GL_LEQUAL);

    // Clear the screen and depth buffer
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

    // Initialize the car and the terrain
    for (int i = 0; i < 3; i++) {
      Car newCar = new Car();
      newCar.setPosition(i * 5.0f, 0, i * 5.0f); // Spread out initial positions
      cars.add(newCar);
    }
    terrain = new Terrain("fractal_terrain.obj"); // Load the terrain from an OBJ file
  }

  private void loop() {
    while (!GLFW.glfwWindowShouldClose(window)) {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glLoadIdentity();

        if (gameState == GameState.MENU || menuState != MenuState.MAIN_MENU) {
          renderMenu();
          handleMenuInput();
        } else if (gameState == GameState.PLAYING) {
            updateCarMovement();
            Car activeCar = cars.get(activeCarIndex);
            updateCamera(activeCar);
            terrain.render();
            for (Car c : cars) {
                c.update();
                c.render(terrain);
            }
        }

        GLFW.glfwSwapBuffers(window);
        GLFW.glfwPollEvents();
    }
  }

  private void renderMenu() {
    GL11.glMatrixMode(GL11.GL_PROJECTION);
    GL11.glPushMatrix();
    GL11.glLoadIdentity();
    GL11.glOrtho(0, width, height, 0, -1, 1);
    GL11.glMatrixMode(GL11.GL_MODELVIEW);
    GL11.glPushMatrix();
    GL11.glLoadIdentity();

    if (menuState == MenuState.MAIN_MENU) {
        drawButton(300, 150, 200, 50, "Start Game");
        drawButton(300, 220, 200, 50, "Host Game");
        drawButton(300, 290, 200, 50, "Join Game");
        drawButton(300, 360, 200, 50, "Exit");

        drawText("Start Game", 400, 150 + 15);
        drawText("Host Game", 400, 220 + 15);
        drawText("Join Game", 400, 290 + 15);
        drawText("Exit", 400, 360 + 15);
    }

    if (menuState == MenuState.HOSTING) {
      drawText("Hosting Server", 300, 100);
      drawText("Your IP: " + hostIp, 300, 130);
      drawText("Waiting for players...", 300, 160);
  
      int y = 200;
      for (String player : playerList) {
          drawText("Player: " + player, 300, y);
          y += 30;
      }
  
      // Back button
      drawButton(300, 500, 200, 50, "Back to Menu");
      drawText("Back to Menu", 400, 515);
  }
  
  if (menuState == MenuState.JOINING) {
      drawText("Enter Host IP:", 300, 150);
      drawText(typedIp + "_", 300, 180);
  
      // Back button
      drawButton(300, 500, 200, 50, "Back to Menu");
      drawText("Back to Menu", 400, 515);
  }  
  
    GL11.glPopMatrix();
    GL11.glMatrixMode(GL11.GL_PROJECTION);
    GL11.glPopMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW);
  }

private void drawText(String text, float x, float y) {
  // Center the text horizontally
  int totalWidth = 0;
  for (int i = 0; i < text.length(); i++) {
      totalWidth += font.getCharWidth(text.charAt(i));
  }
  float centeredX = x - totalWidth / 2f;

  font.drawString(centeredX, y, text);
}

private void drawButton(int x, int y, int w, int h, String label) {
    GL11.glColor3f(0.2f, 0.2f, 0.8f); // Button color
    GL11.glBegin(GL11.GL_QUADS);
    GL11.glVertex2f(x, y);
    GL11.glVertex2f(x + w, y);
    GL11.glVertex2f(x + w, y + h);
    GL11.glVertex2f(x, y + h);
    GL11.glEnd();
}

private void handleMenuInput() {
  if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
      double[] xpos = new double[1];
      double[] ypos = new double[1];
      GLFW.glfwGetCursorPos(window, xpos, ypos);

      // No Y flip — GLFW coords are top-left origin, matching how you draw

      // === MAIN MENU ===
      if (menuState == MenuState.MAIN_MENU) {
          if (xpos[0] >= 300 && xpos[0] <= 500) {
              if (ypos[0] >= 150 && ypos[0] <= 200) {
                  gameState = GameState.PLAYING;
              } else if (ypos[0] >= 220 && ypos[0] <= 270) {
                  System.out.println("HOST button clicked");
                  serverThread = new ServerThread(playerList);
                  serverThread.start();
                  hostIp = getLocalIp();
                  menuState = MenuState.HOSTING;
              } else if (ypos[0] >= 290 && ypos[0] <= 340) {
                  System.out.println("JOIN button clicked");
                  menuState = MenuState.JOINING;
              } else if (ypos[0] >= 360 && ypos[0] <= 410) {
                  GLFW.glfwSetWindowShouldClose(window, true);
              }
          }
      }

      // === HOSTING MENU ===
      if (menuState == MenuState.HOSTING) {
          if (xpos[0] >= 300 && xpos[0] <= 500 && ypos[0] >= 500 && ypos[0] <= 550) {
              System.out.println("Back to Menu clicked (HOSTING)");
              if (serverThread != null) {
                  serverThread.shutdown();
                  serverThread = null;
              }
              playerList.clear();
              menuState = MenuState.MAIN_MENU;
          }
      }

      // === JOINING MENU ===
      if (menuState == MenuState.JOINING) {
          if (xpos[0] >= 300 && xpos[0] <= 500 && ypos[0] >= 500 && ypos[0] <= 550) {
              System.out.println("Back to Menu clicked (JOINING)");
              typedIp = "";
              menuState = MenuState.MAIN_MENU;
          }
      }
  }
}

private String getLocalIp() {
    try {
        return InetAddress.getLocalHost().getHostAddress();
    } catch (Exception e) {
        return "Unknown";
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

    // Set brighter ambient, diffuse, and specular light
    FloatBuffer ambientLight = BufferUtils.createFloatBuffer(4).put(new float[] { 0.4f, 0.4f, 0.4f, 1.0f }); // Increase
                                                                                                             // ambient
                                                                                                             // light
    ambientLight.flip();
    GL11.glLightfv(GL11.GL_LIGHT0, GL11.GL_AMBIENT, ambientLight);

    FloatBuffer diffuseLight = BufferUtils.createFloatBuffer(4).put(new float[] { 1.0f, 1.0f, 1.0f, 1.0f }); // Set
                                                                                                             // material
                                                                                                             // properties
    FloatBuffer materialAmbient = BufferUtils.createFloatBuffer(4).put(new float[] { 0.6f, 0.6f, 0.6f, 1.0f }); // Brighter
                                                                                                                // ambient
                                                                                                                // reflection
    materialAmbient.flip();
    GL11.glMaterialfv(GL11.GL_FRONT, GL11.GL_AMBIENT, materialAmbient);

    FloatBuffer materialDiffuse = BufferUtils.createFloatBuffer(4).put(new float[] { 0.8f, 0.8f, 0.8f, 1.0f }); // Brighter
                                                                                                                // diffuse
                                                                                                                // reflection
    materialDiffuse.flip();
    GL11.glMaterialfv(GL11.GL_FRONT, GL11.GL_DIFFUSE, materialDiffuse);

    FloatBuffer materialSpecular = BufferUtils.createFloatBuffer(4).put(new float[] { 1.0f, 1.0f, 1.0f, 1.0f }); // Specular
                                                                                                                 // highlight
    materialSpecular.flip();
    GL11.glMaterialfv(GL11.GL_FRONT, GL11.GL_SPECULAR, materialSpecular);

    GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS, 50.0f); // Set shininess (higher = more specular reflection)

    // Set global ambient light
    FloatBuffer globalAmbient = BufferUtils.createFloatBuffer(4).put(new float[] { 0.5f, 0.5f, 0.5f, 1.0f });
    globalAmbient.flip();
    GL11.glLightModelfv(GL11.GL_LIGHT_MODEL_AMBIENT, globalAmbient);
    // Increase diffuse light
    diffuseLight.flip();
    GL11.glLightfv(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, diffuseLight);

    FloatBuffer specularLight = BufferUtils.createFloatBuffer(4).put(new float[] { 1.0f, 1.0f, 1.0f, 1.0f }); // Increase
    // specular
    // highlight
    specularLight.flip();
    GL11.glLightfv(GL11.GL_LIGHT0, GL11.GL_SPECULAR, specularLight);

    // Enable color material to allow vertex colors with lighting
    GL11.glEnable(GL11.GL_COLOR_MATERIAL);
    GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);
  }

  private void setPerspectiveProjection(float fov, float aspect, float zNear, float zFar) {
    float ymax = (float) (zNear * Math.tan(Math.toRadians(fov / 2.0)));
    float xmax = ymax * aspect;

    GL11.glMatrixMode(GL11.GL_PROJECTION);
    GL11.glLoadIdentity();
    GL11.glFrustum(-xmax, xmax, -ymax, ymax, zNear, zFar);
    GL11.glMatrixMode(GL11.GL_MODELVIEW);
  }

  private void setupCamera() {
    // Position the camera behind the car, following it
    GL11.glTranslatef(0, -5, -20); // Adjust this for better view
    GL11.glRotatef(20, 1, 0, 0); // Slight downward angle
  }

  private float lerp(float start, float end, float alpha) {
    return start + alpha * (end - start);
  }

  private float cameraX = 0;
  private float cameraY = 5;
  private float cameraZ = 10;

  private void updateCamera(Car car) {
    float cameraDistance = 10.0f; // Distance behind the car
    float cameraHeight = 5.0f; // Height above the car

    // Calculate the desired camera position behind and above the car
    float targetCameraX = car.getX() - (float) (Math.sin(Math.toRadians(car.getAngle())) * cameraDistance);
    float targetCameraZ = car.getZ() - (float) (Math.cos(Math.toRadians(car.getAngle())) * cameraDistance);
    float targetCameraY = car.getY() + cameraHeight;

    // Smoothly interpolate between the current camera position and the target
    // position
    float alpha = 0.1f; // Smoothing factor (0 = no movement, 1 = instant movement)
    cameraX = lerp(cameraX, targetCameraX, alpha);
    cameraY = lerp(cameraY, targetCameraY, alpha);
    cameraZ = lerp(cameraZ, targetCameraZ, alpha);

    // Reset the model-view matrix
    GL11.glLoadIdentity();

    // Set the camera to look at the car
    gluLookAt(cameraX, cameraY, cameraZ, car.getX(), car.getY(), car.getZ(), 0.0f, 1.0f, 0.0f);
  }

  private void gluLookAt(float eyex, float eyey, float eyez, float centerx, float centery, float centerz, float upx,
      float upy, float upz) {
    // Step 1: Calculate the forward vector (the direction the camera is
    // looking)
    float[] forward = { centerx - eyex, centery - eyey, centerz - eyez };
    normalize(forward); // Normalize the forward vector

    // Step 2: Define the up vector (Y-axis typically)
    float[] up = { upx, upy, upz };

    // Step 3: Calculate the side (right) vector using cross product of forward
    // and up
    float[] side = crossProduct(forward, up);
    normalize(side); // Normalize the side vector

    // Step 4: Recalculate the true up vector (should be perpendicular to both
    // side and forward)
    up = crossProduct(side, forward);

    // Step 5: Create the lookAt matrix (view matrix)
    FloatBuffer viewMatrix = BufferUtils.createFloatBuffer(16);
    viewMatrix.put(new float[] { side[0], up[0], -forward[0], 0, side[1], up[1], -forward[1], 0, side[2], up[2],
        -forward[2], 0, -dotProduct(side, new float[] { eyex, eyey, eyez }),
        -dotProduct(up, new float[] { eyex, eyey, eyez }), dotProduct(forward, new float[] { eyex, eyey, eyez }), 1 });
    viewMatrix.flip(); // Flip the buffer for use by OpenGL

    // Step 6: Apply the view matrix
    GL11.glMultMatrixf(viewMatrix);
  }

  // Utility functions for vector math
  private void normalize(float[] v) {
    float length = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    if (length != 0) {
      v[0] /= length;
      v[1] /= length;
      v[2] /= length;
    }
  }

  private float[] crossProduct(float[] a, float[] b) {
    return new float[] { a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0] };
  }

  private float dotProduct(float[] a, float[] b) {
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
  }

  private void updateCarMovement() {
    Car activeCar = cars.get(activeCarIndex);
    if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS) {
      activeCar.accelerate();
    }
    if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS) {
      activeCar.decelerate();
    }
    if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS) {
      activeCar.turnLeft();
    }
    if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS) {
      activeCar.turnRight();
    }

    // Switch between cars using number keys 1-9
    for (int i = 0; i < cars.size(); i++) {
      if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_1 + i) == GLFW.GLFW_PRESS) {
        activeCarIndex = i;
      }
    }
  }
}

class Car {
  private float x = 0, y = 0, z = 0; // Car's position
  private float speed = 0; // Current speed
  private float angle = 0; // Direction the car is facing
  private float maxSpeed = 0.1f;
  private float acceleration = 0.01f;
  private float friction = 0.98f;
  private float turnSpeed = 2.0f; // Speed of turning

  public float getX() {
    return x;
  }

  public float getY() {
    return y;
  }

  public float getZ() {
    return z;
  }

  public float getAngle() {
    return angle;
  }

  public void setPosition(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public void accelerate() {
    if (speed < maxSpeed) {
      speed += acceleration;
    }
  }

  public void decelerate() {
    if (speed > -maxSpeed) {
      speed -= acceleration;
    }
  }

  public void turnLeft() {
    angle += turnSpeed;
  }

  public void turnRight() {
    angle -= turnSpeed;
  }

  public void update() {
    // Update position based on speed and angle
    x += speed * Math.sin(Math.toRadians(angle));
    z += speed * Math.cos(Math.toRadians(angle));

    // Apply friction to slow down the car naturally
    speed *= friction;
  }

  public void render(Terrain terrain) {
    // Get the heights of each wheel
    float frontLeftWheelY = terrain.getTerrainHeightAt(x - 0.9f, z + 1.5f);
    float frontRightWheelY = terrain.getTerrainHeightAt(x + 0.9f, z + 1.5f);
    float rearLeftWheelY = terrain.getTerrainHeightAt(x - 0.9f, z - 1.5f);
    float rearRightWheelY = terrain.getTerrainHeightAt(x + 0.9f, z - 1.5f);

    // Calculate the average height of the car body (based on wheel heights)
    float averageHeight = (frontLeftWheelY + frontRightWheelY + rearLeftWheelY + rearRightWheelY) / 4.0f;

    // Car body dimensions
    float carBodyHeight = 0.5f; // The height of the car body

    // Adjust the height of the car body to be above the wheels
    // The car body is raised by half of its height so the bottom aligns with
    // the wheels
    float carBodyYOffset = 4.0f * carBodyHeight + carBodyHeight / 2.0f;

    // Calculate pitch (forward/backward tilt) and roll (side tilt)
    float pitch = (frontLeftWheelY + frontRightWheelY) / 2.0f - (rearLeftWheelY + rearRightWheelY) / 2.0f;
    float roll = (frontLeftWheelY + rearLeftWheelY) / 2.0f - (frontRightWheelY + rearRightWheelY) / 2.0f;

    // Apply the calculated pitch, roll, and average height to the car body
    GL11.glPushMatrix();

    // Translate the car body to the average height plus the offset to
    // position it above the wheels
    GL11.glTranslatef(x, averageHeight + carBodyYOffset, z);

    // Rotate the car for pitch (tilt forward/backward) and roll (tilt
    // left/right)
    GL11.glRotatef(roll * 10.0f, 0, 0, 1); // Roll around the Z-axis
    GL11.glRotatef(pitch * 10.0f, 1, 0, 0); // Pitch around the X-axis

    // Rotate the car in the direction it's facing
    GL11.glRotatef(angle, 0, 1, 0);

    // Render the car body
    renderCarBody(); // Call the updated renderCarBody method

    // Render the wheels
    renderWheels(terrain); // Render the wheels based on terrain

    GL11.glPopMatrix(); // Restore the transformation state
  }

  private void renderCarBody() {
    GL11.glColor3f(1.0f, 0.2f, 0.2f); // Slightly lighter red for the car body
    GL11.glShadeModel(GL11.GL_SMOOTH); // Smooth shading for Phong

    FloatBuffer carBodySpecular = BufferUtils.createFloatBuffer(4).put(new float[] { 0.9f, 0.9f, 0.9f, 1.0f });
    carBodySpecular.flip();
    GL11.glMaterialfv(GL11.GL_FRONT, GL11.GL_SPECULAR, carBodySpecular);
    GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS, 64.0f); // High shininess for car body

    float length = 4.0f;
    float width = 2.0f;
    float height = 0.5f;

    GL11.glBegin(GL11.GL_QUADS);

    // Front face
    GL11.glNormal3f(0, 0, 1);
    GL11.glVertex3f(-width / 2, -height / 2, length / 2);
    GL11.glVertex3f(width / 2, -height / 2, length / 2);
    GL11.glVertex3f(width / 2, height / 2, length / 2);
    GL11.glVertex3f(-width / 2, height / 2, length / 2);

    // Back face (z = -length / 2)
    GL11.glVertex3f(-width / 2, -height / 2, -length / 2);
    GL11.glVertex3f(width / 2, -height / 2, -length / 2);
    GL11.glVertex3f(width / 2, height / 2, -length / 2);
    GL11.glVertex3f(-width / 2, height / 2, -length / 2);

    // Left face (x = -width / 2)
    GL11.glVertex3f(-width / 2, -height / 2, length / 2);
    GL11.glVertex3f(-width / 2, -height / 2, -length / 2);
    GL11.glVertex3f(-width / 2, height / 2, -length / 2);
    GL11.glVertex3f(-width / 2, height / 2, length / 2);

    // Right face (x = +width / 2)
    GL11.glVertex3f(width / 2, -height / 2, length / 2);
    GL11.glVertex3f(width / 2, -height / 2, -length / 2);
    GL11.glVertex3f(width / 2, height / 2, -length / 2);
    GL11.glVertex3f(width / 2, height / 2, length / 2);

    // Top face (y = +height / 2)
    GL11.glVertex3f(-width / 2, height / 2, -length / 2);
    GL11.glVertex3f(width / 2, height / 2, -length / 2);
    GL11.glVertex3f(width / 2, height / 2, length / 2);
    GL11.glVertex3f(-width / 2, height / 2, length / 2);

    // Bottom face (y = -height / 2)
    GL11.glVertex3f(-width / 2, -height / 2, -length / 2);
    GL11.glVertex3f(width / 2, -height / 2, -length / 2);
    GL11.glVertex3f(width / 2, -height / 2, length / 2);
    GL11.glVertex3f(-width / 2, -height / 2, length / 2);

    GL11.glEnd();
  }

  private void renderWheel() {
    float radius = 0.4f;
    float width = 0.2f;
    int numSegments = 36;

    GL11.glColor3f(0.2f, 0.2f, 0.2f); // Dark gray for wheels
    GL11.glShadeModel(GL11.GL_SMOOTH);

    FloatBuffer wheelSpecular = BufferUtils.createFloatBuffer(4).put(new float[] { 0.5f, 0.5f, 0.5f, 1.0f });
    wheelSpecular.flip();
    GL11.glMaterialfv(GL11.GL_FRONT, GL11.GL_SPECULAR, wheelSpecular);
    GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS, 16.0f); // Low shininess for wheels

    GL11.glPushMatrix();
    GL11.glRotatef(90, 0, 1, 0);

    // Front face (at z = -width / 2)
    GL11.glBegin(GL11.GL_TRIANGLE_FAN);
    GL11.glVertex3f(0.0f, 0.0f, -width / 2); // Center of the circle
    for (int i = 0; i < numSegments; i++) {
      double angle = 2 * Math.PI * i / numSegments;
      GL11.glVertex3f((float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius, -width / 2);
    }
    GL11.glEnd();

    // Rear face (at z = +width / 2)
    GL11.glBegin(GL11.GL_TRIANGLE_FAN);
    GL11.glVertex3f(0.0f, 0.0f, width / 2); // Center of the circle
    for (int i = 0; i < numSegments; i++) {
      double angle = 2 * Math.PI * i / numSegments;
      GL11.glVertex3f((float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius, width / 2);
    }
    GL11.glEnd();

    GL11.glBegin(GL11.GL_QUAD_STRIP);
    for (int i = 0; i < numSegments; i++) {
      double angle = 2 * Math.PI * i / numSegments;
      float x = (float) Math.cos(angle) * radius;
      float y = (float) Math.sin(angle) * radius;

      // Set normals to make wheel sides visible
      GL11.glNormal3f(x, y, 0);

      GL11.glVertex3f(x, y, -width / 2);
      GL11.glVertex3f(x, y, width / 2);
    }
    GL11.glEnd();

    GL11.glPopMatrix();
  }

  private void renderWheels(Terrain terrain) {
    GL11.glColor3f(0.0f, 0.0f, 0.0f); // Black color for wheels

    // Define the wheel height offset
    float wheelHeightOffset = 0.8f; // 0.3f; // Lower the wheels by this amount relative to the car body

    GL11.glPushMatrix(); // Front-left wheel
    float frontLeftWheelY = terrain.getTerrainHeightAt(this.getX() - 0.9f, this.getZ() + 1.5f);
    GL11.glTranslatef(-0.9f, frontLeftWheelY + 0.5f - wheelHeightOffset, 1.5f); // Lower the wheel by the offset
    renderWheel(); // Render the wheel
    GL11.glPopMatrix();

    // Front-right wheel
    GL11.glPushMatrix();
    float frontRightWheelY = terrain.getTerrainHeightAt(this.getX() + 0.9f, this.getZ() + 1.5f);
    GL11.glTranslatef(0.9f, frontRightWheelY + 0.5f - wheelHeightOffset, 1.5f); // Lower the wheel by the offset
    renderWheel();
    GL11.glPopMatrix();

    // Rear-left wheel
    GL11.glPushMatrix();
    float rearLeftWheelY = terrain.getTerrainHeightAt(this.getX() - 0.9f, this.getZ() - 1.5f);
    GL11.glTranslatef(-0.9f, rearLeftWheelY + 0.5f - wheelHeightOffset, -1.5f); // Lower the wheel by the offset
    renderWheel();
    GL11.glPopMatrix();

    // Rear-right wheel
    GL11.glPushMatrix();
    float rearRightWheelY = terrain.getTerrainHeightAt(this.getX() + 0.9f, this.getZ() - 1.5f);
    GL11.glTranslatef(0.9f, rearRightWheelY + 0.5f - wheelHeightOffset, -1.5f); // Lower the wheel by the offset
    renderWheel();
    GL11.glPopMatrix();
  }
}

class OBJLoader {

  public static Model loadModel(String fileName) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(fileName));
    String line;
    List<float[]> vertices = new ArrayList<>();
    List<float[]> normals = new ArrayList<>();
    List<int[]> faces = new ArrayList<>();
    while ((line = reader.readLine()) != null) {
      String[] tokens = line.split("\\s+");
      if (tokens[0].equals("v")) {
        float[] vertex = { Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3]) };
        vertices.add(vertex);
      } else if (tokens[0].equals("vn")) {
        float[] normal = { Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3]) };
        normals.add(normal);
      } else if (tokens[0].equals("f")) {
        int[] face = { Integer.parseInt(tokens[1].split("/")[0]) - 1, Integer.parseInt(tokens[2].split("/")[0]) - 1,
            Integer.parseInt(tokens[3].split("/")[0]) - 1 };
        faces.add(face);
      }
    }
    reader.close();

    // Debugging: Print the number of vertices, normals, and faces loaded
    System.out.println("Vertices loaded: " + vertices.size());
    System.out.println("Normals loaded: " + normals.size());
    System.out.println("Faces loaded: " + faces.size());

    float[] verticesArray = new float[vertices.size() * 3];
    float[] normalsArray = new float[vertices.size() * 3]; // Normals array size matches vertices
    int[] indicesArray = new int[faces.size() * 3];

    int vertexIndex = 0;
    for (float[] vertex : vertices) {
      verticesArray[vertexIndex++] = vertex[0];
      verticesArray[vertexIndex++] = vertex[1];
      verticesArray[vertexIndex++] = vertex[2];
    }

    // If no normals are loaded, generate default normals (e.g., pointing up)
    if (normals.isEmpty()) {
      for (int i = 0; i < vertices.size(); i++) {
        normalsArray[i * 3] = 0.0f; // X component
        normalsArray[i * 3 + 1] = 1.0f; // Y component
        normalsArray[i * 3 + 2] = 0.0f; // Z component
      }
    } else {
      int normalIndex = 0;
      for (float[] normal : normals) {
        normalsArray[normalIndex++] = normal[0];
        normalsArray[normalIndex++] = normal[1];
        normalsArray[normalIndex++] = normal[2];
      }
    }

    int faceIndex = 0;
    for (int[] face : faces) {
      indicesArray[faceIndex++] = face[0];
      indicesArray[faceIndex++] = face[1];
      indicesArray[faceIndex++] = face[2];
    }

    return new Model(verticesArray, normalsArray, indicesArray);
  }
}

class Model {
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

class Terrain {
  private Model model;

  public Terrain(String objFilePath) {
    try {
      this.model = OBJLoader.loadModel(objFilePath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void render() {
    if (model.getVertices().length == 0 || model.getIndices().length == 0) {
      System.err.println("Error: Model has no vertices or indices.");
      return;
    }

    GL11.glColor3f(0.3f, 0.8f, 0.3f); // Lighter green for the terrain
    GL11.glShadeModel(GL11.GL_SMOOTH); // Smooth shading for better Phong effect

    // Adjust terrain material properties to make it brighter
    FloatBuffer terrainAmbient = BufferUtils.createFloatBuffer(4).put(new float[] { 0.6f, 0.8f, 0.6f, 1.0f }); // Higher
                                                                                                               // ambient
                                                                                                               // light
                                                                                                               // reflection
    FloatBuffer terrainDiffuse = BufferUtils.createFloatBuffer(4).put(new float[] { 0.7f, 0.9f, 0.7f, 1.0f }); // Higher
                                                                                                               // diffuse
                                                                                                               // reflection
                                                                                                               // for
                                                                                                               // visibility
    FloatBuffer terrainSpecular = BufferUtils.createFloatBuffer(4).put(new float[] { 0.2f, 0.2f, 0.2f, 1.0f }); // Light
                                                                                                                // specular
                                                                                                                // highlight
                                                                                                                // for
                                                                                                                // subtle
                                                                                                                // shine

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
    int[] indices = model.getIndices(); // Get the triangle indices

    // Iterate through all triangles in the terrain mesh
    for (int i = 0; i < indices.length; i += 3) {
      // Get the vertices of the triangle
      int vertexIndex1 = indices[i] * 3;
      int vertexIndex2 = indices[i + 1] * 3;
      int vertexIndex3 = indices[i + 2] * 3;

      // Vertices of the triangle
      float v1x = vertices[vertexIndex1];
      float v1y = vertices[vertexIndex1 + 1]; // The height at vertex 1
      float v1z = vertices[vertexIndex1 + 2];

      float v2x = vertices[vertexIndex2];
      float v2y = vertices[vertexIndex2 + 1]; // The height at vertex 2
      float v2z = vertices[vertexIndex2 + 2];

      float v3x = vertices[vertexIndex3];
      float v3y = vertices[vertexIndex3 + 1]; // The height at vertex 3
      float v3z = vertices[vertexIndex3 + 2];

      // Check if the point (x, z) is inside this triangle
      if (isPointInTriangle(x, z, v1x, v1z, v2x, v2z, v3x, v3z)) {
        // If the point is in the triangle, calculate the height using
        // barycentric interpolation
        return interpolateHeight(x, z, v1x, v1y, v1z, v2x, v2y, v2z, v3x, v3y, v3z);
      }
    }

    // If no triangle was found, return 0 as a default
    return 0.0f;
  }

  private boolean isPointInTriangle(float px, float pz, float v1x, float v1z, float v2x, float v2z, float v3x,
      float v3z) {
    float d1 = sign(px, pz, v1x, v1z, v2x, v2z);
    float d2 = sign(px, pz, v2x, v2z, v3x, v3z);
    float d3 = sign(px, pz, v3x, v3z, v1x, v1z);

    boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
    boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);

    return !(hasNeg && hasPos); // Point is inside the triangle if all signs are the same
  }

  private float sign(float px, float pz, float v1x, float v1z, float v2x, float v2z) {
    return (px - v2x) * (v1z - v2z) - (v1x - v2x) * (pz - v2z);
  }

  private float interpolateHeight(float x, float z, float v1x, float v1y, float v1z, float v2x, float v2y, float v2z,
      float v3x, float v3y, float v3z) {
    // Calculate the areas needed for barycentric interpolation
    float areaTotal = triangleArea(v1x, v1z, v2x, v2z, v3x, v3z);
    float area1 = triangleArea(x, z, v2x, v2z, v3x, v3z);
    float area2 = triangleArea(x, z, v3x, v3z, v1x, v1z);
    float area3 = triangleArea(x, z, v1x, v1z, v2x, v2z);

    // Calculate the barycentric weights
    float weight1 = area1 / areaTotal;
    float weight2 = area2 / areaTotal;
    float weight3 = area3 / areaTotal;

    // Interpolate the height using the weights
    return weight1 * v1y + weight2 * v2y + weight3 * v3y;
  }

  private float triangleArea(float x1, float z1, float x2, float z2, float x3, float z3) {
    return Math.abs((x1 * (z2 - z3) + x2 * (z3 - z1) + x3 * (z1 - z2)) / 2.0f);
  }
}
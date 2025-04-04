import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class Sumo {

  private long window;
  private int width = 800;
  private int height = 600;
  private Terrain terrain;

  // NOAH: Added carCount variable
  public int carCount = 2;
  // NOAH: END

  // NOAH: Added list of cars and active index
  private List<Car> cars;
  private int currCar = 0;
  private Car enemyCar;
  private boolean tabPressed = false;
  // NOAH: END

  // Noah: 269-277 (until render)
  // Titus: 277-287 (render and onward)
  public static void main(String[] args) {
    new Sumo().run();
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

    window = GLFW.glfwCreateWindow(width, height, "Car Simulation", 0, 0);
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

    // Define light properties
    FloatBuffer lightPosition = BufferUtils.createFloatBuffer(4).put(new float[] { 0.0f, 10.0f, 10.0f, 1.0f });
    lightPosition.flip();
    GL11.glLightfv(GL11.GL_LIGHT0, GL11.GL_POSITION, lightPosition);

    GL11.glEnable(GL11.GL_DEPTH_TEST);
    GL11.glDepthFunc(GL11.GL_LEQUAL);

    // Clear the screen and depth buffer
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

    // NOAH: Initialize list of cars and populate with cars
    cars = new ArrayList<>();
    for (int i = 0; i < carCount; i++) {
      // Create a new car object and set its initial position randomly within a range
      Car car = new Car();
      // Randomize the car's position in a small range
      car.setPosition(10, 0, 10); // TODO: reduce to 10, 0, 10 for defaults
      cars.add(car);
    }

    enemyCar = new Car();
    enemyCar.setPosition(20, 10, 20);
    // NOAH: END

    terrain = new Terrain("terrain.obj"); // Load the terrain from an OBJ file
  }

  private void loop() {
    while (!GLFW.glfwWindowShouldClose(window)) {
      GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

      GL11.glLoadIdentity();

      // Updae car movement based on user input
      updateCarMovement();

      // NOAH START: Update the camera to target the current active car
      Car activeCar = cars.get(currCar);
      updateCamera(activeCar);
      // NOAH: END

      // Render terrain
      terrain.render();
      // NOAH START: Iterate over cars and update them individually
      for (int i = 0; i < cars.size(); i++) { // titus modified so we can see what car we are on
        Car car = cars.get(i); // titus added
        car.update();
        car.render(terrain, i); // titus added carNumber pass
      }

      enemyCar.update();
      enemyCar.render(terrain, -1); // Render the enemy car
      updateEnemyCar();
      // NOAH: END

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

  private void updateCamera(Car car) {
    float cameraDistance = 10.0f; // Distance behind the car
    float cameraHeight = 5.0f; // Height above the car

    // calculate the desired camera position behind and abvove the car
    float targetCameraX = car.getX() - (float) (Math.sin(Math.toRadians(car.getAngle())) * cameraDistance);
    float targetCameraZ = car.getZ() - (float) (Math.cos(Math.toRadians(car.getAngle())) * cameraDistance);
    float targetCameraY = car.getY() + cameraHeight;

    // Smoothly inerpolate between the current camera position and the target
    // position
    float alpha = 0.1f; // Smoothing factor (0 = no movement, 1 = instant movement)
    cameraX = lerp(cameraX, targetCameraX, alpha);
    cameraY = lerp(cameraY, targetCameraY, alpha);
    cameraZ = lerp(cameraZ, targetCameraZ, alpha);

    // Reset the model-view matrix
    GL11.glLoadIdentity();

    // Set the camera to look at the car
    gluLookAt(cameraX, cameraY, cameraZ,
        car.getX(), car.getY(), car.getZ(),
        0, 1, 0); // Up vector
  }

  private void gluLookAt(float eyeX, float eyeY, float eyeZ,
      float centerX, float centerY, float centerZ,
      float upX, float upY, float upZ) {
    // Step 1: Calculate the forward vector (the ddirection the camera is looking)
    float[] forward = {
        centerX - eyeX,
        centerY - eyeY,
        centerZ - eyeZ
    };
    normalize(forward);

    // Step 2: define the up fector (y-axis typically)
    float[] up = { upX, upY, upZ };

    // Step 3: calculate the side (right) vector using cross product of forward and
    // up
    float[] side = crossProduct(forward, up);
    normalize(side); // Normalize the side vector

    // Step 4: calculate thetrue up vector (should be perpendicular to both side and
    // forward
    up = crossProduct(side, forward);

    // Step 5: create the lookAt matrix (view matrix)
    FloatBuffer viewMatrix = BufferUtils.createFloatBuffer(16);
    viewMatrix.put(new float[] {
        side[0], up[0], -forward[0], 0,
        side[1], up[1], -forward[1], 0,
        side[2], up[2], -forward[2], 0,
        -dotProduct(side, new float[] { eyeX, eyeY, eyeZ }),
        -dotProduct(up, new float[] { eyeX, eyeY, eyeZ }),
        dotProduct(forward, new float[] { eyeX, eyeY, eyeZ }),
        1
    });
    viewMatrix.flip(); // Flip the buffer for use by OpenGL

    // Step 6: apply the view matrix
    GL11.glMultMatrixf(viewMatrix);
  }

  // Utility functions for vectoer math
  private void normalize(float[] v) {
    float length = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    if (length > 0) {
      v[0] /= length;
      v[1] /= length;
      v[2] /= length;
    }
  }

  float[] crossProduct(float[] a, float[] b) {
    return new float[] {
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0]
    };
  }

  private float dotProduct(float[] a, float[] b) {
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
  }

  private void updateCarMovement() {

    // NOAH START: handle swapping cars and debouncing tab
    if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_TAB) == GLFW.GLFW_PRESS) {
      if (!tabPressed) {
        currCar = (currCar + 1) % cars.size();
        tabPressed = true;
      }
    } else {
      tabPressed = false;
    }
    // NOAH END

    // NOAH START: Handle movement only for the currently active car
    Car activeCar = cars.get(currCar);
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
    // NOAH END:
  }

  private void updateEnemyCar() {
    // Find the closest player car to the enemy car
    Car closestCar = null;
    float closestDistance = Float.MAX_VALUE;

    for (Car car : cars) {
      float dx = car.getX() - enemyCar.getX();
      float dz = car.getZ() - enemyCar.getZ();
      float distance = (float) Math.sqrt(dx * dx + dz * dz);

      if (distance < closestDistance) {
        closestDistance = distance;
        closestCar = car;
      }
    }

    if (closestCar != null) {
      // Calculate the direction vector from the enemy car to the closest player car
      float dx = closestCar.getX() - enemyCar.getX();
      float dz = closestCar.getZ() - enemyCar.getZ();

      // Normalize the direction vector
      float length = (float) Math.sqrt(dx * dx + dz * dz);
      if (length > 0) {
        dx /= length;
        dz /= length;
      }

      // Move the enemy car towards the closest player car
      float speed = 0.05f; // Adjust the speed of the enemy car
      float newX = enemyCar.getX() + dx * speed;
      float newZ = enemyCar.getZ() + dz * speed;

      // Update the enemy car's position
      enemyCar.setPosition(newX, enemyCar.getY(), newZ);
    }
  }

  public static class Car {
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

    // New setter to allow different starting positions
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

    public void reverse() {
      if (speed > -maxSpeed) {
        speed -= acceleration;
      }
    }

    public void decelerate() {
      if (speed > 0) {
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

      // Apply friction to slow car down naturally
      speed *= friction;
    }

    public void render(Terrain terrain, int carNumber) { // titus added carnumber
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
      // The car body is raised by half of its height so the bottom alighs with the
      // wheels
      float carBodyYOffset = 4.0f * carBodyHeight + carBodyHeight / 2.0f;

      // Calculate pitch (forward/backward tilt) and roll (side tilt)
      float pitch = (frontLeftWheelY + frontRightWheelY) / 2.0f - (rearLeftWheelY + rearRightWheelY) / 2.0f;
      float roll = (frontLeftWheelY + rearLeftWheelY) / 2.0f - (frontRightWheelY + rearRightWheelY) / 2.0f;

      // Apply the calculated pitch, roll, and average height to the car body
      GL11.glPushMatrix();

      // Translate teh car body to the average height plus the offset to position it
      // above the wheels
      GL11.glTranslatef(x, averageHeight + carBodyYOffset, z);

      // Rotate the car for pitch (tilt forward/backward) and roll (tilt left/right)
      GL11.glRotatef(roll * 10.0f, 0, 0, 1); // Roll around the Z-axis
      GL11.glRotatef(pitch * 10.0f, 1, 0, 0); // Pitch around the X-axis

      // Rotate the car in the direction it's facing
      GL11.glRotatef(angle, 0, 1, 0);

      // Render the car body
      renderCarBody(carNumber); // Call thee updated renderCarBody method // Titus added carNumber

      // Render the wheels
      renderWheels(terrain); // Render the wheels based on terrain

      GL11.glPopMatrix();
    }

    private void renderCarBody(int carNumber) { // titus added carNumber
      if (carNumber == -1) {
        GL11.glColor3f(0.0f, 1.0f, 0.0f); // Green for enemy car
      } else {
        if (carNumber % 3 == 0) { // titus added different colors for the cars
          GL11.glColor3f(1.0f, 0.0f, 0.0f); // Red for car 1
        } else if (carNumber % 3 == 1) {
          GL11.glColor3f(1.0f, 1.0f, 1.0f); // White for car 2
        } else {
          GL11.glColor3f(0.0f, 0.0f, 1.0f); // Blue for car 3
        }
      }
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

      // Back face (z = -length/2)
      GL11.glVertex3f(-width / 2, -height / 2, -length / 2);
      GL11.glVertex3f(width / 2, -height / 2, -length / 2);
      GL11.glVertex3f(width / 2, height / 2, -length / 2);
      GL11.glVertex3f(-width / 2, height / 2, -length / 2);

      // Left face (x = -width/2)
      GL11.glVertex3f(-width / 2, -height / 2, -length / 2);
      GL11.glVertex3f(-width / 2, -height / 2, length / 2);
      GL11.glVertex3f(-width / 2, height / 2, length / 2);
      GL11.glVertex3f(-width / 2, height / 2, -length / 2);

      // Right face (x = +width/2)
      GL11.glVertex3f(width / 2, -height / 2, -length / 2);
      GL11.glVertex3f(width / 2, -height / 2, length / 2);
      GL11.glVertex3f(width / 2, height / 2, length / 2);
      GL11.glVertex3f(width / 2, height / 2, -length / 2);

      // Top face (y = +height/2)
      GL11.glVertex3f(-width / 2, height / 2, -length / 2);
      GL11.glVertex3f(width / 2, height / 2, -length / 2);
      GL11.glVertex3f(width / 2, height / 2, length / 2);
      GL11.glVertex3f(-width / 2, height / 2, length / 2);

      // Bottom face (y = -height/2)
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

      GL11.glColor3f(0.2f, 0.2f, 0.2f); // Dark grey for the wheels
      GL11.glShadeModel(GL11.GL_SMOOTH);

      FloatBuffer wheelSpecular = BufferUtils.createFloatBuffer(4).put(new float[] { 0.1f, 0.1f, 0.1f, 1.0f });
      wheelSpecular.flip();
      GL11.glMaterialfv(GL11.GL_FRONT, GL11.GL_SPECULAR, wheelSpecular);
      GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS, 16.0f); // Low shininess for wheels

      GL11.glPushMatrix();
      GL11.glRotatef(90.0f, 0.0f, 1.0f, 0.0f);

      // Front face (at x = -width/2)
      GL11.glBegin(GL11.GL_TRIANGLE_FAN);

      GL11.glVertex3f(0.0f, 0.0f, -width / 2); // Center of the circle
      for (int i = 0; i <= numSegments; i++) {
        double angle = 2 * Math.PI * i / numSegments;
        GL11.glVertex3f((float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius, width / 2);
      }
      GL11.glEnd();

      // Rear face (at x = +width/2)
      GL11.glBegin(GL11.GL_TRIANGLE_FAN);
      GL11.glVertex3f(0.0f, 0.0f, width / 2); // Center of the circle
      for (int i = 0; i <= numSegments; i++) {
        double angle = 2 * Math.PI * i / numSegments;
        GL11.glVertex3f((float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius, width / 2);
      }
      GL11.glEnd();

      GL11.glBegin(GL11.GL_QUAD_STRIP);
      for (int i = 0; i <= numSegments; i++) {
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
      float wheelHeightOffset = 0.8f; // Lower the wheels by this amount relative to the car body

      // Front-left wheel
      GL11.glPushMatrix();
      float frontLeftWheelY = terrain.getTerrainHeightAt(this.getX() - 0.9f, this.getZ() + 1.5f);
      GL11.glTranslatef(-0.9f, frontLeftWheelY + 0.5f - wheelHeightOffset, 1.5f); // Lower the wheel by the offset
      renderWheel();
      GL11.glPopMatrix();

      // Front-right wheel
      GL11.glPushMatrix();
      float frontRightWheelY = terrain.getTerrainHeightAt(this.getX() + 0.9f, this.getZ() + 1.5f);
      GL11.glTranslatef(0.9f, frontRightWheelY + 0.5f - wheelHeightOffset, 1.5f); // Lower the wheel by the offset
      renderWheel();
      GL11.glPopMatrix();

      // Reat-left wheel
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
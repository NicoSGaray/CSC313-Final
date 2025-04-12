import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class terrainGenerator {
    private static final int SIZE = 50; // Size of the terrain
    private static final String FILE_NAME = "terrain.obj";

    public static void main(String[] args) {
        float[][] heightMap = generateHeightMap(SIZE);
        addBumpToHeightMap(heightMap, 30, .5f, 30.0f);
        try {
            saveToObjFile(FILE_NAME, heightMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Generates a flat heightmap
    private static float[][] generateHeightMap(int size) {
        float[][] heightMap = new float[size][size];
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                heightMap[x][z] = 0.0f; // Flat plane with height 0
            }
        }
        return heightMap;
    }

    private static void addBumpToHeightMap(float[][] heightMap, int bumpDiameter, float bumpHeight,
            float flatTopDiameter) {
        int size = heightMap.length;
        int centerX = size / 2;
        int centerZ = size / 2;

        float bumpRadius = bumpDiameter / 2.0f;
        float flatTopRadius = flatTopDiameter / 2.0f;

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                // Calculate distance from the center
                float distance = (float) Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(z - centerZ, 2));

                if (distance < flatTopRadius) {
                    // Flat top region
                    heightMap[x][z] += bumpHeight;
                } else if (distance < bumpRadius) {
                    // Sloped region
                    float slopeFactor = 1.0f - ((distance - flatTopRadius) / (bumpRadius - flatTopRadius));
                    heightMap[x][z] += bumpHeight * slopeFactor;
                }
            }
        }
    }

    private static float[][][] calculateNormals(float[][] heightMap) {
        int size = heightMap.length;
        float[][][] normals = new float[size][size][3];

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                // Flat plane normals point straight up
                normals[x][z][0] = 0.0f;
                normals[x][z][1] = 1.0f;
                normals[x][z][2] = 0.0f;
            }
        }
        return normals;
    }

    // Saves the generated heightmap into a Wavefront .obj file with normals
    private static File saveToObjFile(String fileName, float[][] heightMap) throws IOException {
        int size = heightMap.length;
        float[][][] normals = calculateNormals(heightMap); // Calculate normals

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            // Writing vertices
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    writer.write("v " + x + " " + heightMap[x][z] + " " + z + "\n");
                }
            }

            // Writing normals
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    float[] normal = normals[x][z];
                    writer.write("vn " + normal[0] + " " + normal[1] + " " + normal[2] + "\n");
                }
            }

            // Writing faces (triangles)
            for (int z = 0; z < size - 1; z++) {
                for (int x = 0; x < size - 1; x++) {
                    int topLeft = (z * size) + x + 1;
                    int topRight = topLeft + 1;
                    int bottomLeft = topLeft + size;
                    int bottomRight = bottomLeft + 1;

                    // Writing faces with normals
                    writer.write("f " + topLeft + "//" + topLeft + " " + bottomLeft + "//" + bottomLeft + " " + topRight
                            + "//" + topRight + "\n");
                    writer.write("f " + topRight + "//" + topRight + " " + bottomLeft + "//" + bottomLeft + " "
                            + bottomRight + "//" + bottomRight + "\n");
                }
            }
        }
        return new File(fileName);
    }
}

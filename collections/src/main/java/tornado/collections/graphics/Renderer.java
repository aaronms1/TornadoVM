package tornado.collections.graphics;


import tornado.collections.math.TornadoMath;
import tornado.collections.types.Byte3;
import tornado.collections.types.Byte4;
import tornado.collections.types.Float3;
import tornado.collections.types.Float4;
import tornado.collections.types.ImageByte3;
import tornado.collections.types.ImageByte4;
import tornado.collections.types.ImageFloat;
import tornado.collections.types.ImageFloat3;
import tornado.collections.types.ImageFloat8;
import tornado.collections.types.Matrix4x4Float;
import tornado.collections.types.Short3;
import tornado.collections.types.VolumeOps;
import tornado.collections.types.VolumeShort2;

public class Renderer {
    private static final float INVALID = -2f;

    public static void renderLight( ImageByte4 output,
             ImageFloat3 verticies,  ImageFloat3 normals,
             Float3 light,  Float3 ambient) {

        for (
        int y = 0; y < output.Y(); y++) {
            for (
            int x = 0; x < output.X(); x++) {

                final Float3 normal = normals.get(x, y);
                Byte4 pixel = new Byte4((byte) 0, (byte) 0, (byte) 0,
                        (byte) 255);

                if (normal.getX() != INVALID) {
                    final Float3 vertex = Float3.normalise(Float3.sub(light,
                            verticies.get(x, y)));

                    final float dir = Math.max(Float3.dot(normal, vertex), 0f);

                    final Float3 col = Float3.add(ambient, dir);
                    Float3.clamp(col, 0f, 1f);

                    Float3.scale(col, 255f);

                    pixel = new Byte4((byte) col.getX(), (byte) col.getY(),
                            (byte) col.getZ(), (byte) 255);
                }

                output.set(x, y, pixel);
            }
        }
    }

    public static void renderVolume( ImageByte4 output,
             VolumeShort2 volume,  Float3 volumeDims,
             Matrix4x4Float view,  float nearPlane,
             float farPlane,  float smallStep,  float largeStep,
             Float3 light,  Float3 ambient) {

        for (
        int y = 0; y < output.Y(); y++) {
            for (
            int x = 0; x < output.X(); x++) {

                final Float4 hit = GraphicsMath.raycastPoint(volume,
                        volumeDims, x, y, view, nearPlane, farPlane, smallStep,
                        largeStep);

                final Byte4 pixel;
                if (hit.getW() > 0) {
                    final Float3 test = hit.asFloat3();
                    final Float3 surfNorm = VolumeOps.grad(volume, volumeDims,
                            test);

                    if (Float3.length(surfNorm) > 0) {
                        final Float3 diff = Float3.normalise(Float3.sub(light,
                                test));

                        final Float3 normalizedSurfNorm = Float3
                                .normalise(surfNorm);

                        final float dir = TornadoMath.max(
                                Float3.dot(normalizedSurfNorm, diff), 0f);
                        Float3 col = Float3.add(new Float3(dir, dir, dir),
                                ambient);

                        Float3.clamp(col, 0f, 1f);
                        col = Float3.mult(col, 255f);

                        pixel = new Byte4((byte) col.getX(), (byte) col.getY(),
                                (byte) col.getZ(), (byte) 0);
                    } else {
                        pixel = new Byte4();
                    }
                } else {
                    pixel = new Byte4();
                }

                output.set(x, y, pixel);

            }
        }
    }

    public static void renderNorms(ImageByte3 output, ImageFloat3 normals) {
        for (int y = 0; y < normals.Y(); y++)
            for (int x = 0; x < normals.X(); x++) {

                final Float3 normal = normals.get(x, y);
                final Byte3 pixel = new Byte3();

                if (normal.getX() != INVALID) {
                    Float3.normalise(normal);
                    Float3.mult(normal, 128f);
                    Float3.add(normal, 128f);

                    pixel.setX((byte) normal.getX());
                    pixel.setY((byte) normal.getY());
                    pixel.setZ((byte) normal.getZ());
                }

                output.set(x, y, pixel);
            }
    }

    public static void renderVertex(ImageByte3 output, ImageFloat3 vertices) {
        for (int y = 0; y < vertices.Y(); y++)
            for (int x = 0; x < vertices.X(); x++) {

                final Float3 vertex = vertices.get(x, y);
                final Byte3 pixel = new Byte3();

                if (vertex.getZ() != 0) {
                    Float3.normalise(vertex);
                    Float3.mult(vertex, 128f);
                    Float3.add(vertex, 128f);

                    pixel.setX((byte) vertex.getZ());
                    pixel.setY((byte) 0);
                    pixel.setZ((byte) 0);
                }

                output.set(x, y, pixel);
            }
    }

    public static void renderTrack( ImageByte3 output,
             ImageFloat8 track) {

        for (
        int y = 0; y < track.Y(); y++)
            for (
            int x = 0; x < track.X(); x++) {

                Byte3 pixel = null;
                final int result = (int) track.get(x, y).getS7();

                switch (result) {
                    case 1: // ok GREY
                        pixel = new Byte3((byte) 128, (byte) 128, (byte) 128);
                        break;
                    case -1: // no input BLACK
                        pixel = new Byte3((byte) 0, (byte) 0, (byte) 0);
                        break;
                    case -2: // not in image RED
                        pixel = new Byte3((byte) 255, (byte) 0, (byte) 0);
                        break;
                    case -3: // no correspondence GREEN
                        pixel = new Byte3((byte) 0, (byte) 255, (byte) 0);
                        break;
                    case -4: // too far away BLUE
                        pixel = new Byte3((byte) 0, (byte) 0, (byte) 255);
                        break;
                    case -5: // wrong normal YELLOW
                        pixel = new Byte3((byte) 255, (byte) 255, (byte) 0);
                        break;
                    default:
                        pixel = new Byte3((byte) 255, (byte) 128, (byte) 128);
                        break;
                }

                output.set(x, y, pixel);
            }
    }

    public static void renderDepth( ImageByte4 output,
             ImageFloat depthMap,  float nearPlane,
             float farPlane) {
        final Byte4 BLACK = new Byte4((byte) 255, (byte) 255, (byte) 255,
                (byte) 0);

        for (
        int y = 0; y < depthMap.Y(); y++)
            for (
            int x = 0; x < depthMap.X(); x++) {

                float depth = depthMap.get(x, y);
                Byte4 pixel = null;

                if (depth < nearPlane) {
                    pixel = BLACK; // black
                } else {
                    if (depth >= farPlane) {
                        pixel = new Byte4();
                    } else {
                        final float h = ((depth - nearPlane) / (farPlane - nearPlane)) * 6f;

                        final int sextant = (int) h;
                        final float fract = h - sextant;
                        final float mid1 = 0.25f + (0.5f * fract);
                        final float mid2 = 0.75f - (0.5f * fract);
                        switch (sextant) {
                            case 0:
                                pixel = new Byte4((byte) 191,
                                        (byte) (255f * mid1), (byte) 64,
                                        (byte) 0);
                                break;
                            case 1:
                                pixel = new Byte4((byte) (255f * mid2),
                                        (byte) 191, (byte) 64, (byte) 0);
                                break;
                            case 2:
                                pixel = new Byte4((byte) 64, (byte) 191,
                                        (byte) (255f * mid1), (byte) 0);
                                break;
                            case 3:
                                pixel = new Byte4((byte) 64,
                                        (byte) (255f * mid2), (byte) 191,
                                        (byte) 0);
                                break;
                            case 4:
                                pixel = new Byte4((byte) (255f * mid1),
                                        (byte) 64, (byte) 191, (byte) 0);
                                break;
                            case 5:
                                pixel = new Byte4((byte) 191, (byte) 64,
                                        (byte) (255f * mid2), (byte) 0);
                                break;
                        }
                    }
                }

                output.set(x, y, pixel);
            }
    }

    public final static void gs2rgb(Short3 rgb, float d) {
        final float v = 0.75f;
        float r = 0;
        float g = 0;
        float b = 0;

        if (v > 0) {
            float m = 0.25f;
            float sv = 0.6667f;
            int sextant;
            float fract, vsf, mid1, mid2;

            d *= 6.0;
            sextant = (int) d;
            fract = d - sextant;
            vsf = v * sv * fract;
            mid1 = m + vsf;
            mid2 = v - vsf;
            switch (sextant) {
                case 0:
                    r = v;
                    g = mid1;
                    b = m;
                    break;
                case 1:
                    r = mid2;
                    g = v;
                    b = m;
                    break;
                case 2:
                    r = m;
                    g = v;
                    b = mid1;
                    break;
                case 3:
                    r = m;
                    g = mid2;
                    b = v;
                    break;
                case 4:
                    r = mid1;
                    g = m;
                    b = v;
                    break;
                case 5:
                    r = v;
                    g = m;
                    b = mid2;
                    break;
            }
        }

        rgb.setX((short) (r * 255));
        rgb.setY((short) (g * 255));
        rgb.setZ((short) (b * 255));
    }

    public static void renderRGB(ImageByte3 output, ImageByte3 video) {
        for (int y = 0; y < video.Y(); y++)
            for (int x = 0; x < video.X(); x++)
                output.set(x, y, video.get(x, y));
    }

}
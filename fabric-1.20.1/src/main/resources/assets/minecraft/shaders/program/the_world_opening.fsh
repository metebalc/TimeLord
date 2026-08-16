#version 150

uniform sampler2D DiffuseSampler;
uniform float Time;
uniform vec2 InSize;

in vec2 texCoord;

out vec4 fragColor;

vec3 hue(float h) {
    float r = abs(h * 6.0 - 3.0) - 1.0;
    float g = 2.0 - abs(h * 6.0 - 2.0);
    float b = 2.0 - abs(h * 6.0 - 4.0);
    return clamp(vec3(r, g, b), 0.0, 1.0);
}

vec3 hsvToRgb(vec3 hsv) {
    return ((hue(hsv.x) - 1.0) * hsv.y + 1.0) * hsv.z;
}

vec3 rgbToHsv(vec3 rgb) {
    vec3 hsv = vec3(0.0);

    hsv.z = max(rgb.r, max(rgb.g, rgb.b));
    float minimum = min(rgb.r, min(rgb.g, rgb.b));
    float chroma = hsv.z - minimum;

    if (chroma > 0.0001) {
        hsv.y = chroma / hsv.z;

        vec3 delta = (hsv.z - rgb) / chroma;
        delta.rgb -= delta.brg;
        delta.rg += vec2(2.0, 4.0);

        if (rgb.r >= hsv.z) {
            hsv.x = delta.b;
        } else if (rgb.g >= hsv.z) {
            hsv.x = delta.r;
        } else {
            hsv.x = delta.g;
        }

        hsv.x = fract(hsv.x / 6.0);
    }

    return hsv;
}

float easeOutCubic(float t) {
    return 1.0 - pow(1.0 - t, 3.0);
}

float easeInCubic(float t) {
    return t * t * t;
}

void main() {
    vec2 uv = texCoord;
    vec2 center = vec2(0.5, 0.5);

    vec2 p = uv - center;
    float aspect = InSize.x / InSize.y;
    p.x *= aspect;

    float dist = length(p);

    float totalDuration = 0.90;
    float t = clamp(Time / totalDuration, 0.0, 1.0);

    float expandEnd = 0.52;
    float holdEnd   = 0.68;

    float radius;

    if (t < expandEnd) {
        float local = t / expandEnd;
        radius = mix(0.015, 1.28, easeOutCubic(local));
    } else if (t < holdEnd) {
        radius = 1.28;
    } else {
        float local = (t - holdEnd) / (1.0 - holdEnd);
        radius = mix(1.28, 0.03, easeInCubic(local));
    }

    float edgeSoftness = 0.045;

    float mask = 1.0 - smoothstep(
            radius - edgeSoftness,
            radius + edgeSoftness,
            dist
    );

    float ringWidth = 0.055;
    float ring = 1.0 - smoothstep(
            0.0,
            ringWidth,
            abs(dist - radius)
    );

    vec2 dir = normalize(p + vec2(0.0001));

    float shockStrength = 0.040;
    vec2 shockOffset = dir * ring * shockStrength;

    vec4 source = texture(
            DiffuseSampler,
            uv - shockOffset
    );

    vec3 hsv = rgbToHsv(source.rgb);

    float phase = fract(Time * 1.6);

    float paletteHue;
    if (phase < 0.25) {
        paletteHue = 0.13;
    } else if (phase < 0.50) {
        paletteHue = 0.82;
    } else if (phase < 0.75) {
        paletteHue = 0.23;
    } else {
        paletteHue = 0.67;
    }

    float luminance = dot(
            source.rgb,
            vec3(0.299, 0.587, 0.114)
    );

    vec3 recolored = hsvToRgb(
            vec3(
                    paletteHue,
                    0.55,
                    clamp(luminance * 1.15 + 0.08, 0.0, 1.0)
            )
    );

    vec3 result = mix(
            source.rgb,
            recolored,
            0.72 * mask
    );

    vec3 ringColor = vec3(1.0, 0.95, 0.75);
    result = mix(
            result,
            ringColor,
            ring * 0.35
    );

    float pulse = 1.0 + ring * 0.18;
    result *= pulse;

    result = pow(result, vec3(0.92));

    fragColor = vec4(result, source.a);
}
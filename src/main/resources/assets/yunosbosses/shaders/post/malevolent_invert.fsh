#version 150

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    // Chromatic cursed offset for red channel
    vec2 redOffset = vec2(0.003, 0.001);
    float r = texture(InSampler, texCoord + redOffset).r;
    float g = texture(InSampler, texCoord).g;
    float b = texture(InSampler, texCoord - redOffset).b;
    vec3 color = vec3(r, g, b);

    // Calculate luminance
    float lum = dot(color, vec3(0.299, 0.587, 0.114));

    // True anime negative inversion: bright becomes dark silhouette, dark becomes blinding white
    float invertedLum = 1.0 - lum;

    // Push extreme contrast curve (manga / anime high-contrast frame)
    float highContrast = smoothstep(0.15, 0.85, invertedLum);

    // Stark monochrome with blood-red edge accents
    vec3 negativeColor = vec3(highContrast);
    vec3 bloodRed = vec3(0.95, 0.05, 0.1);

    // Edges and extreme highlights get cursed red fringe
    float edgeDiff = abs(r - b);
    vec3 finalColor = mix(negativeColor, bloodRed, clamp(edgeDiff * 2.5, 0.0, 0.8));

    fragColor = vec4(finalColor, 1.0);
}
